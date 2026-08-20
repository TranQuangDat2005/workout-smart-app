# Feature Specification: Tự xây lộ trình (template tuần + snapshot buổi tập)

**Feature Branch**: `013-manual-workout-builder`

**Created**: 2026-08-18

**Updated**: 2026-08-19

**Status**: Draft

**Input**: User description: "Tự xây = kéo-thả sửa template tuần trên màn Lộ trình; bấm Bắt đầu tập thì copy snapshot độc lập; sửa template không ảnh hưởng buổi đã start. dnd-kit. Set/rep khác nhau từng hiệp là P2. Picker Thêm bài phải giống thư viện: lọc Category/dụng cụ, xem GIF/hướng dẫn rồi mới chốt — không chỉ chọn theo tên."

## Problem (picker Thêm bài)

Người tập thêm bài vào một ngày lịch hiện chỉ gõ tên và bấm một dòng chữ. Kho có 1324 bài, nhiều biến thể gần giống nhau; không lọc được theo vùng cơ thể hay dụng cụ, không thấy động tác. Hệ quả: chọn nhầm bài, bỏ qua bài lạ, hoặc phải nhảy sang Thư viện rồi nhớ tên quay lại.

Thư viện bài tập (012) đã có chip Category + Equipment, tìm tên, thumbnail, GIF 180×180 và hướng dẫn. Picker Thêm bài phải tái sử dụng cùng mô hình đó: lọc nhanh, xem động tác, rồi xác nhận thêm vào ngày đang sửa.

“Nhóm cơ” trên picker = Category kho dữ liệu (10 vùng cơ thể, nhãn tiếng Việt), cùng bộ lọc thư viện — không phải 6 nhóm Rule Engine.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sửa template tuần trên Lộ trình (Priority: P1)

Người tập muốn tự thêm, xóa, đổi thứ tự, chỉnh số hiệp/lần/nghỉ của bài trong từng ngày của lộ trình đang hoạt động. Thay đổi này là mẫu lặp theo thứ trong tuần (mọi Thứ 2 tương lai dùng mẫu mới), không phải sửa một ngày lịch cụ thể.

**Why this priority**: Đây là năng lực “tự xây” — không có editor thì Rule Engine khóa cứng lịch.

**Independent Test**: Mở Lộ trình → Thứ 2 → thêm bài, kéo đổi thứ tự, sửa 3×10 → lưu → tải lại trang vẫn giữ.

**Acceptance Scenarios**:

1. **Given** User có lộ trình active, **When** User mở một ngày, **Then** hệ thống hiển thị danh sách bài có thể chỉnh (thứ tự, sets, reps, nghỉ).
2. **Given** User thêm bài từ thư viện (kể cả bài “Của bạn”), **When** lưu ngày đó, **Then** bài xuất hiện trong template ngày.
3. **Given** User xóa một bài khỏi ngày, **When** lưu, **Then** bài không còn trong template ngày đó; buổi đã start trước đó không đổi.
4. **Given** User kéo-thả đổi thứ tự, **When** lưu, **Then** thứ tự mới được giữ khi tải lại.
5. **Given** User không phải chủ lộ trình / không có plan active, **When** gọi sửa ngày, **Then** hệ thống từ chối.

---

### User Story 2 - Snapshot lúc bắt đầu tập (Priority: P1)

Người tập bấm Bắt đầu: hệ thống copy bài + sets/reps/nghỉ của **ngày trong tuần hôm nay** từ template vào buổi tập. Sửa template sau đó không làm đổi buổi đang/đã tập.

**Why this priority**: Đây là yêu cầu “sửa buổi lặp không ảnh hưởng buổi trước”.

**Independent Test**: Bắt đầu tập Thứ 2 → snapshot có bài A → sửa template Thứ 2 thành bài B → buổi đang active vẫn bài A.

**Acceptance Scenarios**:

1. **Given** Hôm nay là một ngày có trong template, **When** User bắt đầu buổi tập, **Then** buổi chứa bản sao bài/sets/reps/nghỉ đúng thứ tự template ngày đó.
2. **Given** Buổi tập đã start, **When** User sửa template ngày đó, **Then** snapshot buổi đang active không đổi.
3. **Given** Hôm nay không có ngày trong template (ngày nghỉ), **When** User bắt đầu tập, **Then** buổi vẫn được tạo với danh sách bài rỗng (có thể ghi hiệp tự do nếu hệ thống cho phép) hoặc thông báo ngày nghỉ — mặc định: buổi tạo với snapshot rỗng, User vẫn kết thúc được.
4. **Given** User đã có buổi `active`, **When** mở màn tập, **Then** hệ thống cho tiếp tục buổi đó (không tạo mới).

---

### User Story 3 - Ghi hiệp theo từng bài trong snapshot (Priority: P1)

Trong buổi tập, người tập chọn từng bài của snapshot và ghi hiệp (reps/tạ) theo số hiệp mục tiêu của bài đó. Hiệp của bài A không đè hiệp bài B.

**Why this priority**: Tracking hiện ghi một dải hiệp chung cả buổi — không dùng được với nhiều bài.

**Acceptance Scenarios**:

1. **Given** Snapshot có ≥ 2 bài, **When** User lưu hiệp 1 của bài A rồi hiệp 1 của bài B, **Then** cả hai hiệp được lưu độc lập.
2. **Given** User lưu lại cùng số hiệp của cùng bài, **When** gửi lần hai, **Then** hệ thống cập nhật hiệp đó (Last-Write-Wins), không tạo bản ghi trùng.

---

### User Story 4 - Picker Thêm bài giống thư viện (Priority: P1)

Người tập mở “Thêm bài” trên lịch tập muốn thu hẹp kho theo vùng cơ thể và dụng cụ, xem thumbnail + GIF/hướng dẫn của bài đang chọn, rồi mới bấm thêm vào ngày — cùng cách dùng Thư viện bài tập.

**Why this priority**: Không xem được động tác thì “tự xây” chỉ là chọn tên mù; đây là chỗ người tập quyết định bài nào vào template.

**Independent Test**: Lịch tập → Thêm bài → chọn Category “Ngực” + dụng cụ “Tạ đơn” → danh sách chỉ bài khớp → bấm một bài → thấy GIF/ảnh và hướng dẫn → bấm Thêm vào ngày này → bài xuất hiện trên ngày đang sửa.

**Acceptance Scenarios**:

1. **Given** User mở “Thêm bài” trên một ngày template, **When** hộp chọn hiện ra, **Then** hệ thống hiển thị ô tìm theo tên, chip Category (đủ 10 giá trị kho), chip Equipment (đủ 28 giá trị kho), danh sách bài có thumbnail/tên/nhãn, và khung xem trước (trống cho đến khi chọn một bài).
2. **Given** User chọn một hoặc nhiều chip Category và/hoặc Equipment, **When** kết quả cập nhật, **Then** quy tắc lọc giống thư viện: HOẶC trong cùng chiều, VÀ giữa Category và Equipment; không chọn chip = không ràng buộc chiều đó.
3. **Given** User nhập từ khóa tên, **When** kết quả cập nhật, **Then** chỉ bài có tên chứa từ khóa (không phân biệt hoa thường) đồng thời tuân thủ chip đang chọn; phân trang/tải thêm tính lại từ đầu.
4. **Given** User chọn một bài trong danh sách, **When** khung xem trước cập nhật, **Then** hệ thống hiển thị tên, nhãn Category/dụng cụ, ảnh động GIF (nếu không có thì ảnh tĩnh; nếu không có media thì chỗ trống), và hướng dẫn nếu có — **chưa** thêm bài vào ngày.
5. **Given** User đang xem trước một bài, **When** User xác nhận thêm, **Then** bài được thêm vào template ngày đang chọn và hộp chọn đóng lại.
6. **Given** Ngày đã đủ 15 bài, **When** User xác nhận thêm, **Then** hệ thống từ chối kèm thông báo, không thêm.
7. **Given** User quan sát bộ lọc picker, **When** so với thư viện, **Then** không có chip nhóm cơ 6 giá trị, không có chip Target Muscle, và không có form tạo/sửa/xóa bài cá nhân trong picker.

---

### Edge Cases

- Template ngày trống rồi start → snapshot rỗng.
- Bài trong template bị Admin ẩn sau khi snapshot → buổi đang tập vẫn hiện tên đã copy.
- Đổi goal (archive plan) trong lúc session active → snapshot buổi không bị xóa.
- Kéo-thả trên thiết bị cảm ứng web vẫn đổi được thứ tự.
- Picker không có bài khớp bộ lọc → trạng thái trống, không crash.
- Bài không có GIF → hiện ảnh tĩnh; không có cả hai → chỗ trống, vẫn cho thêm.
- Màn hẹp (điện thoại) → danh sách và khung xem trước xếp dọc, vẫn lọc và thêm được.
- Đóng picker giữa chừng → không thêm bài; mở lại thì bộ lọc/tìm kiếm bắt đầu lại từ đầu.

## Requirements *(mandatory)*

- **FR-001**: THE hệ thống SHALL cho phép chủ sở hữu sửa danh sách bài của từng ngày trong lộ trình active: thêm, xóa, đổi thứ tự, chỉnh target sets/reps/thời gian nghỉ (một số reps cho mọi hiệp).
- **FR-002**: WHEN User lưu một ngày template, THE hệ thống SHALL thay thế toàn bộ bài của ngày đó theo thứ tự đã gửi; buổi tập đã start KHÔNG bị ghi đè.
- **FR-003**: WHEN User bắt đầu buổi tập, THE hệ thống SHALL copy bài của ngày trong tuần (giờ địa phương) từ template active thành snapshot thuộc buổi đó.
- **FR-004**: WHERE User đã có buổi active, THE hệ thống SHALL không tạo buổi mới (409 như hiện tại) và SHALL cho phép tiếp tục buổi cũ kèm snapshot.
- **FR-005**: THE hệ thống SHALL ghi hiệp theo (buổi, bài trong snapshot, số hiệp); UPSERT Last-Write-Wins trên khóa đó.
- **FR-006**: THE hệ thống SHALL cho phép kéo-thả đổi thứ tự bài trên màn Lộ trình (web).
- **FR-007**: Feature này KHÔNG materialize từng ngày lịch; KHÔNG hỗ trợ “chỉ hôm nay” trên màn Lộ trình; KHÔNG làm target reps khác nhau từng hiệp (P2).
- **FR-008**: WHEN User mở “Thêm bài” trên lịch tập, THE hệ thống SHALL hiển thị cùng mô hình chọn bài như thư viện: tìm theo tên, chip Category (10 giá trị kho), chip Equipment (28 giá trị kho), danh sách có thumbnail, khung xem trước GIF/ảnh + hướng dẫn.
- **FR-009**: WHEN User chọn chip Category/Equipment trên picker, THE hệ thống SHALL lọc theo cùng quy tắc thư viện (HOẶC trong chiều, VÀ giữa hai chiều; không chọn = không ràng buộc chiều đó).
- **FR-010**: WHEN User chọn một bài trên picker, THE hệ thống SHALL hiển thị động tác (GIF, fallback ảnh tĩnh) và hướng dẫn trước khi thêm; việc chọn xem trước KHÔNG thêm bài vào ngày.
- **FR-011**: WHEN User xác nhận thêm bài từ picker, THE hệ thống SHALL thêm bài vào template ngày đang chọn (tuân thủ tối đa 15 bài/ngày) rồi đóng picker.
- **FR-012**: THE hệ thống SHALL KHÔNG hiển thị chip nhóm cơ 6 giá trị, chip Target Muscle, hay form tạo/sửa/xóa bài cá nhân bên trong picker Thêm bài (tạo bài cá nhân vẫn ở thư viện).

### Key Entities

- **WorkoutPlan / PlanDay / PlanExercise**: template tuần (PlanExercise có thứ tự).
- **WorkoutSessionExercise**: snapshot bài của một buổi.
- **WorkoutSet**: hiệp thực tế gắn snapshot bài.
- **ExercisePickerFilter**: cùng mô hình LibraryFilter (012) — tập Category + tập Equipment + từ khóa tên.

## Success Criteria *(mandatory)*

- **SC-001**: User thêm/xóa/đổi thứ tự/sửa sets-reps một ngày template và thấy kết quả giữ sau khi tải lại trang.
- **SC-002**: 100% buổi start vào ngày có template nhận đủ bài snapshot đúng thứ tự tại thời điểm start.
- **SC-003**: Sau khi start, sửa template không làm đổi số bài/tên/sets mục tiêu của buổi đang active.
- **SC-004**: Lưu hiệp 1 của hai bài khác nhau trong cùng buổi không ghi đè nhau.
- **SC-005**: User lọc Category hoặc Equipment (hoặc gõ tên) trong picker Thêm bài và thấy danh sách khớp hoặc trạng thái trống trong vòng 3 giây trên kết nối bình thường.
- **SC-006**: Trước khi bài được thêm vào ngày, User nhìn thấy động tác của bài đang chọn (GIF hoặc ảnh tĩnh) trừ khi bài không có media.
- **SC-007**: 100% chip Category/Equipment trên picker khớp đúng 10/28 giá trị kho, giống thư viện.

## Assumptions

- Rule Engine vẫn sinh template ban đầu; builder chỉ sửa template đó.
- Picker tái sử dụng cùng nguồn bài và quy tắc lọc của thư viện (012), gồm bài custom của user.
- “Nhóm cơ” mà người dùng muốn lọc trên picker = Category kho dữ liệu (10 vùng), không mở chip 6 nhóm Rule Engine.
- Tối đa 15 bài/ngày template.
- Flutter DnD và picker mobile ngoài phạm vi lần này.
- Không thêm API mới: tìm kiếm/chi tiết bài tập hiện có đủ cho picker.
