package com.workoutsmart.auth.service;

import com.workoutsmart.auth.entity.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Gửi OTP qua SMTP (Gmail App Password) — provider = smtp. */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${app.email.from-address}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendOtp(String email, String code, OtpPurpose purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject(purpose));
        message.setText(content(code));
        mailSender.send(message);
        log.info("OTP email sent to {}", email);
    }

    private String subject(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTER -> "Xác thực email — WorkoutSmartApp";
            case RESET_PASSWORD -> "Đặt lại mật khẩu — WorkoutSmartApp";
            case RESTORE -> "Khôi phục tài khoản — WorkoutSmartApp";
        };
    }

    private String content(String code) {
        return "Mã xác thực của bạn: " + code + "\n"
                + "Mã có hiệu lực trong 10 phút. KHÔNG chia sẻ mã này với bất kỳ ai.";
    }
}
