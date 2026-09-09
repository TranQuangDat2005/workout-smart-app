# Research: 018-fix-social-flows (Phase 0)

Tất cả NEEDS CLARIFICATION đã được owner chốt ở bước clarify — file này ghi các quyết định kỹ thuật + rationale.

## R1 — Bất biến 1 quan hệ bạn bè/pair (FR-001, FR-002)

- **Decision**: Cặp chuẩn hóa `pair_min`/`pair_max` + cờ `active_marker` + unique index thường (portable H2 + PostgreSQL):
  ```sql
  CREATE UNIQUE INDEX uq_friendships_active_pair
    ON friendships (pair_min, pair_max, active_marker);
  ```
  Bản ghi hoạt động có `active_marker = 1`; bản ghi `superseded` có `active_marker = NULL` (unique index cho phép nhiều NULL ở cả H2 lẫn PostgreSQL). App tự gán pair_min/pair_max/active_marker trong `@PrePersist` của entity.
- **Rationale**: Phương án ban đầu (partial index `WHERE status <> 'superseded'` trên biểu thức LEAST/GREATEST) KHÔNG chạy được trên H2 (test DB) — đã kiểm chứng thực nghiệm H2 2.2.224: không hỗ trợ expression index lẫn filtered index. Thiết kế cột chuẩn hóa đạt cùng hiệu ứng, chạy cả hai DB.
- **Alternatives**: (a) partial index chỉ PostgreSQL + bỏ ràng buộc ở H2 (lệch môi trường); (b) xóa cứng bản ghi thua (vi phạm constitution §7 — owner không chọn); (c) archive sang bảng khác (owner chọn A — giữ cùng bảng).

## R2 — Dedupe dữ liệu cũ (FR-001, Clarifications A)

- **Decision**: Migration V20 chạy dedupe: với mỗi cặp có >1 bản ghi, giữ bản ghi "thắng" theo thứ tự accepted > pending > rejected, ưu tiên `updated_at` mới nhất; các bản ghi còn lại → `status='superseded'`. Không DELETE.
- **Rationale**: Bảo toàn dữ liệu (constitution §7), khôi phục được nếu cần; sau dedupe mới tạo unique index để không fail.
- **Alternatives**: xóa cứng (vi phạm §7 — owner không chọn); archive sang bảng khác (thêm bảng, phức tạp hơn mức cần).

## R3 — Repository chịu lỗi khi nhiều bản ghi

- **Decision**: `FriendshipRepository.findBetween` đổi từ `Optional<Friendship>` sang `List<Friendship>`; service lấy bản ghi hoạt động đầu tiên (status khác superseded) nếu còn sót dữ liệu cũ.
- **Rationale**: `Optional` + nhiều row → `IncorrectResultSizeDataAccessException` → 500 (bug C1). `List` không bao giờ ném; sau V20 luôn ≤1 bản ghi hoạt động.

## R4 — Tái dùng bản ghi sau cooldown (FR-002)

- **Decision**: `sendRequest` khi gặp bản ghi `rejected` đã qua 30 ngày → UPDATE bản ghi đó (`status='pending'`, `initiated_by=người gửi mới`) thay vì INSERT.
- **Rationale**: Đảm bảo bất biến 1 row/pair; lịch sử reject vẫn nằm trên cùng row (cooldown dùng `updated_at` như hiện tại).

## R5 — GIF embed Tenor / Instagram (FR-015)

- **Decision**: `community_posts.gif_url` (nullable) — backend validate: URL `https`, host ∈ {`tenor.com`, `*.tenor.com`, `instagram.com`, `*.instagram.com`}; KHÔNG fetch, KHÔNG gọi oEmbed từ backend. Web client: Tenor → iframe embed; Instagram → thẻ link + preview ảnh (nếu có). Tenor pattern v1: `https://tenor.com/view/<slug>-<id>` iframe — xác nhận định dạng khi implement (fallback: thẻ `<a>` + ảnh preview nếu iframe bị chặn).
- **Rationale**: Đúng pattern allowlist của constitution (giống YouTube/Spotify: URL do User cung cấp, chỉ Web client); tránh rủi ro Instagram oEmbed cần token (ck-predict đã cảnh báo).
- **Alternatives**: upload GIF lên SeaweedFS (vi phạm quyết định Q2); gọi oEmbed từ backend (vi phạm allowlist).

## R6 — Media upload: magic bytes (FR-014)

- **Decision**: `SeaweedStorageService` đọc ≤512 bytes đầu file, kiểm tra signature: JPEG (`FF D8 FF`), PNG (`89 50 4E 47`), WEBP (`RIFF....WEBP`). Extension chỉ là gợi ý thứ cấp. Bỏ `gif` khỏi danh sách cho phép.
- **Rationale**: Chặn video/GIF đổi đuôi .jpg (edge case trong spec); rẻ và đủ cho v1.

## R7 — Feed events publish + dedupe (FR-005)

- **Decision (đã điều chỉnh khi implement)**: `ProfileService.completeSession` gọi TRỰC TIẾP `ActivityFeedService.publishStreakMilestone`/`publishPr` trong cùng transaction buổi tập, bọc try/catch + log warn (lỗi feed KHÔNG rollback buổi tập). Milestone: chỉ ghi khi `currentStreakWeeks` tăng so với giá trị lưu trong event `streak_milestone` gần nhất (query 1 row, `detailsJson` chứa giá trị streak); cộng thêm 1 event khi chạm mốc 10/30/50/100. PR: ghi khi tổng volume buổi > giá trị trong event `new_pr` gần nhất.
- **Rationale**: Phương án ban đầu (`@TransactionalEventListener AFTER_COMMIT`) bị bỏ vì transaction mở trong callback afterCommit KHÔNG commit được (Spring quirk — đã tái hiện bằng test: event chạy, save gọi nhưng 0 row). Gọi trực tiếp cùng transaction: đơn giản (KISS), feed commit cùng buổi tập, 2 query nhỏ không đáng kể so với response; try/catch bảo đảm buổi tập không hỏng.
- **Alternatives**: (a) REQUIRES_NEW qua self-proxy (phức tạp, YAGNI); (b) ghi trong transaction chính KHÔNG try/catch (lỗi feed làm hỏng buổi tập — bị loại); (c) queue async (YAGNI).

## R8 — Leaderboard: incremental + scheduled (FR-007/008/009)

- **Decision**: `completeSession` → `LeaderboardSyncService.updateEntry(userId)` tính streak bằng `StreakCalculator` (bỏ cap 1000 session — đếm theo tuần qua query `COUNT` nhóm tuần hoặc giữ danh sách nhưng không giới hạn page size) rồi upsert entry (`streak_start_week` = thứ 2 của tuần đầu chuỗi hiện tại). Job `@Scheduled(fixedDelay=5min)` recompute các user ACTIVE có session mới từ lần chạy trước + gán `rank`. Read path: `GET /leaderboard` đọc thuần `leaderboard_entries` (top 100 theo streak desc, streak_start_week asc, user_id asc) + luôn append row của viewer (tính on-the-fly nếu thiếu).
- **Rationale**: Loại bỏ full-scan/request (H6); đáp ứng batch ≤5 phút (SC-004); `rank` persist thỏa FR-008; tie-break đúng Clarifications Q3.
- **Alternatives**: chỉ scheduled không incremental (độ trễ tối đa 5 phút vẫn OK nhưng feed milestone cần streak ngay → cần tính streak tại thời điểm completeSession — nên có incremental); cache Redis (YAGNI).

## R9 — Challenge finalizer (FR-010/011/012)

- **Decision**: `ChallengeFinalizer` `@Scheduled(fixedDelay=60s)`: (1) `UPDATE challenges SET status='closed' WHERE status='open' AND end_date <= CURRENT_DATE` (guard chống chạy 2 lần/đa instance); (2) với mỗi challenge vừa closed: với mỗi participant tính streak tại end_date (StreakCalculator với dữ liệu đến end_date), sort theo FR-007 → ghi `completed_at`, `final_rank`; (3) `UPDATE challenges SET status='finished'`. Xếp hạng = streak tại end_date (Clarifications B).
- **Rationale**: SC-003 (tổng kết ≤1 phút sau end_date); idempotent nhờ transition state + UPDATE guard; 1 instance v1 (ghi chú).
- **Alternatives**: cron 1 lần/ngày (vi phạm SC-003); trigger DB (vi phạm §3 — logic ở service).

## R10 — Privacy is_private (FR-013)

- **Decision**: `users.is_private boolean NOT NULL DEFAULT true` (migration V22 ở feature này; spec do 001 sở hữu). Social consume: search ẩn email/avatar cho người lạ khi target private; feed friends-tab giữ nguyên (bài của user private chỉ hiện cho bạn bè, trừ bài `audience=public`); leaderboard chỉ display_name + rank (đã đúng).
- **Rationale**: Clarifications A (default private đúng General Spec §3); field đặt ở bảng core nên migration đi kèm feature này để 1 lần deploy.

## R11 — Streak theo múi giờ máy chủ (Clarifications B)

- **Decision**: Giữ `ZoneId.systemDefault()` cho StreakCalculator; ghi TODO chuẩn hóa `users.timezone` ở backlog.
- **Rationale**: Owner chọn B — tránh phình scope đợt này; định nghĩa "tuần" thống nhất theo giờ server cho mọi user.

## R12 — N+1 search relationshipStatus (FR-004)

- **Decision**: 1 query batch: `SELECT user_id_1, user_id_2, status FROM friendships WHERE (user_id_1 = :me AND user_id_2 IN :ids) OR (user_id_2 = :me AND user_id_1 IN :ids) AND status <> 'superseded'` → map sang `none/pending_sent/pending_received/accepted`.
- **Rationale**: search trả ≤20 kết quả; batch 1 query thay vì 20 (ck-predict R4).
