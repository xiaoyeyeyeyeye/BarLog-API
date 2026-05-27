-- 聊天：会话与消息
-- psql -U alcohol_dev -d alcohol_dev -f V8_chat.sql

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

CREATE INDEX IF NOT EXISTS idx_conversation_members_user
    ON conversation_members(user_id, joined_at DESC);

CREATE TABLE IF NOT EXISTS chat_messages (
    id                  VARCHAR(64) PRIMARY KEY,
    conversation_id     VARCHAR(64) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id           VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body                TEXT NOT NULL,
    content_type        VARCHAR(32) NOT NULL DEFAULT 'text',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conversation
    ON chat_messages(conversation_id, created_at DESC);
