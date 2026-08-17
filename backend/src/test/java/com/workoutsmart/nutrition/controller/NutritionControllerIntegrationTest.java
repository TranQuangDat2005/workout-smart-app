package com.workoutsmart.nutrition.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.nutrition.entity.FoodItem;
import com.workoutsmart.nutrition.repository.BodyMetricRepository;
import com.workoutsmart.nutrition.repository.FoodItemRepository;
import com.workoutsmart.nutrition.repository.MealEntryRepository;
import com.workoutsmart.nutrition.repository.MealLogRepository;
import java.math.BigDecimal;
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
class NutritionControllerIntegrationTest {

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
    private FoodItemRepository foodRepository;
    @Autowired
    private MealLogRepository mealLogRepository;
    @Autowired
    private MealEntryRepository mealEntryRepository;
    @Autowired
    private BodyMetricRepository bodyMetricRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        refreshTokenRepository.deleteAll();
        bodyMetricRepository.deleteAll();
        mealEntryRepository.deleteAll();
        mealLogRepository.deleteAll();
        foodRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String loginAndToken(String email) throws Exception {
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .sex("male").age(35).heightCm(new BigDecimal("170")).weightKg(new BigDecimal("68"))
                .activityLevel("moderate").goalType("endurance")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void foodCrudFlow() throws Exception {
        String token = loginAndToken("n1@example.com");

        // Tạo custom food
        MvcResult create = mockMvc.perform(post("/api/v1/foods")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cơm tấm sườn\",\"caloriesPer100g\":200,"
                                + "\"proteinPer100g\":8,\"carbPer100g\":25,\"fatPer100g\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("user_custom"))
                .andReturn();
        long foodId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

        // Tìm thấy
        mockMvc.perform(get("/api/v1/foods?query=cơm tấm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Sửa
        mockMvc.perform(put("/api/v1/foods/" + foodId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cơm tấm sườn bì\",\"caloriesPer100g\":220,"
                                + "\"proteinPer100g\":9,\"carbPer100g\":26,\"fatPer100g\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cơm tấm sườn bì"));

        // Xóa → không tìm thấy
        mockMvc.perform(delete("/api/v1/foods/" + foodId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/foods?query=cơm tấm")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void mealCreateAndSummaryFlow() throws Exception {
        String token = loginAndToken("n2@example.com");
        Long userId = userRepository.findByEmail("n2@example.com").orElseThrow().getId();

        FoodItem rice = foodRepository.save(FoodItem.builder()
                .name("Cơm trắng").source("system")
                .caloriesPer100g(new BigDecimal("130"))
                .proteinPer100g(new BigDecimal("2.7"))
                .carbPer100g(new BigDecimal("28"))
                .fatPer100g(new BigDecimal("0.3"))
                .build());
        FoodItem chicken = foodRepository.save(FoodItem.builder()
                .name("Ức gà").source("system")
                .caloriesPer100g(new BigDecimal("165"))
                .proteinPer100g(new BigDecimal("31"))
                .carbPer100g(new BigDecimal("0"))
                .fatPer100g(new BigDecimal("3.6"))
                .build());

        // Bữa 1: 200g cơm + 150g gà = 260 + 247.5 = 507.5 calo
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealNumber\":1,\"logDate\":\"2026-08-17\",\"entries\":["
                                + "{\"foodItemId\":" + rice.getId() + ",\"portionGrams\":200},"
                                + "{\"foodItemId\":" + chicken.getId() + ",\"portionGrams\":150}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.totalCalories").value(507.5));

        // Bữa 1 trùng → 409
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealNumber\":1,\"logDate\":\"2026-08-17\",\"entries\":["
                                + "{\"foodItemId\":" + rice.getId() + ",\"portionGrams\":100}]}"))
                .andExpect(status().isConflict());

        // Summary: TDEE 2437, nạp 507.5 → thiếu
        mockMvc.perform(get("/api/v1/nutrition/summary?date=2026-08-17")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCalories").value(2437))
                .andExpect(jsonPath("$.status").value("thiếu"))
                .andExpect(jsonPath("$.totalCalories").value(507.5));
    }

    @Test
    void bodyMetricsSyncWeightToUser() throws Exception {
        String token = loginAndToken("n3@example.com");

        mockMvc.perform(post("/api/v1/body-metrics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":74.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg").value(74.5));

        User user = userRepository.findByEmail("n3@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0,
                new BigDecimal("74.50").compareTo(user.getWeightKg()));

        // Lần 2 → delta −0.5
        mockMvc.perform(post("/api/v1/body-metrics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":74.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deltaWeightKg").value(-0.5));

        mockMvc.perform(get("/api/v1/body-metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void summaryWithoutBodyDataReturns422() throws Exception {
        userRepository.save(User.builder()
                .email("n4@example.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"n4@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/nutrition/summary?date=2026-08-17")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void nutritionEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/foods"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/meals"))
                .andExpect(status().isForbidden());
    }
}
