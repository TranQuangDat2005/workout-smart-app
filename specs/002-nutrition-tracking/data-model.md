# Data Model: Dinh dưỡng & Calories (002-nutrition-tracking)

## Bảng (migration V6 — đã tạo)

| Bảng | Mô tả |
|---|---|
| `food_items` | Kho thực phẩm: source=system/user_custom, created_by, deleted_at (soft-delete 1 tuần) |
| `meal_logs` | Bữa ăn (header): user_id, meal_number (1..n), log_date |
| `meal_entries` | Từng món trong bữa: food_item_id, portion_grams, macro đã tính |
| `meal_daily_summaries` | Tổng kết ngày (retention >14 ngày) |
| `body_metrics` | Chỉ số cơ thể: weight_kg (bắt buộc), body_fat/waist/chest/arm |

## Quy tắc nghiệp vụ

- Bữa đánh số 1..n; trùng (user, date, mealNumber) → 409. KHÔNG xóa bữa.
- Food custom: chỉ chủ sở hữu sửa/xóa; food hệ thống không sửa/xóa được (403).
- Macro tính: `per100g × portion/100`.
- TDEE = BMR Mifflin-St Jeor × activity factor; target theo goal: cutting −17%, bulking +12%, endurance giữ nguyên. Thiếu thông số → 422.
- Retention job 03:30 hằng ngày: meal cũ >14 ngày → gộp thành summary, xóa chi tiết.
- Body metric: sync weight mới nhất lên `users.weight_kg`.
