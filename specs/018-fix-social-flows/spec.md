# Feature Specification: Sửa luồng Xã hội & Cộng đồng (fix-social-flows)

**Feature Branch**: `feature/fix-social-flows`

**Created**: 2026-08-27

**Status**: Implemented (spec đồng bộ spec.md/003; backend + web test xanh — 307/64)

**Input**: User description: "Sửa các luồng mạng xã hội (kết bạn, feed hoạt động, leaderboard streak, challenge, privacy/media) cho khớp spec 003 + constitution. Kế thừa quyết định owner từ grill-me (Q1–Q5 đã chốt 2026-08-27) và khuyến nghị từ ck-predict."

## Clarifications

### Session 2026-08-27

- Q: Lời mời chéo (2 user gửi đồng thời) xử lý thế nào? → A: Tự động chấp nhận (auto-accept) — bên nhấn trước là người gửi, quan hệ hai chiều được thiết lập ngay.
- Q: Media bài đăng cộng đồng? → A: Upload trực tiếp chỉ ảnh (PNG/JPG/JPEG/WEBP, KHÔNG GIF/video); GIF dùng embed từ hệ thống ngoài (Tenor/Instagram) với URL do User cung cấp.
- Q: Tie-break bảng xếp hạng "thời gian duy trì sớm hơn" nghĩa là gì? → A: Ai đạt chuỗi tuần hiện tại sớm hơn thì xếp trên (cần lưu tuần bắt đầu chuỗi hiện tại).
- Q: Từ vựng trạng thái Challenge? → B: `open/closed/finished` (theo General Spec + data-model).
- Q: Sự kiện nào được phát lên feed bạn bè? → B: Chỉ `streak_milestone` và `new_pr` (không phát `workout_completed` để tránh spam).
- Q: Field `is_private` (hồ sơ riêng tư) do spec nào sở hữu và mặc định là gì? → A: 001-profile-history sở hữu, mặc định **private** (đúng General Spec §3).
- Q: GIF embed v1 hỗ trợ nguồn nào? → A: Chỉ nhúng Tenor (iframe); Instagram chấp nhận dưới dạng link + preview (không nhúng iframe).
- Q: Tính streak (tuần) theo múi giờ nào đợt này? → B: Giữ múi giờ máy chủ cho mọi user; hỗ trợ múi giờ theo user là nợ kỹ thuật (backlog).
- Q: Tiêu chí xếp hạng Challenge khi tổng kết là gì? → B: Streak hiện tại của từng người tham gia tại thời điểm end_date (dùng chung định nghĩa streak duy nhất; tie-break như leaderboard).
- Q: Xử lý dữ liệu cũ trùng lặp ở quan hệ bạn bè thế nào? → A: Gộp vào bản ghi thắng (accepted > pending > rejected mới nhất); bản ghi thua chuyển trạng thái `superseded`, KHÔNG xóa row (constitution §7).

### Session 2026-08-27 (edge-case review — plan final)

- Q: B1 — Challenge sau end_date tới lúc job tổng kết (cửa sổ ≤60s) gọi là gì? → A: Trạng thái SUY RA (derived) `closed` tại thời điểm truy vấn (`status lưu trữ = open && end_date < today`); KHÔNG ghi xuống DB. Bộ trạng thái lưu trữ chỉ còn `open/finished`. (`closed` từng được nêu ở Clarifications trước đã bị lược bỏ.)
- Q: B1 — end_date được tính thế nào, finalize có bị off-by-one không? → A: `end_date` là ngày cuối TRỌN VẸN (inclusive, `start_date + duration_days`); job chạy `@Scheduled(60s)` với điều kiện `endDate.isBefore(today)` → tick đầu tiên sau nửa đêm tổng kết = **ĐÚNG FR-011 (≤1 phút)**. Không phải off-by-one.
- Q: B1 — khi quá hạn nhưng chưa tổng kết xong, experience người dùng thế nào? → A: UI hiện banner "Đang tổng kết" + ẩn nút Tham gia; user đang trong challenge bị ép thoát khỏi khu vực tham gia (chỉ UI, giữ nguyên dữ liệu participant) — FR-CHALL-NOTIFY.
- Q: Người gửi có được phép RÚT (withdraw) lời mời đang pending không? → A: Có — endpoint DELETE /friendships/{id} cho lời mời `pending` do chính mình gửi, xóa bản ghi (nhất quán với việc hủy kết bạn cũng xóa row). Edge case: phía người nhận có thể thấy lời mời cũ trên màn hình cho tới lượt refresh kế tiếp (không có realtime push).
- Q: Giới hạn "tối đa 5 lời mời/ngày" có còn không? → A: Đã GỠ BỎ hoàn toàn. Chỉ còn cooldown 30 ngày sau khi bị từ chối + cap 500 bạn bè.
- Q: Bảng xếp hạng (bạn bè) với user chưa có bản ghi streak? → A: Hiển thị user đó ở cuối bảng với hạng "—" (unranked) — kể cả viewer chưa có entry.
- Q: Allowlist GIF — link ảnh trực tiếp `media.tenor.com/...` có được chấp nhận không? → A: Có — thêm `media.tenor.com` vào allowlist (hiển thị bằng `<img>`, không iframe); `tenor.com/view/...` embed giữ nguyên.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Kết bạn tin cậy, không lỗi và không trùng lặp (Priority: P1)

Người dùng kết bạn không gặp lỗi hệ thống dù hai người gửi lời mời cùng lúc; hệ thống luôn giữ đúng một quan hệ cho mỗi cặp; khi bị từ chối rồi gửi lại sau 30 ngày, quan hệ được tái sử dụng thay vì tạo bản ghi mới; khi tìm kiếm, nút hành động hiển thị đúng trạng thái quan hệ.

**Why this priority**: Hiện trạng có thể sinh bản ghi trùng và gây lỗi 500 hàng loạt cho search/feed — đây là lỗi nghiêm trọng nhất của nhóm tính năng xã hội.

**Independent Test**: 2 user gửi lời mời ngược chiều gần như đồng thời → database chỉ có 1 bản ghi quan hệ, không có lỗi 500; gửi lại sau khi bị từ chối → vẫn 1 bản ghi/pair; tìm kiếm hiển thị nút đúng theo trạng thái.

**Acceptance Scenarios**:

1. **Given** User A và B chưa có quan hệ, **When** cả hai gửi lời mời cho nhau gần như đồng thời, **Then** hệ thống chỉ tạo 1 quan hệ (auto-accept), cả hai thành bạn bè, không có lỗi hệ thống.
2. **Given** A đã bị B từ chối và đã qua 30 ngày, **When** A gửi lại lời mời, **Then** hệ thống tái sử dụng bản ghi cũ (không tạo bản ghi thứ hai) và chuyển trạng thái sang pending.
3. **Given** A đã có 500 bạn bè, **When** A gửi lời mời mới, **Then** hệ thống từ chối với thông báo rõ ràng (422).
4. **Given** A tìm kiếm B, **When** kết quả hiển thị, **Then** nút hành động đúng trạng thái: "Kết bạn" (chưa có quan hệ), "Đã gửi lời mời" (A đã gửi), "Chấp nhận/Từ chối" (B đã gửi cho A), "Bạn bè" (đã là bạn).

---

### User Story 2 - Feed thành tích bạn bè (Priority: P1)

Người dùng thấy thành tích mới của bạn bè trên feed: chuỗi tuần tập mới (streak milestone) và kỷ lục cá nhân (PR) — không bị ngập bởi từng buổi tập thông thường.

**Why this priority**: Feed hiện gần như trống (chỉ có sự kiện kết bạn) — tính năng tạo động lực xã hội (social accountability) đang không hoạt động.

**Independent Test**: User hoàn thành buổi tập thứ 3 trong tuần (streak tăng) → bạn bè thấy sự kiện "đạt chuỗi N tuần" trong vòng 1 phút; phá kỷ lục tổng khối lượng buổi → bạn bè thấy sự kiện PR.

**Acceptance Scenarios**:

1. **Given** Bạn bè đang xem feed, **When** User A đạt chuỗi tuần mới (hoàn thành buổi thứ 3 của tuần), **Then** feed hiển thị sự kiện streak milestone của A trong vòng 1 phút.
2. **Given** User A phá kỷ lục cá nhân tổng khối lượng (kg) của một buổi tập, **When** buổi tập kết thúc, **Then** feed bạn bè hiển thị sự kiện PR của A.
3. **Given** Feed có nhiều sự kiện, **When** User xem feed, **Then** chỉ hiển thị sự kiện của bạn bè trong 7 ngày gần nhất, mới nhất trước, tối đa 50 sự kiện.
4. **Given** Sự kiện cùng loại phát sinh lặp (đồng bộ offline gọi lại), **When** hệ thống xử lý, **Then** không có sự kiện trùng cho cùng (người, loại, mốc).

---

### User Story 3 - Bảng xếp hạng đúng luật và luôn ghim vị trí của tôi (Priority: P1)

Bảng xếp hạng streak xếp hạng đúng luật tie-break (ai giữ chuỗi hiện tại lâu hơn xếp trên), luôn hiển thị vị trí cá nhân của người xem kể cả khi nằm ngoài top 100, và không làm chậm hệ thống khi nhiều người dùng.

**Why this priority**: Luật tie-break đang sai spec; vị trí cá nhân biến mất ngoài top 100; mỗi lần xem bảng xếp hạng hiện quét toàn bộ dữ liệu buổi tập của mọi user.

**Independent Test**: 2 user cùng streak nhưng khác tuần bắt đầu chuỗi → người giữ chuỗi lâu hơn xếp trên; user rank > 100 vẫn thấy "Vị trí của bạn" được ghim; bảng xếp hạng phản ánh streak mới trong vòng 5 phút.

**Acceptance Scenarios**:

1. **Given** 2 user có cùng streak hiện tại, **When** hệ thống xếp hạng, **Then** người đạt chuỗi hiện tại sớm hơn (tuần bắt đầu chuỗi sớm hơn) xếp trên; nếu cùng tuần bắt đầu thì theo thứ tự cố định (id).
2. **Given** User đang xem bảng xếp hạng, **When** vị trí cá nhân nằm ngoài top 100, **Then** vị trí của user vẫn được trả về và ghim ở cuối bảng.
3. **Given** Streak của user thay đổi sau buổi tập, **When** xem bảng xếp hạng, **Then** kết quả phản ánh thay đổi trong vòng 5 phút (cập nhật theo batch).
4. **Given** Nhiều người cùng xem bảng xếp hạng, **When** hệ thống trả kết quả, **Then** thời gian phản hồi dưới 200ms với 10.000 user đang hoạt động.

---

### User Story 4 - Thử thách có vòng đời và tự tổng kết (Priority: P1)

Challenge (thử thách có thời hạn) có vòng đời rõ ràng: đang mở (open) → hết hạn (closed — trạng thái suy ra khi quá end_date, hiển thị "Đang tổng kết") → đã tổng kết (finished). Khi đến hạn, hệ thống tự động tổng kết, xếp hạng người tham gia và lưu lịch sử; người dùng xem được kết quả chung cuộc.

**Why this priority**: Hiện challenge không bao giờ được tổng kết (FR-015/SC-003 của spec 003 chưa implement) — toàn bộ vòng đời thi đua theo đợt đang thiếu.

**Independent Test**: Admin tạo challenge 1 ngày → 2 user tham gia → qua end_date → hệ thống tự tổng kết trong 1 phút → mỗi người tham gia có final_rank và xem được kết quả.

**Acceptance Scenarios**:

1. **Given** Admin tạo challenge (tên, loại mục tiêu, số ngày), **When** tạo xong, **Then** challenge có trạng thái `open` và hiển thị cho user tham gia.
2. **Given** Challenge đang `open`, **When** User nhấn "Tham gia", **Then** hệ thống ghi nhận người tham gia (1 lần duy nhất).
3. **Given** Challenge đã quá `end_date`, **When** User nhấn "Tham gia", **Then** hệ thống chặn với thông báo rõ ràng (422).
4. **Given** Challenge đến `end_date`, **When** hệ thống tổng kết, **Then** trong vòng 1 phút: trạng thái chuyển `finished`, từng người tham gia có completed_at và final_rank, kết quả được lưu lịch sử.
5. **Given** Challenge đã `finished`, **When** User xem, **Then** thấy bảng kết quả chung cuộc (hạng của từng người tham gia).

---

### User Story 5 - Hồ sơ riêng tư (Priority: P2)

Người dùng có hồ sơ ở chế độ riêng tư: người lạ chỉ thấy tên hiển thị và thứ hạng, không thấy bài đăng trên tường cá nhân; bài đăng công khai vẫn ai cũng xem được.

**Why this priority**: Quyền riêng tư đã có trong spec 003 (FR-012) nhưng chưa có dữ liệu để thực thi — đang là lỗ hổng cam kết sản phẩm.

**Independent Test**: User B bật hồ sơ riêng tư → người lạ A tìm kiếm chỉ thấy tên + hạng; A không xem được bài đăng của B trừ bài public.

**Acceptance Scenarios**:

1. **Given** Hồ sơ B ở chế độ riêng tư, **When** người lạ A tìm kiếm B, **Then** A chỉ thấy tên hiển thị và hạng của B (không thấy email/bài đăng tường cá nhân).
2. **Given** B đăng bài với chế độ công khai, **When** A xem, **Then** A vẫn xem được bài đăng đó.
3. **Given** Hồ sơ B ở chế độ riêng tư, **When** bạn bè của B xem, **Then** bạn bè vẫn thấy hoạt động và bài đăng của B như bình thường.

---

### User Story 6 - Đăng bài với ảnh và GIF embed (Priority: P2)

Người dùng đăng bài cộng đồng kèm ảnh (upload trực tiếp) hoặc GIF (dán link từ Tenor/Instagram để nhúng); hệ thống từ chối mọi file không phải ảnh.

**Why this priority**: Chính sách media hiện mâu thuẫn giữa spec/constitution/code (upload cho phép GIF, web có mã video) — cần thống nhất và enforce một chính sách duy nhất.

**Independent Test**: Upload ảnh PNG/JPG → đăng thành công; upload video/GIF (kể cả đổi đuôi thành .jpg) → bị từ chối; dán link Tenor/Instagram → hiển thị nhúng; dán link ngoài danh sách → bị từ chối.

**Acceptance Scenarios**:

1. **Given** User đăng bài kèm ảnh PNG/JPG/JPEG/WEBP ≤ 10MB, **When** gửi, **Then** bài đăng thành công và hiển thị ảnh.
2. **Given** User đăng bài kèm file GIF hoặc video (kể cả đổi đuôi .jpg), **When** gửi, **Then** hệ thống từ chối với thông báo rõ ràng.
3. **Given** User đăng bài kèm link GIF từ tenor.com hoặc instagram.com, **When** gửi, **Then** bài đăng lưu link; Web hiển thị GIF nhúng (Tenor) hoặc link + preview (Instagram).
4. **Given** User dán link GIF từ domain khác, **When** gửi, **Then** hệ thống từ chối với thông báo rõ ràng.

---

### Edge Cases *(đã phân loại theo Bucket — xem §Edge Cases & Decisions)*

- Điều gì xảy ra khi 2 user gửi lời mời cho nhau gần như đồng thời? (Chỉ 1 quan hệ, auto-accept — FR-001.)
- Điều gì xảy ra khi database đang có sẵn nhiều bản ghi trùng cho cùng một cặp (dữ liệu cũ)? (Migration chuẩn hóa: gộp vào bản ghi thắng, bản ghi thua chuyển `superseded`, không lỗi — xem Assumptions.)
- Điều gì xảy ra khi user gửi lại lời mời sau khi bị từ chối nhưng chưa hết 30 ngày? (Bị chặn với thông báo cooldown.)
- Điều gì xảy ra khi user có hơn 1000 buổi tập đã hoàn thành? (Streak phải tính đúng, không bị cắt thiếu.)
- Điều gì xảy ra khi 2 user cùng streak và cùng tuần bắt đầu chuỗi? (Tie-break cuối theo thứ tự cố định.)
- Điều gì xảy ra khi challenge kết thúc lúc nửa đêm? (Tổng kết trong vòng 1 phút sau end_date; end_date là ngày cuối TRỌN VẸN — không off-by-one. B1.)
- Điều gì xảy ra khi challenge quá end_date nhưng job chưa tổng kết kịp (cửa sổ ≤60s)? (Trạng thái suy ra `closed` — ẩn nút Tham gia, banner "Đang tổng kết", ép thoát UI. FR-CHALL-CLOSE / FR-CHALL-NOTIFY.)
- Điều gì xảy ra khi job tổng kết chạy 2 lần (hoặc nhiều instance)? (Idempotent — không đổi kết quả, không trùng lịch sử.)
- Điều gì xảy ra khi user tham gia challenge rồi bỏ dở giữa chừng? (Vẫn được xếp hạng theo streak hiện tại tại thời điểm end_date — FR-011.)
- Điều gì xảy ra khi user đã mời rồi muốn rút lại lời mời (chưa được chấp nhận)? (Rút được qua DELETE /friendships/{id}; phía người nhận có thể thấy lời mời cũ tới lượt refresh. FR-WITHDRAW.)
- Điều gì xảy ra khi user bị ban/xóa account giữa vòng đời challenge? (Bị loại khỏi ranking khi tổng kết — FR-011; không xuất hiện ở search/leaderboard/feed — FR-VISIBILITY.)
- Điều gì xảy ra khi user không phải bạn bè biết postId của bài đăng private/friends? (Bị chặn khi like/comment/xem comment — FR-AUDIENCE.)
- Điều gì xảy ra khi user chưa có entry xếp hạng xem leaderboard bạn bè? (Vẫn hiện ở cuối bảng hạng "—" — FR-008.)
- Điều gì xảy ra khi upload file video đổi đuôi .jpg? (Từ chối bằng kiểm tra nội dung file — magic bytes, FR-014.)
- Điều gì xảy ra khi upload file không có phần mở rộng? (Suy extension từ magic bytes — FR-014/E6.5.)
- Điều gì xảy ra khi kho lưu trữ media (SeaweedFS) không hoạt động lúc đăng bài? (Báo lỗi rõ ràng, không tạo bài đăng thiếu media.)

### Edge Cases & Decisions *(kết quả phân tích 12-dimension + brainstorm 2026-08-27)*

**Bucket A — Chấp nhận hành vi hiện tại, chỉ ghi chú** (không đổi code):

- Feed hoạt động KHÔNG áp privacy filter (bạn bè xem được thành tích của private user — đã chốt, xem Clarifications).
- Lời mời quá hạn/không có thời hạn; lời mời chéo giải quyết bằng timestamp người nhấn trước.
- Cap 500 bạn bè chỉ tính lượng `accepted`; lời mời quá 500 → 422.
- Search user giới hạn 20 kết quả (`limit(20)`); PR tính từ sessions đang lưu, không ém hồi tố khi xóa session cũ.
- Mốc streak (10/30/50/100 tuần) chỉ phát 1 sự kiện khi CHẠM mốc, không phát sự kiện lặp nếu streak đi xuống rồi lên lại ở ngưỡng dưới mốc.
- Leaderboard stale ≤5 phút do batch update (chấp nhận); friends tab hiện streak bạn bè bất kể hồ sơ riêng tư.
- Challenge 0 participant → `finished` với kết quả rỗng; trùng tên challenge được phép tạo.
- `start_date` quá khứ → challenge mở ngay (tick đầu sau end_date nếu end < today); duration ≥ 1.
- `is_private` mặc định private; private user vẫn hiện tên + hạng trên leaderboard/search (không mưa email/bài đăng).
- Gif embed: gifUrl host-only, backend không gọi external; Instagram chỉ link + preview (không iframe); ≤10MB ảnh.

**Bucket B — Verify bằng test (không đổi behavior)**:

- Tự gửi lời mời cho chính mình → 422 (đã có guard).
- Unfriend → sự kiện `friendship_created` biến mất khỏi feed bạn bè (feed filter theo bạn bè hiện tại lúc truy vấn).
- Tie-break leaderboard deterministic (`streak_start_week` ASC → `user_id` ASC).
- Join challenge race với job finalize → participant mới được gán `finalRank` ở tick sau (job chạy lại `findByStatus(open)`).
- Privacy toggle (001) dùng `user.updated_at` 24h cooldown → trả 429 toàn bộ request kể cả toggle đúng → cooldown.
- `results()` của challenge chưa `finished` → 422 "chưa có kết quả".

**Bucket C — Code changes (phạm vi đợt này)**:

- Withdraw lời mời pending (FR-WITHDRAW — mới).
- Activity feed loại tác giả BANNED/DELETED khỏi sự kiện (một phần FR-VISIBILITY).
- Finalize challenge loại participant BANNED/DELETED khỏi ranking (một phần FR-011/FR-VISIBILITY).
- Từ chối 400 khi bài đăng mang đồng thời cả `media` và `gif_url`.
- Xóa bài đăng → xóa file media tương ứng trên storage (best-effort, giữ orphan trong trường hợp lỗi storage).
- Upload file không có phần mở rộng → suy extension từ magic bytes (FR-014).

**Phát hiện mới từ phân tích (đã encode vào FR)**:

- **I8/A1 (FR-AUDIENCE)**: like/comment/xem comment không kiểm tra quyền xem bài đăng (ai biết postId cũng phản hồi được bài `friends`/`private`) → High, phải fix.
- **U2/U3/U4 (FR-VISIBILITY)**: search/leaderboard/activity feed vẫn hiển thị user BANNED/DELETED — không nhất quán với community feed (`filterActiveAuthors`) → High, phải fix.
- **ST1 (B1 / FR-010, FR-CHALL-CLOSE)**: trạng thái `closed` không code nào gán — dead state → chuyển thành trạng thái suy ra.
- **U5 (FR-008)**: user chưa có entry xếp hạng biến mất khỏi friends-leaderboard → hiện unranked cuối bảng.
- **D1/I2 (FR-015)**: `gifUrl` > 500 ký tự gây lỗi DB 500 thay vì 400 → validate độ dài.
- **T5/D2 (FR-011)**: double-join challenge có thể tạo duplicate participant (thiếu unique index) → unique index `(challenge_id, user_id)` + catch.
- **T6**: tái dùng lời mời sau 30 ngày không tính vào giới hạn 5/ngày → **không còn nghĩa lý** vì giới hạn 5/ngày đã bị gỡ bỏ.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: WHERE 2 User gửi lời mời kết bạn cho nhau gần như đồng thời, hệ thống PHẢI bảo đảm chỉ tồn tại ĐÚNG 1 bản ghi quan hệ HOẠT ĐỘNG (status khác `superseded`) cho cặp (user_id_1, user_id_2) và tự động chấp nhận (auto-accept — bên nhấn trước là người gửi), không trả lỗi hệ thống.
- **FR-002**: WHEN User gửi lời mời cho người đã từ chối trước đó và đã qua cooldown 30 ngày, hệ thống PHẢI tái sử dụng bản ghi quan hệ cũ (chuyển trạng thái thành pending) thay vì tạo bản ghi mới.
- **FR-003**: WHERE User đã có 500 bạn bè (accepted), hệ thống PHẢI chặn lời mời kết bạn mới với HTTP 422.
- **FR-004**: WHEN User tìm kiếm người dùng khác, hệ thống PHẢI trả kèm trạng thái quan hệ (none / pending_sent / pending_received / accepted) cho từng kết quả để hiển thị nút hành động đúng.
- **FR-005**: WHEN chuỗi tuần streak của User tăng lên giá trị mới, hệ thống PHẢI ghi sự kiện `streak_milestone` (tối đa 1 sự kiện mỗi tuần tăng; thêm 1 sự kiện khi chạm mốc 10/30/50/100 tuần). WHEN User đạt kỷ lục cá nhân về tổng khối lượng (kg) của một buổi tập, hệ thống PHẢI ghi sự kiện `new_pr`. Hệ thống KHÔNG phát sự kiện cho từng buổi tập thông thường (`workout_completed` không dùng).
- **FR-006**: WHEN User xem feed hoạt động, hệ thống PHẢI chỉ hiển thị sự kiện của bạn bè trong 7 ngày gần nhất, sắp xếp mới nhất trước, tối đa 50 sự kiện.
- **FR-007**: WHEN hệ thống xếp hạng leaderboard, hệ thống PHẢI xếp theo streak hiện tại giảm dần; WHERE nhiều User có cùng streak, hệ thống PHẢI ưu tiên người đạt chuỗi hiện tại SỚM HƠN (tuần bắt đầu chuỗi sớm hơn xếp trên); nếu vẫn bằng nhau, xếp theo thứ tự cố định (id tăng dần).
- **FR-008**: WHERE User xem bảng xếp hạng toàn server, hệ thống PHẢI luôn trả về vị trí cá nhân của User đó (để ghim) kể cả khi nằm ngoài top 100; WHERE User chưa có bản ghi xếp hạng, hệ thống PHẢI hiển thị User ở cuối bảng với hạng "—" (unranked). WHEN User xem bảng xếp hạng bạn bè, hệ thống PHẢI liệt kê User và các bạn bè kể cả khi chưa có bản ghi xếp hạng, xếp ở cuối với hạng "—" (unranked).
- **FR-009**: WHEN streak của User thay đổi, hệ thống PHẢI cập nhật bản ghi xếp hạng (current_streak_weeks, longest_streak_weeks, streak_start_week, rank, updated_at). Bảng xếp hạng cập nhật theo batch, phản ánh thay đổi trong vòng 5 phút.
- **FR-010**: Challenge PHẢI có trạng thái lưu trữ thuộc bộ `open/finished`: `open` = đang mở đăng ký (từ lúc tạo đến end_date), `finished` = đã tổng kết xong. Trạng thái chỉ chuyển 1 chiều (`open` → `finished`). Trạng thái `closed` KHÔNG lưu trữ trong DB — nó là trạng thái SUY RA (derived) tại thời điểm truy vấn (xem FR-CHALL-CLOSE).
- **FR-CHALL-CLOSE**: WHERE Challenge có status lưu trữ `open` NHƯNG `end_date < LocalDate.today()` (đã quá hạn, job tổng kết chưa kịp chạy — cửa sổ ≤60s), hệ thống PHẢI trình bày challenge đó bằng trạng thái suy ra `closed` (hiển thị "Đang tổng kết", không hiển thị nút "Tham gia", chấp nhận xem). Hệ thống KHÔNG PHẢI ghi derived state xuống DB.
- **FR-011**: WHEN Challenge đến end_date, hệ thống PHẢI tự động tổng kết trong vòng 1 phút: tính completed_at và final_rank cho TỪNG người tham gia còn `ACTIVE` theo **streak hiện tại của từng người tại thời điểm end_date** (chuỗi tuần liên tiếp đạt ≥3 buổi tập — định nghĩa streak DUY NHẤT của constitution); participant ở trạng thái account BANNED/DELETED PHẢI bị LOẠI khỏi bảng xếp hạng (FR-VISIBILITY); tie-break theo FR-007 (ai đạt chuỗi hiện tại sớm hơn xếp trên), lưu lịch sử xếp hạng và chuyển trạng thái sang finished. Job tổng kết PHẢI idempotent (chạy lặp/nhiều instance không đổi kết quả).
- **FR-012**: WHEN Challenge có trạng thái khác `open` hoặc đã quá end_date, hệ thống PHẢI chặn User tham gia với HTTP 422.
- **FR-CHALL-NOTIFY**: WHERE User đang xem một Challenge bị suy ra `closed`, Client PHẢI hiển thị thông báo "Thử thách đã kết thúc, đang tổng kết..." và ép User thoát khỏi khu vực tham gia (sự ép thoát CHỈ ở lớp UI — không thay đổi/xóa dữ liệu participant).
- **FR-013**: WHERE hồ sơ User ở chế độ riêng tư (private), hệ thống PHẢI chỉ cho người lạ (không phải bạn bè) xem display_name và rank — KHÔNG hiển thị bài đăng trên tường cá nhân. WHERE bài đăng được đánh dấu public, hệ thống PHẢI cho mọi người xem bài đăng đó.
- **FR-014**: WHEN User đăng bài kèm file media, hệ thống PHẢI chỉ chấp nhận ảnh PNG/JPG/JPEG/WEBP tối đa 10MB, kiểm tra NỘI DUNG file bằng magic bytes (không chỉ phần mở rộng; file thiếu phần mở rộng cũng suy được từ nội dung) và PHẢI từ chối GIF/video với thông báo rõ ràng.
- **FR-015**: WHEN User đăng bài kèm GIF, hệ thống PHẢI chấp nhận URL GIF từ `tenor.com` (bao gồm cả embed `tenor.com/view/...` hiển thị iframe lẫn link ảnh trực tiếp `media.tenor.com/...` hiển thị bằng `<img>` — backend không gọi dịch vụ ngoài) hoặc `instagram.com` (hiển thị dưới dạng link + preview, KHÔNG nhúng iframe) do User cung cấp; WHERE URL thuộc domain khác, hệ thống PHẢI từ chối với thông báo rõ ràng. WHERE gif_url vượt quá 500 ký tự, hệ thống PHẢI từ chối với HTTP 400 (không được để lỗi DB 500).
- **FR-AUDIENCE**: WHERE User thực hiện thao tác phản hồi (like/unlike, thêm comment, đọc danh sách comment) lên một CommunityPost, hệ thống PHẢI kiểm tra quyền xem của User đó với bài đăng theo `audience`: `public` → mọi User; `friends` → User là bạn bè hoặc tác giả; `private` → chỉ tác giả. WHERE User không có quyền xem, hệ thống PHẢI từ chối với HTTP 403/404 (không rò rỉ sự tồn tại của bài đăng).
- **FR-VISIBILITY**: WHEN hệ thống trả dữ liệu trên mọi bề mặt đọc công khai (tìm kiếm user, leaderboard toàn server/bạn bè, activity feed, community feed, ranking challenge), hệ thống PHẢI CHỈ hiển thị User có account ở trạng thái `ACTIVE` (loại trừ BANNED và DELETED/soft-delete); WHERE User bị ban/xóa giữa vòng đời challenge, hệ thống PHẢI loại khỏi ranking khi tổng kết (FR-011).
- **FR-WITHDRAW**: WHEN User có lời mời kết bạn `pending` do chính mình gửi (`initiated_by = user`), hệ thống PHẢI cho phép User RÚT lời mời qua DELETE /friendships/{id} — xóa bản ghi lời mời, không phát sự kiện feed. WHERE lời mời không còn ở trạng thái `pending` do mình gửi (đã chấp nhận/hủy/hết hạn), hệ thống PHẢI chặn với HTTP 422. Ghi chú edge case: sau khi rút, phía người nhận có thể vẫn thấy lời mời cho tới lần refresh kế tiếp (không có realtime push).

### Key Entities *(include if feature involves data)*

- **Friendship**: Quan hệ giữa 2 User (user_id_1, user_id_2, status: pending/accepted/rejected/superseded, initiated_by, created_at, updated_at) — bất biến: tối đa 1 bản ghi HOẠT ĐỘNG (khác `superseded`) cho mỗi cặp không phân biệt thứ tự.
- **ActivityFeedItem**: Sự kiện hiển thị cho bạn bè (user_id, action_type: friendship_created/streak_milestone/new_pr, details_json, created_at).
- **LeaderboardEntry**: Bản ghi xếp hạng kỳ thi vô tận (user_id, current_streak_weeks, longest_streak_weeks, streak_start_week, rank, updated_at).
- **Challenge**: Thử thách có thời hạn do Admin tạo (name, goal_type, duration_days, start_date, end_date, status: open/finished — `closed` là trạng thái SUY RA, không lưu trữ, created_by).
- **ChallengeParticipant**: Người tham gia (challenge_id, user_id, joined_at, completed_at, final_rank).
- **CommunityPost**: Bài đăng cộng đồng (user_id, content, image, gif_url — tùy chọn, audience: public/friends/private, created_at).
- **User**: bổ sung cờ hồ sơ riêng tư `is_private` (do feature 001-profile-history sở hữu, mặc định private).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% trường hợp lời mời chéo đồng thời chỉ tạo đúng 1 quan hệ, không có lỗi hệ thống (500) trong mọi luồng kết bạn/tìm kiếm/feed.
- **SC-002**: Sự kiện thành tích mới (streak milestone, PR) hiển thị trên feed bạn bè trong vòng 1 phút sau khi xảy ra.
- **SC-003**: 100% Challenge được tổng kết tự động trong vòng 1 phút sau end_date, final_rank đầy đủ cho mọi người tham gia.
- **SC-004**: Bảng xếp hạng phản ánh thay đổi streak trong vòng 5 phút; thời gian phản hồi dưới 200ms với 10.000 user hoạt động.
- **SC-005**: Kết quả tìm kiếm người dùng trả đúng trạng thái quan hệ 100% (không có nút sai trạng thái).
- **SC-006**: 100% file không phải ảnh (video/GIF kể cả đổi đuôi) bị từ chối khi đăng bài; 100% URL GIF ngoài allowlist bị từ chối.

## Assumptions

- Streak giữ định nghĩa DUY NHẤT toàn hệ thống: chuỗi TUẦN liên tiếp đạt ≥3 buổi tập; bỏ 1 tuần → reset (constitution §4). Thống kê và leaderboard dùng chung.
- Kế thừa các giới hạn của spec 003: cooldown 30 ngày sau khi bị từ chối, tối đa 500 bạn bè, feed 7 ngày, leaderboard batch 5 phút. **Giới hạn "tối đa 5 lời mời/ngày" ĐÃ ĐƯỢC GỠ BỎ** (supersede FR-002 spec 003 — quyết định 2026-08-27); chống spam kết bạn dựa vào cooldown 30 ngày sau khi từ chối + cap 500 bạn.
- Dữ liệu cũ có thể đang chứa bản ghi friendship trùng cho cùng cặp; migration chuẩn hóa PHẢI gộp vào bản ghi "thắng" (accepted > pending > rejected mới nhất) và chuyển bản ghi thua sang trạng thái `superseded` — KHÔNG xóa cứng (constitution §7).
- Múi giờ tính streak: dùng múi giờ máy chủ cho mọi user trong đợt này (đã chốt — Clarifications); hỗ trợ múi giờ theo từng user là nợ kỹ thuật.
- `is_private` mặc định private, do 001-profile-history sở hữu (đã chốt trong Clarifications).
- GIF embed chỉ chạy ở Web client (Tenor nhúng iframe; Instagram link + preview); Mobile chưa hỗ trợ trong đợt này.
