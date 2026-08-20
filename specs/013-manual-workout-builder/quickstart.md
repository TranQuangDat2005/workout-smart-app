# Quickstart: Picker Thêm bài trên lịch tập

## Prerequisites

- User đã đăng nhập, có lộ trình active.
- Backend chạy, media `/media/**` phục vụ GIF/ảnh kho.
- Tab Luyện tập → Lịch tập.

## Validate picker (US4)

1. Chọn một ngày → **+ Thêm bài**.
2. Expect: ô tìm tên, chip Category (10), chip Equipment (28), danh sách bài có thumbnail, khung xem trước trống.
3. Chọn Category **Ngực** và dụng cụ **Tạ đơn**.
4. Expect: chỉ bài ngực + tạ đơn; không bài cáp/máy.
5. Bấm một bài trong danh sách.
6. Expect: khung xem trước hiện tên, GIF (hoặc ảnh), hướng dẫn; bài **chưa** vào bảng ngày.
7. Bấm **Thêm vào ngày này**.
8. Expect: picker đóng; bài xuất hiện trên ngày với sets/reps mặc định.
9. Lặp đến 15 bài rồi thêm tiếp → Expect: thông báo tối đa 15, không thêm.

## Validate không lệch thư viện

Mở Thư viện bài tập: cùng 10 Category, 28 Equipment, cùng quy tắc HOẶC/VÀ. Picker không có form tạo bài cá nhân.

## Backend (không đổi cho US4)

`GET /api/v1/exercises?category=chest&equipment=dumbbell&q=&page=0&size=20` → 200, `content` có `gifUrl`/`image`/`instructions`.
