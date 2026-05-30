-- 为匿名/demo 用户（demo@barlog.app）插入联调打卡数据，便于 Diary / Gallery 有内容
-- 在 alcohol_dev 库执行：psql -U alcohol_dev -d alcohol_dev -f V5_demo_checkins.sql

INSERT INTO check_ins (
    id, user_id, photo_url, card_image_url, drink_name, drink_category,
    bar_id, location_name, city, area, mood_tags, rating, vibe_mumbling,
    card_style, visibility, social_status, created_at, expires_at
)
SELECT
    'checkin_demo_001',
    u.id,
    'https://images.barlog.local/checkins/negroni.jpg',
    'https://images.barlog.local/cards/negroni-card.jpg',
    'Smoked Negroni',
    'COCKTAIL',
    'bar_001',
    'Amber Room',
    '上海',
    'Jing''an',
    '["warm","bitter"]',
    9,
    'Orange peel, low lights, no rush.',
    'RECEIPT',
    'PUBLIC',
    'CHAT_OK',
    NOW() - INTERVAL '3 hours',
    NOW() + INTERVAL '21 hours'
FROM users u
WHERE u.email = 'demo@barlog.app'
ON CONFLICT (id) DO UPDATE SET
    visibility = EXCLUDED.visibility,
    social_status = EXCLUDED.social_status,
    created_at = EXCLUDED.created_at,
    expires_at = EXCLUDED.expires_at,
    vibe_mumbling = EXCLUDED.vibe_mumbling;

INSERT INTO check_ins (
    id, user_id, photo_url, drink_name, drink_category,
    bar_id, location_name, city, area, mood_tags, rating, vibe_mumbling,
    card_style, visibility, social_status, created_at, expires_at
)
SELECT
    'checkin_demo_002',
    u.id,
    'https://images.barlog.local/checkins/martini.jpg',
    'Dry Martini',
    'COCKTAIL',
    'bar_003',
    'Last Call Lab',
    '上海',
    'Huangpu',
    '["crisp","quiet"]',
    8,
    'Clean, cold, perfect counter seat.',
    'FILM_TICKET',
    'TONIGHT_ONLY',
    'NONE',
    NOW() - INTERVAL '1 hour',
    NOW() + INTERVAL '23 hours'
FROM users u
WHERE u.email = 'demo@barlog.app'
ON CONFLICT (id) DO UPDATE SET
    visibility = EXCLUDED.visibility,
    social_status = EXCLUDED.social_status,
    created_at = EXCLUDED.created_at,
    expires_at = EXCLUDED.expires_at,
    vibe_mumbling = EXCLUDED.vibe_mumbling;
