# Quickstart: Màn tập trung tối giản (016)

Hướng dẫn chạy & kiểm thử tay feature sau khi implement. Chi tiết thiết kế xem [plan.md](./plan.md) và [spec.md](./spec.md).

## Prerequisites

- PostgreSQL 18 chạy trên `:5432` (DB `workoutDB`, đã migrate v16).
- Backend: `start-backend.bat` (Task Scheduler `WSBackend`, port 8080).
- Frontend: `start-frontend.bat` (Vite `:5173`, proxy `/api` → 8080).
- Tài khoản đã đăng nhập, có lộ trình với ít nhất 1 bài hôm nay (gồm 1 bài reps, 1 bài duration như plank, 1 bài có `restTimeSeconds` khác 60 để verify nghỉ theo DB).

## Run

```bat
:: backend
schtasks /Run /TN WSBackend
:: frontend
schtasks /Run /TN WSFrontend
```

Hoặc dev: `cmd //c "C:\...\maven\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml spring-boot:run"` và `npm.cmd --prefix web run dev`.

Build/Test (để verify trước khi chạy tay):

```bat
cmd //c "C:\Users\daizl\Desktop\Save\zOthers\maven\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml test"
npm --prefix web run build
```

## Kịch bản kiểm thử tay

### T1 — Layout tối giản (FR-001)
1. Mở `http://localhost:5173` → Luyện tập → tab Hôm nay → bắt đầu buổi tập.
2. **Kỳ vọng**: thấy đúng thứ tự — tên bài + đồng hồ thời lượng → GIF/ảnh → "Hiệp 1 - mục tiêu" → nút HOÀN THÀNH. Không có form nhập liệu.

### T2 — Ghi hiệp 1 chạm (FR-002)
1. Bài rep: chạm HOÀN THÀNH.
2. **Kỳ vọng**: hiệp ghi reps = mục tiêu, tạ = tạ hiệp trước (hoặc rỗng ở hiệp đầu), chuyển màn nghỉ; chạm nhanh 3 lần chỉ tạo 1 hiệp.

### T3 — Bài duration tự chuyển (FR-003)
1. Chuyển sang bài plank → thấy đồng hồ đếm ngược + Bắt đầu → chạy hết giờ.
2. **Kỳ vọng**: hiệp tự ghi đúng số giây đã đếm → màn nghỉ tự hiện, không cần chạm.

### T4 — Màn nghỉ theo DB (FR-004)
1. Ghi 1 hiệp ở bài có `restTimeSeconds = 90`.
2. **Kỳ vọng**: đếm ngược bắt đầu từ 90; beep ở 5s và 0s; BỎ QUA trở về màn bài tập; với target hiệp drop-set thì không có màn nghỉ.

### T5 — Sửa sai (FR-006, FR-007)
1. Chạm dòng "Hiệp X - mục tiêu" → sửa reps/tạ → HOÀN THÀNH → hiệp ghi giá trị đã sửa.
2. Trong 60 giây: nút "Hoàn tác" hiện → chạm → hiệp biến mất, số hiệp lùi 1. Sau 60 giây nút ẩn.

### T6 — Hàng đợi & chuyển bài (FR-008)
1. Buổi có ≥ 2 bài → thấy chip hàng đợi (✓/đang/tới) + "Tiếp theo: …" → chạm chip khác → màn chuyển bài, số hiệp theo bài đó.

### T7 — Chống drift & khôi phục (FR-005, FR-010)
1. Đang nghỉ → ẩn tab 5 phút → quay lại: đồng hồ nghỉ đã trôi đúng thực tế (< 1s lệch).
2. Rời tab trong lúc tập → quay lại thấy số "Phân tâm ×N" tự tăng 1 (không cần bấm nút).
3. Reload giữa buổi → session + hiệp đã ghi còn nguyên, đồng hồ buổi tiếp tục từ lúc bắt đầu (không reset 00:00).

### T8 — Endpoint xóa (FR-007 backend)
```bash
# happy path: 204
curl -X DELETE http://localhost:8080/api/v1/workout-sessions/1/sets/10 -H "Authorization: Bearer <token>"
# set không thuộc session: 404
# session completed/expired: 409
```

## Expected Outcomes

- `mvn test` pass (gồm unit test `TrackingService.deleteSet` happy + error path).
- `npm run build` + `npm test` pass (component test ExecutionScreen: render 4 khối, double-tap guard, undo ẩn sau 60s).
