package com.workoutsmart.tracking.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseRepository;
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
class TrackingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkoutSessionRepository sessionRepository;
    @Autowired
    private WorkoutSetRepository setRepository;
    @Autowired
    private WorkoutSessionExerciseRepository sessionExerciseRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void clean() {
        setRepository.deleteAll();
        sessionExerciseRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    private String login(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
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

    private long startSession(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workout-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void startSessionAndRecordSet() throws Exception {
        String token = login("t1@example.com");
        long sessionId = startSession(token);

        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":1,\"repsCompleted\":10,\"weightUsed\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.repsCompleted").value(10));
    }

    @Test
    void startSecondActiveSessionReturns409() throws Exception {
        String token = login("t2@example.com");
        startSession(token);

        mockMvc.perform(post("/api/v1/workout-sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void focusInterruptionIncrements() throws Exception {
        String token = login("t3@example.com");
        long sessionId = startSession(token);

        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/focus-interruption")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.focusInterruptionsCount").value(1));
    }

    @Test
    void deleteSetRemovesRecordedSet() throws Exception {
        String token = login("t4@example.com");
        long sessionId = startSession(token);

        MvcResult created = mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/sets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":1,\"repsCompleted\":10}"))
                .andExpect(status().isOk())
                .andReturn();
        long setId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId + "/sets/" + setId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId + "/sets/" + setId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSetRejectsOtherUsersSet() throws Exception {
        String tokenA = login("t5a@example.com");
        String tokenB = login("t5b@example.com");
        long sessionA = startSession(tokenA);
        long sessionB = startSession(tokenB);

        MvcResult created = mockMvc.perform(post("/api/v1/workout-sessions/" + sessionA + "/sets")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setNumber\":1,\"repsCompleted\":8}"))
                .andExpect(status().isOk())
                .andReturn();
        long setId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        // User B cố xóa set thuộc session của A qua session B → 404
        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionB + "/sets/" + setId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
