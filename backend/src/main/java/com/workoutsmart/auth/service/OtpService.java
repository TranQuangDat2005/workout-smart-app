package com.workoutsmart.auth.service;

import com.workoutsmart.auth.entity.OtpPurpose;
import com.workoutsmart.auth.entity.OtpVerification;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sinh/verify OTP 6 số theo research R3. */
@Service
public class OtpService {

    public static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_SENDS_PER_HOUR = 3;

    private final OtpVerificationRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpVerificationRepository repository,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Sinh OTP mới, lưu hash và gửi qua email. Enforce cooldown + cap. */
    @Transactional
    public void send(Long userId, String email, OtpPurpose purpose) {
        Instant now = Instant.now();

        var latest = repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose);
        if (latest.isPresent()) {
            Instant lastSent = latest.get().getCreatedAt();
            if (lastSent.isAfter(now.minus(RESEND_COOLDOWN))) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ 60 giây trước khi gửi lại mã");
            }
        }
        long sentThisHour = repository.countByUserIdAndPurposeAndCreatedAtAfter(
                userId, purpose, now.minus(Duration.ofHours(1)));
        if (sentThisHour >= MAX_SENDS_PER_HOUR) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Đã gửi quá nhiều mã, vui lòng thử lại sau 1 giờ");
        }

        String code = generateCode();
        repository.save(OtpVerification.builder()
                .userId(userId)
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(now.plus(TTL))
                .attemptsCount(0)
                .build());
        emailService.sendOtp(email, code, purpose);
    }

    /** Verify OTP mới nhất theo purpose. Thất bại → tăng attempts. Thành công → consume. */
    @Transactional
    public void verify(Long userId, OtpPurpose purpose, String rawCode) {
        OtpVerification otp = repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ"));
        if (otp.getUsedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã OTP đã được sử dụng");
        }
        check(userId, purpose, rawCode);
        otp.setUsedAt(Instant.now());
        repository.save(otp);
    }

    /**
     * Kiểm tra OTP hợp lệ nhưng KHÔNG consume — dùng cho luồng restore 2 bước
     * (verify-otp xác nhận sở hữu email, restore-password mới consume).
     */
    @Transactional
    public void check(Long userId, OtpPurpose purpose, String rawCode) {
        OtpVerification otp = repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Mã OTP không hợp lệ"));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
        }
        if (otp.getAttemptsCount() >= OtpVerification.MAX_ATTEMPTS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quá số lần thử, vui lòng gửi lại mã mới");
        }
        if (!passwordEncoder.matches(rawCode, otp.getCodeHash())) {
            otp.setAttemptsCount(otp.getAttemptsCount() + 1);
            repository.save(otp);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã OTP không đúng");
        }
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
