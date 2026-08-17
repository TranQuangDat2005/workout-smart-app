# Implementation Plan: Thống kê & Báo cáo Tiến độ (005-stats-reports)

**Branch**: `feature/005-stats-reports` | **Date**: 2026-08-18 | **Spec**: [spec.md](spec.md)

## Summary

Dashboard thống kê cho 1 user (UC-17): biểu đồ cân nặng theo thời gian, volume (kg nâng/tuần), streak (định nghĩa DUY NHẤT toàn hệ thống), tỷ lệ hoàn thành Workout Plan (active + archived), so sánh calo nạp vs. calo tiêu thụ theo ngày. Bộ lọc 7/30/90 ngày + custom range. Package `com.workoutsmart.stats`, KHÔNG cần migration mới (tái sử dụng bảng V4, V5, V6). Tạo entity `com.workoutsmart.plan` (WorkoutPlan/Day/Exercise) vì bảng `workout_plans*` (V5) chưa có entity — 008-workout-plan sẽ tái sử dụng.

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| Layered + Bean Validation | ✅ Controller mỏng → StatsService |
| Streak định nghĩa DUY NHẤT | ✅ Tái sử dụng `StreakCalculator` |
| Calo nạp > 2 tuần → `meal_daily_summaries` | ✅ Theo retention constitution §4 |
| KHÔNG raw SQL | ✅ Spring Data JPA derived queries |
| Test ≥80% | ✅ Unit + integration |
| OpenAPI cho endpoint mới | ✅ contracts/openapi.yaml |

**GATE: PASS**

## Technical Context

- **Backend**: Spring Boot 3.3, Java 17. Package mới `com.workoutsmart.stats` (controller/dto/service) + `com.workoutsmart.plan` (entity/repository cho bảng V5).
- **Web**: React 18 + TS strict, `web/src/pages/stats/StatsPage.tsx`, `web/src/services/statsApi.ts`. Biểu đồ: SVG tự vẽ (line/bar) — KHÔNG thêm dependency chart.
- **DB**: PostgreSQL 18 dev / H2 test. Không migration mới.
- **Timezone**: dùng `ZoneId.systemDefault()` cho việc gom ngày/tuần (thống nhất với session auto-expire).

## Thiết kế

### Endpoint

`GET /api/v1/stats/dashboard?from=yyyy-MM-dd&to=yyyy-MM-dd`
- Mặc định: 30 ngày gần nhất (nếu thiếu `from`/`to`).
- Validation: `from ≤ to`; khoảng tối đa 366 ngày; sai → 422.

### Cách tính (StatsService)

| Thành phần | Nguồn | Công thức |
|---|---|---|
| Cân nặng | `body_metrics` trong range | điểm (date, weightKg) tăng dần theo thời gian |
| Volume | `workout_sessions` completed trong range → `workout_sets` | Σ(weight × reps) gom theo tuần (thứ 2 đầu tuần); null → 0 |
| Streak | TOÀN BỘ session completed (không theo range) | `StreakCalculator.calculate` (hiện tại + dài nhất) |
| Plan completion | `workout_plans` (active + archived) × `workout_plan_days` × `workout_sessions(plan_id, completed)` | Σ buổi completed / Σ ngày plan × 100 |
| Calo tiêu thụ | session completed trong range | thời lượng (phút) × 5 kcal/phút (MET đơn giản hóa), gom theo ngày |
| Calo nạp | ngày ≤ 14 ngày: Σ `meal_entries.total_calories`; ngày > 14 ngày: `meal_daily_summaries.summary_json` (fallback 0 nếu chưa có summary) | gom theo ngày |

## Files

- Backend main: `stats/controller/StatsController.java`, `stats/dto/*.java`, `stats/service/StatsService.java`, `plan/entity/*.java`, `plan/repository/*.java`
- Backend test: `stats/service/StatsServiceTest.java`, `stats/controller/StatsControllerIntegrationTest.java`
- Repository mở rộng (thêm derived query): `WorkoutSessionRepository`, `WorkoutSetRepository`, `BodyMetricRepository`, `MealLogRepository`, `MealEntryRepository`, `MealDailySummaryRepository`
- Web: `web/src/services/statsApi.ts`, `web/src/pages/stats/StatsPage.tsx`, route `/stats` trong `App.tsx`
