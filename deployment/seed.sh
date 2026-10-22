#!/usr/bin/env bash
# Loads demo data for local testing:
#   - seed.sql into clinic_db (departments, doctors with schedules, patients,
#     medicines and stock, past visits and invoices); skipped once loaded
#   - a Keycloak login for each staff role: the five seeded doctors (with the
#     same user ids as their doctor rows) plus a nurse, receptionist,
#     pharmacist and lab technician, all using SEED_PASSWORD
#   - an ADMIN login (admin@cms.local, SEED_ADMIN_PASSWORD)
#   - a user_profile row for each of them, as POST /api/v1/staff would create
#
# Run keycloak_setup.sh first so the roles exist.
#
#   ./seed.sh
#   SEED_PASSWORD=secret123 ./seed.sh

set -euo pipefail
cd "$(dirname "$0")"

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:1201}"
REALM="${KEYCLOAK_REALM:-clinic}"
SEED_PASSWORD="${SEED_PASSWORD:-doctor1234}"
SEED_ADMIN_PASSWORD="${SEED_ADMIN_PASSWORD:-admin1234}"

# id|email|first name|last name|role — doctor ids must match the doctor rows in
# seed.sql; an empty id lets Keycloak pick one
STAFF=(
  "aaaa0001-0000-0000-0000-000000000001|dr.somchai@cms.local|Somchai|Raksa|DOCTOR"
  "aaaa0002-0000-0000-0000-000000000002|dr.nipa@cms.local|Nipa|Chantra|DOCTOR"
  "aaaa0003-0000-0000-0000-000000000003|dr.wirat@cms.local|Wirat|Phutthon|DOCTOR"
  "aaaa0004-0000-0000-0000-000000000004|dr.malee@cms.local|Malee|Siriwan|DOCTOR"
  "aaaa0005-0000-0000-0000-000000000005|dr.prasong@cms.local|Prasong|Klinkaew|DOCTOR"
  "|nurse@cms.local|Ploy|Wongsa|NURSE"
  "|reception@cms.local|Kanya|Sukjai|RECEPTIONIST"
  "|pharmacist@cms.local|Anan|Thongdee|PHARMACIST"
  "|labtech@cms.local|Wichai|Meesuk|LAB_TECHNICIAN"
)

die() { echo "error: $*" >&2; exit 1; }
env_value() { grep -E "^$1=" .env | cut -d= -f2-; }

command -v jq >/dev/null || die "jq is required"
DB_USER=$(env_value POSTGRES_USERNAME)

echo "==> Clinic data"
if [[ $(docker exec clinic-db psql -U "$DB_USER" -d clinic_db -Atc "select count(*) from department") != 0 ]]; then
  echo "  already loaded, skipping seed.sql"
else
  docker exec -i clinic-db psql -U "$DB_USER" -d clinic_db -v ON_ERROR_STOP=1 -q -1 -f /dev/stdin < seed.sql
  echo "  loaded seed.sql"
fi

echo "==> Staff logins"
TOKEN=$(curl -sS -f "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
          -d grant_type=password -d client_id=admin-cli \
          --data-urlencode "username=${KC_ADMIN_USERNAME:-$(env_value KC_ADMIN_USERNAME)}" \
          --data-urlencode "password=${KC_ADMIN_PASSWORD:-$(env_value KC_ADMIN_PASSWORD)}" \
        | jq -r .access_token) || die "admin login failed; check KC_ADMIN_USERNAME / KC_ADMIN_PASSWORD in .env"

seed_user() {  # id email first last role password
  jq -n --arg id "$1" --arg email "$2" --arg first "$3" --arg last "$4" --arg role "$5" --arg pw "$6" \
    '{username: $email, email: $email, firstName: $first, lastName: $last,
      enabled: true, emailVerified: true, realmRoles: [$role],
      credentials: [{type: "password", value: $pw, temporary: false}]}
     + (if $id == "" then {} else {id: $id} end)'
}

users=$({
  for entry in "${STAFF[@]}"; do
    IFS='|' read -r id email first last role <<<"$entry"
    seed_user "$id" "$email" "$first" "$last" "$role" "$SEED_PASSWORD"
  done
  seed_user "" "admin@cms.local" "Clinic" "Admin" "ADMIN" "$SEED_ADMIN_PASSWORD"
} | jq -s .)

# partial import keeps the given user ids, which the doctor rows reference
result=$(curl -sS -f -X POST "$KEYCLOAK_URL/admin/realms/$REALM/partialImport" \
           -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
           -d "$(jq -n --argjson users "$users" '{ifResourceExists: "SKIP", users: $users}')") \
         || die "importing users failed"
echo "  $(jq -r '"\(.added) added, \(.skipped) already existed"' <<<"$result")"

# GET /api/v1/user-profiles needs a row per user, keyed by their Keycloak id
ids=$(for email in $(jq -r '.[].email' <<<"$users"); do
  curl -sS -f -H "Authorization: Bearer $TOKEN" \
    "$KEYCLOAK_URL/admin/realms/$REALM/users?email=$email&exact=true" | jq -r '.[0].id'
done)
values=$(printf "('%s')," $ids)
docker exec clinic-db psql -U "$DB_USER" -d clinic_db -q -v ON_ERROR_STOP=1 \
  -c "INSERT INTO user_profile (id) VALUES ${values%,} ON CONFLICT (id) DO NOTHING"

for entry in "${STAFF[@]}"; do
  IFS='|' read -r _ email _ _ role <<<"$entry"
  printf '  %-15s %s\n' "$role" "$email"
done
echo "  password: $SEED_PASSWORD"
printf '  %-15s %s (password %s)\n' "ADMIN" "admin@cms.local" "$SEED_ADMIN_PASSWORD"
