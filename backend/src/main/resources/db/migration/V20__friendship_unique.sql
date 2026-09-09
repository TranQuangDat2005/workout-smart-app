-- V20: 018-fix-social-flows — bất biến: tối đa 1 bản ghi HOẠT ĐỘNG cho mỗi cặp bạn bè.
-- Thiết kế portable (H2 + PostgreSQL): cặp chuẩn hóa (pair_min/pair_max) + active_marker
-- (NULL cho bản ghi superseded; unique index cho phép nhiều NULL nhưng chỉ 1 bản ghi hoạt động).

-- 1) Cột cặp chuẩn hóa (không phân biệt thứ tự user_id_1/user_id_2) + cờ hoạt động
ALTER TABLE friendships ADD COLUMN pair_min BIGINT;
ALTER TABLE friendships ADD COLUMN pair_max BIGINT;
ALTER TABLE friendships ADD COLUMN active_marker INT;

-- 2) Backfill cặp chuẩn hóa
UPDATE friendships
SET pair_min = LEAST(user_id_1, user_id_2),
    pair_max = GREATEST(user_id_1, user_id_2)
WHERE pair_min IS NULL;

-- 3) Dedupe dữ liệu cũ (KHÔNG xóa — constitution §7): giữ bản ghi thắng
--    accepted > pending > rejected; cùng trạng thái → updated_at mới nhất, rồi id nhỏ hơn.
--    Bản ghi thua → 'superseded'.
UPDATE friendships f
SET status = 'superseded'
WHERE f.status <> 'superseded'
  AND EXISTS (
    SELECT 1
    FROM friendships w
    WHERE w.status <> 'superseded'
      AND w.pair_min = f.pair_min
      AND w.pair_max = f.pair_max
      AND (
        CASE w.status WHEN 'accepted' THEN 0 WHEN 'pending' THEN 1 WHEN 'rejected' THEN 2 ELSE 3 END
        < CASE f.status WHEN 'accepted' THEN 0 WHEN 'pending' THEN 1 WHEN 'rejected' THEN 2 ELSE 3 END
        OR (
          CASE w.status WHEN 'accepted' THEN 0 WHEN 'pending' THEN 1 WHEN 'rejected' THEN 2 ELSE 3 END
          = CASE f.status WHEN 'accepted' THEN 0 WHEN 'pending' THEN 1 WHEN 'rejected' THEN 2 ELSE 3 END
          AND (w.updated_at > f.updated_at OR (w.updated_at = f.updated_at AND w.id < f.id))
        )
      )
  );

-- 4) Cờ hoạt động: 1 = đang dùng nghiệp vụ, NULL = superseded
UPDATE friendships
SET active_marker = CASE WHEN status <> 'superseded' THEN 1 ELSE NULL END;

-- 5) Unique index: chỉ 1 bản ghi hoạt động cho mỗi cặp (NULLs distinct — H2 + PostgreSQL)
CREATE UNIQUE INDEX uq_friendships_active_pair
  ON friendships (pair_min, pair_max, active_marker);
