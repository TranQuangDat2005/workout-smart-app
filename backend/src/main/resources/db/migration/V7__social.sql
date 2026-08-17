-- V7: Social — theo General Spec §5 (UC-14, UC-15)
CREATE TABLE friendships (
    id           BIGSERIAL PRIMARY KEY,
    user_id_1    BIGINT NOT NULL REFERENCES users(id),
    user_id_2    BIGINT NOT NULL REFERENCES users(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    initiated_by BIGINT NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_friendships_u1 ON friendships (user_id_1, status);
CREATE INDEX idx_friendships_u2 ON friendships (user_id_2, status);

CREATE TABLE activity_feed (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    action_type  VARCHAR(50) NOT NULL,
    details_json TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_feed_user ON activity_feed (user_id, created_at);

CREATE TABLE leaderboard_entries (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL REFERENCES users(id),
    current_streak_weeks  INT NOT NULL DEFAULT 0,
    longest_streak_weeks  INT NOT NULL DEFAULT 0,
    rank                  INT,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_leaderboard_user ON leaderboard_entries (user_id);

CREATE TABLE challenges (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    goal_type     VARCHAR(30),
    duration_days INT NOT NULL,
    start_date    DATE,
    end_date      DATE,
    status        VARCHAR(20) NOT NULL DEFAULT 'open',
    created_by    BIGINT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE challenge_participants (
    id           BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL REFERENCES challenges(id),
    user_id      BIGINT NOT NULL REFERENCES users(id),
    joined_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at TIMESTAMP WITH TIME ZONE,
    final_rank   INT
);

CREATE INDEX idx_challenge_parts_ch ON challenge_participants (challenge_id);
