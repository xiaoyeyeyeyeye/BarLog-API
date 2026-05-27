-- ============================================================
-- V3 — 海外认证（新加坡/东南亚）
-- 适用：已有 P0/P1 库升级；新环境请直接用 schema-full.sql
-- ============================================================

-- users：支持邮箱、国际手机号、OAuth（password/phone 可为空）
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(256);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS country_code VARCHAR(8) NOT NULL DEFAULT '+65';
ALTER TABLE users ADD COLUMN IF NOT EXISTS locale VARCHAR(16) NOT NULL DEFAULT 'en-SG';
ALTER TABLE users ADD COLUMN IF NOT EXISTS primary_auth_provider VARCHAR(32) NOT NULL DEFAULT 'PHONE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS status SMALLINT NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone ON users(phone) WHERE phone IS NOT NULL;

-- 第三方账号绑定（Google / Facebook 等）
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

-- 短信/邮箱验证码
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

COMMENT ON TABLE user_auth_providers IS 'OAuth 绑定：Google/Facebook 等';
COMMENT ON TABLE verification_codes IS 'OTP 验证码（SMS/Email），存 hash 不存明文';
