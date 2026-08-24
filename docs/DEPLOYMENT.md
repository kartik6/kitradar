# Deploying KitPaglu

This is the production deployment reference. KitPaglu currently runs with a Vercel frontend, a Fly.io Spring Boot API in Singapore, Neon PostgreSQL in Singapore, and a GitHub Actions catalogue worker. Secrets remain account-owned and are never committed.

## 1. PostgreSQL

Create a Neon Postgres database, or another managed Postgres instance. KitPaglu applies Flyway migrations on startup.

## 2. Backend

Deploy [`backend/Dockerfile`](../backend/Dockerfile) to Fly.io with `backend/` as the Docker build context. The committed [`../backend/fly.toml`](../backend/fly.toml) configures the `kitradar-api-kartik` application in Singapore, one always-on 1 GB shared-CPU machine, HTTPS and readiness checks. The service listens on `PORT` (default `8080`) and exposes:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Set these runtime values:

| Variable | Required | Purpose |
|---|---:|---|
| `JDBC_DATABASE_URL` | Yes | JDBC host/database URL, e.g. `jdbc:postgresql://ep-example.neon.tech/neondb?sslmode=require`. |
| `DB_USER` | Yes | Database username from the managed Postgres provider. |
| `DB_PASSWORD` | Yes | Database password from the managed Postgres provider. |
| `ADMIN_TOKEN` | Yes | Long random token for refresh and run-history endpoints. |
| `KITRADAR_FRONTEND_ORIGIN` | Yes | Exact deployed frontend origin, e.g. `https://kitradar.vercel.app`. |
| `KITRADAR_REFRESH_CRON` | Recommended | Set to `-` when GitHub Actions owns the nightly refresh. |
| `DB_POOL_MAX_SIZE` | No | Maximum JDBC connections; defaults to `4`, which is sufficient for the single small Fly machine. |
| `DB_POOL_MIN_IDLE` | No | Minimum idle JDBC connections; defaults to `0` so unused connections can be retired. |
| `DB_POOL_KEEPALIVE_MS` | No | Hikari keepalive interval; defaults to `0` (disabled) so it cannot prevent Neon scale-to-zero. |
| `DB_POOL_IDLE_TIMEOUT_MS` | No | Idle connection lifetime; defaults to `60000` (one minute). |
| `PORT` | Host-provided | HTTP port; defaults to `8080` locally. |

Do not expose `ADMIN_TOKEN` to the frontend. The only public CORS-enabled endpoints are the catalogue read APIs.

### Neon compute usage

Neon suspends an inactive compute after five minutes on the Free plan. HikariCP 6.3 otherwise keeps ten idle connections and validates each one every two minutes, which keeps the compute continuously active even with no visitor traffic. The committed datasource defaults disable those validation pings, keep no minimum idle connections, and cap the pool at four. Do not enable a keepalive interval shorter than Neon's suspension window.

In the Neon console, verify this after a backend deployment by opening **Monitoring** and checking that compute usage falls to zero between traffic and catalogue imports. A continuously active baseline usually means another client is querying the database more often than the scale-to-zero window.

## 3. Frontend

Import the repository into Vercel with `frontend` as its Root Directory. Configure the two non-secret URL variables from [`frontend/.env.example`](../frontend/.env.example):

- `KITRADAR_API_URL=https://your-backend.example` for server-side rendering.
- `NEXT_PUBLIC_KITRADAR_API_URL=https://your-backend.example` for browser pagination and favourites.

After the first Vercel deployment, copy its exact origin to `KITRADAR_FRONTEND_ORIGIN` on the backend and redeploy the backend. This is required for browser-side API requests to pass CORS.

## 4. Coupled API and frontend releases

Vercel automatically deploys the frontend when `main` changes. A frontend change that requires a new API contract (a query value, response field, route, or changed behaviour) must therefore not be pushed first: users could receive the new browser bundle while Fly is still serving the old API.

For a coupled release, use this order:

1. Keep the backend backward-compatible where practical (for example, accept both the old and new query value).
2. Run the backend and frontend checks locally.
3. From the intended, committed working tree, deploy Fly first: `cd backend && fly deploy --ha=false`.
4. Confirm `https://kitradar-api-kartik.fly.dev/actuator/health` returns `UP` and exercise the changed API request.
5. Only then push `main`, which triggers the Vercel production deployment. Verify the browser route after Vercel completes.

If the Fly deployment fails, do not push the coupled frontend change to `main`. A backend-only change or a frontend-only presentation change can use its normal deployment path, but any API contract dependency uses the ordered release above.

## 5. Nightly refresh

The committed [nightly workflow](../.github/workflows/nightly-refresh.yml) runs at 03:00 IST (21:30 UTC the preceding day) and can also be started manually from the Actions tab. It reads the protected enabled-source registry, fetches every Shopify, DotPe or WooCommerce source from GitHub Actions, then posts canonical catalogues to the protected import API. Add these repository Action secrets:

- `KITRADAR_API_URL` — the backend’s public HTTPS base URL.
- `ADMIN_TOKEN` — the same backend-only token set above.

Leave the manual `store` input blank to refresh every enabled source, or pass one exact slug to isolate a source. Every source gets a chance to run; a failed source is recorded in run history and the workflow fails only after the remaining sources complete. The 25-minute timeout leaves room for paced public APIs. With this workflow enabled, set `KITRADAR_REFRESH_CRON=-` on the backend to prevent a duplicate in-process run.

## 6. Post-deployment checklist

1. Open `/actuator/health` and confirm `UP`.
2. Load the catalogue, clearance, and favourites routes in a browser.
3. Manually dispatch the nightly workflow and review its start/progress/end logs for each source.
4. Verify the result through `GET /api/v1/admin/runs` with `Authorization: Bearer <ADMIN_TOKEN>`.
5. Set a host-level health check to `/actuator/health/readiness` and enable host log retention/alerts.

The run-history endpoint supports `store`, `page`, and `pageSize` (1–100), for example: `GET /api/v1/admin/runs?store=full-time-store&page=0&pageSize=20`. For the current HLD, LLD and data-flow diagrams, see [`ARCHITECTURE.md`](ARCHITECTURE.md).
