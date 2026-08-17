package com.workoutsmart.auth.service;

import com.workoutsmart.auth.dto.AuthResponse;
import com.workoutsmart.auth.dto.MessageResponse;
import com.workoutsmart.auth.dto.VerifyOtpResponse;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.OtpPurpose;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ đăng ký / xác thực / quên mật khẩu / khôi phục — spec 007-core-auth. */
@Service
public class AuthService {

    /** Rate limit login: tối đa 5 lần sai trong 15 phút (research R9). */
    private static final int MAX_FAILED_LOGINS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final Duration SOFT_DELETE_RESTORE_WINDOW = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final JwtAuthFilter jwtAuthFilter;

    /** Đếm lần đăng nhập sai theo email (in-memory — đủ cho MVP). */
    private final Map<String, FailedLogin> failedLogins = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       OtpService otpService,
                       TokenService tokenService,
                       JwtAuthFilter jwtAuthFilter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.tokenService = tokenService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /** FR-001..FR-003, FR-010: đăng ký; email soft-delete <30 ngày → gửi OTP restore. */
    @Transactional
    public MessageResponse register(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);
        var existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getAccountStatus() == AccountStatus.DELETED
                    && user.getDeletedAt() != null
                    && user.getDeletedAt().isAfter(Instant.now().minus(SOFT_DELETE_RESTORE_WINDOW))) {
                otpService.send(user.getId(), email, OtpPurpose.RESTORE);
                return new MessageResponse(
                        "Email này đang trong thời gian khôi phục. Mã OTP đã được gửi để khôi phục tài khoản.");
            }
            throw new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .build();
        userRepository.save(user);
        otpService.send(user.getId(), email, OtpPurpose.REGISTER);
        return new MessageResponse("Đăng ký thành công. Vui lòng kiểm tra email để nhận mã xác thực.");
    }

    /** FR-004, FR-010: verify OTP đăng ký; OTP restore → báo user đặt mật khẩu mới. */
    @Transactional
    public VerifyOtpResponse verifyOtp(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        User user = requireActiveAccount(email);

        OtpPurpose purpose = resolvePurpose(user);
        if (purpose == OtpPurpose.RESTORE) {
            // Luồng restore 2 bước: bước này chỉ xác nhận sở hữu email, chưa consume OTP
            otpService.check(user.getId(), purpose, code);
            return new VerifyOtpResponse(false, true,
                    "Xác thực thành công. Hãy đặt mật khẩu mới để hoàn tất khôi phục tài khoản.");
        }
        otpService.verify(user.getId(), purpose, code);
        user.setEmailVerified(true);
        userRepository.save(user);
        return new VerifyOtpResponse(true, false, "Xác thực email thành công.");
    }

    /** FR-010: đặt mật khẩu mới và kích hoạt lại tài khoản soft-delete. */
    @Transactional
    public MessageResponse restorePassword(String rawEmail, String code, String newPassword) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        if (user.getAccountStatus() != AccountStatus.DELETED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Tài khoản không ở trạng thái chờ khôi phục");
        }
        otpService.verify(user.getId(), OtpPurpose.RESTORE, code);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setDeletedAt(null);
        userRepository.save(user);
        jwtAuthFilter.invalidate(user.getId());
        return new MessageResponse("Tài khoản đã được khôi phục. Bạn có thể đăng nhập bằng mật khẩu mới.");
    }

    /** FR-005, FR-006: đăng nhập + rate limit 5 lần sai/15 phút. */
    @Transactional
    public AuthResponse login(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);
        checkLoginRateLimit(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (user.getAccountStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Vui lòng xác thực email trước khi đăng nhập");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailedLogin(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        failedLogins.remove(email);
        return tokenService.issue(user);
    }

    /** FR-008, FR-009: silent refresh với rotation. */
    public AuthResponse refresh(String refreshToken) {
        return tokenService.rotate(refreshToken);
    }

    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    /** FR-007: quên mật khẩu — luôn trả 200, chỉ gửi OTP khi email tồn tại và active. */
    public MessageResponse forgotPassword(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        userRepository.findByEmail(email)
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE && u.isEmailVerified())
                .ifPresent(u -> otpService.send(u.getId(), email, OtpPurpose.RESET_PASSWORD));
        return new MessageResponse("Nếu email tồn tại, mã đặt lại mật khẩu đã được gửi.");
    }

    /** FR-007: đặt lại mật khẩu bằng OTP + mật khẩu mới; revoke mọi phiên cũ. */
    @Transactional
    public MessageResponse resetPassword(String rawEmail, String code, String newPassword) {
        String email = normalizeEmail(rawEmail);
        User user = requireActiveAccount(email);
        otpService.verify(user.getId(), OtpPurpose.RESET_PASSWORD, code);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenService.revokeAll(user.getId());
        return new MessageResponse("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    /** Gửi lại OTP — dùng cho cả register/reset/restore theo trạng thái tài khoản. */
    public MessageResponse resendOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không có yêu cầu xác thực nào cho email này"));
        OtpPurpose purpose = resolvePurpose(user);
        otpService.send(user.getId(), email, purpose);
        return new MessageResponse("Mã OTP mới đã được gửi.");
    }

    /** Dành cho 006-admin-management: ban/unban user → revoke token + xóa cache. */
    @Transactional
    public void applyBan(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        user.setAccountStatus(AccountStatus.BANNED);
        userRepository.save(user);
        tokenService.revokeAll(userId);
        jwtAuthFilter.invalidate(userId);
    }

    @Transactional
    public void applyUnban(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        jwtAuthFilter.invalidate(userId);
    }

    private User requireActiveAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
        if (user.getAccountStatus() == AccountStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        return user;
    }

    private OtpPurpose resolvePurpose(User user) {
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            return OtpPurpose.RESTORE;
        }
        return user.isEmailVerified() ? OtpPurpose.RESET_PASSWORD : OtpPurpose.REGISTER;
    }

    private void checkLoginRateLimit(String email) {
        FailedLogin record = failedLogins.get(email);
        if (record != null && record.count >= MAX_FAILED_LOGINS
                && record.firstAttempt.isAfter(Instant.now().minus(LOGIN_WINDOW))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Quá số lần thử đăng nhập. Vui lòng thử lại sau 15 phút.");
        }
    }

    private void recordFailedLogin(String email) {
        failedLogins.compute(email, (k, v) -> {
            if (v == null || v.firstAttempt.isBefore(Instant.now().minus(LOGIN_WINDOW))) {
                return new FailedLogin(1, Instant.now());
            }
            return new FailedLogin(v.count + 1, v.firstAttempt);
        });
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private record FailedLogin(int count, Instant firstAttempt) {
    }
}
