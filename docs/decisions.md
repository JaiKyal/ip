# Decision log

Each entry: what we chose, why, and what we gave up. Use these as viva answers.

## D1. Spring Boot 3.5.16 rather than 4.x (2026-10-22)
- **Chose:** Boot 3.5.16 on Java 21, the last 3.x release.
- **Why:** The brief asks for 3.x, and most tutorials and answers target 3.x. Boot 4 changes several things (Jackson 3, Spring Security 7, Hibernate 7, split starters).
- **Cost:** start.spring.io no longer offers 3.x, so the `pom.xml` is written by hand (only the Maven Wrapper scripts came from Initializr). Open-source support for 3.5 has likely ended, which is acceptable for an academic project. Revisit if we need a supported line.

## D2. JWT in the Authorization header, not cookies (2026-10-22)
- **Chose:** Stateless JWT sent as `Authorization: Bearer <token>`.
- **Why:** The frontend and API may live on different origins (two `*.onrender.com` sites). Browsers block cross-site cookies, which would break login in a deployed demo. Bearer tokens avoid that and also avoid CSRF concerns.
- **Cost:** The token must be stored in the browser (sessionStorage), so XSS matters: we escape all user text with `textContent`. Tokens are short-lived and there is no refresh flow.

## D3. Repo location `C:\dev\ip-project` (2026-10-22)
- **Why:** No spaces in the path avoids Maven, Git and script problems.

## D4. Money as NUMERIC / BigDecimal; price and name snapshots in order_items
- **Why:** Floating point cannot represent money exactly. Snapshots keep old orders correct after products change.

## D5. Soft delete for products
- **Why:** Products referenced by orders are set `active = false` instead of deleted, keeping order history valid.

## D6. Cancellation rules
- Allowed from PENDING, CONFIRMED, PROCESSING. Not from SHIPPED or DELIVERED.
- Cancelling restores stock and sets `payment_status` to REFUNDED (simulated).
- Revenue counts only non-cancelled orders with `payment_status = PAID`.

## D7. Checkout concurrency
- **Chose:** `@Transactional` checkout with `@Lock(PESSIMISTIC_WRITE)` on product rows, ordered by id to avoid deadlocks, plus an idempotency key (unique constraint) against double submits.
- **Proof:** a test where two threads buy the last unit and exactly one succeeds.

## D8. Phase 2 starts with a minimal dependency set
- **Chose:** Web, Validation, Actuator only. JPA, Flyway, PostgreSQL driver arrive in Phase 3; Security in Phase 5.
- **Why:** JPA without a database fails at startup, and Security would lock the health endpoint before auth exists.

## D9. Where each part is hosted: Vercel and single vs two deployments (2026-10-22)
- **Fact:** Vercel hosts static files and serverless functions. It cannot run the Spring Boot jar, so the backend stays on a container host (Render web service via `backend/Dockerfile`).
- **Now:** Vercel serves only `frontend/`, which currently holds a static progress-tracker page (`frontend/index.html`). Project root directory on Vercel = `frontend`, no build command. The repo is connected through https://vercel.com/new (the connector got a 403 when asked to create the project).
- **Decided by the student on 2026-10-22: Option A, two sites.** The frontend is a separate static site and the API is its own Render web service.
  - **Consequences:** the backend needs CORS driven by `CORS_ALLOWED_ORIGINS` (exact origins, no wildcard, since requests carry an `Authorization` header); the frontend reads the API base URL from a single constant in `api.js`; JWT in the header (D2) avoids cross-site cookie problems.
  - **Frontend host:** Vercel or Render Static, either works because the frontend is plain static files. Pick when deploying in Phase 9. The Vercel import of `frontend/` stays as it is.
- **Rejected, Option B (one service):** copying `frontend/` into Spring Boot's `static/` would remove CORS and one service, but it merges the two deployments the original brief asked to keep separate. It remains a fallback if CORS causes trouble near the demo.

## D10. Repository is public (checked 2026-10-22)
- `github.com/JaiKyal/ip` is public. History was scanned for secrets: none found; `.env.example` holds placeholders only.
- Commit author metadata contains a personal email address. Consider switching to the GitHub `noreply` address for future commits.
- CI (`.github/workflows/ci.yml`) runs `./mvnw -B verify` on every push and pull request. `backend/mvnw` is stored with the executable bit (mode 100755) so it runs on the Linux runner.

## D11. Schema choices in V1 (2026-10-22)
- **payments table kept.** It is cheap, matches the brief, and gives each simulated payment a unique `transaction_reference`. `orders.payment_status` is still the value revenue queries use.
- **Idempotency:** `orders.idempotency_key` with `UNIQUE (user_id, idempotency_key)`. A double-clicked checkout carries the same key, so the second insert fails instead of creating a second order. Orders without a key are allowed.
- **Constraints in the database, not only in Java:** `stock_quantity >= 0`, `price >= 0`, `quantity > 0`, status and role value lists, `line_total = unit_price * quantity`, case-insensitive unique email. Even a buggy service cannot store an oversold or negative state.
- **Delete rules:** `order_items.product_id` has no cascade, so a sold product cannot be hard-deleted (matches D5). Deleting a user removes their cart; orders are never cascaded away.
- **Seed data lives in a migration (V2)** because the catalog is demo data. Users and the admin are not seeded there (no password hashes in git); the admin comes from `ADMIN_EMAIL` / `ADMIN_PASSWORD` at startup in Phase 5.
- **Tests run the real migrations** against an embedded PostgreSQL 17 (`io.zonky.test:embedded-postgres`, test scope only), so no Docker or Neon account is needed to verify the schema locally or in CI.
- **Product images** are `placehold.co` placeholder URLs for now.

## D12. Neon setup and how migrations are applied (2026-10-22)
- **Neon project:** `ip shopping cart` (`winter-violet-14112139`), AWS Singapore, free plan, PostgreSQL 18.6.
- **Branches:** `production` (default, not migrated yet, reserved for the deployed app) and `dev` (`br-quiet-dream-aobzl4bt`, migrated: V1 and V2 applied, 3 categories and 15 products verified).
- **Direct connection only.** The connector hands out the pooled `-pooler` host; the `.env` helper rejects it and the direct host (same name without `-pooler`) is used, with `sslmode=require` and no `channel_binding` option.
- **Credentials** live only in the git-ignored `.env`. The repo is public, so they are never committed or written into docs.
- **Migrations are applied with the Flyway Maven plugin** (`./mvnw flyway:info flyway:migrate`, connection from `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`) because the app has no JDBC starter yet, so Spring Boot does not run Flyway at startup. Once JPA is added in Phase 4 the app will migrate on startup as well.
- **Version gap:** Neon runs PostgreSQL 18; Flyway 11.7.2 (managed by Boot 3.5.16) is tested up to 17 and prints a warning but works. The tests use PostgreSQL 17.

## D13. Public product API design (Phase 4, 2026-10-22)
- **DTOs only.** Controllers return records (`ProductResponse`, `CategoryResponse`, `PageResponse`), never entities. The public product JSON leaves out `active`, timestamps and anything internal.
- **Search with JPA Specifications** instead of one big query with optional parameters. A `:param is null` pattern makes PostgreSQL fail on untyped nulls; Specifications add a filter only when the client sent one.
- **Search text is escaped** so `%` and `_` match literally, and all values are bound as parameters, so SQL injection is not possible (tested).
- **Sort is a whitelist** (`featured`, `newest`, `name`, `price_asc`, `price_desc`). Clients cannot sort by arbitrary columns.
- **No N+1 queries.** The repository loads the category in the same query (`@EntityGraph`), and `open-in-view` is off so lazy loading cannot leak into the web layer.
- **Page size is capped at 50**, so one request cannot dump the table.
- **One error shape** (`ApiError`) for every failure, built in one `@RestControllerAdvice`; unexpected errors return a generic message and are logged server-side only.
- **Inactive products** return 404 on the detail endpoint, the same as a missing product, so deactivated items are not revealed.
- **CORS** is configured with exact origins from `CORS_ALLOWED_ORIGINS`. In Phase 5 it moves into the Spring Security filter chain so preflight requests are not blocked by authentication.
- **Tests run the whole app on a real PostgreSQL 17** (`AbstractPostgresIntegrationTest`): Flyway applies the real migrations and Hibernate's `ddl-auto=validate` checks every entity against them.

## Open
- **Frontend host in Phase 9:** Vercel or Render Static (both fit D9).
- **Low-stock threshold:** the seed uses stock 1 to 5 as "low"; confirm the value in Phase 7.
- **payments table:** optional. If time is short, keep `payment_status` on `orders` only.
