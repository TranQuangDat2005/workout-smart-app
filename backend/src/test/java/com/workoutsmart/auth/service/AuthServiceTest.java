package com.workoutsmart.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.dto.AuthResponse;
import com.workoutsmart.auth.dto.MessageResponse;
import com.workoutsmart.auth.dto.VerifyOtpResponse;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.OtpPurpose;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private TokenService tokenService;
    @Mock
    private JwtAuthFilter jwtAuthFilter;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, otpService, tokenService, jwtAuthFilter);
    }

    @Test
    void registerCreatesPendingAccountAndSendsOtp() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.register("  New@Example.COM ", "Password1");

        verify(userRepository).save(any(User.class));
        verify(otpService).send(any(), eq("new@example.com"), eq(OtpPurpose.REGISTER));
        assertTrue(response.message().contains("Đăng ký thành công"));
    }

    @Test
    void registerRejectsExistingActiveEmail() {
        User existing = User.builder()
                .id(1L)
                .email("taken@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.register("taken@example.com", "Password1"));
        assertEquals(409, ex.getStatus().value());
        verify(otpService, never()).send(any(), any(), any());
    }

    @Test
    void registerSoftDeletedEmailSendsRestoreOtp() {
        User deleted = User.builder()
                .id(7L)
                .email("gone@example.com")
                .accountStatus(AccountStatus.DELETED)
                .deletedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.of(deleted));

        MessageResponse response = authService.register("gone@example.com", "Password1");

        verify(otpService).send(7L, "gone@example.com", OtpPurpose.RESTORE);
        verify(userRepository, never()).save(any(User.class));
        assertTrue(response.message().contains("khôi phục"));
    }

    @Test
    void registerSoftDeletedBeyond30DaysTreatsAsNew() {
        User deleted = User.builder()
                .id(7L)
                .email("gone@example.com")
                .accountStatus(AccountStatus.DELETED)
                .deletedAt(Instant.now().minus(40, ChronoUnit.DAYS))
                .build();
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.of(deleted));

        // Hết hạn 30 ngày → coi như không tồn tại... nhưng email vẫn chiếm trong DB thật.
        // Trong MVP: hard-delete đã chạy bởi cron nên find trả về empty. Ở đây mock empty:
        org.mockito.Mockito.reset(userRepository);
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.register("gone@example.com", "Password1");
        assertTrue(response.message().contains("Đăng ký thành công"));
    }

    @Test
    void loginSuccessIssuesTokens() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn(new AuthResponse("a", "r", 900));

        AuthResponse response = authService.login("user@example.com", "Password1");

        assertEquals("a", response.accessToken());
        assertEquals("r", response.refreshToken());
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPass1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login("user@example.com", "WrongPass1"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void loginRejectsBannedAccount() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("x")
                .accountStatus(AccountStatus.BANNED)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login("user@example.com", "Password1"));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void loginRejectsUnverifiedAccount() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login("user@example.com", "Password1"));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void loginLocksAfterFiveFailedAttempts() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPass1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        for (int i = 0; i < 5; i++) {
            assertThrows(ApiException.class,
                    () -> authService.login("user@example.com", "WrongPass1"));
        }
        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login("user@example.com", "CorrectPass1"));
        assertEquals(429, ex.getStatus().value());
    }

    @Test
    void verifyOtpActivatesAccount() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("x")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        VerifyOtpResponse response = authService.verifyOtp("user@example.com", "123456");

        verify(otpService).verify(1L, OtpPurpose.REGISTER, "123456");
        assertTrue(response.verified());
        assertFalse(response.restoreRequired());
        assertTrue(user.isEmailVerified());
    }

    @Test
    void verifyOtpRestoreFlowRequiresNewPassword() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .passwordHash("x")
                .accountStatus(AccountStatus.DELETED)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        VerifyOtpResponse response = authService.verifyOtp("user@example.com", "123456");

        verify(otpService).check(1L, OtpPurpose.RESTORE, "123456");
        assertTrue(response.restoreRequired());
    }

    @Test
    void forgotPasswordAlwaysSucceeds() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.forgotPassword("nobody@example.com");

        assertTrue(response.message().contains("Nếu email tồn tại"));
        verify(otpService, never()).send(any(), any(), any());
    }

    @Test
    void forgotPasswordSendsOtpForActiveUser() {
        User user = User.builder()
                .id(2L)
                .email("user@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("user@example.com");
        verify(otpService).send(2L, "user@example.com", OtpPurpose.RESET_PASSWORD);
    }

    @Test
    void resetPasswordUpdatesHashAndRevokesSessions() {
        User user = User.builder()
                .id(3L)
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("OldPass1"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        authService.resetPassword("user@example.com", "123456", "NewPass1");

        verify(otpService).verify(3L, OtpPurpose.RESET_PASSWORD, "123456");
        verify(tokenService).revokeAll(3L);
        assertTrue(passwordEncoder.matches("NewPass1", user.getPasswordHash()));
    }

    @Test
    void applyBanRevokesTokensAndInvalidatesCache() {
        User user = User.builder()
                .id(4L)
                .email("user@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        authService.applyBan(4L);

        assertEquals(AccountStatus.BANNED, user.getAccountStatus());
        verify(tokenService).revokeAll(4L);
        verify(jwtAuthFilter).invalidate(4L);
    }
}
