# Research: Picker Thêm bài giống thư viện

## 1. Lọc “nhóm cơ” nghĩa là gì?

**Decision**: Dùng Category kho dữ liệu (10 vùng: ngực, lưng, cardio, …) + Equipment (28), giống thư viện 012. Không thêm chip 6 nhóm Rule Engine.

**Rationale**: User muốn picker “giống thư viện”. 012 đã chốt không chip 6 nhóm / Target Muscle. Category là cách người tập nghĩ “nhóm cơ” khi tìm bài.

**Alternatives**: Chip 6 nhóm cơ (lệch thư viện); chỉ search tên (hiện trạng, không thấy động tác).

## 2. Chọn bài: bấm tên là thêm ngay, hay xem trước rồi xác nhận?

**Decision**: Chọn dòng = xem trước GIF/hướng dẫn. Thêm vào ngày chỉ khi bấm xác nhận.

**Rationale**: User nói rõ phải biết động tác trước khi chọn. Thêm ngay khi bấm tên (picker cũ) phá đúng nỗi đau đó.

**Alternatives**: Thêm ngay + tooltip GIF (dễ thêm nhầm); điều hướng sang trang thư viện (thêm bước, mất ngữ cảnh ngày đang sửa).

## 3. Tái sử dụng UI thế nào?

**Decision**: Một component web dùng chung (bộ lọc + danh sách + khung xem trước). Thư viện truyền slot hành động sửa/xóa bài custom. Picker truyền callback xác nhận thêm, ẩn CRUD custom.

**Rationale**: DRY — nếu copy JSX, hai màn sẽ lệch chip/GIF. API search hiện có đủ (`category[]`, `equipment[]`, `q`, phân trang).

**Alternatives**: Iframe/embed trang thư viện (nặng, có nút Tạo bài không thuộc picker); viết picker riêng (lệch UX).

## 4. API / schema

**Decision**: Không endpoint mới, không migration.

**Rationale**: `GET /exercises` đã trả GIF, ảnh, hướng dẫn, source. Modal chỉ cần rộng hơn để hai cột.

**Alternatives**: Endpoint “suggest for day” (YAGNI).
