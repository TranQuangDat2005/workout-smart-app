# Feature Specification: Màn tập trung tối giản cho buổi tập (Workout Execution Mode)

**Feature Branch**: `016-workout-execution-mode`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Thiết kế lại màn Tập hôm nay theo hướng tối giản: [TÊN BÀI TẬP] [thời gian tập từ đầu buổi] → [GIF] → [Hiệp hiện tại - rep/thời gian cần làm] → [Nút hoàn thành]. Nút hoàn thành chỉ cho bài tính rep; bài tính thời gian tự chuyển khi hết giờ; có màn nghỉ khi hết hiệp, thời gian nghỉ lấy từ database."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Màn tập trung tối giản (Priority: P1)

Khi buổi tập đang active, màn Hôm nay hiển thị đúng 4 khối theo thứ tự: (1) tên bài tập + thời gian tập từ đầu buổi, (2) GIF hướng dẫn, (3) dòng "Hiệp X - mục tiêu hiệp X", (4) một nút "HOÀN THÀNH". Không có form nhập liệu, không có nhiều nút.

**Why this priority**: Đây chính là yêu cầu cốt lõi của user — một cú chạm ghi xong một hiệp; mọi thứ khác là phụ trợ.

**Independent Test**: Bắt đầu buổi tập → thấy đúng layout 4 khối → chạm "HOÀN THÀNH" → hiệp được lưu, màn nghỉ hiện ra.

**Acceptance Scenarios**:

1. **Given** buổi tập đang active, **When** mở tab Hôm nay, **Then** hệ thống hiển thị đúng thứ tự: tên bài + thời lượng buổi → GIF → "Hiệp X - mục tiêu" → nút HOÀN THÀNH (bài rep) hoặc đồng hồ đếm ngược (bài tính giây).
2. **Given** bài hiện hành là bài tính rep, **When** User chạm "HOÀN THÀNH", **Then** hệ thống ghi hiệp với reps = mục tiêu hiệp hiện tại, tạ = tạ của hiệp liền trước cùng bài (nếu có), kiểu set = kiểu set của target hiệp trong kế hoạch, rồi chuyển màn nghỉ (trừ drop-set).
3. **Given** User chạm nhanh nhiều lần nút HOÀN THÀNH, **When** request đang gửi, **Then** hệ thống chỉ gửi đúng 1 request (double-tap guard, kế thừa FR-011 của 009).
4. **Given** đã ghi hết số hiệp mục tiêu, **When** User chạm HOÀN THÀNH lần nữa, **Then** hệ thống vẫn ghi hiệp phụ (tự do tập thêm).

---

### User Story 2 - Bài tính thời gian: tự đếm ngược & tự chuyển (Priority: P1)

Bài kiểu `duration` (plank, chạy bộ…) hiển thị đồng hồ đếm ngược thay cho nút HOÀN THÀNH. User bấm "Bắt đầu" rồi tập; hết giờ hệ thống tự ghi hiệp và tự chuyển màn nghỉ — không cần chạm thêm.

**Why this priority**: Khi đang plank/run, user không thể bấm nút — tự động là bắt buộc, không phải tiện ích.

**Independent Test**: Mở bài plank target 60s → bấm Bắt đầu → hết giờ thấy hiệp được ghi 60s → màn nghỉ tự hiện.

**Acceptance Scenarios**:

1. **Given** bài hiện hành kiểu `duration`, **When** hiển thị, **Then** hệ thống hiển thị đồng hồ đếm ngược prefilled bằng mục tiêu hiệp và nút "Bắt đầu" (khi đang chạy đổi thành "Tạm dừng").
2. **Given** đồng hồ chạy về 0, **When** kết thúc, **Then** hệ thống tự ghi hiệp với `durationSeconds` = số giây đã đếm và tự chuyển màn nghỉ.
3. **Given** User tạm dừng giữa chừng, **When** chạm "Lưu", **Then** hệ thống ghi đúng số giây đã đếm được rồi chuyển màn nghỉ.
4. **Given** target hiệp rỗng (0), **When** hiển thị, **Then** đồng hồ mặc định 60 giây.

---

### User Story 3 - Màn nghỉ lấy thời gian từ database (Priority: P1)

Sau mỗi hiệp (trừ drop-set), hệ thống hiển thị màn nghỉ với thời gian nghỉ lấy theo cấu hình `restTimeSeconds` của bài trong database, đếm ngược cỡ lớn, beep lúc 5 giây và 0 giây, nút "BỎ QUA".

**Why this priority**: Màn nghỉ là một phần của vòng lặp tập; thời gian nghỉ phải đúng cấu hình từng bài, không cứng 30 giây.

**Independent Test**: Bài có `restTimeSeconds = 90` → ghi hiệp xong thấy đếm ngược bắt đầu từ 90 → chạm "BỎ QUA" quay lại màn bài tập.

**Acceptance Scenarios**:

1. **Given** vừa lưu hiệp thành công (kiểu khác drop-set), **When** hiển thị, **Then** hệ thống hiển thị màn nghỉ đếm ngược bắt đầu từ `restTimeSeconds` của bài hiện hành trong database.
2. **Given** đếm ngược còn 5 giây và 0 giây, **When** đến mốc, **Then** hệ thống phát beep.
3. **Given** đang nghỉ, **When** User chạm "BỎ QUA", **Then** hệ thống dừng đồng hồ, tăng số hiệp lên 1 và trở về màn bài tập.
4. **Given** hiệp vừa lưu kiểu drop-set, **When** lưu xong, **Then** hệ thống KHÔNG mở màn nghỉ (nghỉ = 0, kế thừa FR-009 của 014).
5. **Given** hết giờ nghỉ, **When** đồng hồ về 0, **Then** hệ thống tự trở về màn bài tập.

---

### User Story 4 - Hàng đợi bài tập & bài kế tiếp (Priority: P2)

Màn tập trung có một dải chip ngang tối giản liệt kê các bài trong buổi: xong (✓), đang tập (highlight), sắp tới. Chạm chip để chuyển bài; hiển thị dòng nhỏ "Tiếp theo: [tên bài]".

**Why this priority**: User cần biết vị trí trong buổi tập và bài kế để chuẩn bị dụng cụ; không cần thiết cho hiệp đầu tiên nên xếp sau P1.

**Independent Test**: Buổi có 3 bài → thấy 3 chip → chạm chip 3 → màn chuyển sang bài 3, dòng "Tiếp theo" cập nhật.

**Acceptance Scenarios**:

1. **Given** buổi tập có nhiều bài, **When** hiển thị màn tập trung, **Then** hệ thống hiển thị chip từng bài với trạng thái xong/đang/tới.
2. **Given** User chạm chip bài khác, **When** chuyển bài, **Then** hệ thống chuyển bài hiện hành; số hiệp hiển thị theo số hiệp đã ghi của bài đó.
3. **Given** bài hiện hành chưa phải bài cuối, **When** hiển thị, **Then** hệ thống hiển thị "Tiếp theo: [tên bài]".

---

### User Story 5 - Sửa sai tối thiểu (Priority: P2)

Ghi nhầm vẫn sửa được: chạm vào dòng "Hiệp X - mục tiêu" mở mini editor để sửa reps/tạ của hiệp sắp ghi; sau khi ghi, có nút nhỏ "Hoàn tác" trong 60 giây để xóa hiệp vừa ghi.

**Why this priority**: Giữ màn hình tối giản (không hiện form) nhưng không đánh đổi tính đúng của dữ liệu lịch sử — sửa sai là lưới an toàn.

**Independent Test**: Chạm dòng mục tiêu → sửa 12 thành 10 → chạm HOÀN THÀNH → hiệp ghi 10 reps; chạm "Hoàn tác" trong 60 giây → hiệp biến mất.

**Acceptance Scenarios**:

1. **Given** màn tập trung đang hiển thị, **When** User chạm dòng "Hiệp X - mục tiêu", **Then** hệ thống mở mini editor (reps/tạ, hoặc mm:ss cho bài duration) đặt ngay dưới dòng đó.
2. **Given** mini editor đang mở, **When** User sửa giá trị rồi chạm HOÀN THÀNH, **Then** hiệp được ghi với giá trị đã sửa.
3. **Given** vừa ghi hiệp thành công, **When** trong 60 giây, **Then** hệ thống hiển thị nút "Hoàn tác" nhỏ; chạm vào sẽ xóa hiệp vừa ghi và lùi số hiệp về 1.
4. **Given** đã quá 60 giây từ lúc ghi, **When** hiển thị, **Then** nút "Hoàn tác" không hiển thị.
5. **Given** buổi tập đã kết thúc/expired, **When** gọi xóa hiệp, **Then** hệ thống từ chối với 409 Conflict.

---

### User Story 6 - Hướng dẫn trực quan (GIF) (Priority: P2)

Bài hiện hành hiển thị GIF/ảnh 180×180 từ kho media local; bài không có media hiển thị icon placeholder.

**Why this priority**: Hướng dẫn kỹ thuật trực quan là mục tiêu chính của sản phẩm (constitution §1); là khối thứ 2 trong layout user chỉ định.

**Independent Test**: Mở bài có GIF → thấy GIF; mở bài không có media → thấy icon placeholder.

**Acceptance Scenarios**:

1. **Given** bài hiện hành có `gifUrl`/`image`, **When** hiển thị, **Then** hệ thống hiển thị media 180×180 từ kho local.
2. **Given** bài không có media, **When** hiển thị, **Then** hệ thống hiển thị icon placeholder.

---

### Edge Cases

- User chuyển bài khi đang nghỉ → màn nghỉ tiếp tục chạy (nghỉ không phụ thuộc bài nào).
- User reload giữa buổi → khôi phục session + hiệp đã ghi từ API; đồng hồ buổi tính từ `startTime` (không reset về 0).
- Tab bị ẩn/nền trong lúc nghỉ hoặc tập → mọi đồng hồ dùng deadline tuyệt đối, không trôi giờ.
- Hiệp số vượt target (tập thêm) → vẫn ghi; mục tiêu hiệp phụ rỗng, dùng target chung của bài.
- Màn hình hẹp (< 360px) → vùng chạm chính vẫn ≥ 44px.
- Buổi tập expired sang ngày mới giữa chừng → lỗi 409 hiển thị "Buổi tập đã hết hạn vì sang ngày mới".
- Bài duration đang đếm mà user chuyển bài → đồng hồ tạm dừng và reset khi quay lại.
- Nút "Kết thúc buổi tập" vẫn phải có (có confirm); phân tâm được ghi TỰ ĐỘNG khi rời tab — không có nút thủ công.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: WHEN buổi tập đang active, THE client SHALL hiển thị màn tập trung tối giản theo đúng thứ tự: (1) tên bài tập + thời lượng buổi tính từ `startTime`, (2) GIF/media hướng dẫn, (3) dòng "Hiệp X - mục tiêu hiệp X", (4) một nút hành động chính; KHÔNG hiển thị form nhập liệu mặc định.
- **FR-002**: WHEN bài hiện hành kiểu `reps_weight`, THE client SHALL hiển thị nút "HOÀN THÀNH"; WHEN User chạm nút, THE hệ thống SHALL ghi hiệp với reps = mục tiêu hiệp hiện tại, tạ = tạ của hiệp liền trước cùng bài (nếu có, nếu không thì rỗng), kiểu set = kiểu set của target hiệp trong kế hoạch; nút SHALL có double-tap guard.
- **FR-003**: WHEN bài hiện hành kiểu `duration`, THE client SHALL hiển thị đồng hồ đếm ngược prefilled bằng mục tiêu hiệp (mặc định 60 giây nếu target rỗng) với nút Bắt đầu/Tạm dừng; WHEN đếm về 0, THE client SHALL tự ghi hiệp với `durationSeconds` = số giây đã đếm và tự chuyển màn nghỉ; WHEN tạm dừng, client SHALL hiển thị nút "Lưu" để ghi số giây đã đếm.
- **FR-004**: WHEN lưu hiệp thành công, WHERE kiểu set khác `drop_set`, THE client SHALL hiển thị màn nghỉ đếm ngược bắt đầu từ `restTimeSeconds` của bài (lấy từ database qua snapshot), cỡ chữ ≥ 2.5rem, beep tại 5s và 0s, nút "BỎ QUA"; WHERE kiểu set là `drop_set`, hệ thống SHALL không mở màn nghỉ.
- **FR-005**: Đồng hồ thời lượng buổi tập, đồng hồ nghỉ và đồng hồ bài duration SHALL tính theo deadline tuyệt đối (timestamp), KHÔNG đếm tăng/giảm dần; WHEN reload giữa buổi, client SHALL khôi phục trạng thái từ `GET /workout-sessions/active` và đồng hồ buổi tiếp tục từ `startTime`.
- **FR-006**: WHEN User chạm dòng "Hiệp X - mục tiêu", THE client SHALL mở mini editor (reps/tạ hoặc mm:ss) để sửa giá trị hiệp sắp ghi; giá trị đã sửa SHALL được dùng khi ghi.
- **FR-007**: WHEN User ghi hiệp thành công, THE client SHALL hiển thị nút "Hoàn tác" trong 60 giây; WHEN chạm hoàn tác, THE hệ thống SHALL xóa hiệp qua `DELETE /workout-sessions/{id}/sets/{setId}` với validation: session thuộc user và đang active; quá 60 giây client SHALL ẩn nút.
- **FR-008**: WHEN buổi tập có nhiều bài, THE client SHALL hiển thị dải chip hàng đợi (xong/đang/tới) + dòng "Tiếp theo: [tên bài]"; WHEN chạm chip, client SHALL chuyển bài hiện hành và hiển thị số hiệp theo số hiệp đã ghi của bài đó.
- **FR-009**: WHEN hiển thị bài hiện hành, THE client SHALL hiển thị media hướng dẫn (GIF/ảnh 180×180) từ kho local nếu có, nếu không SHALL hiển thị icon placeholder; API trả về `mediaUrl` cho từng bài trong session.
- **FR-010**: WHEN tab rời khỏi màn hình (visibility hidden) trong lúc buổi tập đang active, THE client SHALL tự động ghi nhận 1 lần phân tâm qua `POST /workout-sessions/{id}/focus-interruption` (không có nút thủ công); THE client SHALL giữ nút "Kết thúc buổi tập" (có confirm); khi kết thúc sớm, hệ thống SHALL gọi `POST /workout-sessions/{id}/complete` như hiện tại.

### Key Entities

- **WorkoutSet**: không đổi cấu trúc; bổ sung khả năng xóa (undo) với ràng buộc session active + sở hữu.
- **SessionExerciseResponse**: bổ sung `mediaUrl` (nullable) — không đổi bảng, chỉ join khi trả response.
- **UI components (web)**: `ExecutionScreen`, `RestOverlay`, `DurationTimer`, `MiniEditor`, `ExerciseQueueChips` — thuần frontend, thay thế form hiện tại của `WorkoutSession.tsx`.

## Success Criteria *(mandatory)*

- **SC-001**: Ghi 1 hiệp bài tính rep chỉ cần đúng 1 chạm (nút HOÀN THÀNH); bài tính giây KHÔNG cần chạm khi hết giờ.
- **SC-002**: Màn hình hiển thị tối đa 3 phần tử tương tác chính (nút chính, bỏ qua nghỉ, chuyển bài) — không có form dài.
- **SC-003**: Đồng hồ nghỉ và thời lượng buổi sai lệch < 1 giây sau khi tab bị ẩn 5 phút.
- **SC-004**: Thời gian nghỉ khởi tạo đúng `restTimeSeconds` của bài trong database cho 100% trường hợp.
- **SC-005**: 100% hiệp bài `duration` được tự ghi đúng số giây đã đếm khi hết giờ.
- **SC-006**: Không tạo hiệp trùng khi chạm nhanh nút HOÀN THÀNH (đúng 1 request mỗi lần lưu).
- **SC-007**: Vùng chạm chính (nút chính, bỏ qua, chip) ≥ 44×44px trên mọi breakpoint.

## Assumptions

- Chế độ tập trung THAY THẾ form hiện tại của `WorkoutSession.tsx` khi session active — không giữ 2 chế độ song song.
- Nút HOÀN THÀNH ghi reps đúng bằng mục tiêu (không đo thực tế); sai lệch sửa qua mini editor (US5) — đây là đánh đổi tối giản được chấp nhận.
- Tạ tự điền từ hiệp liền trước cùng bài trong buổi hiện tại; buổi đầu/không có hiệp trước thì để trống (user có thể mở mini editor để nhập).
- Màn nghỉ là overlay trong khu vực buổi tập (theo mockup HTML), không phải card in-flow; overlay không vượt ra ngoài container.
- Bố cục single-column, max-width 560px, căn giữa trên mọi breakpoint.
- Media dùng kho local `exercises-dataset/`; KHÔNG gọi external placeholder (mockup dùng via.placeholder chỉ để minh họa).
- Không đổi data model/DB — thay đổi backend giới hạn ở DTO (`mediaUrl`) + 1 endpoint xóa (undo).
- Kiểu set không có nút chọn trên màn tối giản — tự lấy từ target hiệp trong kế hoạch (kế thừa 014); nếu cần đổi, sửa trong tab Lịch tập.
