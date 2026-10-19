#!/bin/bash

BACKUP_DIR="$(dirname "$0")/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

mkdir -p "$BACKUP_DIR"

echo "Backing up cms_db..."
docker exec cms-db pg_dump -U cms_user -d cms_db > "$BACKUP_DIR/cms_db_$TIMESTAMP.sql"

echo "Backing up keycloak_cms_db..."
docker exec cms-db pg_dump -U cms_user -d keycloak_cms_db > "$BACKUP_DIR/keycloak_cms_db_$TIMESTAMP.sql"

echo "Done. Backups saved to $BACKUP_DIR"
