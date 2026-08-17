package com.workoutsmart.auth.entity;

/** Mục đích của mã OTP — khớp bảng otp_verifications.purpose. */
public enum OtpPurpose {
    REGISTER,
    RESET_PASSWORD,
    RESTORE
}
