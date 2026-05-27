-- 回填 demo 打卡 expires_at；未设置且公开可见的打卡补 24h TTL
UPDATE check_ins
SET expires_at = created_at + INTERVAL '24 hours'
WHERE expires_at IS NULL
  AND visibility IN ('PUBLIC', 'TONIGHT_ONLY');
