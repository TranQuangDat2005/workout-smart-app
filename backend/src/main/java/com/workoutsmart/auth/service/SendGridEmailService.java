package com.workoutsmart.auth.service;

import com.workoutsmart.auth.entity.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Gửi OTP qua SendGrid API (allowlist AGENTS.md). */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "sendgrid")
public class SendGridEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    private final String apiKey;
    private final String fromAddress;
    private final RestClient restClient;

    public SendGridEmailService(
            @Value("${app.email.sendgrid-api-key}") String apiKey,
            @Value("${app.email.from-address}") String fromAddress) {
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.restClient = RestClient.create("https://api.sendgrid.com/v3");
    }

    @Override
    public void sendOtp(String email, String code, OtpPurpose purpose) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("SENDGRID_API_KEY chưa cấu hình");
        }
        Map<String, Object> body = Map.of(
                "personalizations", new Object[]{Map.of(
                        "to", new Object[]{Map.of("email", email)},
                        "subject", subject(purpose))},
                "from", Map.of("email", fromAddress),
                "content", new Object[]{Map.of("type", "text/plain", "value", content(code))});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        restClient.post()
                .uri("/mail/send")
                .headers(h -> h.addAll(headers))
                .body(body)
                .retrieve()
                .toBodilessEntity();
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
