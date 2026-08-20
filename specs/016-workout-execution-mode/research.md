# Research: Màn tập trung tối giản (016)

## R1 — Bố cục màn hình

- **Decision**: Single-column, max-width 560px, căn giữa; layout đúng 4 khối user chỉ định: header (tên bài + thời lượng) → GIF → "Hiệp X - mục tiêu" → 1 nút chính. Màn nghỉ là overlay trong container (theo mockup).
- **Rationale**: User chỉ định tường minh layout; single-column khớp ngữ cảnh "để điện thoại xa, nhìn lướt"; tránh side-panel từng gây bug đè UI ở lịch tập.
- **Alternatives rejected**: 2-cột desktop (side queue + panel) — phức tạp hơn, user đã từng yêu cầu bỏ panel bên phải; card in-flow cho màn nghỉ — overlay đúng mockup và tạo cảm giác "chuyển cảnh" rõ ràng hơn.

## R2 — Ghi hiệp 1 chạm: reps theo target

- **Decision**: Nút HOÀN THÀNH ghi reps = mục tiêu hiệp hiện tại, tạ = tạ hiệp liền trước cùng bài (nếu có), kiểu set = setType của target hiệp trong kế hoạch. Sai lệch sửa qua mini editor (chạm dòng mục tiêu) và hoàn tác 60s.
- **Rationale**: Yêu cầu tối giản tuyệt đối ("ko cần chọn quá nhiều nút"); vẫn giữ tính đúng của dữ liệu qua 2 lưới an toàn không chiếm chỗ màn hình.
- **Alternatives rejected**: stepper +/− luôn hiển thị (tăng noise, trái yêu cầu); ghi reps rỗng (phá bảng tiến bộ 30 ngày — không chấp nhận).

## R3 — Đồng hồ (buổi tập, nghỉ, bài duration)

- **Decision**: Mọi đồng hồ tính theo deadline tuyệt đối (`Date.now()` vs timestamp mốc), tick 500ms–1s để render; thời lượng buổi tính từ `session.startTime` (khôi phục sau reload). Beep 5s/0s cho nghỉ.
- **Rationale**: `setInterval` tăng/giảm dần bị browser throttle khi tab ẩn → trôi giờ; bug này vừa được fix ở `WorkoutSession.tsx` hiện tại và mockup HTML tái hiện lại nó — không lặp lại.
- **Alternatives rejected**: đếm dần kiểu mockup (drift); Web Worker timer (quá mức cho use case này — YAGNI).

## R4 — Bài duration tự ghi khi hết giờ

- **Decision**: Đồng hồ prefilled target (mặc định 60s nếu rỗng), Bắt đầu/Tạm dừng; về 0 → tự ghi `durationSeconds` đã đếm → chuyển màn nghỉ; khi tạm dừng có nút "Lưu" ghi số giây đã đếm.
- **Rationale**: User không thể chạm nút khi đang plank/run; tự ghi + tự chuyển là yêu cầu tường minh của user.
- **Alternatives rejected**: tự chuyển không ghi dữ liệu (mất lịch sử); bắt user xác nhận khi hết giờ (phá vỡ auto-flow).

## R5 — Màn nghỉ lấy thời gian từ DB

- **Decision**: Đếm ngược khởi tạo từ `restTimeSeconds` trong snapshot session exercise (đã có sẵn ở `SessionExerciseResponse`); drop-set → nghỉ 0 (kế thừa 014); nút "BỎ QUA".
- **Rationale**: User yêu cầu "thời gian nghỉ lấy trong database"; dữ liệu đã tồn tại, không cần API mới.
- **Alternatives rejected**: nghỉ cứng 30s (mockup) — trái cấu hình từng bài đã có.

## R6 — Undo hiệp (cửa sổ 60 giây)

- **Decision**: Endpoint `DELETE /api/v1/workout-sessions/{id}/sets/{setId}` — server validate session thuộc user + status active; client chỉ hiển thị nút trong 60 giây từ lúc ghi. Không cần migration (không thêm cột timestamp — dùng thời gian client).
- **Rationale**: Chạm nhầm là rủi ro thực tế khi giao diện chỉ có 1 nút; cửa sổ 60s giới hạn phạm vi xóa, tránh phá dữ liệu lịch sử. Không vi phạm retention (retention chỉ chốt cho bữa ăn; workout_sets chưa có hạn retention cố định; đây là sửa lỗi nhập, không phải xóa hàng loạt).
- **Alternatives rejected**: undo không giới hạn thời gian (rủi ro toàn vẹn dữ liệu); soft-update set thành null (rác dữ liệu); thêm cột `created_at` (migration không cần thiết cho MVP này).

## R7 — Media hướng dẫn (GIF 180×180)

- **Decision**: `SessionExerciseResponse` thêm `mediaUrl` (nullable) — join `Exercise.gifUrl`/`image` khi build response (cache theo exerciseId trong 1 request để tránh N+1); client hiển thị 180×180, fallback icon.
- **Rationale**: Constitution §1 bắt buộc hướng dẫn trực quan; entity `Exercise` đã có sẵn `gif_url`/`image` — chỉ cần expose qua DTO.
- **Alternatives rejected**: endpoint media riêng (thừa); external placeholder (vi phạm quy tắc media local + hỏng offline).

## R8 — Phạm vi backend

- **Decision**: Chỉ 2 thay đổi: (1) `mediaUrl` trong `SessionExerciseResponse`, (2) endpoint DELETE set. KHÔNG migration mới (DB đang v16).
- **Rationale**: Mọi nhu cầu còn lại (target từng hiệp, nghỉ theo bài, kiểu set, snapshot) đã có từ 013/014/015.
- **Alternatives rejected**: thêm bảng/cột (không cần); đổi contract record-set (không cần — ghi theo target vẫn đi qua `POST /sets` hiện tại).
