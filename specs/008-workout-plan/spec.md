# Feature Specification: Thiết lập mục tiêu & Tạo lộ trình tập

**Feature Branch**: `008-workout-plan`

**Created**: 2026-08-17

**Status**: Ready

**Input**: User description: "UC-04: Thiết lập mục tiêu; UC-05: Tạo Workout Plan (Rule-based); UC-06: Xem chi tiết bài tập — Nhóm 2: Workout Plan"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Thiết lập mục tiêu cá nhân (Priority: P1)

Người dùng mới (sau đăng ký) muốn khai báo mục tiêu tập luyện (giảm cân / tăng cơ / sức bền) và thiết bị sẵn có để hệ thống hiểu nhu cầu và đưa ra lộ trình phù hợp.

**Why this priority**: Mục tiêu là đầu vào bắt buộc cho toàn bộ quá trình cá nhân hóa. Không có mục tiêu, hệ thống không thể tạo lộ trình (UC-05).

**Independent Test**: Có thể kiểm thử bằng cách chọn mục tiêu "Giảm cân" và thiết bị "Body weight" → xác nhận lựa chọn được lưu và chuyển sang bước tạo lộ trình.

**Acceptance Scenarios**:

1. **Given** Người dùng mới hoàn tất đăng ký, **When** người dùng chọn mục tiêu (giảm cân/tăng cơ/sức bền) và thiết bị sẵn có, **Then** hệ thống lưu lựa chọn vào hồ sơ và kích hoạt tạo lộ trình.
2. **Given** Người dùng bỏ qua bước thiết lập mục tiêu, **When** người dùng tiếp tục, **Then** hệ thống nhắc hoàn tất thiết lập mục tiêu trước khi sinh lộ trình.
3. **Given** Người dùng muốn thay đổi mục tiêu sau này, **When** người dùng sửa goal_type trong hồ sơ, **Then** hệ thống cảnh báo việc archive plan cũ sẽ làm mất tiến trình tập của ngày hôm nay và đưa ra 2 lựa chọn: tạo plan mới NGAY, hoặc để plan mới bắt đầu từ NGÀY MAI.

---

### User Story 2 - Tạo lộ trình tập (Rule-based) (Priority: P1)

Dựa trên mục tiêu (goal_type), trình độ (fitness_level) và thiết bị sẵn có (equipment), hệ thống tự động sinh một lộ trình tập (Workout Plan) phân bổ bài tập vào các ngày trong tuần theo bảng luật Rule Engine v1 — goal_type quyết định số ngày/tuần, cấu trúc, số reps và thời gian nghỉ; fitness_level quyết định khối lượng; equipment lọc tập bài — không cần người dùng tự lập kế hoạch.

**Why this priority**: Đây là giá trị cốt lõi giải quyết bài toán "không biết tập gì". Lộ trình tự động là điểm khác biệt chính của ứng dụng.

**Independent Test**: Có thể kiểm thử bằng cách chọn "Giảm cân" + "Body weight" → xác nhận hệ thống sinh ra lộ trình chứa các bài cardio/waist không cần dụng cụ, phân bổ theo ngày trong tuần.

**Acceptance Scenarios**:

1. **Given** Người dùng đã thiết lập mục tiêu "Giảm cân" và thiết bị "Body weight", **When** hệ thống tạo lộ trình, **Then** lộ trình chỉ chứa bài tập thuộc category cardio/waist không cần dụng cụ.
2. **Given** Người dùng đã có lộ trình cũ, **When** người dùng thay đổi goal_type, **Then** hệ thống cảnh báo việc archive plan cũ sẽ làm mất tiến trình tập của ngày hôm nay và đưa ra 2 lựa chọn: (1) tạo plan mới NGAY — plan cũ chuyển archived, plan mới active; hoặc (2) plan mới bắt đầu từ NGÀY MAI — plan cũ tiếp tục hiệu lực đến hết hôm nay.
3. **Given** Một bài tập bị Admin ẩn (inactive), **When** hệ thống tạo lộ trình mới, **Then** bài tập bị ẩn không xuất hiện trong lộ trình.
4. **Given** Lộ trình đã tạo, **When** người dùng xem lịch trình tuần, **Then** hệ thống hiển thị bài tập phân bổ theo từng ngày.
5. **Given** Một bài tập nằm trong lộ trình active của người dùng, **When** bài tập đó bị Admin ẩn, **Then** hệ thống thông báo cho người dùng, clone tạm thời bài tập vào draft queue để tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành) và gợi ý bài tập thay thế (cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác) nếu có.

---

### User Story 3 - Xem chi tiết bài tập (Priority: P1)

Người dùng muốn xem hướng dẫn chi tiết của một bài tập để biết cách thực hiện đúng kỹ thuật, gồm ảnh động minh họa, ảnh tĩnh và hướng dẫn từng bước.

**Why this priority**: Hướng dẫn kỹ thuật trực quan là yếu tố giảm lo lắng chấn thương cho người mới, trực tiếp phục vụ mục tiêu sản phẩm.

**Independent Test**: Có thể kiểm thử bằng cách mở một bài tập bất kỳ → xác nhận hiển thị ảnh động, ảnh tĩnh 180x180 và đầy đủ hướng dẫn từng bước.

**Acceptance Scenarios**:

1. **Given** Người dùng chọn một bài tập, **When** màn hình chi tiết mở ra, **Then** hệ thống hiển thị ảnh động (GIF), ảnh tĩnh (180x180) và hướng dẫn từng bước.
2. **Given** Ảnh động không tải được, **When** hệ thống gặp lỗi tải, **Then** hệ thống tự động fallback hiển thị ảnh tĩnh.
3. **Given** Người dùng tìm kiếm theo thiết bị "Body weight", **When** hệ thống trả kết quả, **Then** chỉ hiển thị các bài tập không cần dụng cụ.

---

### Edge Cases

- Điều gì xảy ra khi không có bài tập nào khớp với tổ hợp mục tiêu + thiết bị?
- Điều gì xảy ra khi kho bài tập chưa được import (trống)?
- Điều gì xảy ra khi lộ trình trống do tất cả bài tập bị Admin ẩn?
- Điều gì xảy ra khi bài tập trong plan active bị Admin ẩn? → Hệ thống thông báo cho người dùng, clone tạm thời bài tập vào draft queue để tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành) và gợi ý bài tập thay thế nếu có.
- Điều gì xảy ra khi hướng dẫn bài tập thiếu ngôn ngữ hiện tại?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cho phép người dùng thiết lập mục tiêu (giảm cân / tăng cơ / sức bền) và thiết bị sẵn có.
- **FR-002**: WHEN người dùng hoàn thành thiết lập mục tiêu và thiết bị, hệ thống PHẢI dùng logic rule-based (bảng luật Rule Engine v1) để tự động tạo lộ trình phân bổ vào các ngày trong tuần.
- **FR-003**: Hệ thống PHẢI cung cấp kho dữ liệu bài tập được phân loại theo bộ phận cơ thể (category/body_part) và thiết bị (equipment).
- **FR-004**: WHEN người dùng chọn một bài tập, hệ thống PHẢI hiển thị ảnh động (GIF), ảnh tĩnh (180x180) và hướng dẫn từng bước.
- **FR-005**: WHEN người dùng thay đổi goal_type, hệ thống PHẢI cảnh báo rằng việc lưu trữ (archive) plan cũ sẽ làm mất tiến trình tập của ngày hôm nay và đưa ra 2 lựa chọn: (1) tạo plan mới NGAY — plan cũ chuyển archived, plan mới active; hoặc (2) plan mới bắt đầu từ NGÀY MAI — plan cũ tiếp tục hiệu lực đến hết ngày hôm nay.
- **FR-006**: Hệ thống PHẢI loại trừ bài tập bị ẩn (inactive) khỏi kết quả sinh lộ trình và tìm kiếm của người dùng.
- **FR-007**: WHEN ảnh động không tải được, hệ thống PHẢI fallback sang ảnh tĩnh.
- **FR-008**: Hệ thống PHẢI xác định số ngày tập/tuần, cấu trúc lộ trình, số reps và thời gian nghỉ theo goal_type theo bảng luật Rule Engine v1: weight_loss — 4-5 ngày/tuần, cardio + full-body, 12-15 reps, nghỉ 45-60s; muscle_gain — 4 ngày/tuần, split Push/Pull/Legs, 8-12 reps, nghỉ 60-90s; endurance — 3-4 ngày/tuần, circuit toàn thân, 15-20 reps, nghỉ 30-45s.
- **FR-009**: Hệ thống PHẢI xác định khối lượng tập (số bài/ngày × số sets) theo fitness_level: beginner — 2-3 bài/ngày × 3 sets; intermediate — 4-5 bài/ngày × 4 sets; advanced — 5-6 bài/ngày × 4-5 sets.
- **FR-010**: Hệ thống PHẢI lọc tập bài từ kho theo equipment — chỉ chọn bài tập phù hợp với dụng cụ người dùng sẵn có.
- **FR-011**: WHERE một bài tập trong Workout Plan active của người dùng bị Admin ẩn, hệ thống PHẢI thông báo cho người dùng, clone tạm thời bài tập vào draft queue để người dùng tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành), và gợi ý bài tập thay thế (cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác) nếu có.

### Rule Engine v1 — Bảng luật sinh lộ trình

**Theo `goal_type`:**

| goal_type | Số ngày/tuần | Cấu trúc lộ trình | Số reps | Thời gian nghỉ |
|---|---|---|---|---|
| weight_loss (Giảm cân) | 4-5 ngày/tuần | cardio + full-body | 12-15 reps | 45-60s |
| muscle_gain (Tăng cơ) | 4 ngày/tuần | split Push/Pull/Legs | 8-12 reps | 60-90s |
| endurance (Sức bền) | 3-4 ngày/tuần | circuit toàn thân | 15-20 reps | 30-45s |

**Theo `fitness_level`:**

| fitness_level | Khối lượng |
|---|---|
| beginner | 2-3 bài/ngày × 3 sets |
| intermediate | 4-5 bài/ngày × 4 sets |
| advanced | 5-6 bài/ngày × 4-5 sets |

**Theo `equipment`:** lọc tập bài từ kho theo dụng cụ người dùng sẵn có.

### Key Entities

- **Exercise**: Bài tập với tên, bộ phận cơ thể, thiết bị, nhóm cơ, ảnh tĩnh, ảnh động, hướng dẫn đa ngôn ngữ, trạng thái (active/inactive).
- **Workout Plan**: Lộ trình tập của một người dùng với mục tiêu, trạng thái (active/archived).
- **Plan Day**: Ngày tập trong lộ trình (thứ trong tuần) chứa danh sách bài tập.
- **Plan Exercise**: Một bài tập trong một ngày với số hiệp và số lần mục tiêu.
- **Draft Queue**: Bản sao tạm thời của bài tập bị ẩn trong plan active của người dùng, giúp hoàn thành buổi tập hiện tại; bị xóa khi buổi tập hoàn thành.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Hệ thống sinh lộ trình trong vòng 5 giây sau khi người dùng hoàn tất thiết lập mục tiêu.
- **SC-002**: 100% bài tập trong lộ trình khớp đúng với mục tiêu và thiết bị đã chọn.
- **SC-003**: Màn hình chi tiết bài tập hiển thị đầy đủ hướng dẫn trong vòng 2 giây.
- **SC-004**: Người dùng có thể bắt đầu buổi tập với ít hơn 3 lượt chạm từ màn hình chính.
- **SC-005**: 100% lộ trình được sinh tuân thủ bảng luật Rule Engine v1 (số ngày/tuần, cấu trúc, reps, thời gian nghỉ theo goal_type; khối lượng theo fitness_level; bài tập khớp equipment).

## Assumptions

- Kho bài tập gồm 1324 bài đã được import vào database trước khi tính năng hoạt động.
- Logic sinh lộ trình dùng rule-based theo bảng luật Rule Engine v1 (goal_type + fitness_level + equipment), chưa dùng AI/ML.
- fitness_level (beginner / intermediate / advanced) được người dùng khai báo khi thiết lập mục tiêu và là đầu vào của Rule Engine v1.
- Hướng dẫn bài tập mặc định hiển thị tiếng Anh (en); tiếng Việt sẽ bổ sung ở phase sau.
- Bài tập bị ẩn vẫn hiển thị trong lịch sử tập cũ nhưng không xuất hiện trong lộ trình mới.
