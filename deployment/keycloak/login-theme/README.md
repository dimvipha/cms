# Spring Clinic login theme

The pages Keycloak shows for the clinic realm: sign-in, sign-up, forgotten and new passwords, email
verification, two-factor setup and codes, notices, errors and expired links. Built with
[Keycloakify](https://www.keycloakify.dev) (React and Tailwind), in the teal two-panel design of
the sign-in page.

## How it is made

- `src/login/Template.tsx` is the card every page sits in: the teal panel with the clinic's name on
  the left (hidden on phones), the page's form on the right.
- `src/login/pages/` holds the pages drawn by hand: sign-in, sign-up, two-factor setup, notices
  (`Info`) and the expired page.
- Every other page is Keycloakify's own, styled through the Tailwind classes in
  `src/login/classes.ts`, so new Keycloak pages pick up the look without code.
- `src/login/i18n.ts` holds the clinic's texts (its name, the panel lines, the line under each title)
  and a few of Keycloak's reworded.

## Preview

```bash
npm install
npm run dev
```

Then open a page with sample data: http://localhost:5175/?page=login.ftl, `?page=register.ftl`,
`?page=login-config-totp.ftl`, `?page=info.ftl&message=success`, and so on. `&error=1` shows a field
error and `&message=error` (or `success`, `info`, `warning`) a notice.

## Build and install

Building needs Maven on the `PATH`.

```bash
npm run build-keycloak-theme    # builds dist_keycloak/spring-clinic-theme.jar and copies it to ../providers
docker restart keycloak-clinic-db
../../keycloak_setup.sh          # switches the realm's login theme to spring-clinic
```

Keycloak reads theme jars only when it starts, so restart it after every build.
