# Logistics Management System

A modular monolith logistics platform built with Java 21 and Spring Boot.

---

## Option A — Run locally (without Docker)

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (for PostgreSQL only)
- Node.js 22+

### 1. Start PostgreSQL

```bash
cd docker
docker compose up -d postgres
```

### 2. Run the backend

```bash
./mvnw spring-boot:run
```

Backend available at `http://localhost:8080`.  
Swagger UI at `http://localhost:8080/swagger-ui/index.html`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend available at `http://localhost:5173`.  
API calls are proxied to `http://localhost:8080` via the Vite dev proxy.

---

## Option B — Run with Docker (full stack)

### Prerequisites

- Docker with Docker Compose

### Build images

```bash
cd docker
docker compose build
```

### Start all services

```bash
docker compose up -d
```

This starts three containers:

| Container           | Port  | Description               |
|---------------------|-------|---------------------------|
| logistics-postgres  | 5432  | PostgreSQL 16             |
| logistics-backend   | 8080  | Spring Boot API           |
| logistics-frontend  | 80    | React app served by Nginx |

- Frontend: `http://localhost`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Nginx proxies `/api` requests from the frontend to the backend automatically.

### Stop all services

```bash
docker compose down
```

To also remove the database volume:

```bash
docker compose down -v
```

---

## Running tests

```bash
./mvnw test
```

185 tests, 0 failures.

---

## Module status

| Module                  | Status      |
|-------------------------|-------------|
| Order Management        | ✅ Complete |
| Inventory               | ✅ Complete |
| Warehouse               | ✅ Complete |
| Shipment                | ✅ Complete |
| Delivery/Dispatch       | ✅ Complete |
| Tracking                | ✅ Complete |
| Reporting               | ✅ Complete |
| Configuration (Phase 1) | ✅ Complete |
| Frontend                | ✅ Complete |

---

## Authentication

All API endpoints (except `/api/auth/**` and `GET /api/tracking/**`) require a JWT Bearer token.

**Register:**
```
POST /api/auth/register
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Login:**
```
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "password123"
}
```

Both return a `token`. Include it in subsequent requests:
```
Authorization: Bearer <token>
```

### Roles

| Role              | Description                              |
|-------------------|------------------------------------------|
| ADMIN             | Full access including module config      |
| OPERATIONS        | Order, inventory, shipment management    |
| WAREHOUSE_USER    | Inventory and warehouse operations       |
| DISPATCHER        | Shipment and delivery management         |
| FREIGHT_OPERATOR  | Reserved for International Freight phase |
| CUSTOMS_OFFICER   | Reserved for Customs Clearance phase     |
| FINANCE_USER      | Reserved for future finance features     |

> `register` assigns `OPERATIONS` by default. Role upgrades must be done directly in the database.

---

## Order Management APIs

### Customers

| Method | Endpoint            | Description        | Role Required     |
|--------|---------------------|--------------------|-------------------|
| POST   | /api/customers      | Create customer    | ADMIN, OPERATIONS |
| GET    | /api/customers      | List all customers | ADMIN, OPERATIONS |
| GET    | /api/customers/{id} | Get customer by id | ADMIN, OPERATIONS |

### Orders

| Method | Endpoint                | Description         | Role Required     |
|--------|-------------------------|---------------------|-------------------|
| POST   | /api/orders             | Create order        | ADMIN, OPERATIONS |
| GET    | /api/orders             | List all orders     | ADMIN, OPERATIONS |
| GET    | /api/orders/{id}        | Get order by id     | ADMIN, OPERATIONS |
| PUT    | /api/orders/{id}/status | Update order status | ADMIN, OPERATIONS |

### Order status lifecycle

```
CREATED → CONFIRMED → STOCK_RESERVED → PROCESSING → READY_TO_SHIP → SHIPPED → DELIVERED
                                                                             → RETURNED
                                                    → FAILED
         → CANCELLED
```

Stock reservation is event-driven: creating an order fires `OrderCreatedEvent`, which triggers inventory reservation. On success the order transitions to `STOCK_RESERVED`; on failure it transitions to `FAILED` with a reason.

---

## Warehouse APIs

| Method | Endpoint            | Description         | Role Required                    |
|--------|---------------------|---------------------|----------------------------------|
| POST   | /api/warehouses     | Create warehouse    | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/warehouses     | List all warehouses | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/warehouses/{id}| Get warehouse by id | ADMIN, OPERATIONS, WAREHOUSE_USER |

---

## Inventory APIs

### Products

| Method | Endpoint          | Description       | Role Required                    |
|--------|-------------------|-------------------|----------------------------------|
| POST   | /api/products     | Create product    | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/products     | List all products | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/products/{id}| Get product by id | ADMIN, OPERATIONS, WAREHOUSE_USER |

### Stock

| Method | Endpoint                              | Description                    | Role Required                    |
|--------|---------------------------------------|--------------------------------|----------------------------------|
| POST   | /api/inventory/receive                | Receive stock into warehouse   | ADMIN, OPERATIONS, WAREHOUSE_USER |
| POST   | /api/inventory/pick                   | Pick stock for an order        | ADMIN, OPERATIONS, WAREHOUSE_USER |
| POST   | /api/inventory/pack                   | Pack stock for shipment        | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/inventory/warehouse/{warehouseId}| Stock levels by warehouse      | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/inventory/product/{productId}    | Stock levels by product        | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/inventory/low-stock              | Products below threshold       | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET    | /api/inventory/product/{id}/movements | Paginated stock movement audit | ADMIN, OPERATIONS, WAREHOUSE_USER |

---

## Shipment APIs

### Carriers

| Method | Endpoint          | Description                        | Role Required                      |
|--------|-------------------|------------------------------------|------------------------------------|
| POST   | /api/carriers     | Create carrier                     | ADMIN, OPERATIONS, DISPATCHER      |
| GET    | /api/carriers     | List carriers (optional ?medium=)  | ADMIN, OPERATIONS, DISPATCHER      |
| GET    | /api/carriers/{id}| Get carrier by id                  | ADMIN, OPERATIONS, DISPATCHER      |

### Shipments

| Method | Endpoint                          | Description                  | Role Required                 |
|--------|-----------------------------------|------------------------------|-------------------------------|
| POST   | /api/shipments                    | Create shipment              | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/shipments                    | List shipments (paginated)   | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/shipments/{id}               | Get shipment by id           | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/shipments/tracking/{number}  | Get shipment by tracking no. | ADMIN, OPERATIONS, DISPATCHER |
| PUT    | /api/shipments/{id}/status        | Update shipment status       | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/shipments/{id}/tracking      | Get tracking events          | Any authenticated user        |

**Shipment types:** `FULFILLMENT` (linked to an order) or `STANDALONE` (no order).  
**Shipment mediums:** `LAND`, `AIR`, `SEA`.

Company-owned carrier shipments auto-generate a tracking number (`TRK-YYYYMMDD-NNNNN`). Third-party carriers require an operator-provided tracking number.

### Shipment status lifecycle

```
CREATED → PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB → OUT_FOR_DELIVERY → DELIVERED
                                                                                                       → RETURNED
                                                     → FAILED
         → CANCELLED
```

> `OUT_FOR_DELIVERY` and `DELIVERED` for company-owned carrier shipments must go through the Delivery module.

---

## Delivery/Dispatch APIs

### Drivers

| Method | Endpoint               | Description           | Role Required                 |
|--------|------------------------|-----------------------|-------------------------------|
| POST   | /api/drivers           | Create driver         | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/drivers           | List all drivers      | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/drivers/{id}      | Get driver by id      | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/drivers/available | List available drivers| ADMIN, OPERATIONS, DISPATCHER |

### Vehicles

| Method | Endpoint                | Description            | Role Required                 |
|--------|-------------------------|------------------------|-------------------------------|
| POST   | /api/vehicles           | Create vehicle         | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/vehicles           | List all vehicles      | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/vehicles/{id}      | Get vehicle by id      | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/vehicles/available | List available vehicles| ADMIN, OPERATIONS, DISPATCHER |

### Deliveries

| Method | Endpoint                  | Description                  | Role Required                 |
|--------|---------------------------|------------------------------|-------------------------------|
| POST   | /api/deliveries           | Assign delivery              | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/deliveries           | List deliveries (paginated)  | ADMIN, OPERATIONS, DISPATCHER |
| GET    | /api/deliveries/{id}      | Get delivery by id           | ADMIN, OPERATIONS, DISPATCHER |
| PUT    | /api/deliveries/{id}/status| Update delivery status      | ADMIN, OPERATIONS, DISPATCHER |

Delivery assignment is only allowed for company-owned carrier shipments in `ARRIVED_AT_HUB` status. Assigning a delivery sets the driver to `ON_DELIVERY`, vehicle to `IN_USE`, and the shipment to `OUT_FOR_DELIVERY` via events.

### Delivery status lifecycle

```
ASSIGNED → OUT_FOR_DELIVERY → DELIVERED
                            → FAILED
         → CANCELLED
```

---

## Tracking APIs

| Method | Endpoint                        | Description                          | Auth Required |
|--------|---------------------------------|--------------------------------------|---------------|
| GET    | /api/tracking/{trackingNumber}  | Get tracking events by tracking no.  | No (public)   |
| GET    | /api/shipments/{id}/tracking    | Get tracking events by shipment id   | Yes           |

One tracking event is recorded per shipment status change.

---

## Reporting APIs

All reporting endpoints require authentication (`ADMIN` or `OPERATIONS`).

| Method | Endpoint                      | Description                              |
|--------|-------------------------------|------------------------------------------|
| GET    | /api/reports/orders           | Order counts by status + created today   |
| GET    | /api/reports/shipments        | Shipment counts by status                |
| GET    | /api/reports/deliveries       | Delivery counts by status + scheduled today |
| GET    | /api/reports/inventory/summary| Total products, warehouses, stock on hand, low-stock count |
| GET    | /api/reports/inventory/low-stock | Products below their low-stock threshold |

---

## Configuration APIs (Modular Extension Phase 1)

Controls which optional modules are enabled at runtime. All admin endpoints require the `ADMIN` role.

### Module admin

| Method | Endpoint                        | Description                          | Role Required |
|--------|---------------------------------|--------------------------------------|---------------|
| GET    | /api/admin/modules              | List all modules with config         | ADMIN         |
| GET    | /api/admin/modules/{moduleKey}  | Get module config by key             | ADMIN         |
| PUT    | /api/admin/modules/{moduleKey}  | Enable or disable a module           | ADMIN         |
| GET    | /api/admin/modules/audit        | Full audit log of config changes     | ADMIN         |

### Module discovery

| Method | Endpoint        | Description                              | Role Required      |
|--------|-----------------|------------------------------------------|--------------------|
| GET    | /api/me/modules | List enabled modules for current user    | Any authenticated  |

### Module catalog

| Module key                  | Type        | Default  |
|-----------------------------|-------------|----------|
| DOMESTIC_LOGISTICS          | CORE        | enabled  |
| INTERNATIONAL_FREIGHT       | BUSINESS    | disabled |
| CUSTOMS_CLEARANCE           | BUSINESS    | disabled |
| FREIGHT_CUSTOMS_INTEGRATION | INTEGRATION | disabled |
| DOMESTIC_HANDOFF            | INTEGRATION | disabled |
| CUSTOMER_PORTAL             | BUSINESS    | disabled |

**Update request:**
```json
PUT /api/admin/modules/INTERNATIONAL_FREIGHT
{
  "enabled": true,
  "reason": "Activating for client onboarding"
}
```

Every update writes an immutable audit record capturing `previousValue`, `newValue`, `changedBy`, and `reason`.

---

## Error response format

```json
{
  "timestamp": "2026-09-30T10:30:00Z",
  "status": 400,
  "code": "ORDER_VALIDATION_ERROR",
  "message": "Order contains no items",
  "path": "/api/orders"
}
```

---

## Future Capabilities

### Modular Extension (upcoming phases)

- **Phase 2 — International Freight**: freight jobs, house/master bills of lading, multi-leg routing
- **Phase 3 — Customs Clearance**: clearance jobs, document management, duty tracking
- **Phase 4 — Freight-Customs Integration**: link freight shipments to clearance jobs
- **Phase 5 — Domestic Handoff**: create domestic delivery jobs from international arrivals
- **Phase 6 — Customer Portal**: customer-facing tracking and document access

### Shipping / Delivery Costing Engine

The current MVP supports a simple `freightCost` field on shipments and `deliveryCost` on deliveries — manually entered values. A future costing engine would replace this with automated cost calculation based on:

- Weight and dimensions (volumetric weight)
- Origin / destination distance and zone
- Carrier rate cards and fuel surcharges
- Shipment medium (LAND, AIR, SEA) pricing tiers
- Insurance and handling fees
