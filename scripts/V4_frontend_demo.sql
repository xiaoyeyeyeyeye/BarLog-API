-- 前端联调 demo 数据（与 mock-server/db.json 对齐）
-- 用法：psql -U postgres -d alcohol -f scripts/V4_frontend_demo.sql

-- 酒吧英文名/上海坐标（前端 map 使用 city=Shanghai）
UPDATE bars SET city = 'Shanghai', area = COALESCE(area, 'Jing''an')
WHERE id IN ('bar-botanist', 'bar-midnight', 'bar-lantern');

INSERT INTO bars (id, name, type_label, city, area, address, open_hours, avg_rating, review_count, latitude, longitude, is_active)
VALUES
('bar_001', 'Amber Room', 'cocktail,speakeasy', 'Shanghai', 'Jing''an', '88 Yanping Road', '22:00–04:00', 9.4, 128, 31.2298, 121.4442, 1),
('bar_002', 'Neon Fig', 'wine,small plates', 'Shanghai', 'Xuhui', '16 Fuxing West Road', '20:00–02:00', 9.0, 96, 31.2103, 121.4401, 1),
('bar_003', 'Last Call Lab', 'cocktail,experimental', 'Shanghai', 'Huangpu', '212 Beijing East Road', '22:00–04:00', 9.6, 64, 31.2357, 121.4829, 1)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  city = EXCLUDED.city,
  area = EXCLUDED.area,
  address = EXCLUDED.address,
  avg_rating = EXCLUDED.avg_rating,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  is_active = 1;

-- demo 用户由后端 allow-anonymous 首次请求时自动创建（demo@barlog.app / password123）
