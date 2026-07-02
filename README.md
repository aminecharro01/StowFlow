# StowFlow

**StowFlow** is a multi-tenant SaaS platform for inventory management: articles, stock movements, suppliers, purchase orders, replenishment, POS, alerts, and dashboards. The stack is **Spring Boot 3** (REST API) + **Next.js** (App Router) + **PostgreSQL**.

> **Naming:** *StowFlow* = product brand; *Inventra* = technical package name (`com.stowflow.inventra`, `inventra-web`).

---

## Features

| Module | Description |
|--------|-------------|
| **Dashboard** | Live KPIs (stock value, low-stock alerts, movements, orders) with month-over-month deltas |
| **Products** | CRUD articles, SKU uniqueness per tenant, min/max stock rules, logical archive |
| **Inventory** | Stock levels overview and low-stock monitoring |
| **Movements** | Manual stock IN/OUT with validation (insufficient stock blocked) |
| **Suppliers** | Supplier directory and detail pages |
| **Orders** | Purchase order workflow (draft → submitted → received) |
| **Replenishment** | Internal replenishment requests |
| **POS** | Point-of-sale checkout and sales history with detail modal |
| **Reports** | Analytics views (some charts remain demo placeholders — banner shown in UI) |
| **Settings** | Tenant and user administration |
| **Platform** | Super-admin tenant management (`SUPER_ADMIN` only) |
| **AI Assistant** | Floating chat widget powered by Google Gemini (optional, RBAC-aware) |

**Cross-cutting:** JWT authentication, role-based access control (RBAC), multi-tenant isolation via `X-Tenant-Slug`, Flyway migrations, GitHub Actions CI, Docker deployment.

---

## Project structure

| Folder | Role |
|--------|------|
| `stowflow-api` | Spring Boot 3 REST API — JPA, PostgreSQL, Flyway, Swagger (local profile) |
| `inventra-web` | Next.js frontend — Tailwind CSS, TanStack Query |
| `scripts` | Database init/reset and validation scripts |
| `.github/workflows` | CI pipeline (API tests + frontend build) |

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **Java** | 17+ (17 or 21 recommended) | Maven Wrapper included — no global Maven required |
| **Node.js** | 20+ | For local frontend development |
| **PostgreSQL** | 16 | Local install or via Docker |
| **Docker Desktop** | 24+ | Optional — recommended for demos |

---

## Quick start with Docker

The fastest way to run the full stack (PostgreSQL + API + frontend):

```powershell
# From the repository root (folder containing docker-compose.yml)
.\run-docker.ps1
```

Or manually:

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Application | http://localhost:3000 |
| REST API | http://localhost:8000 |
| Swagger UI | http://localhost:8000/swagger-ui.html |
| Health check | http://localhost:8000/actuator/health |

**Demo account:** `stock@default.demo` / `password` (tenant: `default`)

### Docker architecture

```
┌─────────────────────────────────────────────────────────┐
│              docker-compose (internal network)           │
│                                                          │
│  postgres:5432  ◄──  stowflow-api:8000  ◄──  inventra-web:3000
│       │                    │                    │
│   persistent DB        Spring Boot            Next.js
└─────────────────────────────────────────────────────────┘
```

| Container | Image / build | Role |
|-----------|---------------|------|
| `postgres` | `postgres:16-alpine` | Database `stowflow` (host port **5433** → container 5432) |
| `stowflow-api` | `stowflow-api/Dockerfile` | REST API, JWT, demo seeder on boot |
| `inventra-web` | `inventra-web/Dockerfile` | Next.js UI, proxies `/api` → API |

### Useful Docker commands

```bash
docker compose up -d              # run in background
docker compose ps                 # service status
docker compose logs -f stowflow-api
docker compose down               # stop containers
docker compose down -v            # stop + wipe PostgreSQL volume
docker compose build --no-cache   # full rebuild
```

Copy `.env.docker.example` to `.env.docker` (optional) to set `GEMINI_API_KEY` for the AI assistant.

---

## Local development

### Backend (API)

**Windows (PowerShell), from `stowflow-api`:**

```powershell
.\run-dev.ps1
```

This frees port **8000** and activates the `local` profile (Gemini, Swagger, demo seed).

With global Maven:

```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

- API: http://localhost:8000  
- Swagger: http://localhost:8000/swagger-ui.html  
- Tenant header: `X-Tenant-Slug: default` (handled automatically by the frontend)

### Frontend

```bash
cd inventra-web
npm install
npm run dev
```

In development, **do not set** `NEXT_PUBLIC_API_URL` — Next.js proxies `/api/*` to `http://127.0.0.1:8000`. Open http://localhost:3000 (or **3001** if 3000 is busy).

Optional: copy `inventra-web/.env.local.example` → `.env.local` to point at a remote API.

### Run tests

```powershell
# Backend (PostgreSQL required on port 5433 for tests)
cd stowflow-api
.\mvnw.cmd test

# Frontend
cd inventra-web
npm run build
```

---

## Database

| Script | Usage |
|--------|--------|
| `scripts/init-db.sql` | Create database `stowflow` (connect to `postgres`) |
| `scripts/init-db-schema.sql` | Optional manual schema (connect to `stowflow`) |
| `scripts/reset-db.sql` | Reset the database |

**Recommended:** create an empty database, then start the API — Flyway migrations and the demo seeder handle the rest.

After Flyway schema changes in Docker:

```bash
docker compose down -v && docker compose up --build
```

---

## Environment variables

See [`.env.example`](.env.example) for the full reference.

| Variable | Service | Description |
|----------|---------|-------------|
| `JWT_SECRET` | API | JWT signing secret (≥ 32 chars) — **required in production** |
| `GEMINI_API_KEY` | API | Google Gemini API key (chat assistant, optional) |
| `INVENTRA_GEMINI_ENABLED` | API | `true` / `false` (default: `true`) |
| `SPRING_DATASOURCE_*` | API | PostgreSQL connection |
| `API_PROXY_TARGET` | Front (Docker) | Internal API URL (`http://stowflow-api:8000`) |
| `POSTGRES_PORT` | Docker | Host port for PostgreSQL (default: `5433`) |
| `API_PORT` | Docker | Host port for API (default: `8000`) |
| `WEB_PORT` | Docker | Host port for frontend (default: `3000`) |

For local API secrets, use `stowflow-api/src/main/resources/application-local.yml` (gitignored) or environment variables.

---

## Security & authentication

- **JWT (Bearer):** `POST /api/auth/login` returns an `accessToken` (30 min default).
- The frontend stores the token in `sessionStorage` — passwords are never persisted client-side.
- **RBAC** via `@PreAuthorize` on API endpoints.
- **Multi-tenant:** every request is scoped by `X-Tenant-Slug`; users cannot access another tenant's data.
- **Swagger:** use bearer auth — login via `/api/auth/login`, then "Authorize" with `Bearer <token>`.
- **Production:** set `JWT_SECRET`, disable Swagger, use `ddl-auto: validate` (see `application-prod.yml`).
- **Rate limiting:** login endpoint limited to 10 requests/minute per IP.

### Roles

| Role | Capabilities |
|------|--------------|
| `SUPER_ADMIN` | Platform management, cross-tenant access |
| `TENANT_ADMIN` | Full tenant administration |
| `MANAGER` | Oversight, POS, reports, order approval |
| `STOCK_MANAGER` | Articles, suppliers, orders, movements, replenishment |
| `SALES` | POS and read-only stock views |

### Demo accounts

Password for all demo users: **`password`**

| Email | Role | Tenant |
|-------|------|--------|
| `superadmin@stowflow.demo` | SUPER_ADMIN | — |
| `admin@default.demo` | TENANT_ADMIN | default |
| `manager@default.demo` | MANAGER | default |
| `stock@default.demo` | STOCK_MANAGER | default |
| `sales@default.demo` | SALES | default |

### Example: login + API call

```powershell
$login = Invoke-RestMethod -Uri "http://localhost:8000/api/auth/login" -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"stock@default.demo","password":"password"}'

Invoke-RestMethod -Uri "http://localhost:8000/api/articles" `
  -Headers @{
    Authorization = "Bearer $($login.accessToken)"
    "X-Tenant-Slug" = "default"
  }
```

### JWT validation script

```powershell
# From repository root
.\scripts\test-jwt.ps1
```

Runs 28 automated JWT/RBAC scenarios against a running API.

---

## API overview

Main REST endpoints (see Swagger for full documentation):

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/login`, `GET /api/auth/me` |
| Articles | `GET/POST/PUT/DELETE /api/articles` (paginated list) |
| Movements | `GET/POST /api/movements` (paginated list) |
| Suppliers | `GET/POST/PUT /api/suppliers` |
| Purchase orders | `GET/POST /api/purchase-orders`, workflow actions |
| Dashboard | `GET /api/dashboard` |
| POS | `POST /api/pos/sales`, `GET /api/pos/sales/{id}` |
| Chat | `POST /api/chat` (Gemini assistant) |
| Admin | `GET /api/admin/audit`, user management |
| Platform | `GET/POST /api/platform/tenants` (super-admin) |

---

## Troubleshooting

### Frontend (`Failed to fetch`, port conflicts)

1. **Single dev server** — if you see *Another next dev server is already running*, stop the old process and restart `npm run dev`.
2. **Consistent URL** — use either `localhost` or `127.0.0.1`, not both interchangeably.
3. **CORS** — the API allows `localhost` / `127.0.0.1` on ports 3000 and 3001. For other origins, add them in `stowflow-api/src/main/resources/application.yml` under `app.cors.allowed-origins`.

### Docker

- Ensure ports **5433**, **8000**, and **3000** are free.
- API healthcheck may take up to 90s on first boot (Flyway + seed).
- Use `docker compose logs stowflow-api` if the API container fails health checks.

### Tests

Integration tests expect PostgreSQL on port **5433** (see `application-test.yml`).

---

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) runs on push/PR to `main`, `master`, or `develop`:

- **API job:** Maven tests with Testcontainers PostgreSQL, then package
- **Web job:** `npm ci`, lint, production build

---

## Tech stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 17, Spring Boot 3, Spring Security, JPA, Flyway, PostgreSQL |
| Frontend | Next.js (App Router), React, TypeScript, Tailwind CSS, TanStack Query |
| AI | Google Gemini API |
| DevOps | Docker Compose, GitHub Actions, Spring Actuator |

---

## License

Academic / project work — EMSI PFA inventory management platform.
