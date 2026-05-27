-- BarLog Demo 扩展（P1）— 在 P0 schema-full.sql 执行之后运行
-- 对应文档：docs/BARLOG_DEMO_DESIGN.md §3.3–3.7

-- users 扩展
ALTER TABLE users ADD COLUMN IF NOT EXISTS handle VARCHAR(64) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_emoji VARCHAR(16);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_bg_theme SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS mbti VARCHAR(8);
ALTER TABLE users ADD COLUMN IF NOT EXISTS spotify_connected SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS spotify_genres TEXT NOT NULL DEFAULT '[]';
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_check_in_at TIMESTAMP;

-- bars
CREATE TABLE IF NOT EXISTS bars (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    type_label      VARCHAR(64),
    city            VARCHAR(64),
    area            VARCHAR(128),
    address         VARCHAR(256),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    open_hours      VARCHAR(128),
    avg_rating      DECIMAL(3,1),
    review_count    INTEGER NOT NULL DEFAULT 0,
    cover_url       VARCHAR(512),
    is_active       SMALLINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bars_city ON bars(city);

-- bar_favorites
CREATE TABLE IF NOT EXISTS bar_favorites (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
    bar_id          VARCHAR(64) NOT NULL REFERENCES bars(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, bar_id)
);

-- barbti_results
CREATE TABLE IF NOT EXISTS barbti_results (
    user_id         VARCHAR(64) PRIMARY KEY REFERENCES users(id),
    type_code       VARCHAR(32) NOT NULL,
    subtitle        VARCHAR(128),
    description     TEXT,
    trait_tags      TEXT NOT NULL DEFAULT '[]',
    scores          TEXT NOT NULL DEFAULT '{}',
    answers         TEXT,
    completed_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- check_ins 扩展
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS rating SMALLINT;
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS flavor_tags TEXT NOT NULL DEFAULT '[]';
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS diary_text TEXT;
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS voice_note_url VARCHAR(512);
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS ai_card_quote TEXT;
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS ai_card_quote_source VARCHAR(256);
ALTER TABLE check_ins ADD COLUMN IF NOT EXISTS bar_id VARCHAR(64) REFERENCES bars(id);

-- ai_recommend_logs（可选）
CREATE TABLE IF NOT EXISTS ai_recommend_logs (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
    mood            VARCHAR(32),
    spotify_on      SMALLINT NOT NULL DEFAULT 0,
    result_type     VARCHAR(16),
    result_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 示例酒吧种子（Demo 三家）
INSERT INTO bars (id, name, type_label, city, open_hours, avg_rating, review_count, latitude, longitude) VALUES
('bar-botanist', 'The Botanist', 'R&B Bar', '上海', '22:00–04:00', 8.8, 128, 31.2310, 121.4720),
('bar-midnight', 'Midnight Oil', '深夜约会', '上海', '20:00–03:00', 9.2, 96, 31.2280, 121.4680),
('bar-lantern', 'Lantern Bar', '餐吧', '上海', '18:00–02:00', 8.5, 64, 31.2350, 121.4750)
ON CONFLICT (id) DO NOTHING;
