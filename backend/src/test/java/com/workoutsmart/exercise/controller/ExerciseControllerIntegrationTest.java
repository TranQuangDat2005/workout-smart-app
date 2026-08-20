package com.workoutsmart.exercise.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class ExerciseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private WorkoutPlanRepository planRepository;
    @Autowired
    private WorkoutPlanDayRepository planDayRepository;
    @Autowired
    private WorkoutPlanExerciseRepository planExerciseRepository;
    @Autowired
    private DraftExerciseRepository draftRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        draftRepository.deleteAll();
        planExerciseRepository.deleteAll();
        planDayRepository.deleteAll();
        planRepository.deleteAll();
        exerciseRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        draftRepository.deleteAll();
        planExerciseRepository.deleteAll();
        planDayRepository.deleteAll();
        planRepository.deleteAll();
        exerciseRepository.deleteAll();
    }

    private String login(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .displayName("Người Tập")
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
    void searchFiltersByEquipment() throws Exception {
        exerciseRepository.save(Exercise.builder().name("Push Up").category("strength")
                .bodyPart("chest").equipment("body_weight").muscleGroup("chest").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Bench Press").category("strength")
                .bodyPart("chest").equipment("barbell").muscleGroup("chest").status("active").build());
        String token = login("ex1@example.com");

        mockMvc.perform(get("/api/v1/exercises")
                        .header("Authorization", "Bearer " + token)
                        .param("equipment", "body_weight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Push Up"));
    }

    @Test
    void searchCombinesCategoryOrAndEquipmentAnd() throws Exception {
        exerciseRepository.save(Exercise.builder().name("DB Fly").category("chest")
                .bodyPart("chest").equipment("dumbbell").muscleGroup("chest").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Barbell Row").category("back")
                .bodyPart("back").equipment("barbell").muscleGroup("back").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Chest Press").category("chest")
                .bodyPart("chest").equipment("barbell").muscleGroup("chest").status("active").build());
        String token = login("ex-filter@example.com");

        mockMvc.perform(get("/api/v1/exercises")
                        .header("Authorization", "Bearer " + token)
                        .param("category", "chest")
                        .param("category", "back")
                        .param("equipment", "dumbbell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("DB Fly"));
    }

    @Test
    void detailReturnsExercise() throws Exception {
        Exercise saved = exerciseRepository.save(Exercise.builder().name("Plank").category("strength")
                .bodyPart("core").equipment("body_weight").muscleGroup("core").status("active").build());
        String token = login("ex2@example.com");

        mockMvc.perform(get("/api/v1/exercises/" + saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plank"));
    }

    @Test
    void detailUnknownReturns404() throws Exception {
        String token = login("ex3@example.com");

        mockMvc.perform(get("/api/v1/exercises/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void exerciseEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/exercises"))
                .andExpect(status().isForbidden());
    }
}
