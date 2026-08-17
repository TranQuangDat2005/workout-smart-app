package com.workoutsmart.plan.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class WorkoutPlanControllerIntegrationTest {

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
    private WorkoutPlanDayRepository dayRepository;
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
        dayRepository.deleteAll();
        planRepository.deleteAll();
        exerciseRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        draftRepository.deleteAll();
        planExerciseRepository.deleteAll();
        dayRepository.deleteAll();
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

    private void seedExercises() {
        exerciseRepository.save(Exercise.builder().name("Jumping Jacks").category("cardio")
                .bodyPart("full").equipment("body_weight").muscleGroup("core").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Push Up").category("strength")
                .bodyPart("chest").equipment("body_weight").muscleGroup("chest").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Squat").category("strength")
                .bodyPart("legs").equipment("body_weight").muscleGroup("legs").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Plank").category("strength")
                .bodyPart("core").equipment("body_weight").muscleGroup("core").status("active").build());
        exerciseRepository.save(Exercise.builder().name("Lunge").category("strength")
                .bodyPart("legs").equipment("body_weight").muscleGroup("legs").status("active").build());
    }

    @Test
    void setupGoalCreatesActivePlan() throws Exception {
        seedExercises();
        String token = login("wp1@example.com");

        mockMvc.perform(put("/api/v1/users/me/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"weight_loss\",\"fitnessLevel\":\"beginner\",\"equipment\":[\"body_weight\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalType").value("weight_loss"))
                .andExpect(jsonPath("$.planId").isNumber());
    }

    @Test
    void getActivePlanReturnsDaysAndExercises() throws Exception {
        seedExercises();
        String token = login("wp2@example.com");

        mockMvc.perform(put("/api/v1/users/me/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"weight_loss\",\"fitnessLevel\":\"beginner\",\"equipment\":[\"body_weight\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/workout-plans/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days[0].exercises").isArray());
    }

    @Test
    void getActivePlanWithoutPlanReturns404() throws Exception {
        String token = login("wp3@example.com");

        mockMvc.perform(get("/api/v1/workout-plans/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void setupGoalInvalidEnumReturns400() throws Exception {
        String token = login("wp4@example.com");

        mockMvc.perform(put("/api/v1/users/me/goals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goalType\":\"bad\",\"fitnessLevel\":\"beginner\",\"equipment\":[\"body_weight\"]}"))
                .andExpect(status().isBadRequest());
    }
}
