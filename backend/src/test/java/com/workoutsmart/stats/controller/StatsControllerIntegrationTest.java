package com.workoutsmart.stats.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.OtpVerificationRepository;
import com.workoutsmart.auth.repository.RefreshTokenRepository;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.nutrition.entity.BodyMetric;
import com.workoutsmart.nutrition.entity.FoodItem;
import com.workoutsmart.nutrition.entity.MealEntry;
import com.workoutsmart.nutrition.entity.MealLog;
import com.workoutsmart.nutrition.repository.BodyMetricRepository;
import com.workoutsmart.nutrition.repository.FoodItemRepository;
import com.workoutsmart.nutrition.repository.MealDailySummaryRepository;
import com.workoutsmart.nutrition.repository.MealEntryRepository;
import com.workoutsmart.nutrition.repository.MealLogRepository;
import com.workoutsmart.plan.entity.WorkoutPlan;
import com.workoutsmart.plan.entity.WorkoutPlanDay;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import com.workoutsmart.social.repository.LeaderboardRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
class StatsControllerIntegrationTest {

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
    private WorkoutSessionRepository sessionRepository;
    @Autowired
    private WorkoutSetRepository setRepository;
    @Autowired
    private BodyMetricRepository bodyMetricRepository;
    @Autowired
    private FoodItemRepository foodItemRepository;
    @Autowired
    private MealLogRepository mealLogRepository;
    @Autowired
    private MealEntryRepository mealEntryRepository;
    @Autowired
    private MealDailySummaryRepository summaryRepository;
    @Autowired
    private WorkoutPlanRepository planRepository;
    @Autowired
    private WorkoutPlanDayRepository planDayRepository;
    @Autowired
    private WorkoutPlanExerciseRepository planExerciseRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private ActivityFeedRepository feedRepository;
    @Autowired
    private LeaderboardRepository leaderboardRepository;
    @Autowired
    private ChallengeRepository challengeRepository;
    @Autowired
    private ChallengeParticipantRepository participantRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        refreshTokenRepository.deleteAll();
        participantRepository.deleteAll();
        challengeRepository.deleteAll();
        leaderboardRepository.deleteAll();
        feedRepository.deleteAll();
        friendshipRepository.deleteAll();
        mealEntryRepository.deleteAll();
        mealLogRepository.deleteAll();
        summaryRepository.deleteAll();
        foodItemRepository.deleteAll();
        bodyMetricRepository.deleteAll();
        setRepository.deleteAll();
        sessionRepository.deleteAll();
        planExerciseRepository.deleteAll();
        planDayRepository.deleteAll();
        planRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Long createUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Password1"))
                .role("user")
                .displayName("Tester")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build()).getId();
    }

    private String login(String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void dashboardReturnsAllChartsWithData() throws Exception {
        Long userId = createUser("a@example.com");
        String token = login("a@example.com");

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        // Trưa hôm nay → không lệch ngày dù test chạy gần nửa đêm
        var noon = today.atTime(LocalTime.NOON).atZone(zone);

        // 3 buổi completed hôm nay (streak tuần này = 1); 1 buổi gắn plan
        WorkoutPlan plan = planRepository.save(WorkoutPlan.builder()
                .userId(userId).name("Plan A").goalType("muscle_gain").status("active").build());
        planDayRepository.save(WorkoutPlanDay.builder().planId(plan.getId()).dayOfWeek(1).build());
        planDayRepository.save(WorkoutPlanDay.builder().planId(plan.getId()).dayOfWeek(3).build());
        planDayRepository.save(WorkoutPlanDay.builder().planId(plan.getId()).dayOfWeek(5).build());

        WorkoutSession s1 = sessionRepository.save(WorkoutSession.builder()
                .userId(userId).planId(plan.getId()).status("completed")
                .startTime(noon.toInstant()).endTime(noon.plus(30, ChronoUnit.MINUTES).toInstant()).build());
        sessionRepository.save(WorkoutSession.builder()
                .userId(userId).status("completed")
                .startTime(noon.plus(1, ChronoUnit.HOURS).toInstant())
                .endTime(noon.plus(90, ChronoUnit.MINUTES).toInstant()).build());
        sessionRepository.save(WorkoutSession.builder()
                .userId(userId).status("completed")
                .startTime(noon.plus(2, ChronoUnit.HOURS).toInstant())
                .endTime(noon.plus(150, ChronoUnit.MINUTES).toInstant()).build());

        setRepository.save(WorkoutSet.builder().sessionId(s1.getId()).setNumber(1)
                .weightUsed(new BigDecimal("40")).repsCompleted(10).build());
        setRepository.save(WorkoutSet.builder().sessionId(s1.getId()).setNumber(2)
                .weightUsed(new BigDecimal("50")).repsCompleted(8).build());

        bodyMetricRepository.save(BodyMetric.builder().userId(userId)
                .weightKg(new BigDecimal("70.5")).recordedAt(noon.toInstant()).build());

        FoodItem food = foodItemRepository.save(FoodItem.builder()
                .name("Gạo").caloriesPer100g(new BigDecimal("360"))
                .proteinPer100g(BigDecimal.ZERO).carbPer100g(BigDecimal.ZERO).fatPer100g(BigDecimal.ZERO)
                .source("system").build());
        MealLog mealLog = mealLogRepository.save(MealLog.builder()
                .userId(userId).mealNumber(1).logDate(today).build());
        mealEntryRepository.save(MealEntry.builder().mealLogId(mealLog.getId()).foodItemId(food.getId())
                .portionGrams(new BigDecimal("200")).totalCalories(new BigDecimal("600"))
                .totalProtein(BigDecimal.ZERO).totalCarb(BigDecimal.ZERO).totalFat(BigDecimal.ZERO).build());

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .param("from", today.minusDays(6).toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight[0].weightKg").value(70.5))
                // 40*10 + 50*8 = 800
                .andExpect(jsonPath("$.volume[0].totalKg").value(800.0))
                .andExpect(jsonPath("$.streak.currentStreakWeeks").value(1))
                // 1 buổi completed / 3 ngày plan = 33.3%
                .andExpect(jsonPath("$.planCompletion.completedSessions").value(1))
                .andExpect(jsonPath("$.planCompletion.plannedDays").value(3))
                .andExpect(jsonPath("$.planCompletion.completionPct").value(33.3))
                // 3 buổi × 30 phút × 5 kcal = 450; ngày cuối (hôm nay) có điểm
                .andExpect(jsonPath("$.calories[6].caloriesBurned").value(450.0))
                .andExpect(jsonPath("$.calories[6].caloriesIn").value(600.0));
    }

    @Test
    void dashboardEmptyState() throws Exception {
        createUser("b@example.com");
        String token = login("b@example.com");

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight").isEmpty())
                .andExpect(jsonPath("$.volume").isEmpty())
                .andExpect(jsonPath("$.streak.currentStreakWeeks").value(0))
                .andExpect(jsonPath("$.streak.longestStreakWeeks").value(0))
                .andExpect(jsonPath("$.planCompletion.completedSessions").value(0))
                .andExpect(jsonPath("$.planCompletion.completionPct").value(0))
                // Mặc định 30 ngày → 30 điểm calo bằng 0
                .andExpect(jsonPath("$.calories.length()").value(30));
    }

    @Test
    void rejectsInvalidRange() throws Exception {
        createUser("c@example.com");
        String token = login("c@example.com");
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .param("from", today.toString())
                        .param("to", today.minusDays(1).toString()))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .param("from", today.minusDays(400).toString())
                        .param("to", today.toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void dashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/stats/dashboard"))
                .andExpect(status().isForbidden());
    }
}
