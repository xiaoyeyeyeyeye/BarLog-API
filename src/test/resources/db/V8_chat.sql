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
    conversation_id     VARCHAR(64) NOT NULL,
    user_id             VARCHAR(64) NOT NULL,
    unread_count        INT NOT NULL DEFAULT 0,
    last_read_at        TIMESTAMP,
    joined_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id                  VARCHAR(64) PRIMARY KEY,
    conversation_id     VARCHAR(64) NOT NULL,
    sender_id           VARCHAR(64) NOT NULL,
    body                TEXT NOT NULL,
    content_type        VARCHAR(32) NOT NULL DEFAULT 'text',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
