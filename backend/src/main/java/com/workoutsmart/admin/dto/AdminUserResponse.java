package com.workoutsmart.admin.dto;

/** Kết quả tìm kiếm user cho Admin — FR-001/002 (006). */
public record AdminUserResponse(
        Long id,
        String email,
        String displayName,
        String accountStatus,
        boolean emailVerified) {
}
