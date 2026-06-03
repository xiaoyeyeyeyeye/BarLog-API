#!/bin/bash
set -euo pipefail

ENV="/opt/alcohol-api/secrets/dev.env"
CLIENT_ID="${1:?GOOGLE_CLIENT_ID required}"
CLIENT_SECRET="${2:?GOOGLE_CLIENT_SECRET required}"

set_kv() {
  local key="$1"
  local val="$2"
  if sudo grep -q "^${key}=" "$ENV"; then
    sudo sed -i "s|^${key}=.*|${key}=${val}|" "$ENV"
  else
    echo "${key}=${val}" | sudo tee -a "$ENV" >/dev/null
  fi
}

set_kv "GOOGLE_CLIENT_ID" "$CLIENT_ID"
set_kv "GOOGLE_CLIENT_SECRET" "$CLIENT_SECRET"

echo "Updated keys:"
sudo grep -E '^GOOGLE_(CLIENT|OAUTH)' "$ENV" | sed 's/=.*/=***masked***/'

sudo docker stop spring-api
sudo docker rm spring-api
sudo docker run -d \
  --name spring-api \
  --restart unless-stopped \
  --env-file "$ENV" \
  -p 8080:8090 \
  -v /opt/app/backend/app:/app:rw \
  --network backend_default \
  eclipse-temurin:17-jdk \
  java -jar /app/app.jar

sleep 15
curl -sf http://127.0.0.1:8080/health
echo ""
