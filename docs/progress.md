# Progress

Updated: 2026-10-22

## MVP scope (lock this before analytics)
products -> register/login -> cart -> checkout -> my orders -> admin product CRUD -> admin order status. Dashboard, charts and deployment come after.

## Done
- [x] Phase 1: architecture, ER diagram, API/checkout design (`docs/architecture.md`)
- [x] Environment check: JDK 21.0.10, Git 2.55, VS Code + Java extensions, `JAVA_HOME` set
- [x] Repo moved to `C:\dev\ip-project`, git initialised, `.gitignore`, `.env.example`, `CLAUDE.md`, `docs/decisions.md`

- [x] Phase 2: Spring Boot 3.5.16 project in `backend/`, `mvnw.cmd test` passes, packaged jar answers `/actuator/health` with `UP`
- [x] `backend/Dockerfile` (multi-stage, `-Xmx350m`) written

- [x] Pushed to GitHub (public): https://github.com/JaiKyal/ip, `main` in sync with local
- [x] `.gitattributes` keeps `mvnw` and Dockerfile on LF; `mvnw` stored as executable
- [x] Secrets sweep over all history: clean (placeholders only)
- [x] GitHub Actions workflow runs `./mvnw -B verify`; first run on commit f011b41 passed on Linux
- [x] Deployment shape chosen: two sites, frontend separate from the API, with CORS (decisions.md D9)
- [x] Static progress tracker page in `frontend/` (not yet deployed on Vercel)

- [x] Phase 4: entities, repositories, DTOs, services, controllers for `GET /api/products`, `/api/products/{id}`, `/api/categories`; validation, JSON errors, CORS; 33 tests pass on real PostgreSQL 17 and the API was exercised live against Neon `dev` (`docs/api-documentation.md`)
- [x] Phase 5: register, login, `/api/auth/me`, JWT, BCrypt, CUSTOMER/ADMIN roles, admin seeded from `ADMIN_EMAIL`/`ADMIN_PASSWORD`, JSON 401/403, CORS inside Spring Security; 52 tests pass (forged/expired/`alg:none`/edited-role tokens, role access, duplicate emails, SQL injection); verified live on Neon `dev` (one admin and one smoke-test customer exist there)
- [x] Phase 6: cart, idempotent transactional checkout with ordered row locks, simulated payments, order history; V3 (shipping, payment methods) and V4 (colour placeholders) applied to Neon `dev`; customer website (catalog, product, cart, checkout, orders, login/register); 87 tests pass including a last-unit race, a 5-buyers-3-units race, triple-click idempotency and deadlock avoidance; full purchase walked through in a real browser against Neon `dev`
- [x] `scripts/serve-frontend.mjs` serves the website locally on port 5500
- [x] `scripts/run-local.ps1` starts the backend against the Neon `dev` database

## In progress
- [x] Phase 3: Neon project, `dev` branch, `.env` filled (git-ignored), V1 + V2 migrations applied and verified on Neon `dev` (8 tables, 3 categories, 15 products); 14 migration tests pass on PostgreSQL 17
- [ ] Phase 2 leftover: build the Dockerfile locally (needs Docker Desktop; untested)

## Pending
- [ ] Phase 3: Neon database (direct URL, `sslmode=require`, separate dev branch), Flyway, seed data (~15 products, 3 categories)
- [ ] Phase 7: admin CRUD, order status, reports, admin frontend
- [ ] Phase 8: testing, including the two-thread last-unit test
- [ ] Phase 9: deployment (decide one service vs two)
- [ ] Phase 10: report, diagrams, README, viva Q&A, demo script and backup recording

## Blocked / needs the student
- [ ] Create Neon account and project
- [ ] Import `JaiKyal/ip` on https://vercel.com/new with Root Directory `frontend` (connector returned 403)
- [ ] Create Render account
- [ ] Install Docker Desktop to test the Dockerfile locally (not installed yet)
- [ ] Install Postman or Bruno
