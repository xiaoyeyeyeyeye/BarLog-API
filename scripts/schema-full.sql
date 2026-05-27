-- ============================================================
-- BarLog / 余味 — 完整建表 + 种子数据（P0 + P1 合并版）
-- 使用步骤见 scripts/README.md
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id                  VARCHAR(64) PRIMARY KEY,
    phone               VARCHAR(32) UNIQUE,
    password            VARCHAR(128),
    email               VARCHAR(256) UNIQUE,
    email_verified      SMALLINT NOT NULL DEFAULT 0,
    phone_verified      SMALLINT NOT NULL DEFAULT 0,
    country_code        VARCHAR(8) NOT NULL DEFAULT '+65',
    locale              VARCHAR(16) NOT NULL DEFAULT 'en-SG',
    primary_auth_provider VARCHAR(32) NOT NULL DEFAULT 'PHONE',
    status              SMALLINT NOT NULL DEFAULT 1,
    nickname            VARCHAR(64) NOT NULL,
    avatar_url          VARCHAR(512),
    handle              VARCHAR(64) UNIQUE,
    avatar_emoji        VARCHAR(16),
    profile_bg_theme    SMALLINT NOT NULL DEFAULT 0,
    city                VARCHAR(64),
    bio                 VARCHAR(256),
    gender              VARCHAR(16) DEFAULT 'UNSPECIFIED',
    mbti                VARCHAR(8),
    frequent_area       VARCHAR(128),
    favorite_drink      VARCHAR(128),
    spotify_connected   SMALLINT NOT NULL DEFAULT 0,
    spotify_genres      TEXT NOT NULL DEFAULT '[]',
    first_check_in_at   TIMESTAMP,
    privacy_settings    TEXT NOT NULL DEFAULT '{}',
    social_preferences  TEXT NOT NULL DEFAULT '{}',
    tonight_enabled     SMALLINT NOT NULL DEFAULT 0,
    tonight_social_status VARCHAR(32),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS drinks (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(128) NOT NULL UNIQUE,
    category        VARCHAR(32) NOT NULL DEFAULT 'COCKTAIL',
    flavor_tags     TEXT NOT NULL DEFAULT '[]',
    description     TEXT,
    icon_url        VARCHAR(512),
    is_classic      SMALLINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS badges (
    id              VARCHAR(64) PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    icon_url        VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    google_place_id VARCHAR(128),
    source          VARCHAR(16) NOT NULL DEFAULT 'seed',
    synced_at       TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bars_city ON bars(city);
CREATE UNIQUE INDEX IF NOT EXISTS idx_bars_google_place_id ON bars(google_place_id)
    WHERE google_place_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS check_ins (
    id                  VARCHAR(64) PRIMARY KEY,
    user_id             VARCHAR(64) NOT NULL REFERENCES users(id),
    photo_url           VARCHAR(512) NOT NULL,
    drink_name          VARCHAR(128) NOT NULL,
    drink_id            VARCHAR(64) REFERENCES drinks(id),
    drink_category      VARCHAR(32) NOT NULL,
    bar_id              VARCHAR(64) REFERENCES bars(id),
    location_name       VARCHAR(256),
    city                VARCHAR(64),
    area                VARCHAR(128),
    mood_tags           TEXT NOT NULL DEFAULT '[]',
    flavor_tags         TEXT NOT NULL DEFAULT '[]',
    vibe_mumbling       VARCHAR(256),
    diary_text          TEXT,
    rating              SMALLINT,
    voice_note_url      VARCHAR(512),
    ai_card_quote       TEXT,
    ai_card_quote_source VARCHAR(256),
    card_style          VARCHAR(32) NOT NULL,
    card_image_url      VARCHAR(512),
    visibility          VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
    social_status       VARCHAR(32) DEFAULT 'NONE',
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_check_ins_user ON check_ins(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_check_ins_gallery ON check_ins(visibility, expires_at, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_check_ins_drink ON check_ins(drink_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_check_ins_city ON check_ins(city, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_check_ins_bar ON check_ins(bar_id, created_at DESC);

CREATE TABLE IF NOT EXISTS check_in_reactions (
    id              VARCHAR(64) PRIMARY KEY,
    check_in_id     VARCHAR(64) NOT NULL REFERENCES check_ins(id) ON DELETE CASCADE,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(check_in_id, user_id)
);

CREATE TABLE IF NOT EXISTS check_in_comments (
    id              VARCHAR(64) PRIMARY KEY,
    check_in_id     VARCHAR(64) NOT NULL REFERENCES check_ins(id) ON DELETE CASCADE,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body            TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversations (
    id                      VARCHAR(64) PRIMARY KEY,
    type                    VARCHAR(32) NOT NULL DEFAULT 'direct',
    title                   VARCHAR(256),
    last_message_preview    VARCHAR(512),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation_members (
    id                  VARCHAR(64) PRIMARY KEY,
    conversation_id     VARCHAR(64) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id             VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    unread_count        INT NOT NULL DEFAULT 0,
    last_read_at        TIMESTAMP,
    joined_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id                  VARCHAR(64) PRIMARY KEY,
    conversation_id     VARCHAR(64) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id           VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body                TEXT NOT NULL,
    content_type        VARCHAR(32) NOT NULL DEFAULT 'text',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_badges (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
    badge_id        VARCHAR(64) NOT NULL REFERENCES badges(id),
    unlocked_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, badge_id)
);

CREATE TABLE IF NOT EXISTS user_drinks (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
    drink_id        VARCHAR(64) NOT NULL REFERENCES drinks(id),
    check_in_count  INTEGER NOT NULL DEFAULT 1,
    first_unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_check_in_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, drink_id)
);

CREATE TABLE IF NOT EXISTS personas (
    user_id             VARCHAR(64) PRIMARY KEY REFERENCES users(id),
    main_drink_type     VARCHAR(128),
    secondary_drink_type VARCHAR(128),
    flavor_profile      TEXT NOT NULL DEFAULT '[]',
    night_keywords      TEXT NOT NULL DEFAULT '[]',
    social_tendency     VARCHAR(128),
    generated_text      TEXT,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bar_favorites (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
    bar_id          VARCHAR(64) NOT NULL REFERENCES bars(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, bar_id)
);

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

CREATE TABLE IF NOT EXISTS ai_recommend_logs (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id),
    mood            VARCHAR(32),
    spotify_on      SMALLINT NOT NULL DEFAULT 0,
    result_type     VARCHAR(16),
    result_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_auth_providers (
    id                  VARCHAR(64) PRIMARY KEY,
    user_id             VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            VARCHAR(32) NOT NULL,
    provider_user_id    VARCHAR(256) NOT NULL,
    provider_email      VARCHAR(256),
    display_name        VARCHAR(128),
    avatar_url          VARCHAR(512),
    raw_profile         TEXT,
    linked_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, provider_user_id)
);
CREATE INDEX IF NOT EXISTS idx_user_auth_providers_user ON user_auth_providers(user_id);

CREATE TABLE IF NOT EXISTS verification_codes (
    id                  VARCHAR(64) PRIMARY KEY,
    target              VARCHAR(256) NOT NULL,
    channel             VARCHAR(16) NOT NULL,
    purpose             VARCHAR(32) NOT NULL,
    code_hash           VARCHAR(128) NOT NULL,
    country_code        VARCHAR(8),
    expires_at          TIMESTAMP NOT NULL,
    consumed_at         TIMESTAMP,
    attempt_count       SMALLINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_verification_target ON verification_codes(target, purpose, created_at DESC);

-- 种子：经典鸡尾酒
INSERT INTO drinks (id, name, category, flavor_tags, description, is_classic) VALUES
('drink-negroni', 'Negroni', 'COCKTAIL', '["bitter","citrus","herbal","strong"]', '金酒、金巴利、甜味美思的经典苦甜平衡。', 1),
('drink-martini', 'Martini', 'COCKTAIL', '["dry","citrus","herbal"]', '干净利落的金酒或伏特加马天尼。', 1),
('drink-old-fashioned', 'Old Fashioned', 'COCKTAIL', '["whisky","bitter","sweet"]', '威士忌、方糖、苦精的复古经典。', 1),
('drink-manhattan', 'Manhattan', 'COCKTAIL', '["whisky","sweet","herbal"]', '威士忌与甜味美思的都市夜晚。', 1),
('drink-margarita', 'Margarita', 'COCKTAIL', '["citrus","salty","bright"]', '龙舌兰、青柠、橙味利口酒。', 1),
('drink-mojito', 'Mojito', 'COCKTAIL', '["mint","citrus","light"]', '朗姆、薄荷、青柠的清爽夏夜。', 1),
('drink-daiquiri', 'Daiquiri', 'COCKTAIL', '["citrus","rum","clean"]', '朗姆与青柠的简洁之美。', 1),
('drink-gin-tonic', 'Gin Tonic', 'COCKTAIL', '["bitter","citrus","light"]', '金酒与汤力水的气泡感。', 1),
('drink-highball', 'Highball', 'COCKTAIL', '["light","effervescent"]', '威士忌或烧酎加苏打的高球系。', 1),
('drink-aperol-spritz', 'Aperol Spritz', 'COCKTAIL', '["bitter","citrus","bubbly"]', '阿佩罗、起泡酒、苏打。', 1),
('drink-black-russian', 'Black Russian', 'COCKTAIL', '["coffee","dark","strong"]', '伏特加与咖啡利口酒的深夜感。', 1),
('drink-white-russian', 'White Russian', 'COCKTAIL', '["creamy","coffee","smooth"]', '奶油版俄罗斯，柔和顺滑。', 1),
('drink-moscow-mule', 'Moscow Mule', 'COCKTAIL', '["ginger","citrus","spicy"]', '伏特加、姜汁啤酒、青柠。', 1),
('drink-cosmopolitan', 'Cosmopolitan', 'COCKTAIL', '["citrus","fruity","bright"]', '伏特加、蔓越莓、青柠。', 1),
('drink-bloody-mary', 'Bloody Mary', 'COCKTAIL', '["savory","spicy","tomato"]', '伏特加番茄汁的醒酒或续杯。', 1),
('drink-whiskey-sour', 'Whiskey Sour', 'COCKTAIL', '["citrus","whisky","balanced"]', '威士忌酸酒的酸甜结构。', 1),
('drink-sidecar', 'Sidecar', 'COCKTAIL', '["citrus","brandy","elegant"]', '干邑、橙味利口酒、柠檬。', 1),
('drink-gimlet', 'Gimlet', 'COCKTAIL', '["citrus","gin","sharp"]', '金酒与青柠汁的锐利线条。', 1),
('drink-sazerac', 'Sazerac', 'COCKTAIL', '["whisky","herbal","bitter"]', '黑麦威士忌与苦艾的新奥尔良经典。', 1),
('drink-espresso-martini', 'Espresso Martini', 'COCKTAIL', '["coffee","sweet","energetic"]', '伏特加、咖啡利口酒、浓缩咖啡。', 1)
ON CONFLICT (name) DO NOTHING;

INSERT INTO badges (id, code, name, description) VALUES
('badge-first-checkin', 'FIRST_CHECK_IN', 'First Check-in', '完成首次打卡'),
('badge-first-negroni', 'FIRST_NEGRONI', 'First Negroni', '首次打卡 Negroni'),
('badge-solo-night', 'SOLO_NIGHT', 'Solo Night', '独自夜晚打卡'),
('badge-bitter-collector', 'BITTER_COLLECTOR', 'Bitter Taste Collector', '累计 5 次选择苦味系酒款'),
('badge-mocktail', 'MOCKTAIL_FRIENDLY', 'Mocktail Friendly', '打卡无酒精饮品'),
('badge-10-bars', 'TEN_BARS', '10 Bars Checked', '在 10 个不同地点打卡'),
('badge-3-cities', 'THREE_CITIES', '3 Cities Sipped', '在 3 个城市打卡')
ON CONFLICT (code) DO NOTHING;

INSERT INTO bars (id, name, type_label, city, open_hours, avg_rating, review_count, latitude, longitude) VALUES
('bar-botanist', 'The Botanist', 'R&B Bar', '上海', '22:00–04:00', 8.8, 128, 31.2310, 121.4720),
('bar-midnight', 'Midnight Oil', '深夜约会', '上海', '20:00–03:00', 9.2, 96, 31.2280, 121.4680),
('bar-lantern', 'Lantern Bar', '餐吧', '上海', '18:00–02:00', 8.5, 64, 31.2350, 121.4750)
ON CONFLICT (id) DO NOTHING;
