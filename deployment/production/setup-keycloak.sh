#!/usr/bin/env bash
# Sets up the clinic realm on the production Keycloak: ../keycloak_setup.sh with the addresses,
# admin login and mail server from .env, and the production choices: no password login through
# the API, two-factor sign-in for every staff account, and only the web app's address allowed.
# Safe to run again, e.g. after changing .env.
#
#   ./setup-keycloak.sh

set -euo pipefail
cd "$(dirname "$0")"
[[ -f .env ]] || { echo "error: no .env here; copy .env.example and fill it in" >&2; exit 1; }
# .env as Docker Compose reads it: KEY=value lines, no shell quoting (values may hold spaces)
while IFS='=' read -r key value; do
  [[ $key =~ ^[A-Z_][A-Z0-9_]*$ ]] || continue
  value=${value%\"}; value=${value#\"}
  export "$key=$value"
done < .env

KEYCLOAK_URL="https://$AUTH_HOST" \
FRONTEND_ORIGINS="[\"$WEB_ORIGIN\"]" \
FRONTEND_HOME_URL="$WEB_ORIGIN/" \
PASSWORD_LOGIN=false \
STAFF_TWO_FACTOR=true \
  ../keycloak_setup.sh
