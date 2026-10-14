# Render Deployment Guide

## Service settings

Create a **Web Service** from the GitHub repository `Rahulteja2/backend`.

| Render setting | Value |
|---|---|
| Branch | `main` |
| Service name | `lifedrop-backend` |
| Runtime | Docker |
| Root directory | Leave blank (repository root) |
| Dockerfile path | `Dockerfile` |
| Docker build context | Repository root (`.`) |
| Health-check path | `/health` |
| Instance plan | Free, if available for the account; see the limitations below |

The image sets `SPRING_PROFILES_ACTIVE=production`. Render's runtime `PORT` is used for the HTTP listener, which binds to `0.0.0.0`. Do not set a production database URL to `localhost`.

## Environment variables

The deployment error `Missing required production environment variables: JWT_SECRET, APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD, CORS_ALLOWED_ORIGINS` means those variables are not present in the running Render service environment. The application does not create or infer secret values. Add each variable in the **backend Web Service > Environment > Environment Variables** page using the exact key spelling below, then choose **Save, rebuild, and deploy**. Enter secret values as environment-variable values (not Docker build arguments, `application.properties`, or frontend build variables). The validator intentionally stays enabled and reports missing names without exposing values.

Add these in the service's **Environment** page. Never put real values in this guide, source control, build arguments, or frontend variables.

| Name | Required | Value |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `production` |
| `SPRING_DATASOURCE_URL` | Yes | Remote MySQL JDBC URL with verified TLS; format below |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database user from the provider |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password from the provider |
| `JWT_SECRET` | Yes | Generate a unique cryptographically random secret of at least 32 UTF-8 bytes; keep it private |
| `APP_ADMIN_EMAIL` | Yes | Valid email for the initial admin account |
| `APP_ADMIN_PASSWORD` | Yes | Initial admin password, at least 12 characters |
| `CORS_ALLOWED_ORIGINS` | Yes | Exact HTTPS frontend origin(s), comma-separated, no paths or trailing slash |

Generate a JWT secret locally with a cryptographically secure generator (for example, `openssl rand -base64 48`) and paste its output directly into Render's secret-value field. The application uses JJWT's `signWith(SecretKey)` HMAC signing and rejects keys shorter than 32 UTF-8 bytes. If a seeded admin already exists, the configured admin password will not reset it; use the existing account's password. `PORT` is assigned by Render and takes precedence; `SERVER_PORT` is only a local fallback override and does not need to be set in Render.

## MySQL database

Render's documented free database offering is PostgreSQL, not MySQL, so do **not** select it for this application. The inspected provider documentation confirms Aiven has a managed MySQL product and TiDB Cloud uses the MySQL protocol, but I could not verify a currently available, clearly priced $0 managed MySQL tier from the official pricing pages retrieved. No database has been provisioned, and availability/pricing must be checked before choosing a provider. Do not start a paid plan or enter payment details without approval.

Manual setup:

1. Select a managed MySQL 8-compatible provider/plan yourself. Confirm the price is $0 (or that any trial is explicitly acceptable) before creating anything. Check storage, sleep/expiry, backup, TLS, and network-access limits; free services may not be suitable for sensitive production data.
2. In that provider's dashboard, create a database and a dedicated application user. Grant the user access only to that database. Do not use the MySQL root account.
3. Open the provider's **Connect** or **Connection details** page and copy the hostname, port, database name, username, and password. Follow its instructions to enable TLS and permit connections from the Render service; do not disable certificate verification or expose the database broadly if the provider supports narrower network access.
4. In Render, construct `SPRING_DATASOURCE_URL` as:

   ```text
   jdbc:mysql://<HOST>:<PORT>/<DATABASE>?sslMode=VERIFY_IDENTITY&serverTimezone=UTC
   ```

   Replace each angle-bracket placeholder with the provider's actual connection details. The production validator requires a remote hostname, explicit port, database name, and either `sslMode=VERIFY_IDENTITY` or `sslMode=VERIFY_CA`; it rejects localhost and credentials embedded in the URL. Prefer `VERIFY_IDENTITY`. If the provider cannot support either certificate-verifying mode, do not weaken the application setting: confirm a compatible connection configuration before deploying.
5. Set the URL, username, and password as the three `SPRING_DATASOURCE_*` variables in Render. Keep `SPRING_PROFILES_ACTIVE=production`. Hibernate's existing `ddl-auto=update` setting creates/updates the application tables on startup; there is no separate SQL initialization file. The application seeds the initial admin only when no admin exists.

The local Compose MySQL service and its `mysql_data` volume are local-only. Render cannot reach that database. Create the database itself in the provider dashboard before starting the backend; the application does not create the remote database. Hibernate `ddl-auto=update` creates/updates the application tables at startup, so grant the application user the required table DDL privileges on that database.

## Frontend and CORS

The local frontend reads `VITE_API_URL` and otherwise targets `http://localhost:8080`. When deploying the frontend, set its build-time `VITE_API_URL` to the backend's public HTTPS URL, for example `https://<your-service>.onrender.com` (no trailing slash), then rebuild/redeploy the frontend.

Set the backend's `CORS_ALLOWED_ORIGINS` to the exact frontend origin, such as `https://<your-frontend>.onrender.com`. For multiple frontends, provide comma-separated exact origins. Do not use `*`; credentials are not enabled.

## Logs and troubleshooting

- **Missing `JWT_SECRET`, `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD`, or `CORS_ALLOWED_ORIGINS`:** Add them to the backend service's runtime **Environment Variables** with those exact names and non-empty values; save and redeploy. Do not rename them to `jwt.secret`, `app.admin.email`, or Docker build args. The application profile maps these external names to its internal Spring properties.
- **Missing datasource variables:** Add `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` the same way. The production profile has no localhost or development-credential defaults.
- **Weak JWT secret / invalid CORS / rejected JDBC URL:** Use a random JWT value of at least 32 UTF-8 bytes; provide exact comma-separated HTTPS origins without paths; use a MySQL JDBC URL with remote host, port, database, and `sslMode=VERIFY_IDENTITY` or `VERIFY_CA`. The startup validator never echoes values.
- **Build logs:** In the Render Dashboard, open the service and select **Events** or **Deploys**, then open the build for the relevant deploy.
- **Runtime logs:** Open the service's **Logs** page and select the latest instance/deploy.
- **Missing environment variable:** Confirm every required variable above is present, non-empty, and spelled exactly as shown. Production configuration has no local DB, JWT, CORS, or admin-password fallback.
- **`CJCommunicationsException` / `Connection refused`:** This is a network endpoint/reachability failure, not a password or schema error. Confirm the provider database is provisioned and running; copy the external/public hostname and port (not `localhost`, `127.0.0.1`, a Compose service name such as `db`, or a provider-private-only hostname); enable external access or the provider's Render network allowlist; and confirm the selected region/network permits inbound traffic. Recheck the actual endpoint in the provider console. Do not attempt connection tests until you have supplied valid provider connection details.
- **Database authentication/access denied:** Recopy the provider-issued database name, username, and password; ensure this is the dedicated application user and that it has access to the selected database.
- **TLS handshake/certificate error:** Use the provider's documented TLS host and CA requirements. Keep certificate verification on (`VERIFY_IDENTITY` preferred, or `VERIFY_CA` if documented); never disable TLS verification to silence the error.
- **Schema errors:** Confirm the selected service speaks MySQL protocol and the database user can create/alter tables; this app uses Hibernate `update` on startup.
- **Port binding:** Keep `SPRING_PROFILES_ACTIVE=production`; do not override Render's `PORT`. The service listens on `0.0.0.0`.
- **Health-check failure:** Open `https://<your-service>.onrender.com/health`. A healthy response is HTTP 200 with `{"status":"UP"}`. This endpoint also executes `SELECT 1`, so a database outage makes the health check fail. Inspect runtime logs and DB connectivity.
- **401 on protected APIs:** Sign in through `/api/auth/login`, send the returned token as `Authorization: Bearer <token>`, and check that the account has the required role. A new/empty database seeds the admin from the configured admin variables; an existing admin is not reset.
- **CORS in browser:** Verify the browser's frontend origin exactly matches `CORS_ALLOWED_ORIGINS` (scheme and host included, no path or trailing slash), then save and redeploy the backend.

## Redeployment checklist

- [ ] Confirm the remote MySQL database exists, is running, and accepts connections from Render.
- [ ] Copy its public hostname, port, and database name into a JDBC URL using certificate-verifying TLS.
- [ ] Add all seven required variables above to the **backend service runtime environment** using the exact names; do not leave placeholders.
- [ ] Use a unique random JWT secret of at least 32 bytes and an initial admin password of at least 12 characters.
- [ ] Set `CORS_ALLOWED_ORIGINS` to the actual deployed frontend HTTPS origin(s).
- [ ] Save environment changes and trigger **Save, rebuild, and deploy**.
- [ ] Check startup logs for missing-name/configuration errors, then check `GET /health` for HTTP 200.
- [ ] Verify an unauthenticated request to `/api/admin/users` receives 401, then test login without sharing the password or returned token.

Test without printing credentials:

```text
GET https://<your-service>.onrender.com/health
POST https://<your-service>.onrender.com/api/auth/login
```

For login, submit the admin email and password as a JSON request body over HTTPS; do not put them in a URL, shell command, screenshot, or shared log. Never publish the returned JWT.

## Free-plan limitations and readiness

Render documents its free web services as unsuitable for production and notes that they can spin down when idle. It also documents only PostgreSQL as its free managed database offering. A separate MySQL-compatible database is therefore required, and its current free availability, reliability, and data-handling suitability must be verified by you before deployment. Keep any real donor/health information out of test deployments until the database, backups, access controls, and provider terms have been reviewed.

Documentation checked on 2026-10-14:

- [Render Docker services](https://render.com/docs/docker)
- [Render web services and port binding](https://render.com/docs/web-services)
- [Render HTTP health checks](https://render.com/docs/health-checks)
- [Render free plan limitations](https://render.com/docs/free)
- [Render database options](https://render.com/docs/databases)
- [Aiven for MySQL](https://aiven.io/mysql)
- [Aiven pricing](https://aiven.io/pricing)
- [TiDB Cloud](https://www.pingcap.com/tidb/cloud/)
- [TiDB MySQL compatibility](https://docs.pingcap.com/tidb/stable/mysql-compatibility/)
- [TiDB Cloud pricing](https://tidbcloud.com/pricing)
