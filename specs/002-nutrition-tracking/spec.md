# Feature Specification: Dinh dưỡng & Theo dõi Calories

**Feature Branch**: `002-nutrition-tracking`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "UC-12: Ghi nhận bữa ăn & tính calories; UC-13: Theo dõi chỉ số cơ thể theo thời gian — Nhóm 5: Dinh dưỡng / Theo dõi calories"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ghi nhận bữa ăn & tính calories (Priority: P1)

Người dùng đã đăng nhập muốn ghi nhận bữa ăn hàng ngày (bữa được đánh số "Bữa 1, Bữa 2, ..., Bữa n"), chọn nhiều món cho một bữa, tìm kiếm thực phẩm trong database, nhập khẩu phần từng món và để hệ thống tự động tính tổng calo + macro (protein, carb, fat) của từng món và cả bữa. Hệ thống cập nhật tổng calo tiêu thụ trong ngày và so sánh với mục tiêu calo (theo cutting/bulking/tùy chỉnh).

**Why this priority**: Tính năng dinh dưỡng là yếu tố quyết định giúp User đạt mục tiêu (giảm cân cần thâm hụt calo, tăng cơ cần thặng dư calo). Đây là tính năng bổ trợ trực tiếp cho lộ trình tập luyện.

**Independent Test**: Có thể kiểm thử bằng cách nhấn "Thêm bữa ăn" → chọn "Bữa 1" → tìm "Trứng luộc" → nhập 100g → thêm món "Cơm trắng" → nhập 200g → xác nhận calo và macro từng món hiển thị đúng → lưu → tổng calo ngày cập nhật.

**Acceptance Scenarios**:

1. **Given** User đã đăng nhập, **When** User nhấn "Thêm bữa ăn", **Then** hệ thống hiển thị giao diện chọn số bữa trong ngày (mặc định "Bữa 1, Bữa 2, Bữa 3"; User có thể thêm "Bữa 4", ... nếu ăn nhiều hơn) và ô tìm kiếm thực phẩm để thêm nhiều món vào bữa đã chọn.
2. **Given** User đã chọn "Bữa 1", **When** User tìm kiếm "Cơm trắng", **Then** hệ thống trả kết quả từ database thực phẩm gồm: tên, calo/100g, protein, carb, fat.
3. **Given** User đã chọn nhiều món cho bữa ăn, **When** User nhập khẩu phần từng món (ví dụ món A 200g, món B 100g), **Then** hệ thống tự động tính calo = (calo/100g × gram/100) cho từng món, hiển thị macro tương ứng và cộng dồn tổng calo/macro của cả bữa.
4. **Given** User đã thêm ít nhất 1 món cho bữa ăn, **When** User nhấn xác nhận, **Then** hệ thống lưu bữa ăn (header + danh sách từng món) vào nhật ký ngày hôm đó và cập nhật tổng calo tiêu thụ.
5. **Given** User muốn sửa bữa ăn đã ghi nhận, **When** User thay đổi khẩu phần, thêm/bớt món hoặc đổi thực phẩm khác, **Then** hệ thống lưu đè bản ghi cũ và tự động tính lại tổng calo tiêu thụ trong ngày. (KHÔNG cho phép xóa bữa ăn).
6. **Given** User đã ghi nhận bữa ăn trong ngày, **When** User xem tổng quan ngày, **Then** hệ thống hiển thị tổng calo tiêu thụ so với mục tiêu calo theo goal (cutting/bulking/tùy chỉnh) (thanh tiến trình / biểu đồ vòng).
7. **Given** User tìm kiếm thực phẩm không có trong kho chung, **When** không tìm thấy kết quả, **Then** hệ thống cho phép User tạo thực phẩm mới và lưu vào kho cá nhân.

---

### User Story 2 - Theo dõi chỉ số cơ thể theo thời gian (Priority: P1)

Người dùng muốn ghi nhận các chỉ số cơ thể (cân nặng bắt buộc, tùy chọn: % mỡ, vòng eo, vòng ngực, vòng tay) theo thời gian để theo dõi xu hướng thay đổi. Hệ thống tính chênh lệch so với lần nhập gần nhất và hiển thị xu hướng tăng/giảm.

**Why this priority**: Dữ liệu chỉ số cơ thể là đầu vào trực tiếp cho UC-17 (Thống kê & Báo cáo) và giúp User nhìn thấy kết quả tập luyện qua thời gian — yếu tố duy trì động lực cao nhất.

**Independent Test**: Có thể kiểm thử bằng cách nhập cân nặng 75 kg hôm nay → nhập 74.5 kg ngày mai → xác nhận hệ thống hiển thị xu hướng giảm 0.5 kg.

**Acceptance Scenarios**:

1. **Given** User đã đăng nhập, **When** User nhấn "Cập nhật chỉ số", **Then** hệ thống hiển thị form nhập gồm: cân nặng (bắt buộc), % mỡ (tùy chọn), vòng eo/ngực/tay (tùy chọn).
2. **Given** User đã nhập cân nặng 74.5 kg, **When** User nhấn lưu, **Then** hệ thống lưu bản ghi kèm timestamp, đồng bộ mức cân nặng mới lên hồ sơ User (bảng `users`), và hiển thị chênh lệch so với lần nhập gần nhất.
3. **Given** User đã có nhiều bản ghi chỉ số, **When** User xem lịch sử chỉ số, **Then** hệ thống hiển thị xu hướng tăng/giảm (mũi tên lên/xuống + số chênh lệch).
4. **Given** User chưa có bản ghi chỉ số nào, **When** User nhập lần đầu, **Then** hệ thống lưu thành công và thông báo "Đây là lần đo đầu tiên, hãy cập nhật thường xuyên!".

---

### User Story 3 - Tạo & tái sử dụng đồ ăn trong kho cá nhân (Priority: P2)

Người dùng không tìm thấy thực phẩm trong kho chung (ví dụ món ăn địa phương, món tự chế biến) muốn tự định nghĩa một thực phẩm mới với đầy đủ thông tin dinh dưỡng (tên, calo, protein, carb, fat trên 100g) và lưu vào kho cá nhân của mình. Lần sau, thực phẩm này xuất hiện trong kết quả tìm kiếm cùng với thực phẩm từ kho chung — User chỉ cần gọi tên và nhập khối lượng, hệ thống tự động tính calo và macro.

**Why this priority**: Kho chung không thể bao phủ mọi món ăn (đặc biệt món Việt và món tự nấu). Kho cá nhân giảm ma sát nhập liệu hằng ngày — món quen chỉ nhập định nghĩa một lần, tái sử dụng mãi. P2 vì việc ghi nhận bữa ăn vẫn hoạt động được chỉ với kho chung.

**Independent Test**: Có thể kiểm thử bằng cách: thêm mới "Cơm tấm sườn" với đầy đủ macro → tìm kiếm "Cơm tấm sườn" → thực phẩm hiển thị từ kho cá nhân → chọn và nhập 200g → hệ thống tính đúng calo/macro tương ứng.

**Acceptance Scenarios**:

1. **Given** User đang thêm bữa ăn và không tìm thấy thực phẩm trong kho chung, **When** User chọn "Tạo thực phẩm mới", **Then** hệ thống hiển thị form nhập: tên, calo/100g, protein/100g, carb/100g, fat/100g.
2. **Given** User đã nhập đầy đủ thông tin hợp lệ, **When** User lưu, **Then** hệ thống lưu thực phẩm vào kho cá nhân của User và có thể sử dụng ngay trong bữa ăn hiện tại.
3. **Given** User đã có thực phẩm trong kho cá nhân, **When** User tìm kiếm tên thực phẩm đó, **Then** hệ thống trả về kết quả hợp nhất từ kho chung và kho cá nhân (kết quả từ kho cá nhân được ưu tiên hiển thị).
4. **Given** User muốn chỉnh sửa thực phẩm trong kho cá nhân, **When** User sửa thông tin và lưu, **Then** hệ thống cập nhật định nghĩa mới; các bữa ăn đã ghi trước đó giữ nguyên giá trị đã tính.
5. **Given** User muốn xóa thực phẩm trong kho cá nhân, **When** User xóa, **Then** hệ thống soft-delete thực phẩm (cột `deleted_at`): thực phẩm ẩn khỏi kết quả tìm kiếm và không dùng được cho bữa ăn mới, nhưng tên vẫn hiển thị trong lịch sử bữa ăn cũ; sau 1 tuần hệ thống xóa cứng bản ghi, không ảnh hưởng kho chung hay người dùng khác.

---

### Edge Cases

- Điều gì xảy ra khi User nhập khẩu phần 0 gram hoặc giá trị âm?
- Điều gì xảy ra khi database thực phẩm trống hoặc chưa được seed?
- Điều gì xảy ra khi User ăn nhiều hơn 3 bữa trong ngày (thêm "Bữa 4", "Bữa 5")? Sang hôm sau hệ thống có reset về mặc định 3 bữa không?
- Điều gì xảy ra khi User nhập cân nặng thay đổi bất thường (ví dụ từ 70 kg xuống 30 kg)?
- Điều gì xảy ra khi User tạo thực phẩm tùy chỉnh nhưng bỏ trống protein/carb/fat?
- Điều gì xảy ra khi User tạo thực phẩm trùng tên với thực phẩm trong kho chung?
- Điều gì xảy ra khi User xóa thực phẩm tùy chỉnh đã từng dùng trong nhật ký bữa ăn?
- Điều gì xảy ra khi User lưu bữa ăn không có món nào (bữa trống)?
- Điều gì xảy ra khi User nhấn nhanh nhiều lần nút lưu bữa ăn?
- Điều gì xảy ra khi 2 thiết bị cùng sửa một bữa ăn?
- Điều gì xảy ra khi hồ sơ User thiếu cân nặng/chiều cao/tuổi/giới tính/mức vận động để tính TDEE?
- Điều gì xảy ra khi User xem lịch sử bữa ăn cũ hơn 2 tuần (chi tiết món đã bị xóa theo retention)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: WHEN User nhấn "Thêm bữa ăn", hệ thống PHẢI hiển thị giao diện chọn số bữa trong ngày (mặc định "Bữa 1, Bữa 2, Bữa 3"; User có thể thêm bữa mới nếu ăn nhiều hơn, sang hôm sau reset về mặc định 3 bữa), cho phép chọn NHIỀU món cho một bữa và tìm kiếm thực phẩm.
- **FR-002**: Hệ thống PHẢI tìm kiếm thực phẩm trong kho chung VÀ kho cá nhân của User, trả kết quả gồm: tên, calo/100g, protein, carb, fat, nguồn gốc (kho chung / kho cá nhân).
- **FR-003**: Hệ thống PHẢI tự động tính calo và macro của từng món dựa trên khẩu phần (gram) mà User nhập, đồng thời cộng dồn tổng calo/macro của cả bữa.
- **FR-004**: Hệ thống PHẢI lưu bữa ăn vào nhật ký dinh dưỡng theo ngày gồm bản ghi header `meal_logs` (id, user_id, meal_number, log_date, created_at) và các bản ghi món `meal_entries` (id, meal_log_id, food_item_id, portion_grams, total_calories, total_protein, total_carb, total_fat), sau đó cập nhật tổng calo tiêu thụ.
- **FR-005**: Hệ thống PHẢI so sánh tổng calo tiêu thụ trong ngày với mục tiêu calo của User (theo goal: cutting/bulking hoặc tùy chỉnh) và hiển thị trực quan (thặng dư / thâm hụt).
- **FR-006**: WHEN không tìm thấy thực phẩm trong kho chung, hệ thống PHẢI cho phép User tự định nghĩa thực phẩm mới với đầy đủ calo và macro (protein, carb, fat trên 100g), lưu vào kho cá nhân để tái sử dụng.
- **FR-006b**: WHEN User muốn sửa bữa ăn, hệ thống PHẢI cho phép ghi đè khẩu phần từng món, thêm/bớt món hoặc đổi thực phẩm, sau đó tự động tính toán lại calo/macro tổng (Không hỗ trợ xóa cứng bữa ăn).
- **FR-006c**: Hệ thống PHẢI cho phép User chỉnh sửa hoặc xóa thực phẩm do chính mình tạo trong kho cá nhân; User KHÔNG được sửa/xóa thực phẩm thuộc kho chung. WHEN User xóa thực phẩm custom, hệ thống PHẢI soft-delete (cột `deleted_at`): thực phẩm ẩn khỏi tìm kiếm, KHÔNG dùng được cho bữa ăn mới nhưng tên vẫn hiển thị trong lịch sử bữa ăn cũ; sau 1 tuần, hệ thống PHẢI xóa cứng bản ghi cùng các bản ghi liên quan.
- **FR-007**: WHEN User nhấn "Cập nhật chỉ số", hệ thống PHẢI cho phép nhập cân nặng (bắt buộc) và các chỉ số tùy chọn (% mỡ, vòng eo, vòng ngực, vòng tay).
- **FR-008**: Hệ thống PHẢI lưu mỗi bản ghi chỉ số cơ thể kèm timestamp chính xác. Cân nặng mới nhất PHẢI được đồng bộ tự động sang hồ sơ người dùng (`users.weight_kg`).
- **FR-009**: Hệ thống PHẢI tính chênh lệch giữa bản ghi mới nhất và bản ghi gần nhất trước đó, hiển thị xu hướng tăng/giảm.
- **FR-010**: Hệ thống PHẢI cung cấp dữ liệu chỉ số cơ thể dạng chuỗi thời gian để phục vụ UC-17 (Thống kê & Báo cáo).
- **FR-011**: Hệ thống PHẢI giữ chi tiết từng món (`meal_entries`) cho tuần hiện tại và tuần liền trước. Đối với các tuần cũ hơn, hệ thống PHẢI xóa chi tiết món khỏi database và chỉ giữ bản tổng kết ngày trong bảng `meal_daily_summaries` (mỗi ngày: tổng calo của từng bữa Bữa 1/2/.../n, không còn chi tiết món).
- **FR-012**: Hệ thống PHẢI tính TDEE = BMR theo công thức Mifflin-St Jeor × hệ số vận động do User chọn (Ít vận động 1.2 / Nhẹ 1.375 / Trung bình 1.55 / Tích cực 1.725 / Rất tích cực 1.9), dựa trên giới tính (`sex`) và mức vận động (`activity_level`) của User. Mục tiêu calo mặc định theo goal: cutting = TDEE − 15~20%, bulking = TDEE + 10~15%, hoặc User tùy chỉnh mức thâm hụt/thặng dư.
- **FR-013**: WHEN thiếu thông số để tính TDEE (cân nặng, chiều cao, tuổi, giới tính, mức vận động), hệ thống PHẢI yêu cầu User nhập đủ trước khi hiển thị so sánh calo.
- **FR-014**: WHEN User nhấn nhanh nhiều lần nút lưu bữa ăn, client PHẢI chỉ gửi đúng 1 request (guard trạng thái "đang gửi") để tránh tạo bản ghi trùng lặp.
- **FR-015**: WHEN 2 thiết bị sửa cùng một bữa ăn, hệ thống PHẢI áp dụng chiến lược Last-Write-Wins: request được server xử lý sau cùng quyết định giá trị lưu; hệ thống KHÔNG hiển thị cảnh báo conflict.

### Key Entities

- **Food Item**: Thực phẩm trong kho (tên, calo/100g, protein, carb, fat, nguồn gốc). Gồm 2 nguồn: kho chung (system) và kho cá nhân của từng User (user_custom). Hỗ trợ tìm kiếm theo tên. Thực phẩm custom hỗ trợ soft-delete qua cột `deleted_at` (ẩn khỏi tìm kiếm, giữ tên cho lịch sử bữa ăn cũ; xóa cứng sau 1 tuần).
- **Meal Log** (`meal_logs` — header bữa ăn): Bản ghi bữa ăn của User trong ngày (id, user_id, meal_number, log_date, created_at). Bữa được đánh số "Bữa 1, Bữa 2, ..., Bữa n" trong ngày — không dùng loại bữa sáng/trưa/tối/snack.
- **Meal Entry** (`meal_entries`): Từng món trong bữa ăn (id, meal_log_id, food_item_id, portion_grams, total_calories, total_protein, total_carb, total_fat). Liên kết n-1 với Meal Log.
- **Meal Daily Summary** (`meal_daily_summaries`): Bản tổng kết dinh dưỡng theo ngày (mỗi ngày: tổng calo của từng bữa Bữa 1/2/.../n, không còn chi tiết món). Dùng cho dữ liệu cũ hơn 2 tuần sau khi chi tiết món bị xóa.
- **Body Metric Record**: Bản ghi chỉ số cơ thể (cân nặng, % mỡ, vòng eo/ngực/tay, timestamp). Liên kết n-1 với User.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User có thể ghi nhận 1 bữa ăn hoàn chỉnh (chọn bữa → chọn nhiều món → nhập khẩu phần từng món → xác nhận) trong vòng 30 giây.
- **SC-002**: 100% bữa ăn được lưu chính xác và tổng calo ngày được cập nhật tức thì sau mỗi bữa.
- **SC-003**: Hệ thống hiển thị xu hướng chỉ số cơ thể chính xác (tăng/giảm) so với lần nhập gần nhất trong 100% trường hợp.
- **SC-004**: User có thể cập nhật chỉ số cơ thể trong vòng 15 giây (mở form → nhập cân nặng → lưu).
- **SC-005**: User có thể tạo một thực phẩm tùy chỉnh và dùng ngay trong bữa ăn trong vòng 60 giây; 100% thực phẩm tùy chỉnh chỉ hiển thị cho chính người tạo.

## Assumptions

- Database thực phẩm được seed sẵn với ít nhất vài trăm thực phẩm phổ biến (Việt Nam + quốc tế).
- Thực phẩm trong kho cá nhân là private — chỉ hiển thị cho chính User tạo ra, không chia sẻ cho người dùng khác trong phase này.
- Khi tạo thực phẩm tùy chỉnh, calo và 3 macro (protein, carb, fat) là bắt buộc nhập để đảm bảo tính toán dinh dưỡng chính xác.
- TDEE = BMR theo công thức Mifflin-St Jeor × hệ số vận động do User chọn (Ít vận động 1.2 / Nhẹ 1.375 / Trung bình 1.55 / Tích cực 1.725 / Rất tích cực 1.9). Bảng `users` đã có 2 cột `sex` (giới tính) và `activity_level` (mức vận động) phục vụ tính TDEE.
- Mặc định mỗi ngày 3 bữa ("Bữa 1, Bữa 2, Bữa 3"); ngày nào ăn nhiều hơn thì User tự thêm bữa; sang hôm sau hệ thống reset về mặc định 3 bữa.
- Chi tiết từng món (`meal_entries`) chỉ được giữ cho tuần hiện tại và tuần liền trước; dữ liệu cũ hơn bị xóa chi tiết món và chỉ giữ tổng kết ngày trong `meal_daily_summaries` (tổng calo từng bữa, không chi tiết macro).
- Thực phẩm custom khi bị xóa sẽ ở trạng thái soft-delete (`deleted_at`) trong 1 tuần để giữ tên hiển thị cho lịch sử bữa ăn cũ, sau đó bị xóa cứng cùng bản ghi liên quan.
- Chỉ số cơ thể không cần nhập hàng ngày — User tự quyết định tần suất.
