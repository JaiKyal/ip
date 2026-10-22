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

## In progress
- [ ] Phase 3 (partly done): Flyway + driver in the pom, `application-local.properties` (git-ignored, placeholders), `V1__schema.sql`, `V2__seed_catalog.sql`, and 14 migration tests passing on a real PostgreSQL 17. Remaining: connect to Neon (needs the student's credentials) and run the migrations there
- [ ] Phase 2 leftover: build the Dockerfile locally (needs Docker Desktop; untested)

## Pending
- [ ] Phase 3: Neon database (direct URL, `sslmode=require`, separate dev branch), Flyway, seed data (~15 products, 3 categories)
- [ ] Phase 4: entities, repositories, DTOs, public product API
- [ ] Phase 5: JWT auth, roles, admin seeded from `ADMIN_EMAIL` / `ADMIN_PASSWORD`
- [ ] Phase 6: cart, checkout, orders, customer frontend (`api.js`, `ui.js`)
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
