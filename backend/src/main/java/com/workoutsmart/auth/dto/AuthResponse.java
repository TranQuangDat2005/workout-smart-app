package com.workoutsmart.auth.dto;

/** Phản hồi đăng nhập/refresh: cặp token theo research R2. */
public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds) {
}
