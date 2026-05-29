#!/usr/bin/env bash
# 社区/聊天表 + 演示打卡数据 — EC2 一次性执行
# 用法（在仓库根目录，文件已上传到 /tmp/）：
#   sudo docker exec -i postgres psql -U alcohol_dev -d alcohol_dev < /tmp/V5_demo_checkins.sql
#   sudo docker exec -i postgres psql -U alcohol_dev -d alcohol_dev < /tmp/V7_community_interactions.sql
#   sudo docker exec -i postgres psql -U alcohol_dev -d alcohol_dev < /tmp/V8_chat.sql
#   sudo docker exec -i postgres psql -U alcohol_dev -d alcohol_dev < /tmp/V9_community_expires_backfill.sql

set -euo pipefail

DB_USER="${DB_USER:-alcohol_dev}"
DB_NAME="${DB_NAME:-alcohol_dev}"
CONTAINER="${POSTGRES_CONTAINER:-postgres}"

run_sql() {
  local file="$1"
  echo "==> $file"
  sudo docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$file"
}

for script in V5_demo_checkins.sql V7_community_interactions.sql V8_chat.sql V9_community_expires_backfill.sql; do
  if [[ -f "/tmp/$script" ]]; then
    run_sql "/tmp/$script"
  elif [[ -f "scripts/$script" ]]; then
    run_sql "scripts/$script"
  else
    echo "skip missing $script"
  fi
done

echo "Done. Verify: curl -H \"Authorization: Bearer <token>\" \"http://127.0.0.1:8080/api/gallery/feed?range=7d\""
