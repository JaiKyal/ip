# Project conventions: Shopping Cart & Order Management System

College IP project. Customer storefront + admin dashboard on one Java backend and one PostgreSQL database. The student must be able to explain every part in a viva, so explain decisions in plain language.

## Stack (pinned)
- Java 21, Spring Boot 3.5.16 (set in `backend/pom.xml`), Maven via the wrapper (`backend\mvnw.cmd`)
- PostgreSQL (Neon), Flyway migrations, Spring Data JPA / Hibernate
- Frontend: vanilla HTML/CSS/JS, Fetch API, Chart.js only. No frontend frameworks.
- Auth: JWT sent in the `Authorization: Bearer` header (not cookies). See `docs/decisions.md`.

## Rules
- Layering: Controller -> Service -> Repository. Business rules live in services only.
- Money is `BigDecimal` in Java and `NUMERIC(12,2)` in SQL. Never `double` or `float`.
- Schema changes only through Flyway files in `backend/src/main/resources/db/migration`. `spring.jpa.hibernate.ddl-auto=validate`.
- Never return entities from controllers; use DTOs. Never expose `password_hash`.
- Validate input with Bean Validation. Prices and totals are always recomputed on the server.
- Role checks are enforced on the backend (Spring Security), never only by hiding UI.
- No secrets in code, docs or commits. Config comes from environment variables; `.env` is git-ignored; `.env.example` lists names only.
- Frontend: all HTTP goes through one `api.js`; use `textContent`, not `innerHTML`, for user-supplied text.
- Checkout is one `@Transactional` method with ordered pessimistic row locks on products.
- Run the tests before saying a phase is done. Do not claim a feature works until it was exercised.

## Workflow
- Work one phase at a time; the plan is in `docs/architecture.md`, status in `docs/progress.md`.
- Log every notable choice in `docs/decisions.md` (what, why).
- Commit at the end of each phase with a clear message.
- Windows + PowerShell. Repo path has no spaces: `C:\dev\ip-project`.
