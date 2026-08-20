-- V19: 018 social feed — bài đăng cộng đồng (text + ảnh SeaweedFS) + like + comment
CREATE TABLE community_posts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    content    TEXT,
    media_url  VARCHAR(500),
    media_type VARCHAR(10),
    audience   VARCHAR(10) NOT NULL DEFAULT 'public',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_created ON community_posts (id DESC);

CREATE TABLE post_likes (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_post_like UNIQUE (post_id, user_id)
);

CREATE TABLE post_comments (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_comments_post ON post_comments (post_id, id);
