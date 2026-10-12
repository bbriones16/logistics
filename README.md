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

From the project root:

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

| Container           | Port  | Description              |
|---------------------|-------|--------------------------|
| logistics-postgres  | 5432  | PostgreSQL 16            |
| logistics-backend   | 8080  | Spring Boot API          |
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

---

## Module status

| Module            | Status      |
|-------------------|-------------|
| Order Management  | ✅ Complete |
| Inventory         | ✅ Complete |
| Warehouse         | ✅ Complete |
| Shipment          | ✅ Complete |
| Delivery/Dispatch | ✅ Complete |
| Tracking          | ✅ Complete |
| Reporting         | ✅ Complete |
| Frontend          | ✅ Complete |

---

## Authentication

All API endpoints (except `/api/auth/**`) require a JWT Bearer token.

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
