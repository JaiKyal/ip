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

### GET /actuator/health
`{ "status": "UP" }`. Used by the hosting platform to check the service is alive.

## Planned (not built yet)
Authentication, cart, checkout, orders, and the admin endpoints listed in `docs/architecture.md`.
