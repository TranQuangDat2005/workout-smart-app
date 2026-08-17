package com.workoutsmart.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** Khóa tài khoản kèm lý do bắt buộc — FR-004 (006). */
public record BanUserRequest(
        @NotBlank(message = "Lý do khóa là bắt buộc")
        String reason) {
}
