# Logistics Management System — Project Plan

## 1. Project Overview

This project is a Logistics Management System MVP built as a modular monolith using Java 21 and Spring Boot. It is designed to be practical, incrementally developed, and production-ready in architecture from day one.

The system covers the full logistics lifecycle — from customer order creation through inventory reservation, shipment management, last-mile delivery dispatch, real-time shipment tracking, and operational reporting — with a clean, well-tested REST API backend.

---

## 2. Project Goals

### Primary goals

- Build a complete, working logistics application incrementally.
- Use modern Java/Spring technologies.
- Keep the architecture simple enough for an MVP but structured for growth.
- Establish clean module boundaries from the beginning.
- Demonstrate senior-level Java architecture and development practices.
- Leave room for future AI capabilities and microservice extraction.

### Secondary goals

The project is positioned to become:

- A portfolio project demonstrating modern Java backend development
- A demonstration platform for consulting engagements
- A potential SaaS/product concept for small-to-mid logistics operators
- A foundation for AI-assisted logistics services

---

## 3. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (Bearer token) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL (production), H2 (tests) |
| Migrations | Liquibase |
| API Docs | OpenAPI 3 / Swagger UI |
| Build | Maven |
| Containerization | Docker (PostgreSQL via Docker Compose) |
| Testing | JUnit 5, Mockito, Spring Boot Test |

---

## 4. Architecture

### Modular Monolith

The system is built as a **modular monolith** — a single deployable Spring Boot application with clearly separated business modules. Each module owns its domain, service layer, repository, DTOs, and REST controllers.

```
logistics-system/
│
├── order/          Order and customer management
├── inventory/      Products, warehouses, stock movements
├── warehouse/      Warehouse management
├── shipment/       Carriers and shipment lifecycle
├── delivery/       Driver, vehicle, and last-mile dispatch
├── tracking/       Shipment tracking event history
├── reporting/      Operational dashboards and summaries
├── security/       JWT authentication and role-based access
└── common/         Shared exceptions, events, error handling
```

### Event-Driven Integration

Modules communicate via **Spring Application Events** — a loosely coupled, Kafka-ready pattern. No module calls another module's service directly for cross-cutting workflows.

```
OrderService  ──publishes──▶  OrderCreatedEvent
                                      │
                              InventoryEventListener
                                      │
                         ┌────────────┴────────────┐
                   StockReservedEvent     StockReservationFailedEvent
                         │                         │
                  OrderEventListener        OrderEventListener
                  (STOCK_RESERVED)          (FAILED + reason)


ShipmentService / DeliveryEventListener
        │
        └──publishes──▶  ShipmentStatusChangedEvent
                                  │
                          TrackingEventListener
                          (records TrackingEvent)
```

This architecture is designed so that migrating to Kafka in the future requires only swapping `@EventListener` for `@KafkaListener` and `ApplicationEventPublisher` for `KafkaTemplate` — no business logic changes.

---

## 5. Security Model

All API endpoints require a JWT Bearer token except `/api/auth/**` and `GET /api/tracking/**`.

### Authentication

| Endpoint | Description |
|---|---|
| `POST /api/auth/register` | Register a new user |
| `POST /api/auth/login` | Login and receive JWT token |

### Roles

| Role | Access |
|---|---|
| `ADMIN` | Full access to all modules |
| `OPERATIONS` | Orders, customers, inventory, shipments, deliveries, tracking, reports |
| `WAREHOUSE_USER` | Products, warehouses, inventory |
| `DISPATCHER` | Carriers, shipments, drivers, vehicles, deliveries, tracking |

---

## 6. Module Status

| Module | Status | Tests |
|---|---|---|
| Order Management | ✅ Complete | ✅ |
| Warehouse / Inventory | ✅ Complete | ✅ |
| Shipment Management | ✅ Complete | ✅ |
| Delivery / Dispatch | ✅ Complete | ✅ |
| Tracking | ✅ Complete | ✅ |
| Reporting | ✅ Complete | ✅ |

**Total: 163 tests passing, 0 failures.**

---

## 7. Module 1 — Order Management

Manages the full customer order lifecycle.

### Entities

- **Customer** — name, email, phone, address
- **Order** — customer, status, notes, failure reason, items
- **OrderItem** — SKU, product name, quantity, unit price

### Order Status Lifecycle

```
CREATED → CONFIRMED → STOCK_RESERVED → PROCESSING → READY_TO_SHIP → SHIPPED → DELIVERED
                                                                             → RETURNED
                                                    → FAILED
         → CANCELLED
```

State transitions are enforced by a strict state machine. Invalid transitions return `422 Unprocessable Entity`.

### Key Business Rules

- Orders must contain at least one item
- Stock reservation is triggered automatically via event after order creation
- Orders transition to `FAILED` with a reason if stock reservation fails
- Orders transition to `STOCK_RESERVED` automatically when inventory confirms reservation
- Orders transition to `READY_TO_SHIP` automatically when a shipment is created

### APIs

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | /api/customers | Create customer | ADMIN, OPERATIONS |
| GET | /api/customers | List customers | ADMIN, OPERATIONS |
| GET | /api/customers/{id} | Get customer | ADMIN, OPERATIONS |
| POST | /api/orders | Create order | ADMIN, OPERATIONS |
| GET | /api/orders | List orders (paginated) | ADMIN, OPERATIONS |
| GET | /api/orders/{id} | Get order | ADMIN, OPERATIONS |
| PUT | /api/orders/{id}/status | Update order status | ADMIN, OPERATIONS |

---

## 8. Module 2 — Warehouse / Inventory

Manages products, warehouses, stock levels, and all stock movements.

### Entities

- **Product** — SKU, name, description, unit of measure, reorder level
- **Warehouse** — code, name, address
- **Inventory** — product + warehouse combination, quantity on hand, quantity reserved
- **StockMovement** — full audit trail of every stock change (RECEIVE, RESERVE, RESERVE_RELEASE, PICK, PACK, ADJUSTMENT)

### Key Business Rules

- Each product-warehouse combination has exactly one inventory record
- Stock reservation uses a **validate-all-then-reserve-all** pattern — no partial reservations
- If any item in an order cannot be reserved, no items are reserved and the order is marked `FAILED`
- Available quantity = quantity on hand − quantity reserved
- Low-stock detection based on product reorder level

### APIs

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | /api/products | Create product | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/products | List products | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/products/{id} | Get product | ADMIN, OPERATIONS, WAREHOUSE_USER |
| POST | /api/warehouses | Create warehouse | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/warehouses | List warehouses | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/warehouses/{id} | Get warehouse | ADMIN, OPERATIONS, WAREHOUSE_USER |
| POST | /api/inventory/receive | Receive stock | ADMIN, OPERATIONS, WAREHOUSE_USER |
| POST | /api/inventory/pick | Pick stock | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/inventory/warehouse/{id} | Stock by warehouse | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/inventory/product/{id} | Stock by product | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/inventory/low-stock | Low stock items | ADMIN, OPERATIONS, WAREHOUSE_USER |
| GET | /api/inventory/product/{id}/movements | Stock movement history (paginated) | ADMIN, OPERATIONS, WAREHOUSE_USER |

---

## 9. Module 3 — Shipment Management

Manages carriers and the full shipment lifecycle. Supports two shipment types:

- **FULFILLMENT** — tied to an order, triggered after stock reservation
- **STANDALONE** — independent shipment not tied to an order (e.g. bulk cargo, crates of goods)

### Entities

- **Carrier** — code, name, medium (LAND/AIR/SEA), company-owned flag, contact details
- **Shipment** — type, order (nullable), carrier, medium, tracking number, status, cargo details, POD, delivery dates

### Tracking Number Generation

- **Company-owned carriers** — tracking number auto-generated: `TRK-yyyyMMdd-NNNNN`
- **Third-party carriers** — operator must provide the carrier's tracking number

### Shipment Status Lifecycle

```
CREATED → PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB → OUT_FOR_DELIVERY → DELIVERED
                                                                                                      → FAILED
       → CANCELLED
PROCESSING → CANCELLED
READY_FOR_PICKUP → CANCELLED
```

### Key Business Rules

- FULFILLMENT shipments require the order to be in `STOCK_RESERVED` status
- One shipment per order (enforced)
- STANDALONE shipments require a cargo description
- For company-owned carrier shipments, `OUT_FOR_DELIVERY` and `DELIVERED` transitions are blocked — these must go through the Delivery module
- For third-party carrier shipments, status is updated manually through this module
- `DELIVERED` requires proof of delivery (POD) and sets actual delivery date
- Every status change publishes a `ShipmentStatusChangedEvent` consumed by the Tracking module

### APIs

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | /api/carriers | Create carrier | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/carriers | List carriers | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/carriers/{id} | Get carrier | ADMIN, OPERATIONS, DISPATCHER |
| POST | /api/shipments | Create shipment | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/shipments | List shipments (paginated) | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/shipments/{id} | Get shipment | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/shipments/tracking/{trackingNumber} | Get by tracking number | ADMIN, OPERATIONS, DISPATCHER |
| PUT | /api/shipments/{id}/status | Update shipment status | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/shipments/{id}/tracking | Get tracking history | ADMIN, OPERATIONS, DISPATCHER |

---

## 10. Module 4 — Delivery / Dispatch

Manages last-mile delivery for company-owned fleet shipments. Handles driver and vehicle assignment, dispatch workflow, and delivery completion.

### Scope

This module handles **LAND shipments only** for the MVP. AIR and SEA fleet management is planned for a future phase.

### Entities

- **Driver** — name, license number, phone, status (AVAILABLE / ON_DELIVERY / OFF_DUTY)
- **Vehicle** — plate number, type (MOTORCYCLE / VAN / TRUCK), capacity (kg), status (AVAILABLE / IN_USE / MAINTENANCE)
- **Delivery** — links shipment + driver + vehicle, status, scheduled date, delivery cost, notes

### Delivery Status Lifecycle

```
ASSIGNED → OUT_FOR_DELIVERY → DELIVERED
                             → FAILED
         → CANCELLED
```

### Key Business Rules

- Delivery assignment only allowed for **company-owned carrier** shipments
- Delivery assignment only allowed for **LAND** shipments (MVP scope)
- Shipment must be in `ARRIVED_AT_HUB` status before a delivery can be assigned
- One delivery per shipment (enforced)
- Driver must be `AVAILABLE` at time of assignment
- Vehicle must be `AVAILABLE` at time of assignment
- On delivery assignment: driver → `ON_DELIVERY`, vehicle → `IN_USE`, shipment → `OUT_FOR_DELIVERY`
- On delivery completed: driver → `AVAILABLE`, vehicle → `AVAILABLE`, shipment → `DELIVERED` with POD and actual delivery date
- On delivery failed/cancelled: driver → `AVAILABLE`, vehicle → `AVAILABLE`, shipment → `FAILED`
- Proof of delivery (POD) is required to mark a delivery as `DELIVERED`
- POD and actual delivery date are stored on the Shipment as the single source of truth

### APIs

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | /api/drivers | Create driver | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/drivers | List drivers | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/drivers/{id} | Get driver | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/drivers/available | List available drivers | ADMIN, OPERATIONS, DISPATCHER |
| POST | /api/vehicles | Create vehicle | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/vehicles | List vehicles | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/vehicles/{id} | Get vehicle | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/vehicles/available | List available vehicles | ADMIN, OPERATIONS, DISPATCHER |
| POST | /api/deliveries | Assign delivery (accepts optional `deliveryCost`) | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/deliveries | List deliveries (paginated) | ADMIN, OPERATIONS, DISPATCHER |
| GET | /api/deliveries/{id} | Get delivery | ADMIN, OPERATIONS, DISPATCHER |
| PUT | /api/deliveries/{id}/status | Update delivery status | ADMIN, OPERATIONS, DISPATCHER |

---

## 11. Module 5 — Tracking

Records an immutable event log for every shipment status change, providing a full timeline per shipment. The public tracking endpoint requires no authentication, allowing customers to track their own shipments.

### Entity

- **TrackingEvent** — shipment ID, tracking number, status, location (optional), notes (optional), occurred at timestamp

### How Events Are Recorded

Every `ShipmentStatusChangedEvent` is published by:
- `ShipmentService` — on shipment creation and every manual status update
- `DeliveryEventListener` — when delivery drives shipment to `OUT_FOR_DELIVERY`, `DELIVERED`, or `FAILED`

The `TrackingEventListener` consumes `ShipmentStatusChangedEvent` using `@TransactionalEventListener(AFTER_COMMIT)` to ensure the shipment is committed and visible before the tracking record is written.

### APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | /api/tracking/{trackingNumber} | Get tracking history by tracking number | **Public** |
| GET | /api/shipments/{id}/tracking | Get tracking history by shipment ID | ADMIN, OPERATIONS, DISPATCHER |

### Example Tracking Response

```json
{
  "shipmentId": 42,
  "trackingNumber": "TRK-20261010-00001",
  "currentStatus": "DELIVERED",
  "events": [
    { "status": "CREATED",          "occurredAt": "2026-10-10T08:00:00Z" },
    { "status": "PROCESSING",       "occurredAt": "2026-10-10T08:15:00Z" },
    { "status": "PICKED_UP",        "occurredAt": "2026-10-10T09:30:00Z" },
    { "status": "IN_TRANSIT",       "occurredAt": "2026-10-10T11:00:00Z" },
    { "status": "ARRIVED_AT_HUB",   "occurredAt": "2026-10-10T14:00:00Z" },
    { "status": "OUT_FOR_DELIVERY", "occurredAt": "2026-10-11T08:20:00Z" },
    { "status": "DELIVERED",        "occurredAt": "2026-10-11T11:45:00Z" }
  ]
}
```

---

## 12. Module 6 — Reporting

Provides operational summaries and dashboards by aggregating data from existing modules. No new database tables — all queries run against indexed columns on existing tables.

### APIs

| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | /api/reports/orders | Order counts by status + created today | ADMIN, OPERATIONS |
| GET | /api/reports/shipments | Shipment counts by status | ADMIN, OPERATIONS |
| GET | /api/reports/deliveries | Delivery counts by status + scheduled today | ADMIN, OPERATIONS |
| GET | /api/reports/inventory/summary | Total products, warehouses, stock on hand, low-stock count | ADMIN, OPERATIONS |
| GET | /api/reports/inventory/low-stock | Full list of items below reorder threshold | ADMIN, OPERATIONS |

### Example Responses

**GET /api/reports/orders**
```json
{
  "total": 142,
  "createdToday": 8,
  "byStatus": {
    "CREATED": 5,
    "CONFIRMED": 3,
    "STOCK_RESERVED": 12,
    "PROCESSING": 8,
    "READY_TO_SHIP": 6,
    "SHIPPED": 18,
    "DELIVERED": 84,
    "CANCELLED": 4,
    "FAILED": 2,
    "RETURNED": 0
  }
}
```

**GET /api/reports/inventory/summary**
```json
{
  "totalProducts": 48,
  "totalWarehouses": 3,
  "totalStockOnHand": 12450,
  "lowStockItemCount": 5
}
```

---

## 13. End-to-End Business Flow

```
Customer
    │
    ▼
Order Created  ──[OrderCreatedEvent]──▶  InventoryEventListener
    │                                           │
    │                              ┌────────────┴────────────┐
    │                        All stock OK             Any item short
    │                              │                         │
    │                    [StockReservedEvent]    [StockReservationFailedEvent]
    │                              │                         │
    │                    Order → STOCK_RESERVED     Order → FAILED
    │
    ▼
Shipment Created (FULFILLMENT)
    │
    ├──[ShipmentCreatedEvent]──▶  Order → READY_TO_SHIP
    │
    └──[ShipmentStatusChangedEvent: CREATED]──▶  TrackingEvent recorded
    │
    ▼
Shipment progresses through status lifecycle
    │  (each transition publishes ShipmentStatusChangedEvent → TrackingEvent)
    │
    ├── Third-party carrier
    │       └── Operator updates status manually → DELIVERED (with POD)
    │
    └── Company-owned carrier (LAND)
            └── Delivery assigned (Driver + Vehicle)
                    │
                    ├──[DeliveryAssignedEvent]──▶  Shipment → OUT_FOR_DELIVERY
                    │                              Driver → ON_DELIVERY
                    │                              Vehicle → IN_USE
                    │                              TrackingEvent recorded
                    │
                    └── Delivery → DELIVERED (with POD)
                            │
                            └──[DeliveryCompletedEvent]──▶  Shipment → DELIVERED
                                                            POD + actual date set
                                                            Driver → AVAILABLE
                                                            Vehicle → AVAILABLE
                                                            TrackingEvent recorded
```

---

## 14. Database Schema

Eight Liquibase migrations, applied in order:

| Migration | Description |
|---|---|
| V001 | Orders, customers, order items |
| V002 | Users (authentication) |
| V003 | Products, warehouses, inventory, stock movements |
| V004 | Failure reason column on orders |
| V005 | Carriers, shipments |
| V006 | Drivers, vehicles, deliveries |
| V007 | Tracking events (indexes on shipment_id, tracking_number) |
| V008 | Performance indexes on orders, shipments, deliveries, stock_movements, order_items |
| V009 | Add `delivery_cost` column to deliveries |

### V008 Indexes Added

| Table | Indexed Columns |
|---|---|
| orders | status, created_at, customer_id |
| order_items | order_id |
| shipments | status, created_at, carrier_id |
| deliveries | status, scheduled_date, driver_id |
| stock_movements | product_id, warehouse_id, created_at |

---

## 15. Pagination

High-volume list endpoints support pagination via Spring's `Pageable`. All return a `Page<T>` response with `content`, `totalElements`, `totalPages`, `number`, and `size`.

| Endpoint | Default Page Size | Default Sort |
|---|---|---|
| GET /api/orders | 20 | createdAt DESC |
| GET /api/shipments | 20 | createdAt DESC |
| GET /api/deliveries | 20 | createdAt DESC |
| GET /api/inventory/product/{id}/movements | 50 | createdAt DESC |

**Usage:** `GET /api/orders?page=0&size=20&sort=createdAt,desc`

---

## 16. Error Handling

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-10-10T10:30:00Z",
  "status": 422,
  "code": "SHIPMENT_INVALID_ORDER_STATUS",
  "message": "Order must be in STOCK_RESERVED status to create a shipment, current status: CREATED",
  "path": "/api/shipments"
}
```

| HTTP Status | Usage |
|---|---|
| 400 | Validation errors |
| 401 | Missing or invalid JWT token |
| 403 | Insufficient role |
| 404 | Resource not found |
| 422 | Business rule violation (invalid state transition, duplicate, unavailable resource) |
| 500 | Unexpected server error |

---

## 17. Testing

All business logic is covered by unit and integration tests.

| Test Type | Count | Coverage |
|---|---|---|
| Unit tests | ~65 | Services, event listeners, state machines, business rules |
| Integration tests | ~98 | REST endpoints, full event flows, security, DB interactions |
| **Total** | **163** | **0 failures** |

### Key scenarios tested

- Full order → stock reservation → shipment → delivery → tracking end-to-end flow
- Partial stock reservation prevention (validate-all-then-reserve-all)
- All invalid state transitions (orders, shipments, deliveries)
- Duplicate resource enforcement (one shipment per order, one delivery per shipment)
- Driver/vehicle availability enforcement
- Company-owned vs third-party carrier routing
- LAND-only delivery restriction
- JWT authentication and role-based access control
- Proof of delivery requirement enforcement
- Public tracking endpoint (no auth required)
- Tracking event recorded for every status change
- All five reporting endpoints return correct structure and data
- Pagination on list endpoints

---

## 18. Running the System

### Prerequisites

- Java 21
- Maven 3.9+
- Docker

### Start PostgreSQL

```bash
cd docker
docker compose up -d
```

### Build and run

```bash
./mvnw spring-boot:run
```

API available at: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Run tests

```bash
./mvnw test
```

---

## 19. Future Capabilities

### Shipping / Delivery Costing Engine

The current MVP supports a simple `freightCost` field on shipments and `deliveryCost` on deliveries — manually entered values. A future costing engine would replace this with automated cost calculation based on:

- Weight and dimensions (volumetric weight)
- Origin / destination distance and zone
- Carrier rate cards and fuel surcharges
- Shipment medium (LAND, AIR, SEA) pricing tiers
- Insurance and handling fees

This would integrate with the Carrier module to store rate configurations and auto-populate costs at shipment/delivery creation time.

### Kafka Migration

The event architecture is designed for Kafka readiness. Migration path:

```
@EventListener              →  @KafkaListener
ApplicationEventPublisher   →  KafkaTemplate
```

No business logic changes required.

### Microservice Extraction

Individual modules can be extracted into independent services when justified by scale:

```
Modular Monolith
      │
      ▼
Identify scaling bottlenecks
      │
      ▼
Extract selected modules (e.g. Tracking, Reporting)
      │
      ▼
Microservices where justified
```

### AI Capabilities (Future)

- Natural language logistics queries ("Which shipments are delayed today?")
- Delivery delay prediction based on historical data
- Route optimization for company-owned fleet
- AI customer support for shipment tracking queries
- AI-assisted reporting and analytics

---

## 20. Project Philosophy

1. **Start small, build working functionality before adding complexity.**
2. **Modular design with clean boundaries from day one.**
3. **No premature microservices — extract only when justified.**
4. **Test from the beginning — every module has unit and integration tests.**
5. **Event-driven where it matters — loose coupling without distributed system complexity.**
6. **Business rules are explicit and enforced — not left to the caller.**
7. **Performance considered from the start — indexes and pagination built in.**
8. **Designed for future growth without over-engineering the MVP.**

---

## 21. Current Project Status

| Area | Status |
|---|---|
| Architecture | ✅ Modular monolith, event-driven |
| Authentication | ✅ JWT, role-based access control |
| Order Management | ✅ Complete |
| Warehouse / Inventory | ✅ Complete |
| Shipment Management | ✅ Complete |
| Delivery / Dispatch | ✅ Complete (LAND fleet, MVP scope) |
| Tracking | ✅ Complete (public + authenticated endpoints) |
| Reporting | ✅ Complete (5 operational reports) |
| Pagination | ✅ Orders, shipments, deliveries, stock movements |
| Database Indexes | ✅ V008 — 13 indexes across 5 tables |
| Test Coverage | ✅ 163 tests, 0 failures |
| API Documentation | ✅ Swagger UI |
| Database Migrations | ✅ Liquibase (9 migrations) |
| Docker | ✅ PostgreSQL via Docker Compose |
| Frontend | ✅ React + TypeScript (Phase 8 complete) |
| Source Control | ✅ GitHub — github.com/bbriones16/logistics |

---

## 22. Phase 8 — Frontend

### Stack

| Concern | Library |
|---|---|
| Framework | React 18 + TypeScript + Vite 5 |
| Styling | Tailwind CSS v4 + custom shadcn-style components |
| Routing | React Router v6 |
| Server state | TanStack Query v5 |
| Forms | React Hook Form + Zod |
| HTTP | Axios (with JWT interceptor + 401 redirect) |
| Auth state | React Context + localStorage |

### Project structure

```
frontend/
├── src/
│   ├── api/          # Axios instance + one file per module
│   │   ├── client.ts       JWT interceptor, 401 handler
│   │   ├── auth.ts
│   │   ├── orders.ts
│   │   ├── inventory.ts
│   │   ├── shipments.ts    includes trackingApi
│   │   ├── delivery.ts
│   │   └── reporting.ts
│   ├── components/
│   │   ├── ui/             Button, Card, Input, Label, Badge, Select
│   │   ├── AppLayout.tsx   Sidebar nav + main content shell
│   │   └── ProtectedRoute.tsx
│   ├── context/
│   │   └── AuthContext.tsx  login, logout, isAuthenticated
│   ├── pages/
│   │   ├── auth/           LoginPage (login + register)
│   │   ├── dashboard/      DashboardPage (reporting summary)
│   │   ├── orders/         OrdersPage, OrderDetailPage, CustomersPage
│   │   ├── inventory/      InventoryPage
│   │   ├── shipments/      ShipmentsPage, ShipmentDetailPage, CarriersPage
│   │   ├── delivery/       DeliveriesPage, DriversPage, VehiclesPage
│   │   └── tracking/       TrackPage (public)
│   └── types/
│       └── index.ts        TypeScript types mirroring all backend DTOs
```

### Pages

| Route | Page | Auth |
|---|---|---|
| `/login` | Login + register form | Public |
| `/track` | Public shipment tracker | Public |
| `/` | Reporting dashboard — 4 summary cards + low-stock alert | Protected |
| `/orders` | Orders list (paginated) + create form | Protected |
| `/orders/:id` | Order detail + items breakdown | Protected |
| `/customers` | Customers list + create form | Protected |
| `/inventory` | Stock by warehouse + low-stock panel + receive stock | Protected |
| `/shipments` | Shipments list (paginated) + create form | Protected |
| `/shipments/:id` | Shipment detail + status update + tracking timeline | Protected |
| `/carriers` | Carriers list + create form | Protected |
| `/delivery` | Deliveries list (paginated) + assign form + inline status update | Protected |
| `/drivers` | Drivers list + create form | Protected |
| `/vehicles` | Vehicles list + create form | Protected |

### Key implementation details

- Vite dev server proxies `/api` → `http://localhost:8080` (no CORS config needed in dev)
- Axios request interceptor attaches `Authorization: Bearer <token>` from localStorage
- Axios response interceptor clears token and redirects to `/login` on 401
- `totalAmount` computed on frontend from `items` (not returned by backend)
- Shipment status transitions enforced client-side to show only valid next states
- Delivery status update inline in the table row (select + optional POD input + button)
- Public `/track` page requires no authentication — calls `GET /api/tracking/{trackingNumber}` directly
- Tracking timeline displayed newest-first with a vertical timeline UI

### Running the frontend

```bash
cd frontend
npm install
npm run dev
```

App available at: `http://localhost:5173`

---

*Last updated: October 2026*
