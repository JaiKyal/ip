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

## In progress
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
- [ ] Create GitHub and Render accounts
- [ ] Install Docker Desktop to test the Dockerfile locally (not installed yet)
- [ ] Install Postman or Bruno
