#!/bin/bash

BACKUP_DIR="$(dirname "$0")/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

mkdir -p "$BACKUP_DIR"

echo "Backing up clinic_db..."
docker exec clinic-db pg_dump -U clinic_user -d clinic_db > "$BACKUP_DIR/clinic_db_$TIMESTAMP.sql"

echo "Backing up keycloak_clinic_db..."
docker exec clinic-db pg_dump -U clinic_user -d keycloak_clinic_db > "$BACKUP_DIR/keycloak_clinic_db_$TIMESTAMP.sql"

echo "Done. Backups saved to $BACKUP_DIR"
