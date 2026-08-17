package com.workoutsmart.auth.service;

import com.workoutsmart.auth.entity.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Implementation dev: ghi OTP ra log thay vì gửi email thật. */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LogEmailService.class);

    @Override
    public void sendOtp(String email, String code, OtpPurpose purpose) {
        log.info("[DEV-OTP] email={} purpose={} code={}", email, purpose, code);
    }
}
