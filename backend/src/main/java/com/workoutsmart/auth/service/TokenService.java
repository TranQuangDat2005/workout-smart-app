package com.workoutsmart.auth.service;

import com.workoutsmart.auth.dto.AuthResponse;
import com.workoutsmart.auth.entity.RefreshToken;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cấp/rotation/revoke refresh token theo research R2. */
@Service
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final long refreshExpiryDays;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtProvider jwtProvider,
                        RefreshTokenRepository repository,
                        UserRepository userRepository,
                        @Value("${app.jwt.refresh-expiry-days}") long refreshExpiryDays) {
        this.jwtProvider = jwtProvider;
        this.repository = repository;
        this.userRepository = userRepository;
        this.refreshExpiryDays = refreshExpiryDays;
    }

    /** Cấp cặp token mới cho lần đăng nhập. */
    @Transactional
    public AuthResponse issue(User user) {
        String refreshToken = newRefreshToken(user.getId());
        return new AuthResponse(
                jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole()),
                refreshToken,
                jwtProvider.getAccessExpirySeconds());
    }

    /** Rotation: verify refresh cũ → revoke → cấp cặp mới. */
    @Transactional
    public AuthResponse rotate(String rawRefreshToken) {
        String hash = sha256Hex(rawRefreshToken);
        RefreshToken stored = repository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ"));
        if (stored.getRevokedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã bị thu hồi");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn");
        }
        stored.setRevokedAt(Instant.now());
        repository.save(stored);

        User user = findUser(stored.getUserId());
        String newRefreshToken = newRefreshToken(user.getId());
        return new AuthResponse(
                jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole()),
                newRefreshToken,
                jwtProvider.getAccessExpirySeconds());
    }

    /** Logout: revoke refresh token hiện tại. */
    @Transactional
    public void revoke(String rawRefreshToken) {
        repository.findByTokenHash(sha256Hex(rawRefreshToken))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> {
                    t.setRevokedAt(Instant.now());
                    repository.save(t);
                });
    }

    /** Ban user (FR-012): revoke toàn bộ refresh token. */
    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }

    private String newRefreshToken(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .expiresAt(Instant.now().plus(Duration.ofDays(refreshExpiryDays)))
                .build());
        return raw;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại"));
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
