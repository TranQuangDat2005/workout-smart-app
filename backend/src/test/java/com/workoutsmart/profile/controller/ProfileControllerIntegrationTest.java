package com.workoutsmart.profile.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.OtpPurpose;
import com.workoutsmart.auth.entity.OtpVerification;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OtpVerificationRepository otpRepository;
    @Autowired
    private WorkoutSessionRepository sessionRepository;
    @Autowired
    private WorkoutSetRepository setRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private com.workoutsmart.auth.repository.RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private com.workoutsmart.nutrition.repository.BodyMetricRepository bodyMetricRepository;
    @Autowired
    private com.workoutsmart.nutrition.repository.MealEntryRepository mealEntryRepository;
    @Autowired
    private com.workoutsmart.nutrition.repository.MealLogRepository mealLogRepository;
    @Autowired
    private com.workoutsmart.nutrition.repository.FoodItemRepository foodItemRepository;

    @BeforeEach
    void clean() {
        refreshTokenRepository.deleteAll();
        bodyMetricRepository.deleteAll();
        mealEntryRepository.deleteAll();
        mealLogRepository.deleteAll();
        foodItemRepository.deleteAll();
        setRepository.deleteAll();
        sessionRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createVerifiedUserAndLogin(String email) throws Exception {
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .displayName("Người Tập")
                .age(25)
                .heightCm(new java.math.BigDecimal("170.00"))
                .goalType("weight_loss")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
        MvcResult login = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = login.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void getProfileReturnsCurrentUser() throws Exception {
        String token = createVerifiedUserAndLogin("p1@example.com");

        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("p1@example.com"))
                .andExpect(jsonPath("$.displayName").value("Người Tập"))
                .andExpect(jsonPath("$.goalType").value("weight_loss"));
    }

    @Test
    void updateProfileHappyPath() throws Exception {
        String token = createVerifiedUserAndLogin("p2@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Tên Mới\",\"age\":30,\"heightCm\":175.5,\"goalType\":\"muscle_gain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalChanged").value(true))
                .andExpect(jsonPath("$.profile.goalType").value("muscle_gain"));
    }

    @Test
    void updateProfileInvalidAgeReturns400() throws Exception {
        String token = createVerifiedUserAndLogin("p3@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\":150}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfileInvalidGoalReturns400() throws Exception {
        String token = createVerifiedUserAndLogin("p4@example.com");

        mockMvc.perform(put("/api/v1/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"super_strong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccountSoftDeletesAndBlocksApi() throws Exception {
        String token = createVerifiedUserAndLogin("p5@example.com");

        mockMvc.perform(delete("/api/v1/account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        User deleted = userRepository.findByEmail("p5@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(AccountStatus.DELETED, deleted.getAccountStatus());

        // Request tiếp theo bị chặn 403 (middleware ban-check)
        mockMvc.perform(get("/api/v1/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSessionsReturnsHistoryWithDetail() throws Exception {
        String token = createVerifiedUserAndLogin("p6@example.com");
        User user = userRepository.findByEmail("p6@example.com").orElseThrow();

        WorkoutSession session = sessionRepository.save(WorkoutSession.builder()
                .userId(user.getId())
                .status("completed")
                .startTime(Instant.now().minus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build());
        setRepository.save(WorkoutSet.builder()
                .sessionId(session.getId()).setNumber(1)
                .repsCompleted(10).weightUsed(new java.math.BigDecimal("50.00")).build());

        mockMvc.perform(get("/api/v1/workout-sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].totalSets").value(1))
                .andExpect(jsonPath("$.content[0].totalVolumeKg").value(500.0));

        mockMvc.perform(get("/api/v1/workout-sessions/" + session.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets[0].repsCompleted").value(10));
    }

    @Test
    void getSessionDetailUnknownReturns404() throws Exception {
        String token = createVerifiedUserAndLogin("p7@example.com");

        mockMvc.perform(get("/api/v1/workout-sessions/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearHistoryDeletesNonActiveAndKeepsActive() throws Exception {
        String token = createVerifiedUserAndLogin("p8@example.com");
        User user = userRepository.findByEmail("p8@example.com").orElseThrow();

        WorkoutSession done = sessionRepository.save(WorkoutSession.builder()
                .userId(user.getId()).status("completed").startTime(Instant.now().minus(2, ChronoUnit.DAYS)).build());
        setRepository.save(WorkoutSet.builder()
                .sessionId(done.getId()).setNumber(1).repsCompleted(10).weightUsed(new java.math.BigDecimal("50.00")).build());
        WorkoutSession active = sessionRepository.save(WorkoutSession.builder()
                .userId(user.getId()).status("active").startTime(Instant.now()).build());
        setRepository.save(WorkoutSet.builder()
                .sessionId(active.getId()).setNumber(1).repsCompleted(5).weightUsed(new java.math.BigDecimal("20.00")).build());

        mockMvc.perform(delete("/api/v1/workout-sessions/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã xóa 1 buổi tập khỏi lịch sử"));

        // Buổi active vẫn còn, set của nó vẫn còn.
        mockMvc.perform(get("/api/v1/workout-sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(active.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(1, setRepository.findBySessionIdOrderBySetNumberAsc(active.getId()).size());
    }

    @Test
    void clearHistoryEmptyReturnsMessage() throws Exception {
        String token = createVerifiedUserAndLogin("p9@example.com");

        mockMvc.perform(delete("/api/v1/workout-sessions/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Không có lịch sử để xóa"));
    }

    @Test
    void profileRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isForbidden());
    }
}
