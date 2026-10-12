# Logistics Management System

A modular monolith logistics platform built with Java 21 and Spring Boot.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker

## Running locally

**1. Start PostgreSQL**

```bash
cd docker
docker compose up -d
```

**2. Build and run**

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

API documentation (Swagger UI) is available at `http://localhost:8080/swagger-ui/index.html`.

---

## Module status

| Module            | Status      |
|-------------------|-------------|
| Order Management  | ✅ Complete |
| Inventory         | 🔜 Planned  |
| Warehouse         | 🔜 Planned  |
| Shipment          | 🔜 Planned  |
| Delivery/Dispatch | 🔜 Planned  |
| Tracking          | 🔜 Planned  |
| Reporting         | 🔜 Planned  |

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

| Method | Endpoint               | Description        | Role Required         |
|--------|------------------------|--------------------|-----------------------|
| POST   | /api/customers         | Create customer    | ADMIN, OPERATIONS     |
| GET    | /api/customers         | List all customers | ADMIN, OPERATIONS     |
| GET    | /api/customers/{id}    | Get customer by id | ADMIN, OPERATIONS     |

### Orders

| Method | Endpoint                  | Description         | Role Required         |
|--------|---------------------------|---------------------|-----------------------|
| POST   | /api/orders               | Create order        | ADMIN, OPERATIONS     |
| GET    | /api/orders               | List all orders     | ADMIN, OPERATIONS     |
| GET    | /api/orders/{id}          | Get order by id     | ADMIN, OPERATIONS     |
| PUT    | /api/orders/{id}/status   | Update order status | ADMIN, OPERATIONS     |

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

---

## Running tests

```bash
./mvnw test
```
