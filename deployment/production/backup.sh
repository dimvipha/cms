#!/usr/bin/env bash
# Dumps the clinic's database and Keycloak's into backups/ next to this script, compressed and
# dated, and keeps the last 14 of each. Meant for cron, e.g. every night at 2:
#   0 2 * * * /home/deploy/cms/deployment/production/backup.sh >> /home/deploy/backup.log 2>&1
# Copy backups/ off the server too: a backup on the same disk does not survive losing the server.

set -euo pipefail
cd "$(dirname "$0")"
POSTGRES_USER=$(grep -E '^POSTGRES_USER=' .env | cut -d= -f2-)
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p backups

for db in clinic_db keycloak; do
  docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$db" --clean --if-exists \
    | gzip > "backups/$db-$STAMP.sql.gz"
  # shellcheck disable=SC2012 # names are ours, without spaces
  ls -1t "backups/$db-"*.sql.gz | tail -n +15 | xargs -r rm --
done
echo "$(date -Is) backed up clinic_db and keycloak to $(pwd)/backups"
