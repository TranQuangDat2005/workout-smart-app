-- V6: Nutrition — theo General Spec §5 (UC-12, UC-13)
CREATE TABLE food_items (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    calories_per_100g NUMERIC(8,2) NOT NULL,
    protein_per_100g  NUMERIC(8,2) NOT NULL DEFAULT 0,
    carb_per_100g     NUMERIC(8,2) NOT NULL DEFAULT 0,
    fat_per_100g      NUMERIC(8,2) NOT NULL DEFAULT 0,
    source            VARCHAR(20) NOT NULL DEFAULT 'system',
    created_by        BIGINT,
    deleted_at        TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_food_items_name ON food_items (name);
CREATE INDEX idx_food_items_created_by ON food_items (created_by);

CREATE TABLE meal_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    meal_number INT NOT NULL,
    log_date    DATE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_meal_logs_user_date ON meal_logs (user_id, log_date);

CREATE TABLE meal_entries (
    id              BIGSERIAL PRIMARY KEY,
    meal_log_id     BIGINT NOT NULL REFERENCES meal_logs(id),
    food_item_id    BIGINT NOT NULL REFERENCES food_items(id),
    portion_grams   NUMERIC(8,2) NOT NULL,
    total_calories  NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_protein   NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_carb      NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_fat       NUMERIC(8,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_meal_entries_log ON meal_entries (meal_log_id);

CREATE TABLE meal_daily_summaries (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    log_date     DATE NOT NULL,
    summary_json TEXT NOT NULL,
    UNIQUE (user_id, log_date)
);

CREATE TABLE body_metrics (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    weight_kg    NUMERIC(5,2) NOT NULL,
    body_fat_pct NUMERIC(5,2),
    waist_cm     NUMERIC(6,2),
    chest_cm     NUMERIC(6,2),
    arm_cm       NUMERIC(6,2),
    recorded_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_body_metrics_user ON body_metrics (user_id, recorded_at);
