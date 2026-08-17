package com.workoutsmart.nutrition.service;

import com.workoutsmart.nutrition.entity.MealDailySummary;
import com.workoutsmart.nutrition.entity.MealLog;
import com.workoutsmart.nutrition.repository.MealDailySummaryRepository;
import com.workoutsmart.nutrition.repository.MealEntryRepository;
import com.workoutsmart.nutrition.repository.MealLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retention: bữa ăn chi tiết giữ 2 tuần; cũ hơn → gộp thành tổng kết ngày
 * (meal_daily_summaries) rồi xóa chi tiết.
 */
@Service
public class MealRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(MealRetentionJob.class);
    private static final int RETENTION_DAYS = 14;

    private final MealLogRepository mealLogRepository;
    private final MealEntryRepository mealEntryRepository;
    private final MealDailySummaryRepository summaryRepository;

    public MealRetentionJob(MealLogRepository mealLogRepository,
                            MealEntryRepository mealEntryRepository,
                            MealDailySummaryRepository summaryRepository) {
        this.mealLogRepository = mealLogRepository;
        this.mealEntryRepository = mealEntryRepository;
        this.summaryRepository = summaryRepository;
    }

    @Scheduled(cron = "0 30 3 * * *") // 03:30 hằng ngày
    @Transactional
    public void purgeOldMealDetails() {
        LocalDate threshold = LocalDate.now().minusDays(RETENTION_DAYS);
        List<MealLog> oldLogs = mealLogRepository.findByLogDateBefore(threshold);
        if (oldLogs.isEmpty()) {
            return;
        }

        Map<String, List<MealLog>> byUserAndDate = oldLogs.stream()
                .collect(Collectors.groupingBy(ml -> ml.getUserId() + "|" + ml.getLogDate()));

        for (Map.Entry<String, List<MealLog>> entry : byUserAndDate.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            Long userId = Long.valueOf(parts[0]);
            LocalDate date = LocalDate.parse(parts[1]);

            Map<Integer, BigDecimal> caloriesByMeal = new java.util.HashMap<>();
            for (MealLog ml : entry.getValue()) {
                BigDecimal total = mealEntryRepository.findByMealLogId(ml.getId()).stream()
                        .map(e -> e.getTotalCalories())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                caloriesByMeal.put(ml.getMealNumber(), total);
                mealEntryRepository.deleteByMealLogId(ml.getId());
                mealLogRepository.delete(ml);
            }

            String summaryJson = buildSummaryJson(caloriesByMeal);
            summaryRepository.findByUserIdAndLogDate(userId, date)
                    .ifPresentOrElse(
                            existing -> {
                                existing.setSummaryJson(summaryJson);
                                summaryRepository.save(existing);
                            },
                            () -> summaryRepository.save(MealDailySummary.builder()
                                    .userId(userId)
                                    .logDate(date)
                                    .summaryJson(summaryJson)
                                    .build()));
        }
        log.info("Đã gộp {} bữa ăn cũ hơn {} ngày thành tổng kết ngày", oldLogs.size(), RETENTION_DAYS);
    }

    private String buildSummaryJson(Map<Integer, BigDecimal> caloriesByMeal) {
        StringBuilder sb = new StringBuilder("{");
        caloriesByMeal.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    if (sb.length() > 1) {
                        sb.append(",");
                    }
                    sb.append("\"meal").append(e.getKey()).append("Calories\":")
                            .append(e.getValue());
                });
        sb.append("}");
        return sb.toString();
    }
}
