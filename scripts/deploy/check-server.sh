#!/bin/bash
# 在服务器上快速检查研发环境是否正常
set -euo pipefail
BASE="${1:-http://127.0.0.1:8090}"
echo "Checking $BASE ..."
curl -sf "$BASE/health" | head -c 200
echo ""
curl -sf "$BASE/api/diary/summary" | head -c 200
echo ""
echo "OK"
