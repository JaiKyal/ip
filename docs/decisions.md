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

## Open
- **Deployment shape:** two Render sites (static + API, needs CORS) or one service serving `static/` (no CORS). Decide in Phase 9; JWT works with both.
- **payments table:** optional. If time is short, keep `payment_status` on `orders` only.
