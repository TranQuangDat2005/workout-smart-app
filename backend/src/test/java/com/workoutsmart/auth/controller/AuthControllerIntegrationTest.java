package com.workoutsmart.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.OtpPurpose;
import com.workoutsmart.auth.entity.OtpVerification;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.service.LogEmailService;
import com.workoutsmart.auth.service.TokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OtpVerificationRepository otpRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- US1: Đăng ký + verify ---

    @Test
    void registerAndVerifyHappyPath() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Lấy OTP từ DB (LogEmailService không expose — đọc hash và giả lập code qua việc dùng code gửi lên)
        // Vì OTP lưu dạng bcrypt hash nên trong IT ta inject code trực tiếp: tạo OTP riêng với code biết trước.
        User user = userRepository.findByEmail("new@example.com").orElseThrow();
        String knownCode = "246810";
        otpRepository.deleteAll();
        otpRepository.save(OtpVerification.builder()
                .userId(user.getId())
                .purpose(OtpPurpose.REGISTER)
                .codeHash(passwordEncoder.encode(knownCode))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attemptsCount(0)
                .build());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"code\":\"" + knownCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        User verified = userRepository.findByEmail("new@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(verified.isEmailVerified());
    }

    @Test
    void registerDuplicateEmailReturns409() throws Exception {
        userRepository.save(User.builder()
                .email("taken@example.com")
                .passwordHash("x")
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email đã tồn tại"));
    }

    @Test
    void registerInvalidPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak@example.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void verifyOtpWithWrongCodeReturns400() throws Exception {
        User user = userRepository.save(User.builder()
                .email("pending@example.com")
                .passwordHash("x")
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .build());
        otpRepository.save(OtpVerification.builder()
                .userId(user.getId())
                .purpose(OtpPurpose.REGISTER)
                .codeHash(passwordEncoder.encode("111111"))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attemptsCount(0)
                .build());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pending@example.com\",\"code\":\"999999\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- US2: Login + refresh + logout + ban ---

    @Test
    void loginHappyPathAndRefreshRotation() throws Exception {
        userRepository.save(User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(body).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Token cũ đã bị revoke → refresh lần nữa phải 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWrongPasswordReturns401() throws Exception {
        userRepository.save(User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không đúng"));
    }

    @Test
    void loginBannedAccountReturns403() throws Exception {
        userRepository.save(User.builder()
                .email("banned@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .accountStatus(AccountStatus.BANNED)
                .emailVerified(true)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"banned@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointRejectsBannedUserEvenWithValidToken() throws Exception {
        // User active → login lấy access token
        User user = userRepository.save(User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Ban user (như Admin làm ở 006) + xóa cache
        user.setAccountStatus(AccountStatus.BANNED);
        userRepository.save(user);

        // Request protected với token còn hạn → 403 (FR-013)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/health")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    // --- US3: Quên mật khẩu ---

    @Test
    void forgotPasswordReturns200EvenForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPasswordHappyPath() throws Exception {
        userRepository.save(User.builder()
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode("OldPass1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
        User user = userRepository.findByEmail("user@example.com").orElseThrow();
        String knownCode = "654321";
        otpRepository.save(OtpVerification.builder()
                .userId(user.getId())
                .purpose(OtpPurpose.RESET_PASSWORD)
                .codeHash(passwordEncoder.encode(knownCode))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attemptsCount(0)
                .build());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"code\":\"" + knownCode
                                + "\",\"newPassword\":\"NewPass1\"}"))
                .andExpect(status().isOk());

        // Đăng nhập bằng mật khẩu mới
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"NewPass1\"}"))
                .andExpect(status().isOk());
    }

    // --- US1b: Restore soft-delete ---

    @Test
    void restoreSoftDeletedAccountFlow() throws Exception {
        User deleted = userRepository.save(User.builder()
                .email("gone@example.com")
                .passwordHash(passwordEncoder.encode("OldPass1"))
                .role("user")
                .accountStatus(AccountStatus.DELETED)
                .emailVerified(true)
                .deletedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build());

        // Đăng ký lại email soft-delete → nhận thông báo khôi phục (không tạo account mới)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gone@example.com\",\"password\":\"Whatever1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("khôi phục")));

        // Inject OTP restore + verify
        String knownCode = "112233";
        otpRepository.save(OtpVerification.builder()
                .userId(deleted.getId())
                .purpose(OtpPurpose.RESTORE)
                .codeHash(passwordEncoder.encode(knownCode))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attemptsCount(0)
                .build());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gone@example.com\",\"code\":\"" + knownCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restoreRequired").value(true));

        // Đặt mật khẩu mới → account active lại
        mockMvc.perform(post("/api/v1/auth/restore-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gone@example.com\",\"code\":\"" + knownCode
                                + "\",\"newPassword\":\"FreshPass1\"}"))
                .andExpect(status().isOk());

        User restored = userRepository.findByEmail("gone@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(AccountStatus.ACTIVE, restored.getAccountStatus());
        org.junit.jupiter.api.Assertions.assertNull(restored.getDeletedAt());
    }
}
