# Deploying KitRadar

This repository is ready to deploy, but no cloud account, database, DNS name, or secret is committed here. Create those resources in your own accounts, then configure the values below.

## 1. PostgreSQL

Create a Neon Postgres database, or another managed Postgres instance. KitRadar applies Flyway migrations on startup.

## 2. Backend

Deploy [`backend/Dockerfile`](../backend/Dockerfile) to a Docker-capable host such as Render or Fly.io, with `backend/` as the Docker build context. The service listens on `PORT` (default `8080`) and exposes health checks at:

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
| `PORT` | Host-provided | HTTP port; defaults to `8080` locally. |

Do not expose `ADMIN_TOKEN` to the frontend. The only public CORS-enabled endpoints are the catalogue read APIs.

## 3. Frontend

Import the repository into Vercel with `frontend` as its Root Directory. Configure the two non-secret URL variables from [`frontend/.env.example`](../frontend/.env.example):

- `KITRADAR_API_URL=https://your-backend.example` for server-side rendering.
- `NEXT_PUBLIC_KITRADAR_API_URL=https://your-backend.example` for browser pagination and favourites.

After the first Vercel deployment, copy its exact origin to `KITRADAR_FRONTEND_ORIGIN` on the backend and redeploy the backend. This is required for browser-side API requests to pass CORS.

## 4. Nightly refresh

The committed [nightly workflow](../.github/workflows/nightly-refresh.yml) runs at 03:00 IST (21:30 UTC the preceding day) and can also be started manually from the Actions tab. Add these repository Action secrets:

- `KITRADAR_API_URL` — the backend’s public HTTPS base URL.
- `ADMIN_TOKEN` — the same backend-only token set above.

The workflow calls `POST /api/v1/admin/refresh` and fails visibly if the refresh endpoint returns a non-2xx response. Its ten-minute limit leaves room for the paced Goal Thread import. With this workflow enabled, set `KITRADAR_REFRESH_CRON=-` on the backend to prevent a duplicate in-process run.

## 5. Post-deployment checklist

1. Open `/actuator/health` and confirm `UP`.
2. Load the catalogue, clearance, and favourites routes in a browser.
3. Manually dispatch the nightly workflow and review its JSON response.
4. Verify the result through `GET /api/v1/admin/runs` with `Authorization: Bearer <ADMIN_TOKEN>`.
5. Set a host-level health check to `/actuator/health/readiness` and enable host log retention/alerts.

The run-history endpoint supports `store`, `page`, and `pageSize` (1–100), for example: `GET /api/v1/admin/runs?store=full-time-store&page=0&pageSize=20`.
