package com.workoutsmart.stats.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutsmart.auth.exception.ApiException;
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
import com.workoutsmart.social.service.StreakCalculator;
import com.workoutsmart.stats.dto.StatsDashboardResponse;
import com.workoutsmart.stats.dto.StatsDashboardResponse.CaloriePoint;
import com.workoutsmart.stats.dto.StatsDashboardResponse.PlanCompletion;
import com.workoutsmart.stats.dto.StatsDashboardResponse.VolumePoint;
import com.workoutsmart.stats.dto.StatsDashboardResponse.WeightPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Thống kê & báo cáo tiến độ — spec 005-stats-reports (UC-17). */
@Service
public class StatsService {

    /** Số ngày giữ chi tiết bữa ăn (constitution §4): cũ hơn chỉ còn tổng kết ngày. */
    static final int DETAIL_RETENTION_DAYS = 14;

    /** Ước tính calo tiêu thụ đơn giản hóa (~5 MET, người ~60kg). */
    private static final BigDecimal KCAL_PER_MINUTE = new BigDecimal("5");

    private static final String STATUS_COMPLETED = "completed";

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;
    private final BodyMetricRepository bodyMetricRepository;
    private final MealLogRepository mealLogRepository;
    private final MealEntryRepository mealEntryRepository;
    private final MealDailySummaryRepository summaryRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository planDayRepository;
    private final ObjectMapper objectMapper;
    private final ZoneId zone;

    public StatsService(WorkoutSessionRepository sessionRepository,
                        WorkoutSetRepository setRepository,
                        BodyMetricRepository bodyMetricRepository,
                        MealLogRepository mealLogRepository,
                        MealEntryRepository mealEntryRepository,
                        MealDailySummaryRepository summaryRepository,
                        WorkoutPlanRepository planRepository,
                        WorkoutPlanDayRepository planDayRepository,
                        ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.setRepository = setRepository;
        this.bodyMetricRepository = bodyMetricRepository;
        this.mealLogRepository = mealLogRepository;
        this.mealEntryRepository = mealEntryRepository;
        this.summaryRepository = summaryRepository;
        this.planRepository = planRepository;
        this.planDayRepository = planDayRepository;
        this.objectMapper = objectMapper;
        this.zone = ZoneId.systemDefault();
    }

    public StatsDashboardResponse dashboard(Long userId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(zone);
        LocalDate start = from == null ? today.minusDays(29) : from;
        LocalDate end = to == null ? today : to;
        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        if (Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays() > 366) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Khoảng thời gian tối đa là 366 ngày");
        }

        Instant fromInstant = start.atStartOfDay(zone).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(zone).toInstant();

        return new StatsDashboardResponse(
                weight(userId, fromInstant, toInstant),
                volume(userId, fromInstant, toInstant),
                streak(userId),
                planCompletion(userId),
                calories(userId, start, end));
    }

    // ---------- Cân nặng ----------

    private List<WeightPoint> weight(Long userId, Instant from, Instant to) {
        return bodyMetricRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, from, to)
                .stream()
                .map(m -> new WeightPoint(m.getRecordedAt().atZone(zone).toLocalDate(), m.getWeightKg()))
                .toList();
    }

    // ---------- Volume (kg nâng/tuần) ----------

    private List<VolumePoint> volume(Long userId, Instant from, Instant to) {
        List<WorkoutSession> sessions =
                sessionRepository.findByUserIdAndStatusAndStartTimeBetween(userId, STATUS_COMPLETED, from, to);
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<Long, LocalDate> weekBySession = new LinkedHashMap<>();
        for (WorkoutSession session : sessions) {
            weekBySession.put(session.getId(), session.getStartTime().atZone(zone).toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        }
        List<Long> sessionIds = new ArrayList<>(weekBySession.keySet());
        Map<LocalDate, BigDecimal> byWeek = new LinkedHashMap<>();
        for (WorkoutSet set : setRepository.findBySessionIdIn(sessionIds)) {
            LocalDate weekStart = weekBySession.get(set.getSessionId());
            if (weekStart == null) {
                continue;
            }
            BigDecimal weight = set.getWeightUsed() == null ? BigDecimal.ZERO : set.getWeightUsed();
            int reps = set.getRepsCompleted() == null ? 0 : set.getRepsCompleted();
            byWeek.merge(weekStart, weight.multiply(BigDecimal.valueOf(reps)), BigDecimal::add);
        }
        List<VolumePoint> result = new ArrayList<>();
        byWeek.forEach((week, kg) ->
                result.add(new VolumePoint(week, kg.setScale(1, RoundingMode.HALF_UP))));
        return result;
    }

    // ---------- Streak (định nghĩa duy nhất, toàn bộ lịch sử) ----------

    private StatsDashboardResponse.StreakStats streak(Long userId) {
        List<Instant> starts = sessionRepository.findByUserIdAndStatus(userId, STATUS_COMPLETED)
                .stream()
                .map(WorkoutSession::getStartTime)
                .toList();
        StreakCalculator.StreakResult result = new StreakCalculator().calculate(starts, zone);
        return new StatsDashboardResponse.StreakStats(result.currentStreakWeeks(), result.longestStreakWeeks());
    }

    // ---------- Tỷ lệ hoàn thành plan (active + archived) ----------

    private PlanCompletion planCompletion(Long userId) {
        List<WorkoutPlan> plans = planRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long plannedDays = 0;
        long completed = 0;
        for (WorkoutPlan plan : plans) {
            plannedDays += planDayRepository.countByPlanId(plan.getId());
            completed += sessionRepository.countByPlanIdAndStatus(plan.getId(), STATUS_COMPLETED);
        }
        BigDecimal pct = plannedDays == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(plannedDays), 1, RoundingMode.HALF_UP);
        return new PlanCompletion(completed, plannedDays, pct);
    }

    // ---------- Calo nạp vs tiêu thụ ----------

    private List<CaloriePoint> calories(Long userId, LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> burnedByDay = caloriesBurned(userId, start, end);
        Map<LocalDate, BigDecimal> intakeByDay = caloriesIn(userId, start, end);

        List<CaloriePoint> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            points.add(new CaloriePoint(d,
                    intakeByDay.getOrDefault(d, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP),
                    burnedByDay.getOrDefault(d, BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP)));
        }
        return points;
    }

    private Map<LocalDate, BigDecimal> caloriesBurned(Long userId, LocalDate start, LocalDate end) {
        Instant from = start.atStartOfDay(zone).toInstant();
        Instant to = end.plusDays(1).atStartOfDay(zone).toInstant();
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        for (WorkoutSession session :
                sessionRepository.findByUserIdAndStatusAndStartTimeBetween(userId, STATUS_COMPLETED, from, to)) {
            if (session.getEndTime() == null) {
                continue;
            }
            long minutes = Math.max(0, Duration.between(session.getStartTime(), session.getEndTime()).toMinutes());
            LocalDate date = session.getStartTime().atZone(zone).toLocalDate();
            byDay.merge(date, KCAL_PER_MINUTE.multiply(BigDecimal.valueOf(minutes)), BigDecimal::add);
        }
        return byDay;
    }

    private Map<LocalDate, BigDecimal> caloriesIn(Long userId, LocalDate start, LocalDate end) {
        LocalDate detailFrom = LocalDate.now(zone).minusDays(DETAIL_RETENTION_DAYS);
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();

        // Ngày gần (≤ 14 ngày): tổng chi tiết meal_entries
        LocalDate fromDetail = start.isBefore(detailFrom) ? detailFrom : start;
        List<MealLog> logs = mealLogRepository.findByUserIdAndLogDateBetween(userId, fromDetail, end);
        if (!logs.isEmpty()) {
            Map<Long, LocalDate> dateById = new LinkedHashMap<>();
            logs.forEach(l -> dateById.put(l.getId(), l.getLogDate()));
            for (MealEntry entry : mealEntryRepository.findByMealLogIdIn(new ArrayList<>(dateById.keySet()))) {
                byDay.merge(dateById.get(entry.getMealLogId()), entry.getTotalCalories(), BigDecimal::add);
            }
        }

        // Ngày cũ (> 14 ngày): chỉ còn tổng kết ngày
        if (start.isBefore(detailFrom)) {
            LocalDate summaryEnd = detailFrom.minusDays(1);
            for (MealDailySummary summary :
                    summaryRepository.findByUserIdAndLogDateBetween(userId, start, summaryEnd)) {
                byDay.put(summary.getLogDate(), parseSummaryCalories(summary));
            }
        }
        return byDay;
    }

    private BigDecimal parseSummaryCalories(MealDailySummary summary) {
        try {
            JsonNode node = objectMapper.readTree(summary.getSummaryJson());
            if (node.has("totalCalories")) {
                return node.get("totalCalories").decimalValue();
            }
        } catch (Exception ignored) {
            // summary hỏng/thiếu → tính 0 thay vì lỗi cả dashboard
        }
        return BigDecimal.ZERO;
    }
}
