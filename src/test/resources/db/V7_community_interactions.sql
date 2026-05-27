CREATE TABLE IF NOT EXISTS check_in_reactions (
    id              VARCHAR(64) PRIMARY KEY,
    check_in_id     VARCHAR(64) NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(check_in_id, user_id)
);

CREATE TABLE IF NOT EXISTS check_in_comments (
    id              VARCHAR(64) PRIMARY KEY,
    check_in_id     VARCHAR(64) NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    body            TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
