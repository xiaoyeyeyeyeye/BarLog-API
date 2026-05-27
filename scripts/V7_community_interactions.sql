-- 社区互动：点赞与评论
-- psql -U alcohol_dev -d alcohol_dev -f V7_community_interactions.sql

CREATE TABLE IF NOT EXISTS check_in_reactions (
    id              VARCHAR(64) PRIMARY KEY,
    check_in_id     VARCHAR(64) NOT NULL REFERENCES check_ins(id) ON DELETE CASCADE,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(check_in_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_check_in_reactions_check_in
    ON check_in_reactions(check_in_id, created_at DESC);

CREATE TABLE IF NOT EXISTS check_in_comments (
    id              VARCHAR(64) PRIMARY KEY,
    check_in_id     VARCHAR(64) NOT NULL REFERENCES check_ins(id) ON DELETE CASCADE,
    user_id         VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body            TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_check_in_comments_check_in
    ON check_in_comments(check_in_id, created_at ASC);
