# 社区/聊天表 — 服务器一次性执行
# 用法（EC2）：
#   sudo docker exec -i postgres psql -U postgres -d alcohol_dev < scripts/V7_community_interactions.sql
#   sudo docker exec -i postgres psql -U postgres -d alcohol_dev < scripts/V8_chat.sql
#   sudo docker exec -i postgres psql -U postgres -d alcohol_dev < scripts/V9_community_expires_backfill.sql

echo "Run the three psql commands above on the server if community returns 500."
