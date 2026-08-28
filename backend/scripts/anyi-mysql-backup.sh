#!/usr/bin/env bash
set -Eeuo pipefail

ENV_FILE="${ANYI_ENV_FILE:-/opt/anyiapp2/backend/.env}"
BACKUP_DIR="${ANYI_BACKUP_DIR:-/home/ubuntu/anyi-db-backups}"
RETENTION_DAYS="${ANYI_BACKUP_RETENTION_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"

if [[ ! -r "$ENV_FILE" ]]; then
  echo "Cannot read env file: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

: "${MYSQL_HOST:?MYSQL_HOST is required}"
: "${MYSQL_USER:?MYSQL_USER is required}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"

MYSQL_PORT="${MYSQL_PORT:-3306}"
UPLOADS_DIR="${ANYI_UPLOADS_DIR:-${ANYI_DATA_DIR:-/var/lib/anyi-memorial-api}/uploads}"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR" 2>/dev/null || true

tmp_cnf="$(mktemp)"
db_tmp=""
uploads_tmp=""
cleanup() {
  rm -f "$tmp_cnf"
  if [[ -n "$db_tmp" ]]; then
    rm -f "$db_tmp"
  fi
  if [[ -n "$uploads_tmp" ]]; then
    rm -f "$uploads_tmp"
  fi
}
trap cleanup EXIT

chmod 600 "$tmp_cnf"
cat >"$tmp_cnf" <<EOF
[client]
user=${MYSQL_USER}
password=${MYSQL_PASSWORD}
host=${MYSQL_HOST}
port=${MYSQL_PORT}
protocol=TCP
EOF

db_dump="${BACKUP_DIR}/anyi-mysql-${MYSQL_DATABASE}-${STAMP}.sql.gz"
db_tmp="${db_dump}.tmp"
mysqldump \
  --defaults-extra-file="$tmp_cnf" \
  --single-transaction \
  --quick \
  --triggers \
  --no-tablespaces \
  "$MYSQL_DATABASE" \
  | gzip -c >"$db_tmp"
mv "$db_tmp" "$db_dump"
db_tmp=""

echo "MySQL backup written: $db_dump"

if [[ -d "$UPLOADS_DIR" ]]; then
  uploads_tar="${BACKUP_DIR}/anyi-uploads-${STAMP}.tar.gz"
  uploads_tmp="${uploads_tar}.tmp"
  tar -C "$(dirname "$UPLOADS_DIR")" -czf "$uploads_tmp" "$(basename "$UPLOADS_DIR")"
  mv "$uploads_tmp" "$uploads_tar"
  uploads_tmp=""
  echo "Uploads backup written: $uploads_tar"
else
  echo "Uploads directory not found, skipped: $UPLOADS_DIR"
fi

if [[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] && [[ "$RETENTION_DAYS" -gt 0 ]]; then
  find "$BACKUP_DIR" -type f \( \
    -name 'anyi-mysql-*.sql.gz' -o \
    -name 'anyi-uploads-*.tar.gz' \
  \) -mtime +"$RETENTION_DAYS" -delete
  echo "Retention applied: ${RETENTION_DAYS} days"
else
  echo "Retention skipped: ANYI_BACKUP_RETENTION_DAYS=$RETENTION_DAYS"
fi
