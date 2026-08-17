package com.workoutsmart.admin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.admin.repository.AuditLogRepository;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.repository.DraftExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import org.junit.jupiter.api.AfterEach;
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
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private DraftExerciseRepository draftRepository;
    @Autowired
    private WorkoutPlanDayRepository planDayRepository;
    @Autowired
    private WorkoutPlanExerciseRepository planExerciseRepository;
    @Autowired
    private WorkoutPlanRepository planRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void clean() {
        auditLogRepository.deleteAll();
        draftRepository.deleteAll();
        planExerciseRepository.deleteAll();
        planDayRepository.deleteAll();
        planRepository.deleteAll();
        exerciseRepository.deleteAll();
    }

    private String login(String email, String role) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void hideExerciseAsAdminWritesAudit() throws Exception {
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .name("Push Up").equipment("body_weight").status("active").build());
        String token = login("admin1@example.com", "admin");

        mockMvc.perform(patch("/api/v1/admin/exercises/" + exercise.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"inactive\",\"reason\":\"test\"}"))
                .andExpect(status().isNoContent());

        assertEquals("inactive", exerciseRepository.findById(exercise.getId()).orElseThrow().getStatus());
        assertEquals(1, auditLogRepository.count());
    }

    @Test
    void nonAdminCannotHideExercise() throws Exception {
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .name("Squat").equipment("body_weight").status("active").build());
        String token = login("user1@example.com", "user");

        mockMvc.perform(patch("/api/v1/admin/exercises/" + exercise.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"inactive\",\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidStatusReturns400() throws Exception {
        Exercise exercise = exerciseRepository.save(Exercise.builder()
                .name("Plank").equipment("body_weight").status("active").build());
        String token = login("admin2@example.com", "admin");

        mockMvc.perform(patch("/api/v1/admin/exercises/" + exercise.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"bad\"}"))
                .andExpect(status().isBadRequest());
    }
}
