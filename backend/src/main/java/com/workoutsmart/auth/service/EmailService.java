package com.workoutsmart.auth.service;

import com.workoutsmart.auth.entity.OtpPurpose;

/** Trừu tượng hóa gửi email OTP — research R4 (provider swap qua config). */
public interface EmailService {

    void sendOtp(String email, String code, OtpPurpose purpose);
}
