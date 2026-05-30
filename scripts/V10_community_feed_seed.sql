-- 社区 Feed 演示数据：多用户、公开/今夜可见、时间戳随 NOW() 刷新
-- 用法：psql -U alcohol_dev -d alcohol_dev -f V10_community_feed_seed.sql

-- 刷新 V5 两条 demo 打卡的时间窗口
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

-- 其他用户的社区酒卡（按 email 绑定已有账号）
INSERT INTO check_ins (
    id, user_id, photo_url, card_image_url, drink_name, drink_category,
    bar_id, location_name, city, area, mood_tags, rating, vibe_mumbling,
    card_style, visibility, social_status, created_at, expires_at
)
SELECT v.id, u.id, v.photo_url, v.card_image_url, v.drink_name, v.drink_category,
       v.bar_id, v.location_name, v.city, v.area, v.mood_tags, v.rating, v.vibe_mumbling,
       v.card_style, v.visibility, v.social_status, v.created_at, v.expires_at
FROM (VALUES
    ('checkin_feed_001', 'demo2@barlog.app', 'https://images.barlog.local/checkins/old-fashioned.jpg', 'https://images.barlog.local/cards/old-fashioned-card.jpg', 'Old Fashioned', 'COCKTAIL', 'bar_002', 'Neon Fig', '上海', 'Xuhui', '["smoky","slow"]', 9, 'Bourbon glow, jazz in the background.', 'RECEIPT', 'PUBLIC', 'CHAT_OK', NOW() - INTERVAL '5 hours', NOW() + INTERVAL '19 hours'),
    ('checkin_feed_002', '2411438681@qq.com', 'https://images.barlog.local/checkins/espresso-martini.jpg', NULL, 'Espresso Martini', 'COCKTAIL', 'bar_001', 'Amber Room', '上海', 'Jing''an', '["bold","sweet"]', 8, 'Foam like midnight, caffeine kick at the end.', 'FILM_TICKET', 'TONIGHT_ONLY', 'OPEN_TO_CHAT', NOW() - INTERVAL '4 hours', NOW() + INTERVAL '20 hours'),
    ('checkin_feed_003', 'qw@qq.app', 'https://images.barlog.local/checkins/gin-tonic.jpg', NULL, 'Garden Gin & Tonic', 'COCKTAIL', 'bar_003', 'Last Call Lab', '上海', 'Huangpu', '["fresh","herbal"]', 7, 'Cucumber cold, window seat, city lights.', 'RECEIPT', 'PUBLIC', 'NONE', NOW() - INTERVAL '6 hours', NOW() + INTERVAL '18 hours'),
    ('checkin_feed_004', 'demo@qq.app', 'https://images.barlog.local/checkins/negroni.jpg', NULL, 'White Negroni', 'COCKTAIL', 'bar_002', 'Neon Fig', '上海', 'Xuhui', '["floral","dry"]', 9, 'Lighter than expected, still bitter enough.', 'RECEIPT', 'TONIGHT_ONLY', 'FIND_BUDDY', NOW() - INTERVAL '2 hours', NOW() + INTERVAL '22 hours'),
    ('checkin_feed_005', 'elliezhang893@gmail.com', 'https://images.barlog.local/checkins/wine.jpg', NULL, 'Orange Natural Wine', 'WINE', 'bar_001', 'Amber Room', '上海', 'Jing''an', '["funky","bright"]', 8, 'Skin contact, orange peel, low light.', 'FILM_TICKET', 'PUBLIC', 'CHAT_OK', NOW() - INTERVAL '7 hours', NOW() + INTERVAL '17 hours'),
    ('checkin_feed_006', '1320287144@qq.com', 'https://images.barlog.local/checkins/whisky.jpg', NULL, 'Highball', 'WHISKY', 'bar_003', 'Last Call Lab', '上海', 'Huangpu', '["easy","cold"]', 7, 'Whisky soda, long night, no rush.', 'RECEIPT', 'PUBLIC', 'NONE', NOW() - INTERVAL '8 hours', NOW() + INTERVAL '16 hours'),
    ('checkin_feed_007', 'suzannewa233@gmail.com', 'https://images.barlog.local/checkins/martini.jpg', NULL, 'Vesper', 'COCKTAIL', 'bar_002', 'Neon Fig', '上海', 'Xuhui', '["sharp","classic"]', 10, 'Shaken cold, lemon twist, spy-movie energy.', 'FILM_TICKET', 'TONIGHT_ONLY', 'OPEN_TO_CHAT', NOW() - INTERVAL '90 minutes', NOW() + INTERVAL '22 hours'),
    ('checkin_feed_008', '1625309103@qq.com', 'https://images.barlog.local/checkins/negroni.jpg', NULL, 'Boulevardier', 'COCKTAIL', 'bar_001', 'Amber Room', '上海', 'Jing''an', '["rich","warm"]', 9, 'Whiskey negroni cousin, winter in a glass.', 'RECEIPT', 'PUBLIC', 'CHAT_OK', NOW() - INTERVAL '30 minutes', NOW() + INTERVAL '23 hours')
) AS v(id, email, photo_url, card_image_url, drink_name, drink_category, bar_id, location_name, city, area, mood_tags, rating, vibe_mumbling, card_style, visibility, social_status, created_at, expires_at)
JOIN users u ON u.email = v.email
ON CONFLICT (id) DO UPDATE SET
    visibility = EXCLUDED.visibility,
    social_status = EXCLUDED.social_status,
    created_at = EXCLUDED.created_at,
    expires_at = EXCLUDED.expires_at,
    vibe_mumbling = EXCLUDED.vibe_mumbling,
    drink_name = EXCLUDED.drink_name,
    location_name = EXCLUDED.location_name;
