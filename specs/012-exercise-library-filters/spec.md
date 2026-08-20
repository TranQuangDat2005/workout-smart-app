# Feature Specification: Lọc thư viện bài tập theo Category & Equipment

**Feature Branch**: `012-exercise-library-filters`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Tận dụng cách filter trong exercises-dataset/index.html (trừ Target Muscle) cho thư viện bài tập: Category + Equipment đa chọn, search theo tên. Form tạo bài tập cá nhân dùng cùng bộ Category/Equipment; nhóm cơ 6 giá trị vẫn bắt buộc khi tạo bài (Rule Engine) nhưng không hiện chip lọc trên thư viện. Không làm buổi tập builder trong feature này."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lọc danh sách theo Category và Equipment (Priority: P1)

Người tập mở thư viện bài tập muốn thu hẹp kho 1324 bài theo vùng cơ thể (Category) và dụng cụ (Equipment) giống bộ lọc của kho dữ liệu gốc, nhưng không lọc theo cơ đích (Target Muscle) vì quá chi tiết so với nhu cầu tìm bài.

**Why this priority**: Đây là năng lực cốt lõi để tìm bài trong kho lớn; thiếu đúng taxonomy thì người tập không tìm được bài cáp, máy đòn bẩy, cardio máy…

**Independent Test**: Mở thư viện → chọn Category "Ngực" và Equipment "Tạ đơn" → chỉ còn bài khớp cả hai chiều; chọn thêm Category "Lưng" → bài ngực HOẶC lưng, vẫn phải khớp tạ đơn.

**Acceptance Scenarios**:

1. **Given** User đã đăng nhập và mở thư viện, **When** trang tải xong, **Then** hệ thống hiển thị danh sách bài mặc định (không bắt buộc nhập từ khóa) kèm đủ chip Category (10 giá trị kho dữ liệu) và Equipment (28 giá trị kho dữ liệu).
2. **Given** User chọn một hoặc nhiều chip Category, **When** kết quả cập nhật, **Then** chỉ bài có Category thuộc tập đã chọn xuất hiện; nhiều Category được kết hợp theo quan hệ HOẶC.
3. **Given** User chọn một hoặc nhiều chip Equipment, **When** kết quả cập nhật, **Then** chỉ bài có Equipment thuộc tập đã chọn xuất hiện; nhiều Equipment được kết hợp theo quan hệ HOẶC.
4. **Given** User đã chọn cả Category và Equipment, **When** kết quả cập nhật, **Then** bài phải thỏa cả hai chiều (VÀ giữa Category và Equipment).
5. **Given** User bỏ chọn hết chip một chiều, **When** kết quả cập nhật, **Then** chiều đó không còn ràng buộc (tương đương "tất cả").
6. **Given** User đang xem thư viện, **When** quan sát bộ lọc, **Then** hệ thống KHÔNG hiển thị chip nhóm cơ 6 giá trị và KHÔNG hiển thị chip Target Muscle.

---

### User Story 2 - Tìm theo tên kết hợp bộ lọc (Priority: P1)

Người tập nhớ một phần tên bài (ví dụ "press") và muốn kết hợp với Category/Equipment đã chọn để ra đúng biến thể.

**Why this priority**: Search tên là cách tìm nhanh nhất khi đã biết bài; phải hoạt động cùng filter chứ không thay thế filter.

**Independent Test**: Gõ "press" + chọn Equipment "Tạ đòn" → chỉ bài tên chứa "press" và dùng tạ đòn; đổi chữ → phân trang tính lại từ đầu.

**Acceptance Scenarios**:

1. **Given** User nhập từ khóa tên, **When** hệ thống áp dụng tìm kiếm, **Then** chỉ bài có tên chứa từ khóa (không phân biệt hoa thường) xuất hiện, đồng thời vẫn tuân thủ Category/Equipment đang chọn.
2. **Given** User đổi từ khóa hoặc đổi chip, **When** kết quả cập nhật, **Then** phân trang được tính lại từ trang đầu và tổng số bài khớp được hiển thị.
3. **Given** Không có bài nào khớp, **When** kết quả trả về, **Then** hệ thống hiển thị trạng thái trống hướng dẫn thử bộ lọc khác.

---

### User Story 3 - Form bài tập cá nhân dùng cùng taxonomy (Priority: P2)

Người tập tạo/sửa bài tập cá nhân cần gắn Category và Equipment đúng như kho hệ thống để bài đó lọc được trong thư viện; đồng thời vẫn chọn nhóm cơ 6 giá trị cho Rule Engine.

**Why this priority**: Nếu form vẫn chỉ 5 dụng cụ / không có Category, bài tự tạo biến mất khi lọc theo taxonomy mới.

**Independent Test**: Tạo bài "Kéo cáp 1 tay", Category Lưng, Equipment Cáp, nhóm cơ Lưng → bài hiện khi lọc Lưng + Cáp; không hiện khi lọc chỉ Tạ đơn.

**Acceptance Scenarios**:

1. **Given** User mở form tạo/sửa bài cá nhân, **When** form hiển thị, **Then** hệ thống yêu cầu chọn Category (đủ 10 giá trị kho), Equipment (đủ 28 giá trị kho) và nhóm cơ (6 giá trị Rule Engine).
2. **Given** User thiếu Category hoặc Equipment hoặc nhóm cơ hoặc tên, **When** User lưu, **Then** hệ thống từ chối kèm lỗi rõ và không lưu.
3. **Given** User đã lưu bài cá nhân với Category/Equipment hợp lệ, **When** User lọc thư viện theo đúng Category và Equipment đó, **Then** bài xuất hiện kèm đánh dấu "Của bạn".

---

### Edge Cases

- Điều gì xảy ra khi User chọn mọi chip Category (hoặc mọi Equipment) — kết quả phải tương đương không lọc chiều đó hoặc lọc đúng tập đầy đủ, không lỗi.
- Điều gì xảy ra khi từ khóa không khớp bài nào dù chưa chọn chip — trạng thái trống, không crash.
- Điều gì xảy ra khi bài hệ thống có Category/Equipment không nằm trong danh sách 10/28 (dữ liệu lệch) — bài vẫn tìm được bằng tên; không hiện khi lọc chip không khớp.
- Điều gì xảy ra khi bài cá nhân cũ chưa có Category — bài vẫn hiện khi không lọc Category; biến mất khi User đang lọc một Category cụ thể.
- Điều gì xảy ra khi User đăng xuất / token hết hạn khi đang lọc — xử lý auth như các màn khác (không lộ bài của người khác).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: WHEN User mở thư viện bài tập, THE hệ thống SHALL hiển thị danh sách bài mặc định kèm phân trang và bộ lọc Category + Equipment.
- **FR-002**: THE hệ thống SHALL cung cấp đúng 10 giá trị Category của kho dữ liệu bài tập: back, cardio, chest, lower arms, lower legs, neck, shoulders, upper arms, upper legs, waist — nhãn tiếng Việt trên giao diện.
- **FR-003**: THE hệ thống SHALL cung cấp đúng 28 giá trị Equipment của kho dữ liệu bài tập (dạng chuẩn hóa snake_case đã lưu trong kho): assisted, band, barbell, body_weight, bosu_ball, cable, dumbbell, elliptical_machine, ez_barbell, hammer, kettlebell, leverage_machine, medicine_ball, olympic_barbell, resistance_band, roller, rope, skierg_machine, sled_machine, smith_machine, stability_ball, stationary_bike, stepmill_machine, tire, trap_bar, upper_body_ergometer, weighted, wheel_roller — nhãn tiếng Việt trên giao diện.
- **FR-004**: WHEN User chọn nhiều Category, THE hệ thống SHALL lọc theo quan hệ HOẶC trong chiều Category.
- **FR-005**: WHEN User chọn nhiều Equipment, THE hệ thống SHALL lọc theo quan hệ HOẶC trong chiều Equipment.
- **FR-006**: WHEN User chọn cả Category và Equipment, THE hệ thống SHALL kết hợp hai chiều theo quan hệ VÀ.
- **FR-007**: WHEN User không chọn chip nào ở một chiều, THE hệ thống SHALL không ràng buộc chiều đó.
- **FR-008**: THE hệ thống SHALL KHÔNG hiển thị bộ lọc Target Muscle và KHÔNG hiển thị bộ lọc nhóm cơ 6 giá trị trên màn thư viện.
- **FR-009**: WHEN User nhập từ khóa tên, THE hệ thống SHALL lọc bài có tên chứa từ khóa (không phân biệt hoa thường) đồng thời với Category/Equipment đang chọn.
- **FR-010**: WHEN bộ lọc hoặc từ khóa thay đổi, THE hệ thống SHALL tính lại phân trang từ trang đầu và cập nhật tổng số bài khớp.
- **FR-011**: WHERE không có bài khớp, THE hệ thống SHALL hiển thị trạng thái trống.
- **FR-012**: WHEN User tạo hoặc sửa bài tập cá nhân, THE hệ thống SHALL bắt buộc tên, Category (một trong 10 giá trị), Equipment (một trong 28 giá trị) và nhóm cơ (một trong 6 giá trị: chest, back, shoulders, arms, legs, core).
- **FR-013**: Feature này SUPERSEDE `specs/008-workout-plan/spec.md` FR-013 đối với UI thư viện: lọc theo Category + Equipment, không còn bắt buộc chip nhóm cơ trên thư viện. Trường nhóm cơ 6 giá trị vẫn tồn tại trên bài tập (Rule Engine + form tự tạo).

### Key Entities

- **Exercise**: bài tập hệ thống hoặc cá nhân với Category (vùng cơ thể kho dữ liệu), Equipment (dụng cụ kho dữ liệu), nhóm cơ thô 6 giá trị (Rule Engine), tên, media, hướng dẫn.
- **LibraryFilter**: tập Category đang chọn + tập Equipment đang chọn + từ khóa tên; rỗng = không ràng buộc chiều đó.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% chip Category trên thư viện khớp đúng 10 giá trị kho dữ liệu; 100% chip Equipment khớp đúng 28 giá trị kho dữ liệu.
- **SC-002**: Khi chọn đồng thời ít nhất một Category và một Equipment, 100% bài trong kết quả thuộc Category đã chọn VÀ Equipment đã chọn.
- **SC-003**: Người tập thu hẹp được danh sách (đổi chip hoặc gõ tên) và thấy kết quả/trạng thái trống trong vòng 3 giây trên kết nối bình thường.
- **SC-004**: 100% bài tập cá nhân mới tạo có đủ Category, Equipment và nhóm cơ; bài đó xuất hiện khi lọc đúng Category + Equipment của chính nó.

## Assumptions

- Taxonomy lấy từ kho `exercises-dataset` hiện có; Category trùng `body_part` trong kho.
- Equipment trong kho đã được chuẩn hóa dấu cách thành gạch dưới (body_weight).
- Feature 013 (kéo-thả lộ trình / snapshot buổi tập) NẰM NGOÀI phạm vi này; picker Thêm bài trên lịch tập tái sử dụng cùng mô hình lọc/xem trước (xem `specs/013-manual-workout-builder/spec.md` US4).
- Không làm badge filter đang bật, nút xóa tất cả, nút “+N more”, layout sidebar của file HTML gốc.
- Form tạo bài cá nhân (011) được mở rộng taxonomy; không thay đổi quyền riêng tư bài custom.
- Goal Setup / Rule Engine vẫn dùng 5 dụng cụ rút gọn; không mở rộng auto-plan sang 28 dụng cụ trong feature này.
- Màn Admin quản lý bài tập không nằm trong phạm vi UI lần này.
