-- V24: 018-fix-social-flows — bất biến: tối đa 1 lần đăng ký mỗi (challenge_id, user_id).
-- Chặn join trùng + race đồng thời (T5/D2) ở tầng DB; service bắt DataIntegrityViolation → 422.
-- Kiểu dữ liệu giữa H2 và PostgreSQL đều là BIGINT (portable — không dùng UNIQUE ràng buộc FK inline).

ALTER TABLE challenge_participants
    ADD CONSTRAINT uq_challenge_participants_challenge_user UNIQUE (challenge_id, user_id);