# Quickstart: 012 Exercise Library Filters

## Backend

1. Đăng nhập lấy JWT.
2. `GET /api/v1/exercises?page=0&size=20` → danh sách mặc định.
3. `GET /api/v1/exercises?category=chest&category=back&equipment=dumbbell` → bài ngực HOẶC lưng, VÀ tạ đơn.
4. `GET /api/v1/exercises?q=press&equipment=barbell` → tên chứa press, tạ đòn.
5. `POST /api/v1/exercises/custom` với `name`, `category`, `equipment`, `muscleGroup` — thiếu trường → 400.

## Web

1. Mở `/exercises`.
2. Chip Category/Equipment đa chọn; không còn chip 6 nhóm cơ.
3. Gõ tên → kết quả cập nhật; phân trang về trang 1.
4. Tạo bài custom: chọn 1 category, 1 equipment, 1 nhóm cơ.
