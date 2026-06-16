-- Expired public check-in: should still appear in Gallery / Community feed
INSERT INTO check_ins (
    id, user_id, photo_url, card_image_url, drink_name, drink_category,
    bar_id, location_name, city, area, mood_tags, rating, vibe_mumbling,
    card_style, visibility, social_status, created_at, expires_at
)
SELECT
    'checkin_expired_public',
    u.id,
    'https://images.barlog.local/checkins/expired.jpg',
    'https://images.barlog.local/cards/expired-card.jpg',
    'Old Fashioned',
    'COCKTAIL',
    'bar_001',
    'Amber Room',
    '上海',
    'Jing''an',
    '["warm"]',
    8,
    'Still visible after expiry window.',
    'RECEIPT',
    'PUBLIC',
    'CHAT_OK',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '2 days'
FROM users u
WHERE u.email = 'demo@barlog.app'
ON CONFLICT (id) DO UPDATE SET
    visibility = EXCLUDED.visibility,
    created_at = EXCLUDED.created_at,
    expires_at = EXCLUDED.expires_at,
    vibe_mumbling = EXCLUDED.vibe_mumbling;
