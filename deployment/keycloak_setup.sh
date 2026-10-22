#!/usr/bin/env bash
# Sets up the clinic realm's permission model in Keycloak:
#   - base roles (ADMIN, DOCTOR, ...) and the permission roles the API checks
#     (doctor:read, invoice:write, ...); each base role carries its permissions
#     as a composite role
#   - the patient-management group that self-registered patients join
#   - the backend and frontend clients, their login flows and token scopes, the
#     API's audience, and how long tokens and sessions last
#   - sign-in with Google, once GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are in .env
#   - optionally, two-factor sign-in for existing staff (STAFF_TWO_FACTOR=true)
#
# Safe to re-run: mappings are synced, so a permission removed here is also
# removed in Keycloak. Reads the Keycloak admin login from .env next to it.
#
#   ./keycloak_setup.sh
#   KEYCLOAK_URL=http://other-host:1201 ./keycloak_setup.sh
#   PASSWORD_LOGIN=false STAFF_TWO_FACTOR=true ./keycloak_setup.sh   # production

set -euo pipefail
cd "$(dirname "$0")"

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:1201}"
REALM="${KEYCLOAK_REALM:-clinic}"
BACKEND_CLIENT="private-app"   # the Spring API: password login + admin API through its service account
FRONTEND_CLIENT="cms-app"      # a browser/mobile app: authorization code flow with PKCE
FRONTEND_ORIGINS='["http://localhost:5173", "http://localhost:3000"]'
PATIENT_GROUP="patient-management"

# where Keycloak sends account emails (staff invites, password resets). Once a
# server is configured (e.g. Gmail in the admin console) it is kept; until then,
# or when SMTP_HOST is given, these values are used — by default the mailpit
# container from docker-compose.yml. Servers that need a login are set in the console.
SMTP_HOST="${SMTP_HOST:-}"
SMTP_PORT="${SMTP_PORT:-1025}"
SMTP_FROM="${SMTP_FROM:-no-reply@clinic.local}"

# branded invite and password-reset emails, from deployment/keycloak/clinic-theme
EMAIL_THEME="clinic"
# the sign-in, sign-up and account pages, from deployment/keycloak/login-theme (built into
# keycloak/providers/spring-clinic-theme.jar); LOGIN_THEME= leaves the realm's choice alone
LOGIN_THEME="${LOGIN_THEME-spring-clinic}"
REALM_DISPLAY_NAME="CMS Clinic"

# the audience the API checks (API_AUDIENCE in its .env): tokens name it in aud, so a token
# Keycloak issued for some other app of the realm is refused
API_AUDIENCE="${API_AUDIENCE:-clinic-api}"
AUDIENCE_SCOPE="clinic-api-audience"

# basic puts the user id (sub) in tokens, roles puts realm_access.roles and the audience scope
# puts aud; the API needs all three
DEFAULT_SCOPES="basic roles $AUDIENCE_SCOPE"

# Password login straight to the token endpoint (POST /api/v1/auth/login, for API clients such
# as Postman). The OAuth security best practice (RFC 9700) rules it out: set false in production,
# with PASSWORD_ENDPOINTS=false on the API.
PASSWORD_LOGIN="${PASSWORD_LOGIN:-true}"

# Two-factor sign-in (an authenticator app) for staff who have not set it up yet. Invited staff
# are asked for it anyway; this asks the existing ones at their next sign-in. Off by default,
# since an account with a pending step cannot use password login (Postman). True in production.
STAFF_TWO_FACTOR="${STAFF_TWO_FACTOR:-false}"
STAFF_ROLES="ADMIN DOCTOR NURSE RECEPTIONIST PHARMACIST LAB_TECHNICIAN"

# Tokens and sessions, in seconds: access tokens last 5 minutes; each refresh token works once
# (a stolen one that is replayed is refused); a session ends after 30 idle minutes or 10 hours.
ACCESS_TOKEN_LIFESPAN=300
SSO_SESSION_IDLE=1800
SSO_SESSION_MAX=36000

# realm-management roles the backend's service account needs to create users,
# assign their roles and add them to groups, and to see whether Google sign-in is set up
SERVICE_ACCOUNT_ROLES="manage-users view-users query-users query-groups view-realm view-identity-providers"

# ─── Roles ────────────────────────────────────────────────────────────────────

BASE_ROLES=(
  "ADMIN|System administrator with full access"
  "DOCTOR|Doctor user"
  "NURSE|Nurse user"
  "RECEPTIONIST|Receptionist user"
  "PHARMACIST|Pharmacist user"
  "LAB_TECHNICIAN|Lab technician user"
  "PATIENT|Patient user"
)

# the authorities SecurityConfig checks
PERMISSIONS=(
  "department:read|Read departments"
  "department:write|Create departments"
  "department:update|Update departments"
  "department:delete|Delete departments"
  "doctor:read|Read doctors"
  "doctor:write|Activate doctors, set their department and specialties"
  "doctor:update|Update own doctor schedule"
  "doctor:delete|Delete doctors"
  "patient:read|Read own patient record"
  "patient:read-all|Read all patient records"
  "patient:write|Create patient records"
  "patient:write-all|Create all patient records"
  "patient:update|Update own patient record"
  "patient:update-all|Update all patient records"
  "appointment:read|Read own appointments, as patient or doctor"
  "appointment:read-all|Read all appointments"
  "appointment:write|Book and change own appointments"
  "appointment:write-all|Book, change, check in and mark no-shows for any patient"
  "visit:read|Read visits"
  "visit:write|Start, update and close visits"
  "vitals:write|Record vital signs"
  "prescription:read|Read and print prescriptions"
  "prescription:write|Write prescriptions"
  "medicine:read|Read medicine catalogue"
  "medicine:write|Add and edit medicines and stock"
  "invoice:read|Read and print invoices"
  "invoice:write|Create, edit, issue, void invoices"
  "payment:record|Record payments"
)

ALL_PERMISSIONS=""
for p in "${PERMISSIONS[@]}"; do ALL_PERMISSIONS+="${p%%|*} "; done

# Permissions each base role carries as a composite role. POST /api/v1/staff
# gives staff only their base role, so this is where their access comes from.
role_permissions() {
  case $1 in
    ADMIN)          echo "$ALL_PERMISSIONS" ;;
    DOCTOR)         echo "department:read doctor:read doctor:update patient:read-all patient:update-all
                          appointment:read visit:read visit:write vitals:write
                          prescription:read prescription:write medicine:read" ;;
    NURSE)          echo "department:read doctor:read patient:read-all patient:update-all appointment:read-all
                          visit:read vitals:write prescription:read medicine:read" ;;
    RECEPTIONIST)   echo "department:read doctor:read patient:read-all patient:write-all patient:update-all
                          appointment:read-all appointment:write-all invoice:read invoice:write payment:record" ;;
    PHARMACIST)     echo "patient:read-all prescription:read medicine:read medicine:write invoice:read payment:record" ;;
    LAB_TECHNICIAN) echo "patient:read-all visit:read" ;;
    PATIENT)        echo "" ;;   # patients get theirs from the patient-management group
  esac
}

PATIENT_GROUP_PERMISSIONS="department:read doctor:read patient:read patient:write patient:update
                           appointment:read appointment:write"

# ─── Helpers ──────────────────────────────────────────────────────────────────

die() { echo "error: $*" >&2; exit 1; }

env_value() { grep -E "^$1=" .env | cut -d= -f2-; }

# request METHOD PATH [JSON] — calls the admin API under /admin/realms and prints
# the response body. A GET that finds nothing prints nothing; other errors abort.
request() {
  local method=$1 path=$2 body=${3:-} out status
  local args=(-sS -X "$method" -w $'\n%{http_code}'
              -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
  if [[ -n $body ]]; then args+=(--data "$body"); fi
  out=$(curl "${args[@]}" "$KEYCLOAK_URL/admin/realms$path") || die "cannot reach Keycloak at $KEYCLOAK_URL"
  status=${out##*$'\n'}
  out=${out%$'\n'*}
  if [[ $method == GET && $status == 404 ]]; then return 0; fi
  if (( status >= 400 )); then die "$method $path -> HTTP $status: $out"; fi
  printf '%s' "$out"
}

# pick_roles ROLES_JSON NAME... — the representations of the named roles; aborts on an unknown name
pick_roles() {
  local from=$1
  shift
  jq -c '[.[] | select(.name | IN($ARGS.positional[]))] as $found
         | ($ARGS.positional - ($found | map(.name))) as $missing
         | if $missing == [] then $found else error("unknown role(s): \($missing | join(", "))") end' \
     --args "$@" <<<"$from"
}

# sync_roles LABEL READ_PATH WRITE_PATH ROLES_JSON NAME... — makes the role
# mappings at an endpoint exactly the named roles, adding and removing as needed
sync_roles() {
  local label=$1 read_path=$2 write_path=$3 available=$4 wanted current add remove
  shift 4
  wanted=$(pick_roles "$available" "$@")
  current=$(request GET "$read_path")
  add=$(jq -c --argjson cur "$current" '[.[] | select(.id | IN($cur[].id) | not)]' <<<"$wanted")
  remove=$(jq -c --argjson want "$wanted" '[.[] | select(.id | IN($want[].id) | not)]' <<<"$current")
  if [[ $add != "[]" ]]; then
    request POST "$write_path" "$add" >/dev/null
    echo "  $label: + $(jq -r 'map(.name) | join(", ")' <<<"$add")"
  fi
  if [[ $remove != "[]" ]]; then
    request DELETE "$write_path" "$remove" >/dev/null
    echo "  $label: - $(jq -r 'map(.name) | join(", ")' <<<"$remove")"
  fi
}

client_uuid() { request GET "$R/clients?clientId=$1" | jq -r '.[0].id // empty'; }

# ensure_client CLIENT_ID — creates a bare client if missing; sets CLIENT_CREATED
ensure_client() {
  CLIENT_CREATED=false
  if [[ -z $(client_uuid "$1") ]]; then
    request POST "$R/clients" "$(jq -n --arg c "$1" '{clientId: $c}')" >/dev/null
    CLIENT_CREATED=true
    echo "  + client $1"
  fi
}

# update_client UUID JQ_FILTER [JQ_ARGS...] — applies a jq edit to the client's settings
update_client() {
  local rep
  rep=$(request GET "$R/clients/$1")
  request PUT "$R/clients/$1" "$(jq -c "${@:3}" "$2" <<<"$rep")" >/dev/null
}

# ─── Setup ────────────────────────────────────────────────────────────────────

command -v jq >/dev/null || die "jq is required"
command -v curl >/dev/null || die "curl is required"
for flag in PASSWORD_LOGIN STAFF_TWO_FACTOR; do
  [[ ${!flag} == true || ${!flag} == false ]] || die "$flag must be true or false"
done

TOKEN=$(curl -sS -f "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
          -d grant_type=password -d client_id=admin-cli \
          --data-urlencode "username=${KC_ADMIN_USERNAME:-$(env_value KC_ADMIN_USERNAME)}" \
          --data-urlencode "password=${KC_ADMIN_PASSWORD:-$(env_value KC_ADMIN_PASSWORD)}" \
        | jq -r .access_token) || die "admin login failed; check KC_ADMIN_USERNAME / KC_ADMIN_PASSWORD in .env"

R="/$REALM"

echo "==> Realm $REALM"
if [[ -z $(request GET "$R") ]]; then
  request POST "" "$(jq -n --arg r "$REALM" '{realm: $r, enabled: true}')" >/dev/null
  echo "  + realm $REALM"
fi

echo "==> Email"
REALM_REP=$(request GET "$R")
CURRENT_SMTP_HOST=$(jq -r '.smtpServer.host // empty' <<<"$REALM_REP")
if [[ -n $CURRENT_SMTP_HOST && -z $SMTP_HOST ]]; then
  echo "  keeping smtp $CURRENT_SMTP_HOST"
else
  SMTP=$(jq -n --arg h "${SMTP_HOST:-mailpit}" --arg p "$SMTP_PORT" --arg f "$SMTP_FROM" \
           '{host: $h, port: $p, from: $f, fromDisplayName: "CMS Clinic"}')
  if [[ $(jq -c '.smtpServer | {host, port, from, fromDisplayName}' <<<"$REALM_REP") != "$(jq -c . <<<"$SMTP")" ]]; then
    request PUT "$R" "$(jq -n --argjson s "$SMTP" '{smtpServer: $s}')" >/dev/null
    echo "  smtp -> ${SMTP_HOST:-mailpit}:$SMTP_PORT, from $SMTP_FROM"
  fi
fi
if [[ $(jq -r '"\(.emailTheme)|\(.displayName)"' <<<"$REALM_REP") != "$EMAIL_THEME|$REALM_DISPLAY_NAME" ]]; then
  request PUT "$R" "$(jq -n --arg t "$EMAIL_THEME" --arg d "$REALM_DISPLAY_NAME" \
                        '{emailTheme: $t, displayName: $d}')" >/dev/null
  echo "  email theme -> $EMAIL_THEME, display name -> $REALM_DISPLAY_NAME"
fi

echo "==> Sign-in page"
# the web app signs everyone in on Keycloak's page (FRONTEND_CLIENT): patients sign up there with
# their email as username and reset forgotten passwords there
LOGIN=$(jq -n '{registrationAllowed: true, registrationEmailAsUsername: true,
                loginWithEmailAllowed: true, resetPasswordAllowed: true}')
if [[ $(jq -c '{registrationAllowed, registrationEmailAsUsername, loginWithEmailAllowed, resetPasswordAllowed}' <<<"$REALM_REP") != "$(jq -c . <<<"$LOGIN")" ]]; then
  request PUT "$R" "$LOGIN" >/dev/null
  echo "  sign-up, email login and password reset on"
fi
if [[ -n $LOGIN_THEME ]]; then
  # Keycloak loads theme jars when it starts, so a new one needs a restart first
  if ! curl -sS -H "Authorization: Bearer $TOKEN" "$KEYCLOAK_URL/admin/serverinfo" \
       | jq -e --arg t "$LOGIN_THEME" 'any(.themes.login[]; .name == $t)' >/dev/null; then
    echo "  login theme $LOGIN_THEME is not loaded: restart Keycloak after building it, then run this again"
  elif [[ $(jq -r '.loginTheme // ""' <<<"$REALM_REP") != "$LOGIN_THEME" ]]; then
    request PUT "$R" "$(jq -n --arg t "$LOGIN_THEME" '{loginTheme: $t}')" >/dev/null
    echo "  login theme -> $LOGIN_THEME"
  fi
fi

echo "==> Tokens and sessions"
TOKENS=$(jq -n --argjson a "$ACCESS_TOKEN_LIFESPAN" --argjson i "$SSO_SESSION_IDLE" --argjson m "$SSO_SESSION_MAX" \
           '{revokeRefreshToken: true, refreshTokenMaxReuse: 0, accessTokenLifespan: $a,
             ssoSessionIdleTimeout: $i, ssoSessionMaxLifespan: $m}')
if [[ $(jq -c '{revokeRefreshToken, refreshTokenMaxReuse, accessTokenLifespan, ssoSessionIdleTimeout, ssoSessionMaxLifespan}' <<<"$REALM_REP") != "$(jq -c . <<<"$TOKENS")" ]]; then
  request PUT "$R" "$TOKENS" >/dev/null
  echo "  refresh tokens used once; access ${ACCESS_TOKEN_LIFESPAN}s, idle ${SSO_SESSION_IDLE}s, session ${SSO_SESSION_MAX}s"
fi

echo "==> Roles"
for entry in "${BASE_ROLES[@]}" "${PERMISSIONS[@]}"; do
  name=${entry%%|*}
  description=${entry#*|}
  current=$(request GET "$R/roles/$name")
  if [[ -z $current ]]; then
    request POST "$R/roles" "$(jq -n --arg n "$name" --arg d "$description" '{name: $n, description: $d}')" >/dev/null
    echo "  + $name"
  elif [[ $(jq -r '.description // ""' <<<"$current") != "$description" ]]; then
    request PUT "$R/roles/$name" "$(jq -n --arg n "$name" --arg d "$description" '{name: $n, description: $d}')" >/dev/null
    echo "  ~ $name: $description"
  fi
done
REALM_ROLES=$(request GET "$R/roles")

echo "==> Role permissions (composites)"
for entry in "${BASE_ROLES[@]}"; do
  role=${entry%%|*}
  # shellcheck disable=SC2046 # word splitting of the permission list is intended
  sync_roles "$role" "$R/roles/$role/composites/realm" "$R/roles/$role/composites" \
             "$REALM_ROLES" $(role_permissions "$role")
done

echo "==> Group $PATIENT_GROUP"
find_group() { request GET "$R/groups?search=$PATIENT_GROUP" | jq -r --arg n "$PATIENT_GROUP" '.[] | select(.name == $n) | .id'; }
GROUP_ID=$(find_group)
if [[ -z $GROUP_ID ]]; then
  request POST "$R/groups" "$(jq -n --arg n "$PATIENT_GROUP" '{name: $n}')" >/dev/null
  GROUP_ID=$(find_group)
  echo "  + group $PATIENT_GROUP"
fi
# shellcheck disable=SC2086
sync_roles "$PATIENT_GROUP" "$R/groups/$GROUP_ID/role-mappings/realm" "$R/groups/$GROUP_ID/role-mappings/realm" \
           "$REALM_ROLES" $PATIENT_GROUP_PERMISSIONS

echo "==> Client $BACKEND_CLIENT"
ensure_client "$BACKEND_CLIENT"
BACKEND_CREATED=$CLIENT_CREATED
BACKEND_ID=$(client_uuid "$BACKEND_CLIENT")
update_client "$BACKEND_ID" '
    .publicClient = false
  | .clientAuthenticatorType = "client-secret"
  | .standardFlowEnabled = false          # browsers sign in through the frontend client
  | .implicitFlowEnabled = false
  | .directAccessGrantsEnabled = $password  # POST /api/v1/auth/login (password grant, API clients)
  | .serviceAccountsEnabled = true        # admin API: create users, assign roles, join groups
  | .fullScopeAllowed = false             # tokens carry only the roles in its scope (below)
  | .redirectUris = []
  | .webOrigins = []
  | del(.attributes["pkce.code.challenge.method"])' \
  --argjson password "$PASSWORD_LOGIN"

echo "==> Client $FRONTEND_CLIENT"
ensure_client "$FRONTEND_CLIENT"
FRONTEND_ID=$(client_uuid "$FRONTEND_CLIENT")
update_client "$FRONTEND_ID" '
    .publicClient = true                  # no secret: it runs in the browser / on the device
  | .standardFlowEnabled = true           # authorization code flow
  | .implicitFlowEnabled = false
  | .directAccessGrantsEnabled = false
  | .serviceAccountsEnabled = false
  | .fullScopeAllowed = false
  # exact addresses, no wildcards (RFC 9700): the sign-in callback, and the login page that
  # invite and password-reset emails link back to (FRONTEND_LOGIN_URL on the API)
  | .redirectUris = ($origins | map(. + "/auth/callback", . + "/login"))
  | .webOrigins = $origins
  | .attributes["pkce.code.challenge.method"] = "S256"
  | .attributes["post.logout.redirect.uris"] = ($origins | map(. + "/login") | join("##"))' \
  --argjson origins "$FRONTEND_ORIGINS"

echo "==> Audience $API_AUDIENCE"
find_scope() { request GET "$R/client-scopes" | jq -r --arg n "$AUDIENCE_SCOPE" '.[] | select(.name == $n) | .id'; }
SCOPE_ID=$(find_scope)
if [[ -z $SCOPE_ID ]]; then
  request POST "$R/client-scopes" "$(jq -n --arg n "$AUDIENCE_SCOPE" '{
      name: $n, protocol: "openid-connect", description: "Names the clinic API in access tokens (aud)",
      attributes: {"include.in.token.scope": "false", "display.on.consent.screen": "false"}}')" >/dev/null
  SCOPE_ID=$(find_scope)
  echo "  + client scope $AUDIENCE_SCOPE"
fi
MAPPER=$(jq -n --arg a "$API_AUDIENCE" '{
    name: "audience", protocol: "openid-connect", protocolMapper: "oidc-audience-mapper",
    config: {"included.custom.audience": $a, "access.token.claim": "true",
             "introspection.token.claim": "true", "id.token.claim": "false"}}')
MAPPER_ID=$(request GET "$R/client-scopes/$SCOPE_ID/protocol-mappers/models" | jq -r '.[] | select(.name == "audience") | .id')
if [[ -z $MAPPER_ID ]]; then
  request POST "$R/client-scopes/$SCOPE_ID/protocol-mappers/models" "$MAPPER" >/dev/null
  echo "  + aud $API_AUDIENCE"
else
  request PUT "$R/client-scopes/$SCOPE_ID/protocol-mappers/models/$MAPPER_ID" \
              "$(jq -c --arg id "$MAPPER_ID" '. + {id: $id}' <<<"$MAPPER")" >/dev/null
fi

echo "==> Token scopes"
CLIENT_SCOPES=$(request GET "$R/client-scopes")
for cid in "$BACKEND_ID" "$FRONTEND_ID"; do
  current=$(request GET "$R/clients/$cid/default-client-scopes")
  for scope in $DEFAULT_SCOPES; do
    if ! jq -e --arg n "$scope" 'any(.[]; .name == $n)' <<<"$current" >/dev/null; then
      sid=$(jq -r --arg n "$scope" '.[] | select(.name == $n) | .id' <<<"$CLIENT_SCOPES")
      if [[ -z $sid ]]; then die "client scope $scope does not exist in realm $REALM"; fi
      request PUT "$R/clients/$cid/default-client-scopes/$sid" "{}" >/dev/null
      echo "  + default scope $scope on $(request GET "$R/clients/$cid" | jq -r .clientId)"
    fi
  done
done

# with full scope off, a token only carries user roles that are in its client's scope
APP_ROLES="$ALL_PERMISSIONS"
for entry in "${BASE_ROLES[@]}"; do APP_ROLES+=" ${entry%%|*}"; done
# shellcheck disable=SC2086
sync_roles "$BACKEND_CLIENT scope" "$R/clients/$BACKEND_ID/scope-mappings/realm" \
           "$R/clients/$BACKEND_ID/scope-mappings/realm" "$REALM_ROLES" $APP_ROLES
# shellcheck disable=SC2086
sync_roles "$FRONTEND_CLIENT scope" "$R/clients/$FRONTEND_ID/scope-mappings/realm" \
           "$R/clients/$FRONTEND_ID/scope-mappings/realm" "$REALM_ROLES" $APP_ROLES

echo "==> Service account of $BACKEND_CLIENT"
RM_ID=$(client_uuid realm-management)
RM_ROLES=$(request GET "$R/clients/$RM_ID/roles")
SA_ID=$(request GET "$R/clients/$BACKEND_ID/service-account-user" | jq -r .id)
# shellcheck disable=SC2086
sync_roles "realm-management roles" "$R/users/$SA_ID/role-mappings/clients/$RM_ID" \
           "$R/users/$SA_ID/role-mappings/clients/$RM_ID" "$RM_ROLES" $SERVICE_ACCOUNT_ROLES
# the admin API reads these roles from the service account's token, so they must be in scope too
# shellcheck disable=SC2086
sync_roles "$BACKEND_CLIENT scope (realm-management)" "$R/clients/$BACKEND_ID/scope-mappings/clients/$RM_ID" \
           "$R/clients/$BACKEND_ID/scope-mappings/clients/$RM_ID" "$RM_ROLES" $SERVICE_ACCOUNT_ROLES

echo "==> Sign in with Google"
# optional: a .env without them is fine (grep finds nothing, which pipefail would treat as an error)
GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID:-$(env_value GOOGLE_CLIENT_ID || true)}
GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET:-$(env_value GOOGLE_CLIENT_SECRET || true)}
if [[ -z $GOOGLE_CLIENT_ID || -z $GOOGLE_CLIENT_SECRET ]]; then
  echo "  skipped: put GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in .env to offer it"
else
  # trustEmail: Google has checked the address. The first broker login flow links a Google
  # account to an existing user with the same email once they prove it is theirs.
  IDP=$(jq -n --arg id "$GOOGLE_CLIENT_ID" --arg secret "$GOOGLE_CLIENT_SECRET" '{
      alias: "google", providerId: "google", enabled: true, trustEmail: true,
      firstBrokerLoginFlowAlias: "first broker login",
      config: {clientId: $id, clientSecret: $secret, defaultScope: "openid profile email", syncMode: "IMPORT"}}')
  if [[ -z $(request GET "$R/identity-provider/instances/google") ]]; then
    request POST "$R/identity-provider/instances" "$IDP" >/dev/null
    echo "  + google"
  else
    request PUT "$R/identity-provider/instances/google" "$IDP" >/dev/null
    echo "  ~ google"
  fi
  echo "  Google's OAuth client needs this authorized redirect URI:"
  echo "  $KEYCLOAK_URL/realms/$REALM/broker/google/endpoint"
fi

echo "==> Two-factor sign-in for staff"
if [[ $STAFF_TWO_FACTOR != true ]]; then
  echo "  skipped: STAFF_TWO_FACTOR=true asks staff without it to set it up at their next sign-in"
else
  declare -A SEEN=()
  for role in $STAFF_ROLES; do
    for uid in $(request GET "$R/roles/$role/users?first=0&max=1000" | jq -r '.[].id'); do
      if [[ -n ${SEEN[$uid]:-} ]]; then continue; fi
      SEEN[$uid]=1
      # already set up, or already asked
      if request GET "$R/users/$uid/credentials" | jq -e 'any(.[]; .type == "otp")' >/dev/null; then continue; fi
      user=$(request GET "$R/users/$uid")
      if jq -e '(.requiredActions // []) | index("CONFIGURE_TOTP") != null' <<<"$user" >/dev/null; then continue; fi
      request PUT "$R/users/$uid" "$(jq -c '.requiredActions += ["CONFIGURE_TOTP"]' <<<"$user")" >/dev/null
      echo "  + $(jq -r .username <<<"$user") ($role)"
    done
  done
fi

echo "==> Done"
if [[ $BACKEND_CREATED == true ]]; then
  echo "  $BACKEND_CLIENT is new: copy its secret (Clients -> $BACKEND_CLIENT -> Credentials)"
  echo "  into KEYCLOAK_CLIENT_SECRET in src/main/resources/.env"
fi
