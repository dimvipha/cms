# Deploying the API on a Linode

This folder runs the clinic API in production on one Linux server: Postgres, Keycloak and the API,
behind [Caddy](https://caddyserver.com), which serves them over HTTPS with free Let's Encrypt
certificates. The web app is hosted elsewhere and talks to the two addresses set up here.

```
              Internet (HTTPS only)
                      │
           ┌──────────▼──────────┐
           │  Caddy  :80 :443    │   certificates, HTTP → HTTPS
           └──────┬───────┬──────┘
   api.example.com│       │auth.example.com
           ┌──────▼──┐ ┌──▼────────┐
           │   API   │ │ Keycloak  │   sign-in pages, accounts, tokens
           └────┬────┘ └────┬──────┘
                └─────┬─────┘
               ┌──────▼──────┐
               │  Postgres   │   clinic_db and keycloak
               └─────────────┘
```

| File | What it is |
|---|---|
| `compose.yaml` | the four services; only Caddy is open to the internet |
| `Caddyfile` | HTTPS and the two addresses |
| `.env.example` | every setting; copy to `.env` |
| `setup-keycloak.sh` | sets up the clinic realm with the production choices |
| `create-admin.sh` | makes the first administrator |
| `backup.sh` | nightly dumps of both databases |
| `init-keycloak-db.sql` | makes Keycloak's database on the first start |

The API's image is built from the `Dockerfile` at the repository root. Its tables come from the
Flyway migrations in `src/main/resources/db/migration`, applied when it starts.

## 1. Create the server

In the Linode Cloud Manager, **Create Linode**:

- **Image:** Ubuntu 24.04 LTS
- **Region:** the one closest to the clinic (Singapore for Cambodia and Thailand)
- **Plan:** Shared CPU, **Linode 4 GB** or larger. Keycloak and the API are Java programs and use
  about 2.5 GB together; 2 GB is not enough.
- **SSH Keys:** add your public key (`cat ~/.ssh/id_ed25519.pub` on your computer; make one with
  `ssh-keygen -t ed25519` if you have none)
- **Backups:** worth turning on; it snapshots the whole server daily

Note its IPv4 address, shown as `203.0.113.10` below.

## 2. Point two names at it

Where your domain's DNS is managed (your registrar, Cloudflare, or Linode's **Domains**), add two
**A** records with the server's IPv4 address:

| Name | Type | Value |
|---|---|---|
| `api` | A | `203.0.113.10` |
| `auth` | A | `203.0.113.10` |

With Cloudflare, set them to **DNS only** (grey cloud) so Caddy can get its certificates. Check from
your computer that both answer with the server's address before step 6:

```bash
dig +short api.example.com
dig +short auth.example.com
```

## 3. Secure the server

```bash
ssh root@203.0.113.10

apt update && apt -y upgrade
adduser deploy                                  # your everyday user; choose a password
usermod -aG sudo deploy
rsync --archive --chown=deploy:deploy ~/.ssh /home/deploy   # same SSH key for deploy

# SSH with keys only, no root login
sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication no/; s/^#\?PermitRootLogin .*/PermitRootLogin no/' /etc/ssh/sshd_config
systemctl restart ssh

# firewall: SSH and the web only
ufw allow OpenSSH && ufw allow 80/tcp && ufw allow 443/tcp && ufw allow 443/udp
ufw --force enable

timedatectl set-timezone Asia/Bangkok
exit
```

From now on, sign in as `deploy`: `ssh deploy@203.0.113.10`. Postgres and Keycloak are not
published on any port, so the firewall only needs SSH, 80 and 443.

## 4. Install Docker

```bash
sudo apt install -y ca-certificates curl git jq
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker deploy
exit                                            # sign in again so the docker group applies
```

Check with `docker compose version`.

## 5. Get the code and fill in the settings

The repository is private, so the server needs read access. The simplest is a **deploy key**:
on the server run `ssh-keygen -t ed25519 -C linode-deploy` (no passphrase), then
`cat ~/.ssh/id_ed25519.pub`, and add it in GitHub under the repository's
**Settings → Deploy keys** (read-only).

```bash
git clone git@github.com:dimvipha/cms.git
cd cms
git checkout main                               # or the branch you deploy from
cd deployment/production
cp .env.example .env
chmod 600 .env
nano .env
```

In `.env`:

- `API_HOST`, `AUTH_HOST`: the two names from step 2, without `https://`
- `WEB_ORIGIN`: the web app's address, with `https://` and no trailing `/`
- `ACME_EMAIL`: your email, for certificate notices
- `POSTGRES_PASSWORD`, `KC_ADMIN_PASSWORD`: new strong passwords, each from
  `openssl rand -base64 32`. Keep them in your password manager.
- `SMTP_*`: the mail server for invites and password resets. For Gmail, turn on 2-step
  verification for the Google account, make an **app password** (Google account → Security → App
  passwords), and use `smtp.gmail.com`, port `587`, the address as `SMTP_FROM` and `SMTP_USER`,
  and the app password as `SMTP_PASSWORD`.
- `CLINIC_*`, `BILLING_*`, `CONSULTATION_FEE`: what prescriptions and invoices show
- Leave `KEYCLOAK_CLIENT_SECRET` empty for now (step 7).

## 6. Start everything

```bash
docker compose up -d --build
```

The first start takes a few minutes: the API's image is built, and Keycloak prepares itself.
Follow along with `docker compose logs -f` (Ctrl+C stops following, not the services). When it
settles:

```bash
docker compose ps                                           # all four "running"
curl -s https://auth.example.com/realms/master | jq .realm  # "master"
curl -s https://api.example.com/api/v1/public/clinic        # {"departments":[],"doctors":[]}
```

The API made its tables on this first start (Flyway, migration V1).

## 7. Set up Keycloak

```bash
./setup-keycloak.sh
```

It creates the `clinic` realm with its roles, permissions, clients and token rules, as on your
computer, but for production: only `WEB_ORIGIN` allowed to sign in, the Spring Clinic sign-in
pages, your mail server, and the lockout after 5 wrong passwords with the password rules.

Then give the API its client secret:

1. Open `https://auth.example.com/admin` and sign in with `KC_ADMIN_USERNAME` and
   `KC_ADMIN_PASSWORD`.
2. Switch to the **clinic** realm (top-left menu), then **Clients → private-app → Credentials**,
   and copy the **Client secret**.
3. Put it in `.env` as `KEYCLOAK_CLIENT_SECRET=...` and restart the API:
   `docker compose up -d api`

## 8. Make the first administrator

```bash
./create-admin.sh you@example.com Dara Sok +85512345678
```

The phone number is needed when the realm's sign-up form asks for one. The script prints a
one-time password. Open the web app's `/login`, sign in with your email and that password, and the
app offers **Finish setting up**: Keycloak's page asks for a new password and brings you back signed
in. Later sign-ins happen on the app's own page. From the app you can then add departments, invite staff (they get an email) and activate doctors.

Do not run `../seed.sh` here: it creates demo accounts with known passwords.

## 9. Connect the web app

Build the web app with the two addresses, wherever it is hosted:

```bash
VITE_API_URL=https://api.example.com
VITE_KEYCLOAK_URL=https://auth.example.com
VITE_KEYCLOAK_REALM=clinic
VITE_KEYCLOAK_CLIENT_ID=cms-app
```

Its address must be exactly `WEB_ORIGIN`. If you change it later, update `.env`, then run
`./setup-keycloak.sh` and `docker compose up -d api` again.

## 10. Backups

```bash
crontab -e
```

and add:

```
0 2 * * * /home/deploy/cms/deployment/production/backup.sh >> /home/deploy/backup.log 2>&1
```

Every night it writes `backups/clinic_db-*.sql.gz` and `backups/keycloak-*.sql.gz` and keeps the
last 14. Copy them off the server now and then (`scp`, or Linode Object Storage); a backup on the
same disk is lost with the server. To restore one:

```bash
gunzip -c backups/clinic_db-20261101-020000.sql.gz | docker compose exec -T postgres psql -U clinic -d clinic_db
```

## Updating

```bash
cd ~/cms && git pull
cd deployment/production
docker compose up -d --build api      # new API code; Flyway applies any new migrations
docker compose restart keycloak       # after a new theme jar in ../keycloak/providers
./setup-keycloak.sh                   # after changes to keycloak_setup.sh or .env
```

A change to an entity needs a new migration in `src/main/resources/db/migration`
(`V2__what_changed.sql`, and so on). In production Hibernate only checks the tables, so the API
will not start if the migration is missing. Never edit a migration that has already run.

## When something is wrong

| What you see | Look at |
|---|---|
| The browser warns about the certificate, or Caddy's log says it cannot get one | The DNS records (step 2) point at this server; ports 80 and 443 are open; `docker compose logs caddy` |
| Every API call is 401 | `KEYCLOAK_CLIENT_SECRET` set and the API restarted; `setup-keycloak.sh` has run (tokens must carry `aud: clinic-api`); `docker compose logs api` |
| The browser says CORS | `WEB_ORIGIN` is exactly the web app's address (https, no trailing `/`), then `docker compose up -d api` |
| Keycloak says "Invalid parameter: redirect_uri" | `WEB_ORIGIN` as above, then `./setup-keycloak.sh` |
| No invite or reset emails | `SMTP_*` in `.env`, then `./setup-keycloak.sh`; Keycloak's **Realm settings → Email → Test connection** |
| The API stops at start with a schema error | A new entity field has no migration (see Updating) |
| The server is slow or a container restarts | `docker stats`; 4 GB is the minimum |

`docker compose logs -f api` (or `keycloak`, `caddy`, `postgres`) shows each service's log.
