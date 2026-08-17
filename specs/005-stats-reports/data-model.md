# Data Model: Thống kê & Báo cáo (005-stats-reports)

Không tạo bảng mới — tái sử dụng schema hiện có. Thay đổi duy nhất: bổ sung entity cho bảng `workout_plans*` (V5) vốn chưa có entity.

## Entity mới — `com.workoutsmart.plan`

| Entity | Bảng | Fields |
|---|---|---|
| `WorkoutPlan` | `workout_plans` | id, userId, name, goalType, status (active/archived), createdAt |
| `WorkoutPlanDay` | `workout_plan_days` | id, planId, dayOfWeek (1..7) |
| `WorkoutPlanExercise` | `workout_plan_exercises` | id, dayId, exerciseId, targetSets, targetReps |

- Không relationship JPA (giữ khóa ngoại dạng cột `*_id` — đồng nhất với các entity hiện tại).
- Repository: `WorkoutPlanRepository.findByUserIdOrderByCreatedAtDesc`, `WorkoutPlanDayRepository.countByPlanId`.

## Repository mở rộng (derived queries — KHÔNG raw SQL)

| Repository | Phương thức thêm |
|---|---|
| `WorkoutSessionRepository` | `findByUserIdAndStatusAndStartTimeBetween`, `findByUserIdAndStatus`, `countByPlanIdAndStatus` |
| `WorkoutSetRepository` | `findBySessionIdIn` |
| `BodyMetricRepository` | `findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc` |
| `MealLogRepository` | `findByUserIdAndLogDateBetween` |
| `MealEntryRepository` | `findByMealLogIdIn` |
| `MealDailySummaryRepository` | `findByUserIdAndLogDateBetween` |

## DTO response (Java record)

```text
StatsDashboardResponse(
  List<WeightPoint> weight,        // {date, weightKg}
  List<VolumePoint> volume,        // {weekStart, totalKg}
  StreakStats streak,              // {currentStreakWeeks, longestStreakWeeks}
  PlanCompletion planCompletion,   // {completedSessions, plannedDays, completionPct}
  List<CaloriePoint> calories      // {date, caloriesIn, caloriesBurned}
)
```

## Retention mapping

- Ngày ≤ 14 ngày trước: calo nạp = Σ `meal_entries` của các `meal_logs` trong ngày.
- Ngày > 14 ngày trước: calo nạp = `meal_daily_summaries.summary_json.totalCalories` (không có → 0; chi tiết macro KHÔNG cần cho biểu đồ calo).
