#!/usr/bin/env bash
# Makes the clinic's first administrator: a Keycloak user in the clinic realm with the ADMIN role
# and a profile in the API's database. It prints a one-time password; at the first sign-in
# Keycloak asks for a new one and for two-factor setup. Other staff are then invited from the app.
#
#   ./create-admin.sh EMAIL FIRST_NAME LAST_NAME [PHONE]
#
# Run it after setup-keycloak.sh, with the stack up (the API makes the tables on its first start).

set -euo pipefail
cd "$(dirname "$0")"
[[ $# -ge 3 ]] || { echo "usage: $0 EMAIL FIRST_NAME LAST_NAME [PHONE]" >&2; exit 1; }
EMAIL=$1 FIRST=$2 LAST=$3 PHONE=${4:-}
# .env as Docker Compose reads it: KEY=value lines, no shell quoting (values may hold spaces)
while IFS='=' read -r key value; do
  [[ $key =~ ^[A-Z_][A-Z0-9_]*$ ]] || continue
  value=${value%\"}; value=${value#\"}
  export "$key=$value"
done < .env
KC="https://$AUTH_HOST"
die() { echo "error: $*" >&2; exit 1; }

TOKEN=$(curl -sS -f "$KC/realms/master/protocol/openid-connect/token" \
          -d grant_type=password -d client_id=admin-cli \
          --data-urlencode "username=$KC_ADMIN_USERNAME" --data-urlencode "password=$KC_ADMIN_PASSWORD" \
        | jq -r .access_token) || die "Keycloak admin login failed; check KC_ADMIN_* in .env"
api() { curl -sS -f -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$@"; }

PASSWORD=$(head -c 18 /dev/urandom | base64 | tr '+/' '-_')
ACCOUNT=$(jq -n --arg e "$EMAIL" --arg f "$FIRST" --arg l "$LAST" --arg p "$PASSWORD" --arg phone "$PHONE" '{
    username: $e, email: $e, firstName: $f, lastName: $l, enabled: true, emailVerified: true,
    requiredActions: ["CONFIGURE_TOTP"],
    credentials: [{type: "password", value: $p, temporary: true}]
  } + if $phone == "" then {} else {attributes: {phoneNumber: [$phone]}} end')
api -X POST "$KC/admin/realms/clinic/users" --data "$ACCOUNT" >/dev/null \
  || die "Keycloak refused the user (already there? a required field such as PHONE missing?)"

ID=$(api "$KC/admin/realms/clinic/users?email=$(jq -rn --arg e "$EMAIL" '$e|@uri')&exact=true" | jq -r '.[0].id')
ROLE=$(api "$KC/admin/realms/clinic/roles/ADMIN")
api -X POST "$KC/admin/realms/clinic/users/$ID/role-mappings/realm" --data "[$ROLE]" >/dev/null

docker compose exec -T postgres psql -U "$POSTGRES_USER" -d clinic_db -q -v ON_ERROR_STOP=1 \
  -c "INSERT INTO user_profile (id) VALUES ('$ID') ON CONFLICT (id) DO NOTHING"

echo "Administrator $EMAIL is ready. One-time password (shown once):"
echo "  $PASSWORD"
echo "Sign in on the web app; Keycloak then asks for a new password and two-factor setup."
