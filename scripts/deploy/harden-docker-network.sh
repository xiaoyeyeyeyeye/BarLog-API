#!/bin/bash
# 关闭 PostgreSQL / API 的公网端口映射，仅保留 Docker 内网 + Nginx :80
# 在 EC2 上执行：sudo bash harden-docker-network.sh
set -euo pipefail

ENV_FILE="${ENV_FILE:-/opt/alcohol-api/secrets/dev.env}"
NETWORK="${NETWORK:-backend_default}"
API_VOLUME="${API_VOLUME:-/opt/app/backend/app:/app:rw}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

echo "== Detect postgres volume =="
PG_VOLUME=$(docker inspect postgres --format '{{range .Mounts}}{{if eq .Destination "/var/lib/postgresql/data"}}{{.Name}}{{end}}{{end}}' 2>/dev/null || true)
if [[ -z "$PG_VOLUME" ]]; then
  PG_VOLUME=$(docker inspect postgres --format '{{range .Mounts}}{{if eq .Destination "/var/lib/postgresql/data"}}{{.Source}}{{end}}{{end}}' 2>/dev/null || true)
fi
if [[ -z "$PG_VOLUME" ]]; then
  echo "Could not detect postgres data volume; aborting." >&2
  exit 1
fi
echo "Postgres data: $PG_VOLUME"

POSTGRES_USER=$(docker inspect postgres --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_USER=//p' | head -1)
POSTGRES_PASSWORD=$(docker inspect postgres --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_PASSWORD=//p' | head -1)
POSTGRES_DB=$(docker inspect postgres --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^POSTGRES_DB=//p' | head -1)

echo "== Recreate postgres WITHOUT public 5432 =="
docker stop postgres
docker rm postgres

if [[ "$PG_VOLUME" == /* ]]; then
  docker run -d \
    --name postgres \
    --restart unless-stopped \
    --network "$NETWORK" \
    -e "POSTGRES_USER=${POSTGRES_USER}" \
    -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    -e "POSTGRES_DB=${POSTGRES_DB}" \
    -v "${PG_VOLUME}:/var/lib/postgresql/data" \
    postgres:15
else
  docker run -d \
    --name postgres \
    --restart unless-stopped \
    --network "$NETWORK" \
    -e "POSTGRES_USER=${POSTGRES_USER}" \
    -e "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    -e "POSTGRES_DB=${POSTGRES_DB}" \
    -v "${PG_VOLUME}:/var/lib/postgresql/data" \
    postgres:15
fi

echo "== Recreate spring-api bound to localhost:8080 only =="
docker stop spring-api
docker rm spring-api
docker run -d \
  --name spring-api \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  -p 127.0.0.1:8080:8090 \
  -v "$API_VOLUME" \
  --network "$NETWORK" \
  eclipse-temurin:17-jdk \
  java -jar /app/app.jar

sleep 12
echo "== Verify ports (5432 should NOT listen on 0.0.0.0) =="
ss -tlnp | grep -E '5432|8080|80 ' || true
curl -sf http://127.0.0.1:8080/health && echo
curl -sf http://127.0.0.1/health && echo

echo "Done. Also close AWS Security Group inbound for ports 5432 and 8080 if still open."
