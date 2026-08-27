package com.workoutsmart.stats.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.nutrition.entity.BodyMetric;
import com.workoutsmart.nutrition.entity.MealDailySummary;
import com.workoutsmart.nutrition.entity.MealEntry;
import com.workoutsmart.nutrition.entity.MealLog;
import com.workoutsmart.nutrition.repository.BodyMetricRepository;
import com.workoutsmart.nutrition.repository.MealDailySummaryRepository;
import com.workoutsmart.nutrition.repository.MealEntryRepository;
import com.workoutsmart.nutrition.repository.MealLogRepository;
import com.workoutsmart.plan.entity.WorkoutPlan;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.stats.dto.StatsDashboardResponse;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseRepository;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseSetRepository;
import com.workoutsmart.tracking.entity.WorkoutSessionExercise;
import com.workoutsmart.tracking.entity.WorkoutSessionExerciseSet;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatsServiceTest {

    private WorkoutSessionRepository sessionRepository;
    private WorkoutSetRepository setRepository;
    private BodyMetricRepository bodyMetricRepository;
    private MealLogRepository mealLogRepository;
    private MealEntryRepository mealEntryRepository;
    private MealDailySummaryRepository summaryRepository;
    private WorkoutPlanRepository planRepository;
    private WorkoutPlanDayRepository planDayRepository;
    private WorkoutSessionExerciseRepository sessionExerciseRepository;
    private WorkoutSessionExerciseSetRepository sessionExerciseSetRepository;
    private StatsService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(WorkoutSessionRepository.class);
        setRepository = mock(WorkoutSetRepository.class);
        bodyMetricRepository = mock(BodyMetricRepository.class);
        mealLogRepository = mock(MealLogRepository.class);
        mealEntryRepository = mock(MealEntryRepository.class);
        summaryRepository = mock(MealDailySummaryRepository.class);
        planRepository = mock(WorkoutPlanRepository.class);
        planDayRepository = mock(WorkoutPlanDayRepository.class);
        sessionExerciseRepository = mock(WorkoutSessionExerciseRepository.class);
        sessionExerciseSetRepository = mock(WorkoutSessionExerciseSetRepository.class);
        service = new StatsService(sessionRepository, setRepository, bodyMetricRepository,
                mealLogRepository, mealEntryRepository, summaryRepository,
                planRepository, planDayRepository, sessionExerciseRepository,
                sessionExerciseSetRepository, new ObjectMapper());
    }

    @Test
    void dashboardAggregatesWeightVolumeAndCalories() {
        LocalDate today = LocalDate.now();
        Long userId = 1L;
        ZoneId zone = ZoneId.systemDefault();
        // Trưa hôm nay → không lệch ngày dù test chạy gần nửa đêm
        Instant noon = today.atTime(LocalTime.NOON).atZone(zone).toInstant();

        // body metric hôm nay
        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of(BodyMetric.builder().userId(userId)
                        .weightKg(new BigDecimal("70.5")).recordedAt(noon).build()));

        // 2 session completed hôm nay
        WorkoutSession s1 = WorkoutSession.builder().id(10L).userId(userId).status("completed")
                .startTime(noon).endTime(noon.plus(30, ChronoUnit.MINUTES)).build();
        WorkoutSession s2 = WorkoutSession.builder().id(11L).userId(userId).status("completed")
                .startTime(noon.plus(1, ChronoUnit.HOURS)).endTime(noon.plus(90, ChronoUnit.MINUTES)).build();
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of(s1, s2));
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of(s1, s2));

        when(setRepository.findBySessionIdIn(anyList())).thenReturn(List.of(
                WorkoutSet.builder().sessionId(10L).weightUsed(new BigDecimal("40")).repsCompleted(10).build(),
                WorkoutSet.builder().sessionId(10L).weightUsed(new BigDecimal("50")).repsCompleted(8).build(),
                // bodyweight: weight null → đóng góp 0
                WorkoutSet.builder().sessionId(11L).repsCompleted(15).build()));

        // bữa ăn hôm nay: 1 log + entry 600 kcal
        MealLog log = MealLog.builder().id(20L).userId(userId).mealNumber(1)
                .logDate(today).createdAt(noon).build();
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any()))
                .thenReturn(List.of(log));
        when(mealEntryRepository.findByMealLogIdIn(anyList()))
                .thenReturn(List.of(MealEntry.builder().mealLogId(20L).foodItemId(1L)
                        .portionGrams(new BigDecimal("100")).totalCalories(new BigDecimal("600"))
                        .totalProtein(BigDecimal.ZERO).totalCarb(BigDecimal.ZERO).totalFat(BigDecimal.ZERO).build()));

        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());

        StatsDashboardResponse response = service.dashboard(userId, today.minusDays(6), today);

        assertEquals(1, response.weight().size());
        assertEquals(new BigDecimal("70.5"), response.weight().get(0).weightKg());

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertEquals(1, response.volume().size());
        assertEquals(weekStart, response.volume().get(0).weekStart());
        // 40*10 + 50*8 + 0 = 800
        assertEquals(new BigDecimal("800.0"), response.volume().get(0).totalKg());

        // streak: 2 session cùng tuần → chưa đủ 3 → 0
        assertEquals(0, response.streak().currentStreakWeeks());

        // calo: burned = 30 phút * 5 + 30 phút * 5 = 300
        var todayPoint = response.calories().stream()
                .filter(p -> p.date().equals(today)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("300.0"), todayPoint.caloriesBurned());
        assertEquals(new BigDecimal("600.0"), todayPoint.caloriesIn());
    }

    @Test
    void planCompletionAggregatesActiveAndArchived() {
        Long userId = 1L;
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of());
        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());

        WorkoutPlan active = WorkoutPlan.builder().id(1L).userId(userId).status("active").build();
        WorkoutPlan archived = WorkoutPlan.builder().id(2L).userId(userId).status("archived").build();
        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(active, archived));
        when(planDayRepository.countByPlanId(1L)).thenReturn(3L);
        when(planDayRepository.countByPlanId(2L)).thenReturn(4L);
        when(sessionRepository.countByPlanIdAndStatus(1L, "completed")).thenReturn(3L);
        when(sessionRepository.countByPlanIdAndStatus(2L, "completed")).thenReturn(1L);

        StatsDashboardResponse response = service.dashboard(userId, null, null);

        assertEquals(4, response.planCompletion().completedSessions());
        assertEquals(7, response.planCompletion().plannedDays());
        // 4/7 = 57.1
        assertEquals(new BigDecimal("57.1"), response.planCompletion().completionPct());
    }

    @Test
    void oldDaysUseDailySummariesInsteadOfEntries() {
        LocalDate today = LocalDate.now();
        Long userId = 1L;
        LocalDate oldDay = today.minusDays(20);

        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of());
        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        // Không có meal log chi tiết cho ngày cũ
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(mealEntryRepository.findByMealLogIdIn(anyList())).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any()))
                .thenReturn(List.of(MealDailySummary.builder().userId(userId).logDate(oldDay)
                        .summaryJson("{\"totalCalories\": 1800}").build()));

        StatsDashboardResponse response = service.dashboard(userId, today.minusDays(25), today);

        var oldPoint = response.calories().stream()
                .filter(p -> p.date().equals(oldDay)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1800.0"), oldPoint.caloriesIn());
    }

    @Test
    void everyDayInRangeHasCaloriePoint() {
        Long userId = 1L;
        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of());
        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());

        LocalDate today = LocalDate.now();
        StatsDashboardResponse response = service.dashboard(userId, today.minusDays(2), today);

        assertEquals(3, response.calories().size());
        assertTrue(response.calories().stream().allMatch(
                p -> p.caloriesIn().compareTo(BigDecimal.ZERO) == 0
                        && p.caloriesBurned().compareTo(BigDecimal.ZERO) == 0));
    }

    @Test
    void rejectsFromAfterTo() {
        LocalDate today = LocalDate.now();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.dashboard(1L, today, today.minusDays(1)));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void rejectsRangeLongerThan366Days() {
        LocalDate today = LocalDate.now();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.dashboard(1L, today.minusDays(400), today));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void defaultsToLast30Days() {
        Long userId = 1L;
        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of());
        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());

        StatsDashboardResponse response = service.dashboard(userId, null, null);

        // from = today-29, to = today → 30 điểm calo
        assertEquals(30, response.calories().size());
    }

    @Test
    void noCompletedSessionsMeansZeroVolumeAndZeroStreak() {
        Long userId = 1L;
        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of());
        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());

        StatsDashboardResponse response = service.dashboard(userId, null, null);

        assertTrue(response.volume().isEmpty());
        assertEquals(0, response.streak().currentStreakWeeks());
        assertEquals(0, response.streak().longestStreakWeeks());
    }

    @Test
    void targetAttainmentUsesActualSetsAgainstSnapshotTargets() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        WorkoutSession session = WorkoutSession.builder().id(10L).userId(userId).status("completed")
                .startTime(start).endTime(start.plus(20, ChronoUnit.MINUTES)).build();

        when(sessionRepository.findByUserIdAndStatusAndStartTimeBetween(any(), any(), any(), any()))
                .thenReturn(List.of(session));
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(List.of(session));
        when(sessionExerciseRepository.findBySessionIdIn(anyList())).thenReturn(List.of(
                WorkoutSessionExercise.builder().id(20L).sessionId(10L).measureType("reps_weight").build()));
        when(sessionExerciseSetRepository.findBySessionExerciseIdIn(anyList())).thenReturn(List.of(
                WorkoutSessionExerciseSet.builder().sessionExerciseId(20L).setNumber(1)
                        .targetReps(10).build(),
                WorkoutSessionExerciseSet.builder().sessionExerciseId(20L).setNumber(2)
                        .targetReps(10).build()));
        when(setRepository.findBySessionIdIn(anyList())).thenReturn(List.of(
                WorkoutSet.builder().sessionId(10L).sessionExerciseId(20L).setNumber(1)
                        .repsCompleted(12).build(),
                WorkoutSet.builder().sessionId(10L).sessionExerciseId(20L).setNumber(2)
                        .repsCompleted(8).build()));
        when(bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
        when(mealLogRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(summaryRepository.findByUserIdAndLogDateBetween(any(), any(), any())).thenReturn(List.of());
        when(planRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        StatsDashboardResponse response = service.dashboard(userId, today, today);

        assertEquals(1, response.targetAttainment().achievedSets());
        assertEquals(2, response.targetAttainment().totalSets());
        assertEquals(new BigDecimal("50.0"), response.targetAttainment().attainmentPct());
    }
}
