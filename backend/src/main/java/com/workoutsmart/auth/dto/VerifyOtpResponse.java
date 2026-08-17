package com.workoutsmart.auth.dto;

/** Phản hồi verify OTP — restoreRequired=true khi email thuộc tài khoản soft-delete. */
public record VerifyOtpResponse(boolean verified, boolean restoreRequired, String message) {
}
