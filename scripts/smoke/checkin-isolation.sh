# 打卡用户隔离 — 部署后验证（双账号 curl）
#
# 用法（Linux / EC2）：
#   export BASE=http://127.0.0.1:8080
#   bash scripts/smoke/checkin-isolation.sh
#
# 前置：服务器 dev.env 已设 COMPAT_ALLOW_ANONYMOUS=false，且已部署含 CheckInAccessHelper 的 JAR

set -euo pipefail

BASE="${BASE:-http://127.0.0.1:8080}"
EMAIL_A="alice-smoke-$(date +%s)@test.com"
EMAIL_B="bob-smoke-$(date +%s)@test.com"

echo "== Register user A =="
RESP_A=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"displayName\":\"Alice\",\"email\":\"$EMAIL_A\",\"password\":\"password123\"}")
TOKEN_A=$(echo "$RESP_A" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
USER_A=$(echo "$RESP_A" | python3 -c "import sys,json; print(json.load(sys.stdin)['user']['id'])")

echo "== User A creates private check-in =="
CHECKIN=$(curl -s -X POST "$BASE/api/checkins" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{
    "photoUrl":"https://images.barlog.local/test.jpg",
    "drinkName":"Smoke Test",
    "drinkCategory":"cocktail",
    "barId":"bar_001",
    "barName":"Amber Room",
    "city":"Shanghai",
    "moodTags":["warm"],
    "cardStyle":"receipt",
    "visibility":"private"
  }')
CHECKIN_ID=$(echo "$CHECKIN" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

echo "== Register user B =="
RESP_B=$(curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"displayName\":\"Bob\",\"email\":\"$EMAIL_B\",\"password\":\"password123\"}")
TOKEN_B=$(echo "$RESP_B" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "== User B recent must not contain User A drink =="
RECENT_B=$(curl -s "$BASE/api/checkins/recent" -H "Authorization: Bearer $TOKEN_B")
echo "$RECENT_B" | python3 -c "import sys,json; items=json.load(sys.stdin).get('items',[]); assert all(i.get('drinkName')!='Smoke Test' for i in items), items"

echo "== User B detail on A check-in must 403 =="
CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/checkins/$CHECKIN_ID" -H "Authorization: Bearer $TOKEN_B")
test "$CODE" = "403"

echo "== User B list A user checkins must 403 =="
CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/users/$USER_A/checkins" -H "Authorization: Bearer $TOKEN_B")
test "$CODE" = "403"

echo "OK: check-in isolation verified"
