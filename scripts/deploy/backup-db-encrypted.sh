#!/bin/bash
# 加密 PostgreSQL 逻辑备份（需 gpg 或 openssl）
# 用法：BACKUP_PASSPHRASE='your-strong-pass' bash backup-db-encrypted.sh alcohol_dev
set -euo pipefail

DB_NAME="${1:-alcohol_dev}"
DB_USER="${DB_USER:-alcohol_dev}"
CONTAINER="${PG_CONTAINER:-postgres}"
OUT_DIR="${BACKUP_DIR:-/opt/alcohol-api/backups}"
STAMP=$(date +%Y%m%d-%H%M%S)
PLAIN="${OUT_DIR}/${DB_NAME}-${STAMP}.sql.gz"
ENC="${PLAIN}.enc"

mkdir -p "$OUT_DIR"
chmod 700 "$OUT_DIR"

echo "Dumping ${DB_NAME} ..."
docker exec "$CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" --no-owner --no-acl | gzip > "$PLAIN"

if [[ -n "${BACKUP_PASSPHRASE:-}" ]]; then
  openssl enc -aes-256-cbc -pbkdf2 -salt -pass pass:"$BACKUP_PASSPHRASE" -in "$PLAIN" -out "$ENC"
  rm -f "$PLAIN"
  chmod 600 "$ENC"
  echo "Encrypted backup: $ENC"
else
  chmod 600 "$PLAIN"
  echo "WARNING: BACKUP_PASSPHRASE not set; backup is gzip only (not encrypted): $PLAIN"
fi

find "$OUT_DIR" -type f -mtime +14 -delete 2>/dev/null || true
echo "Old backups (>14d) pruned."
