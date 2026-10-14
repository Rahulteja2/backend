# LifeDrop Backend

## Run with Docker Compose

1. Copy `.env.example` to `.env` and replace the password and JWT values with local development secrets.
2. From the backend directory, run `docker compose up --build`.
3. The API is available at `http://localhost:8080`; MySQL is available on port `3306`.

Compose waits for MySQL to become healthy before starting the backend and stores database data in the `mysql_data` volume. Run `docker compose down` to stop the services while retaining the database volume. The credentials in `.env` are for local development; do not reuse them in production.

For Render deployment settings, production environment variables, and hosted MySQL setup, see [RENDER_DEPLOYMENT_GUIDE.md](./RENDER_DEPLOYMENT_GUIDE.md).
