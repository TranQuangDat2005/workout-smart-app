package com.workoutsmart.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.OtpPurpose;
import com.workoutsmart.auth.entity.OtpVerification;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository repository;
    @Mock
    private EmailService emailService;

    private PasswordEncoder passwordEncoder;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        otpService = new OtpService(repository, passwordEncoder, emailService);
    }

    @Test
    void sendGeneratesSixDigitCodeAndSendsEmail() {
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());
        when(repository.countByUserIdAndPurposeAndCreatedAtAfter(eq(1L), eq(OtpPurpose.REGISTER), any()))
                .thenReturn(0L);
        doNothing().when(emailService).sendOtp(anyString(), anyString(), eq(OtpPurpose.REGISTER));

        otpService.send(1L, "user@example.com", OtpPurpose.REGISTER);

        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(repository).save(captor.capture());
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtp(eq("user@example.com"), codeCaptor.capture(), eq(OtpPurpose.REGISTER));

        assertTrue(codeCaptor.getValue().matches("^\\d{6}$"), "OTP phải là 6 chữ số");
        assertEquals(OtpPurpose.REGISTER, captor.getValue().getPurpose());
        assertTrue(captor.getValue().getExpiresAt().isAfter(Instant.now().plus(Duration.ofMinutes(9))),
                "OTP phải hết hạn sau ~10 phút");
    }

    @Test
    void sendRejectsWhenCooldownNotElapsed() {
        OtpVerification recent = OtpVerification.builder()
                .userId(1L)
                .purpose(OtpPurpose.REGISTER)
                .codeHash("x")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
                .createdAt(Instant.now().minusSeconds(30))
                .build();
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(recent));

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.send(1L, "user@example.com", OtpPurpose.REGISTER));
        assertEquals(429, ex.getStatus().value());
    }

    @Test
    void sendRejectsWhenHourlyCapReached() {
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());
        when(repository.countByUserIdAndPurposeAndCreatedAtAfter(eq(1L), eq(OtpPurpose.REGISTER), any()))
                .thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.send(1L, "user@example.com", OtpPurpose.REGISTER));
        assertEquals(429, ex.getStatus().value());
    }

    @Test
    void verifySucceedsWithCorrectCode() {
        String code = "123456";
        OtpVerification otp = OtpVerification.builder()
                .userId(1L)
                .purpose(OtpPurpose.REGISTER)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .attemptsCount(0)
                .build();
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(otp));

        otpService.verify(1L, OtpPurpose.REGISTER, code);
        verify(repository).save(otp);
        org.junit.jupiter.api.Assertions.assertNotNull(otp.getUsedAt());
    }

    @Test
    void verifyFailsWithWrongCodeAndIncrementsAttempts() {
        OtpVerification otp = OtpVerification.builder()
                .userId(1L)
                .purpose(OtpPurpose.REGISTER)
                .codeHash(passwordEncoder.encode("123456"))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .attemptsCount(0)
                .build();
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(otp));

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.verify(1L, OtpPurpose.REGISTER, "999999"));
        assertEquals(400, ex.getStatus().value());
        assertEquals(1, otp.getAttemptsCount());
    }

    @Test
    void verifyFailsWhenExpired() {
        OtpVerification otp = OtpVerification.builder()
                .userId(1L)
                .purpose(OtpPurpose.REGISTER)
                .codeHash("x")
                .expiresAt(Instant.now().minusSeconds(60))
                .attemptsCount(0)
                .build();
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(otp));

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.verify(1L, OtpPurpose.REGISTER, "123456"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void verifyFailsAfterMaxAttempts() {
        OtpVerification otp = OtpVerification.builder()
                .userId(1L)
                .purpose(OtpPurpose.REGISTER)
                .codeHash("x")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .attemptsCount(5)
                .build();
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(otp));

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.verify(1L, OtpPurpose.REGISTER, "123456"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void verifyNoPendingOtpThrows() {
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.verify(1L, OtpPurpose.REGISTER, "123456"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void verifyUsedOtpThrows() {
        OtpVerification otp = OtpVerification.builder()
                .userId(1L)
                .purpose(OtpPurpose.REGISTER)
                .codeHash("x")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .attemptsCount(0)
                .usedAt(Instant.now())
                .build();
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.of(otp));

        ApiException ex = assertThrows(ApiException.class,
                () -> otpService.verify(1L, OtpPurpose.REGISTER, "123456"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void emailServiceCalledOnceOnSend() {
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(1L, OtpPurpose.REGISTER))
                .thenReturn(Optional.empty());
        when(repository.countByUserIdAndPurposeAndCreatedAtAfter(eq(1L), eq(OtpPurpose.REGISTER), any()))
                .thenReturn(0L);

        otpService.send(1L, "user@example.com", OtpPurpose.REGISTER);
        verify(emailService, times(1)).sendOtp(anyString(), anyString(), any());
    }
}
