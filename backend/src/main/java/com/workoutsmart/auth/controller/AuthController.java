package com.workoutsmart.auth.controller;

import com.workoutsmart.auth.dto.AuthResponse;
import com.workoutsmart.auth.dto.ForgotPasswordRequest;
import com.workoutsmart.auth.dto.LoginRequest;
import com.workoutsmart.auth.dto.MessageResponse;
import com.workoutsmart.auth.dto.RefreshRequest;
import com.workoutsmart.auth.dto.RegisterRequest;
import com.workoutsmart.auth.dto.ResendOtpRequest;
import com.workoutsmart.auth.dto.ResetPasswordRequest;
import com.workoutsmart.auth.dto.VerifyOtpRequest;
import com.workoutsmart.auth.dto.VerifyOtpResponse;
import com.workoutsmart.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controller mỏng — toàn bộ logic nằm ở AuthService (constitution §3). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.email(), request.password());
    }

    @PostMapping("/verify-otp")
    public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request.email(), request.code());
    }

    @PostMapping("/resend-otp")
    public MessageResponse resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return authService.resendOtp(request.email());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request.email(), request.code(), request.newPassword());
    }

    @PostMapping("/restore-password")
    public MessageResponse restorePassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.restorePassword(request.email(), request.code(), request.newPassword());
    }
}
