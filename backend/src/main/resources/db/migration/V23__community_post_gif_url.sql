-- V23: 018-fix-social-flows — GIF embed bài đăng cộng đồng (FR-015).
-- URL do User cung cấp; host allowlist tenor.com/instagram.com (validate ở service, backend không gọi external).
ALTER TABLE community_posts ADD COLUMN gif_url VARCHAR(500);
