# API documentation

Base URL locally: `http://localhost:8080`. All responses are JSON. Money values are decimal numbers (INR).
Endpoints are added phase by phase; this file lists only what exists.

## Conventions

**Error shape.** Every error uses the same body, and never includes stack traces or internals:

```json
{ "timestamp": "2026-10-22T13:21:48Z", "status": 404, "error": "Not Found",
  "message": "Product 999999 not found", "path": "/api/products/999999" }
```

| Status | Meaning |
|---|---|
| 400 | Invalid input (bad number, out-of-range page size, unknown sort, text longer than 100 characters) |
| 404 | Unknown record, or a deactivated product, or an unknown URL |
| 405 | Wrong HTTP method for the URL |
| 500 | Unexpected server error (details are logged on the server only) |

**Paged lists** return `{ items, page, size, totalElements, totalPages }` with zero-based `page`.

**CORS.** Browsers may call `/api/**` only from the origins listed in `CORS_ALLOWED_ORIGINS`.

**Authentication.** Log in (or register) to get a token, then send it on every protected request:
`Authorization: Bearer <token>`. Tokens last 2 hours (`JWT_EXPIRY_MINUTES`). Roles are `CUSTOMER` and `ADMIN`.

| Access | Endpoints |
|---|---|
| Public | `GET /api/products`, `GET /api/products/{id}`, `GET /api/categories`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /actuator/health` |
| Any logged-in user | `GET /api/auth/me`, and every other endpoint not listed here |
| Admin only | everything under `/api/admin/**` |

Additional error statuses: **401** no token, or an invalid, expired or forged token; **403** logged in but the role is not allowed; **409** the email is already registered.

## Public endpoints (no login)

### GET /api/products
Lists active products. Out-of-stock products are included and flagged.

| Query parameter | Default | Rules |
|---|---|---|
| `q` | none | Name search, case-insensitive, matches part of the name. Max 100 characters. `%` and `_` are treated as plain text. |
| `categoryId` | none | Only products in this category. |
| `sort` | `featured` | `featured`, `newest`, `name`, `price_asc`, `price_desc`. Anything else returns 400. |
| `page` | `0` | 0 or more. |
| `size` | `12` | 1 to 50. |

Example: `GET /api/products?categoryId=1&sort=price_desc&size=2`

```json
{
  "items": [
    { "id": 3, "name": "Smart Watch", "description": "Fitness tracker with heart-rate monitor...",
      "price": 2999.00, "stockQuantity": 5, "inStock": true,
      "imageUrl": "https://placehold.co/600x400?text=Smart+Watch",
      "category": { "id": 1, "name": "Electronics", "description": "Audio, wearables and charging accessories" } }
  ],
  "page": 0, "size": 2, "totalElements": 5, "totalPages": 3
}
```

Errors: 400 for invalid parameters.

### GET /api/products/{id}
One active product (same shape as an item above). Errors: 404 if the product does not exist or is inactive, 400 if `id` is not a number.

### GET /api/categories
All categories, sorted by name: `[ { "id": 1, "name": "Electronics", "description": "..." }, ... ]`

### POST /api/auth/register
Creates a customer account and logs them in. There is no role field: every new account is a `CUSTOMER`.

Request: `{ "fullName": "Asha Rao", "email": "asha@example.com", "password": "at-least-8-chars" }`
(`fullName` required, up to 120 characters; `email` valid, up to 255, stored lower-case; `password` 8 to 72 characters.)

Response `201`:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresInSeconds": 7200,
  "user": { "id": 2, "fullName": "Asha Rao", "email": "asha@example.com", "role": "CUSTOMER" } }
```
Errors: `400` with the names of the invalid fields (rejected values are never echoed), `409` email already registered (ignoring upper/lower case).

### POST /api/auth/login
Request: `{ "email": "asha@example.com", "password": "..." }`. Response `200`: same shape as register.
Error `401` `"Invalid email or password"`. The message is identical for an unknown email, a wrong password and a disabled account, so the API does not reveal which emails exist.

### GET /api/auth/me
Requires a token. Returns `{ "id", "fullName", "email", "role" }` for the user in the token. Errors: `401`.

### GET /actuator/health
`{ "status": "UP" }`. Used by the hosting platform to check the service is alive.

## Cart (login required)

The cart belongs to the user in the token. Prices are never sent by the browser; they are read from the database.
Every cart response has this shape:

```json
{ "items": [ { "id": 7, "productId": 3, "name": "Smart Watch", "imageUrl": "https://...", "unitPrice": 2999.00,
               "quantity": 2, "lineTotal": 5998.00, "stockQuantity": 5, "available": true } ],
  "totalQuantity": 2, "subtotal": 5998.00, "hasUnavailableItems": false }
```
`available` is false when the product was hidden or its stock fell below the quantity in the cart.

| Method and URL | Body | Result |
|---|---|---|
| `GET /api/cart` | | 200 cart (created empty on first use) |
| `POST /api/cart/items` | `{ "productId": 3, "quantity": 1 }` (1 to 99) | 201 cart. Adding the same product again increases the quantity. |
| `PATCH /api/cart/items/{itemId}` | `{ "quantity": 4 }` (1 to 99) | 200 cart |
| `DELETE /api/cart/items/{itemId}` | | 200 cart |

Errors: `400` invalid quantity, `404` unknown or hidden product, or an item that is not in *your* cart, `409` "Only N left in stock for X" or "X is out of stock".

## Checkout (login required)

### POST /api/checkout
Headers: `Authorization: Bearer <token>` and `Idempotency-Key: <random 8 to 64 characters>` (the website uses a UUID per attempt).

Request (no prices, no items: the server uses the cart and database prices):
```json
{ "shipping": { "name": "Asha Rao", "phone": "9876543210", "address": "12 MG Road, Indiranagar",
                "city": "Bengaluru", "pincode": "560038" },
  "paymentMethod": "UPI" }
```
`paymentMethod` is `UPI`, `CARD` (both simulated and approved instantly) or `COD` (cash on delivery).

Response `201` (new order) or `200` (the same Idempotency-Key was already used, so the existing order is returned):
```json
{ "id": 1, "orderNumber": "ORD-20261022-9XEESU", "status": "CONFIRMED", "paymentStatus": "PAID", "paymentMethod": "UPI",
  "subtotal": 7297.00, "totalAmount": 7297.00, "orderDate": "2026-10-22T14:32:10Z",
  "shipping": { "name": "Asha Rao", "phone": "9876543210", "address": "12 MG Road, Indiranagar", "city": "Bengaluru", "pincode": "560038" },
  "items": [ { "productId": 3, "name": "Smart Watch", "quantity": 2, "unitPrice": 2999.00, "lineTotal": 5998.00 } ],
  "payment": { "reference": "SIM-8FE450BE44B64347", "status": "PAID", "simulated": true } }
```
UPI and CARD orders start `CONFIRMED` + `PAID`. COD orders start `PENDING` + `PENDING` with no payment record.

Errors: `400` empty cart, missing or malformed Idempotency-Key, invalid shipping fields (field names listed), unknown payment method; `402` simulated payment declined (nothing is saved, stock and cart unchanged); `409` stock ran out or a product was hidden since it was added to the cart (nothing is saved).

## Orders (login required, own orders only)

| Method and URL | Result |
|---|---|
| `GET /api/orders/my-orders?page=0&size=10` | Paged list, newest first: `{ id, orderNumber, status, paymentStatus, paymentMethod, totalAmount, orderDate, itemCount }` |
| `GET /api/orders/{orderId}` | One order, same shape as the checkout response. Another customer's order returns `404`, exactly like a missing one. |

## Planned (not built yet)
The admin endpoints listed in `docs/architecture.md`.
