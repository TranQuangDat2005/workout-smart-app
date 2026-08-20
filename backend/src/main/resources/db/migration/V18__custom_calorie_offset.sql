-- V18: 019 nutrition needs — tùy chỉnh mức calo (calorie_goal = 'custom')
-- custom_calorie_offset: kcal/ngày cộng thêm vào TDEE (âm = giảm, dương = tăng); chỉ dùng khi calorie_goal = 'custom'
ALTER TABLE users ADD COLUMN custom_calorie_offset INT;
