#!/bin/bash
# 将 EC2 上 spring-api 切换为测试环境（alcohol_test 库）
# 不修改 dev.env / alcohol_dev 密码
# 用法：sudo bash switch-server-to-test.sh
set -euo pipefail

ENV_DIR="${ENV_DIR:-/opt/alcohol-api/secrets}"
DEV_ENV="${ENV_DIR}/dev.env"
TEST_ENV="${ENV_DIR}/test.env"
NETWORK="${NETWORK:-backend_default}"
API_VOLUME="${API_VOLUME:-/opt/app/backend/app:/app:rw}"
SQL_DIR="${SQL_DIR:-/tmp/barlog-sql}"
TEST_DB_PASSWORD="${TEST_DB_PASSWORD:-barlog_test}"
TEST_JWT="${TEST_JWT:-barlog-test-jwt-secret-for-uat-only-32}"

if [[ ! -f "$DEV_ENV" ]]; then
  echo "Missing $DEV_ENV" >&2
  exit 1
fi

echo "== Set alcohol_test password (simple, test-only) =="
docker exec postgres psql -U app_user -d app_db -c \
  "ALTER USER alcohol_test WITH PASSWORD '${TEST_DB_PASSWORD}';"

echo "== Initialize alcohol_test schema (if empty) =="
TABLE_COUNT=$(docker exec postgres psql -U alcohol_test -d alcohol_test -tAc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';")
if [[ "${TABLE_COUNT:-0}" -eq "0" ]]; then
  for f in schema-full.sql V4_frontend_demo.sql V5_demo_checkins.sql; do
    if [[ -f "${SQL_DIR}/${f}" ]]; then
      docker cp "${SQL_DIR}/${f}" postgres:/tmp/"${f}"
      docker exec postgres psql -U alcohol_test -d alcohol_test -f /tmp/"${f}"
    else
      echo "WARN: missing ${SQL_DIR}/${f}, skip" >&2
    fi
  done
else
  echo "alcohol_test already has ${TABLE_COUNT} tables, skip SQL import"
fi

echo "== Build test.env from dev.env (keep Google/CORS secrets) =="
sudo python3 - <<PY
from pathlib import Path
dev = Path("${DEV_ENV}").read_text().splitlines()
overrides = {
    "SPRING_PROFILES_ACTIVE": "test",
    "SPRING_DATASOURCE_URL": "jdbc:postgresql://postgres:5432/alcohol_test?currentSchema=public",
    "SPRING_DATASOURCE_USERNAME": "alcohol_test",
    "SPRING_DATASOURCE_PASSWORD": "${TEST_DB_PASSWORD}",
    "JWT_SECRET": "${TEST_JWT}",
    "UPLOAD_DIR": "/app/uploads-test",
    "ALCOHOL_UPLOAD_DIR": "/app/uploads-test",
    "COMPAT_ALLOW_ANONYMOUS": "false",
    "ALCOHOL_COMPAT_ALLOW_ANONYMOUS": "false",
    "SECURITY_PASSWORD_POLICY": "false",
    "SECURITY_RATE_LIMIT_ENABLED": "true",
    "SECURITY_AUTH_MAX_RPM": "60",
    "SECURITY_MAX_REGISTER_PER_HOUR": "30",
    "SECURITY_MAX_FAILED_LOGINS": "20",
}
out = []
seen = set()
for line in dev:
    if not line.strip() or line.strip().startswith("#"):
        out.append(line)
        continue
    key = line.split("=", 1)[0]
    if key in overrides:
        out.append(f"{key}={overrides[key]}")
        seen.add(key)
    else:
        out.append(line)
        seen.add(key)
for key, val in overrides.items():
    if key not in seen:
        out.append(f"{key}={val}")
Path("${TEST_ENV}").write_text("\n".join(out) + "\n")
PY
sudo chmod 600 "$TEST_ENV"

sudo mkdir -p /opt/app/backend/app/uploads-test
sudo chown -R ec2-user:nginx /opt/app/backend/app/uploads-test 2>/dev/null || true

echo "== Recreate spring-api with test.env =="
docker stop spring-api
docker rm spring-api
docker run -d \
  --name spring-api \
  --restart unless-stopped \
  --env-file "$TEST_ENV" \
  -p 127.0.0.1:8080:8090 \
  -v "$API_VOLUME" \
  --network "$NETWORK" \
  eclipse-temurin:17-jdk \
  java -jar /app/app.jar

sleep 15
curl -sf http://127.0.0.1:8080/health
echo ""
curl -sf http://127.0.0.1/health
echo ""
docker exec spring-api printenv SPRING_PROFILES_ACTIVE
docker exec spring-api printenv SPRING_DATASOURCE_URL | sed 's/alcohol_test/alcohol_test/'

echo ""
echo "Done. Test DB: alcohol_test / user alcohol_test / password ${TEST_DB_PASSWORD}"
echo "Dev env unchanged: ${DEV_ENV}"
