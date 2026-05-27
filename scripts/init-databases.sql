-- 在 PostgreSQL 上创建三套独立数据库（同一 PG 实例即可）
-- 以超级用户执行：docker exec postgres psql -U app_user -d app_db -f /tmp/init-databases.sql
-- 执行前把下面三处 REPLACE_* 改成强密码（不要写进 Git，只在服务器上改）

-- ========== 1. 服务器研发库（远程同事联调）==========
CREATE USER alcohol_dev WITH PASSWORD 'REPLACE_DEV_DB_PASSWORD';
CREATE DATABASE alcohol_dev OWNER alcohol_dev;
GRANT ALL PRIVILEGES ON DATABASE alcohol_dev TO alcohol_dev;

-- ========== 2. 测试库（以后启用）==========
CREATE USER alcohol_test WITH PASSWORD 'REPLACE_TEST_DB_PASSWORD';
CREATE DATABASE alcohol_test OWNER alcohol_test;
GRANT ALL PRIVILEGES ON DATABASE alcohol_test TO alcohol_test;

-- ========== 3. 生产库（以后启用，先建库不部署应用）==========
CREATE USER alcohol_prod WITH PASSWORD 'REPLACE_PROD_DB_PASSWORD';
CREATE DATABASE alcohol_prod OWNER alcohol_prod;
GRANT ALL PRIVILEGES ON DATABASE alcohol_prod TO alcohol_prod;

-- 建表（对每个库各执行一次 schema-full.sql）：
--   psql -U alcohol_dev  -d alcohol_dev  -f scripts/schema-full.sql
--   psql -U alcohol_dev  -d alcohol_dev  -f scripts/V4_frontend_demo.sql
--   psql -U alcohol_test -d alcohol_test -f scripts/schema-full.sql
--   psql -U alcohol_prod -d alcohol_prod -f scripts/schema-full.sql

-- 本机已有库 alcohol 可继续给本地 mvn spring-boot:run 使用，无需删除。
