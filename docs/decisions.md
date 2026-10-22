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

## Open
- **Frontend host in Phase 9:** Vercel or Render Static (both fit D9).
- **payments table:** optional (see D8 notes); decide in Phase 3 when writing the schema.
- **payments table:** optional. If time is short, keep `payment_status` on `orders` only.
