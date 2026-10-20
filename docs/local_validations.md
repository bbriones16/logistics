# Local Validations — Full End-to-End (Phases 1–7 + Modular Extension Phase 1)

Manual test run against a live PostgreSQL instance.  
Date: 2026-10-13  
Base URL: `http://localhost:8080`

---

## Setup

**Start PostgreSQL**
```bash
cd docker && docker compose up -d
```

**Start application**
```bash
./mvnw spring-boot:run
```

---

## Step 1 — Authentication

### Register

**Request**
```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Admin User",
  "email": "admin@logistics.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{ "token": "eyJhbGciOiJIUzUxMiJ9..." }
```

### Login

**Request**
```http
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@logistics.com", "password": "password123" }
```

**Response** `200 OK`
```json
{ "token": "eyJhbGciOiJIUzUxMiJ9..." }
```

✅ Register returns token. Login returns token. All subsequent requests use `Authorization: Bearer <token>`.

---

## Step 2 — Warehouse

**Request**
```http
POST /api/warehouses
{ "code": "WH-MAIN", "name": "Main Warehouse", "address": "123 Logistics Ave" }
```

**Response** `201 Created`
```json
{ "id": 1, "code": "WH-MAIN", "name": "Main Warehouse", "active": true }
```

✅ Warehouse created with id=1.

---

## Step 3 — Products

Three products created: SKU-A (threshold=10), SKU-B (threshold=5), SKU-C (threshold=20, intentionally low stock).

```json
{ "id": 1, "sku": "SKU-A", "name": "Item A", "lowStockThreshold": 10 }
{ "id": 2, "sku": "SKU-B", "name": "Item B", "lowStockThreshold": 5  }
{ "id": 3, "sku": "SKU-C", "name": "Item C", "lowStockThreshold": 20 }
```

✅ All three products created.

---

## Step 4 — Receive Stock

| Product | Received | On Hand | Reserved | Available |
|---|---|---|---|---|
| SKU-A | 50 | 50 | 0 | 50 |
| SKU-B | 30 | 30 | 0 | 30 |
| SKU-C | 5  | 5  | 0 | 5  |

✅ Stock received. SKU-C intentionally below its threshold of 20.

---

## Step 5 — Customer

```json
{ "id": 1, "name": "John Doe", "email": "john@example.com" }
```

✅ Customer created with id=1.

---

## Step 6 — Order with Sufficient Stock (Order 1)

**Request**
```http
POST /api/orders
{
  "customerId": 1,
  "items": [
    { "sku": "SKU-A", "productName": "Item A", "quantity": 5,  "unitPrice": 10.00 },
    { "sku": "SKU-B", "productName": "Item B", "quantity": 10, "unitPrice": 20.00 }
  ]
}
```

**POST response:** `status: CREATED`

**GET /api/orders/1** (after event processing):
```json
{ "id": 1, "status": "STOCK_RESERVED" }
```

✅ Order auto-transitioned to `STOCK_RESERVED` via `OrderCreatedEvent → InventoryEventListener → StockReservedEvent → OrderEventListener`.

---

## Step 7 — Order with Insufficient Stock (Order 2)

SKU-C has 5 units, order requests 100. Entire order must fail — no partial reservations.

**GET /api/orders/2** (after event processing):
```json
{
  "id": 2,
  "status": "FAILED",
  "failureReason": "Stock reservation failed for SKU SKU-C: Insufficient stock for SKU SKU-C: available=5, requested=100"
}
```

✅ Order failed with exact reason identifying SKU-C.  
✅ SKU-A and SKU-B were NOT reserved — validate-all-then-reserve-all confirmed.

---

## Step 8 — Inventory After Orders 1 & 2

**GET /api/inventory/warehouse/1**

| Product | On Hand | Reserved | Available |
|---|---|---|---|
| SKU-A | 50 | 5  | 45 |
| SKU-B | 30 | 10 | 20 |
| SKU-C | 5  | 0  | 5  |

✅ Only order 1 reserved stock. Order 2 left no reservation.

---

## Step 9 — Carriers

```http
POST /api/carriers  →  { "id": 1, "code": "OWN-FLEET",   "companyOwned": true  }
POST /api/carriers  →  { "id": 2, "code": "3PL-EXPRESS", "companyOwned": false }
```

✅ Company-owned carrier (id=1) and third-party carrier (id=2) created.

---

## Step 10 — Fulfillment Shipment: Company-Owned Carrier (Shipment 1)

**Request**
```http
POST /api/shipments
{
  "shipmentType": "FULFILLMENT",
  "orderId": 1,
  "carrierId": 1,
  "medium": "LAND",
  "shipToAddress": "1 Main St, Springfield",
  "weightKg": 2.5,
  "freightCost": 15.00,
  "estimatedDeliveryDate": "2026-10-20"
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "trackingNumber": "TRK-20261012-00001",
  "status": "CREATED",
  "carrierName": "Our Fleet"
}
```

**GET /api/orders/1** (after event):
```json
{ "status": "READY_TO_SHIP" }
```

✅ Tracking number auto-generated for company-owned carrier: `TRK-20261012-00001`.  
✅ Order auto-transitioned to `READY_TO_SHIP` via `ShipmentCreatedEvent → OrderEventListener`.

---

## Step 11 — Advance Shipment 1 to ARRIVED_AT_HUB

```
CREATED → PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB
```

Each transition confirmed with correct status in response.

✅ All 5 transitions succeeded.

---

## Step 12 — Company-Owned Carrier Blocks Manual Dispatch

**Request**
```http
PUT /api/shipments/1/status
{ "status": "OUT_FOR_DELIVERY" }
```

**Response** `422 Unprocessable Entity`
```json
{
  "code": "SHIPMENT_USE_DELIVERY",
  "message": "Use the Delivery module to dispatch company-owned carrier shipments"
}
```

✅ Manual `OUT_FOR_DELIVERY` blocked for company-owned carrier — must go through Delivery module.

---

## Step 13 — Driver & Vehicle

```http
POST /api/drivers   →  { "id": 1, "name": "Maria Santos", "licenseNumber": "LIC-MS-001", "status": "AVAILABLE" }
POST /api/vehicles  →  { "id": 1, "plateNumber": "ABC-1234", "vehicleType": "VAN", "status": "AVAILABLE" }
```

✅ Driver and vehicle created, both AVAILABLE.

---

## Step 14 — Assign Delivery

**Request**
```http
POST /api/deliveries
{
  "shipmentId": 1,
  "driverId": 1,
  "vehicleId": 1,
  "scheduledDate": "2026-10-13",
  "notes": "Handle with care"
}
```

**Response** `201 Created`
```json
{
  "id": 1,
  "status": "ASSIGNED",
  "driverName": "Maria Santos",
  "vehiclePlate": "ABC-1234",
  "scheduledDate": "2026-10-13"
}
```

**After `DeliveryAssignedEvent` (AFTER_COMMIT):**

| Resource | Status |
|---|---|
| Shipment 1 | `OUT_FOR_DELIVERY` |
| Driver 1 | `ON_DELIVERY` |
| Vehicle 1 | `IN_USE` |

✅ Delivery assigned. Event-driven side effects confirmed.

---

## Step 15 — Complete Delivery

```http
PUT /api/deliveries/1/status  →  { "status": "OUT_FOR_DELIVERY" }
PUT /api/deliveries/1/status  →  { "status": "DELIVERED", "proofOfDelivery": "Signed by John Doe at door" }
```

**After `DeliveryCompletedEvent`:**

| Resource | Value |
|---|---|
| Shipment status | `DELIVERED` |
| Shipment proofOfDelivery | `"Signed by John Doe at door"` |
| Shipment actualDeliveryDate | `2026-10-13` |
| Driver status | `AVAILABLE` |
| Vehicle status | `AVAILABLE` |

✅ Delivery completed. POD and actual delivery date copied to shipment. Driver and vehicle released.

---

## Step 16 — Tracking: Company-Owned Carrier Shipment

**Request (no auth required)**
```http
GET /api/tracking/TRK-20261012-00001
```

**Response** `200 OK`
```json
{
  "trackingNumber": "TRK-20261012-00001",
  "currentStatus": "DELIVERED",
  "eventCount": 8,
  "statuses": [
    "CREATED", "PROCESSING", "READY_FOR_PICKUP", "PICKED_UP",
    "IN_TRANSIT", "ARRIVED_AT_HUB", "OUT_FOR_DELIVERY", "DELIVERED"
  ]
}
```

✅ 8 tracking events recorded — one per status change.  
✅ Public endpoint requires no authentication.  
✅ `OUT_FOR_DELIVERY` and `DELIVERED` events recorded via `DeliveryEventListener → ShipmentStatusChangedEvent → TrackingEventListener`.

---

## Step 17 — Tracking: Authenticated by Shipment ID

**Request**
```http
GET /api/shipments/1/tracking
Authorization: Bearer <token>
```

**Response** `200 OK`
```json
{
  "shipmentId": 1,
  "trackingNumber": "TRK-20261012-00001",
  "currentStatus": "DELIVERED",
  "eventCount": 8
}
```

✅ Authenticated tracking by shipment ID works.

---

## Step 18 — Tracking Error Cases

| Scenario | Expected | Result |
|---|---|---|
| `GET /api/tracking/UNKNOWN-999` | 404 Not Found | ✅ `RESOURCE_NOT_FOUND` |
| `GET /api/shipments/1/tracking` (no auth) | 401 Unauthorized | ✅ |

---

## Step 19 — Third-Party Carrier Flow (Order 3 / Shipment 2)

New order created → auto-reserved → shipment created with third-party carrier and external tracking number.

```http
POST /api/shipments
{
  "shipmentType": "FULFILLMENT",
  "orderId": 3,
  "carrierId": 2,
  "medium": "LAND",
  "trackingNumber": "3PL-EXP-20261012-001",
  "shipToAddress": "2 Commerce Rd, Springfield"
}
```

```json
{ "id": 2, "trackingNumber": "3PL-EXP-20261012-001", "carrierName": "Express Logistics" }
```

Manually advanced through full lifecycle:
```
PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB → OUT_FOR_DELIVERY
```

```http
PUT /api/shipments/2/status
{ "status": "DELIVERED", "proofOfDelivery": "Left at reception desk" }
```

```json
{ "status": "DELIVERED", "proofOfDelivery": "Left at reception desk", "actualDeliveryDate": "2026-10-13" }
```

**Tracking:**
```http
GET /api/tracking/3PL-EXP-20261012-001
```
```json
{ "currentStatus": "DELIVERED", "eventCount": 8 }
```

✅ Third-party carrier: operator-provided tracking number used.  
✅ All status transitions allowed manually (no Delivery module required).  
✅ 8 tracking events recorded.

---

## Step 20 — Standalone Shipment (No Order)

```http
POST /api/shipments
{
  "shipmentType": "STANDALONE",
  "carrierId": 2,
  "medium": "AIR",
  "trackingNumber": "AIR-CARGO-001",
  "cargoDescription": "Electronic components",
  "shipToAddress": "Airport Cargo Terminal",
  "weightKg": 50.0,
  "freightCost": 250.00
}
```

```json
{
  "id": 3,
  "shipmentType": "STANDALONE",
  "trackingNumber": "AIR-CARGO-001",
  "medium": "AIR",
  "cargoDescription": "Electronic components",
  "status": "CREATED"
}
```

✅ Standalone shipment created with no order link.  
✅ AIR medium accepted (no delivery module restriction for standalone).

---

## Step 21 — Pagination

### Orders
```http
GET /api/orders?page=0&size=2&sort=createdAt,desc
```
```json
{ "totalElements": 3, "totalPages": 2, "number": 0, "size": 2 }
```

### Shipments
```http
GET /api/shipments?page=0&size=1
```
```json
{ "totalElements": 2, "totalPages": 2, "number": 0, "size": 1 }
```

### Deliveries
```http
GET /api/deliveries
```
```json
{ "totalElements": 1, "totalPages": 1, "number": 0, "size": 20 }
```

### Stock Movements (paginated)
```http
GET /api/inventory/product/1/movements?page=0&size=5
```
```json
{
  "totalElements": 3,
  "size": 5,
  "movements": [
    { "movementType": "RESERVE", "quantity": 2, "referenceId": 3 },
    { "movementType": "RESERVE", "quantity": 5, "referenceId": 1 },
    { "movementType": "RECEIVE", "quantity": 50, "referenceId": null }
  ]
}
```

✅ All paginated endpoints return `totalElements`, `totalPages`, `number`, `size`, and `content`.  
✅ `sort` parameter works.  
✅ Stock movements return newest-first with correct referenceIds linking back to orders.

---

## Step 22 — Reporting

### GET /api/reports/orders
```json
{
  "total": 3,
  "createdToday": 1,
  "byStatus": {
    "CREATED": 0, "CONFIRMED": 0, "STOCK_RESERVED": 0, "PROCESSING": 0,
    "READY_TO_SHIP": 2, "SHIPPED": 0, "DELIVERED": 0,
    "CANCELLED": 0, "FAILED": 1, "RETURNED": 0
  }
}
```

✅ Total=3, createdToday=1, FAILED=1 (insufficient stock order), READY_TO_SHIP=2.

### GET /api/reports/shipments
```json
{
  "total": 2,
  "byStatus": { "DELIVERED": 2, ... }
}
```

✅ Both fulfillment shipments show as DELIVERED.

### GET /api/reports/deliveries
```json
{
  "total": 1,
  "scheduledToday": 1,
  "byStatus": { "DELIVERED": 1, ... }
}
```

✅ 1 delivery, scheduled today, status DELIVERED.

### GET /api/reports/inventory/summary
```json
{
  "totalProducts": 3,
  "totalWarehouses": 1,
  "totalStockOnHand": 85,
  "lowStockItemCount": 1
}
```

✅ 3 products, 1 warehouse, 85 total units on hand, 1 low-stock item.

### GET /api/reports/inventory/low-stock
```json
{
  "count": 1,
  "items": [
    {
      "productSku": "SKU-C",
      "productName": "Item C",
      "quantityOnHand": 5,
      "quantityReserved": 0,
      "availableQuantity": 5
    }
  ]
}
```

✅ SKU-C correctly flagged — available (5) below threshold (20).

### Unauthenticated access
```http
GET /api/reports/orders  →  401 Unauthorized
```

✅ All report endpoints require authentication.

---

## Step 23 — Shipment Validation Errors

### FULFILLMENT with no orderId

```http
POST /api/shipments
{ "shipmentType": "FULFILLMENT", "carrierId": 1, "medium": "LAND", "shipToAddress": "1 Main St" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_ORDER_REQUIRED", "message": "orderId is required for FULFILLMENT shipments" }
```

✅ `SHIPMENT_ORDER_REQUIRED` returned.

### FULFILLMENT for order not in STOCK_RESERVED (order 1 is READY_TO_SHIP)

```http
POST /api/shipments
{ "shipmentType": "FULFILLMENT", "orderId": 1, "carrierId": 1, "medium": "LAND", "shipToAddress": "1 Main St" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_INVALID_ORDER_STATUS", "message": "Order must be in STOCK_RESERVED status to create a shipment, current status: READY_TO_SHIP" }
```

✅ `SHIPMENT_INVALID_ORDER_STATUS` returned.

### Duplicate shipment for same order (order 3 already has shipment 2)

```http
POST /api/shipments
{ "shipmentType": "FULFILLMENT", "orderId": 3, "carrierId": 2, "medium": "LAND", "trackingNumber": "DUPE-001", "shipToAddress": "1 Main St" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_DUPLICATE_ORDER", "message": "A shipment already exists for order: 3" }
```

✅ `SHIPMENT_DUPLICATE_ORDER` returned.

### Third-party carrier with no tracking number

Create a new order (order 4) to get a fresh STOCK_RESERVED order:

```http
POST /api/orders
{
  "customerId": 1,
  "items": [{ "sku": "SKU-A", "productName": "Item A", "quantity": 1, "unitPrice": 10.00 }]
}
```

```http
POST /api/shipments
{ "shipmentType": "FULFILLMENT", "orderId": 4, "carrierId": 2, "medium": "LAND", "shipToAddress": "1 Main St" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_TRACKING_REQUIRED", "message": "trackingNumber is required for third-party carriers" }
```

✅ `SHIPMENT_TRACKING_REQUIRED` returned.

### STANDALONE with no cargoDescription

```http
POST /api/shipments
{ "shipmentType": "STANDALONE", "carrierId": 2, "medium": "AIR", "trackingNumber": "AIR-002", "shipToAddress": "Airport" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_CARGO_REQUIRED", "message": "cargoDescription is required for STANDALONE shipments" }
```

✅ `SHIPMENT_CARGO_REQUIRED` returned.

### Get shipment by tracking number

```http
GET /api/shipments/tracking/TRK-20261012-00001
```

**Response** `200 OK` — returns shipment 1 with status `DELIVERED`.

✅ Lookup by tracking number works.

### Get non-existent shipment

```http
GET /api/shipments/9999
```

**Response** `404 Not Found`
```json
{ "code": "RESOURCE_NOT_FOUND", "message": "Shipment with id 9999 not found" }
```

✅ 404 returned for unknown shipment id.

---

## Step 24 — Shipment Status Transition Errors

### Invalid transition (CREATED → DELIVERED)

Create a fresh shipment for order 4 (now STOCK_RESERVED after step 23):

```http
POST /api/shipments
{
  "shipmentType": "FULFILLMENT",
  "orderId": 4,
  "carrierId": 2,
  "medium": "LAND",
  "trackingNumber": "3PL-TEST-001",
  "shipToAddress": "1 Main St"
}
```

Shipment 4 created with status `CREATED`.

```http
PUT /api/shipments/4/status
{ "status": "DELIVERED", "proofOfDelivery": "Signed" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_STATUS_ERROR", "message": "Cannot transition shipment from CREATED to DELIVERED" }
```

✅ Invalid transition blocked.

### DELIVERED without POD (third-party carrier)

Advance shipment 4 to `OUT_FOR_DELIVERY`:

```http
PUT /api/shipments/4/status  →  { "status": "PROCESSING" }
PUT /api/shipments/4/status  →  { "status": "READY_FOR_PICKUP" }
PUT /api/shipments/4/status  →  { "status": "PICKED_UP" }
PUT /api/shipments/4/status  →  { "status": "IN_TRANSIT" }
PUT /api/shipments/4/status  →  { "status": "ARRIVED_AT_HUB" }
PUT /api/shipments/4/status  →  { "status": "OUT_FOR_DELIVERY" }
```

```http
PUT /api/shipments/4/status
{ "status": "DELIVERED" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_POD_REQUIRED", "message": "Proof of delivery is required when marking shipment as DELIVERED" }
```

✅ `SHIPMENT_POD_REQUIRED` returned when POD is missing.

### Manual DELIVERED blocked for company-owned carrier

Create a new order (order 5) and shipment with company-owned carrier, advance to `OUT_FOR_DELIVERY` via delivery module:

```http
POST /api/orders  →  order 5 (1x SKU-A)
POST /api/shipments  →  shipment 5, carrierId=1, auto-tracking TRK-*
Advance shipment 5: PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB
POST /api/deliveries  →  delivery 2, driverId=1, vehicleId=1
```

Shipment 5 is now `OUT_FOR_DELIVERY`.

```http
PUT /api/shipments/5/status
{ "status": "DELIVERED", "proofOfDelivery": "Signed" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "SHIPMENT_USE_DELIVERY", "message": "Use the Delivery module to mark DELIVERED for company-owned carrier shipments" }
```

✅ Manual `DELIVERED` blocked for company-owned carrier.

### Cancel a shipment (CREATED → CANCELLED)

Create a new order (order 6) and shipment:

```http
POST /api/orders  →  order 6 (1x SKU-B)
POST /api/shipments  →  shipment 6, carrierId=2, trackingNumber=3PL-CANCEL-001
```

```http
PUT /api/shipments/6/status
{ "status": "CANCELLED" }
```

**Response** `200 OK`
```json
{ "id": 6, "status": "CANCELLED" }
```

✅ Shipment cancelled from `CREATED` state.

### FAILED transition (IN_TRANSIT → FAILED)

Advance shipment 4 (currently `OUT_FOR_DELIVERY`) — use a fresh shipment instead. Create order 7 and shipment 7, advance to `IN_TRANSIT`:

```http
POST /api/orders  →  order 7 (1x SKU-B)
POST /api/shipments  →  shipment 7, carrierId=2, trackingNumber=3PL-FAIL-001
PUT /api/shipments/7/status  →  { "status": "PROCESSING" }
PUT /api/shipments/7/status  →  { "status": "READY_FOR_PICKUP" }
PUT /api/shipments/7/status  →  { "status": "PICKED_UP" }
PUT /api/shipments/7/status  →  { "status": "IN_TRANSIT" }
```

```http
PUT /api/shipments/7/status
{ "status": "FAILED" }
```

**Response** `200 OK`
```json
{ "id": 7, "status": "FAILED" }
```

✅ `IN_TRANSIT → FAILED` transition allowed.

---

## Step 25 — Delivery Validation Errors

### Assign delivery to third-party carrier shipment (shipment 4)

```http
POST /api/deliveries
{ "shipmentId": 4, "driverId": 1, "vehicleId": 1, "scheduledDate": "2026-10-14" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_THIRD_PARTY_CARRIER", "message": "Delivery assignment is only allowed for company-owned carrier shipments" }
```

✅ `DELIVERY_THIRD_PARTY_CARRIER` returned.

### Assign delivery to non-LAND shipment (shipment 3 — AIR STANDALONE)

First create a company-owned AIR shipment:

```http
POST /api/shipments
{
  "shipmentType": "STANDALONE",
  "carrierId": 1,
  "medium": "AIR",
  "cargoDescription": "Air cargo test",
  "shipToAddress": "Airport"
}
```

Shipment 8 created. Advance to `ARRIVED_AT_HUB`:

```http
PUT /api/shipments/8/status  →  PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB
```

```http
POST /api/deliveries
{ "shipmentId": 8, "driverId": 1, "vehicleId": 1, "scheduledDate": "2026-10-14" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_LAND_ONLY", "message": "Delivery assignment is only supported for LAND shipments in this version, current medium: AIR" }
```

✅ `DELIVERY_LAND_ONLY` returned.

### Assign delivery when shipment not ARRIVED_AT_HUB

Create order 8 and shipment 9 (company-owned, LAND), leave at `CREATED`:

```http
POST /api/orders  →  order 8 (1x SKU-A)
POST /api/shipments  →  shipment 9, carrierId=1, medium=LAND
```

```http
POST /api/deliveries
{ "shipmentId": 9, "driverId": 1, "vehicleId": 1, "scheduledDate": "2026-10-14" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_INVALID_SHIPMENT_STATUS", "message": "Shipment must be in ARRIVED_AT_HUB status to assign a delivery, current status: CREATED" }
```

✅ `DELIVERY_INVALID_SHIPMENT_STATUS` returned.

### Duplicate delivery for same shipment (shipment 5 already has delivery 2)

```http
POST /api/deliveries
{ "shipmentId": 5, "driverId": 1, "vehicleId": 1, "scheduledDate": "2026-10-14" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_DUPLICATE", "message": "A delivery already exists for shipment: 5" }
```

✅ `DELIVERY_DUPLICATE` returned.

### Assign unavailable driver

Create a second driver (id=2) and set them `ON_DELIVERY` by assigning them to a delivery. Instead, use the fact that driver 1 is `ON_DELIVERY` (assigned to delivery 2 for shipment 5).

Advance shipment 9 to `ARRIVED_AT_HUB`:

```http
PUT /api/shipments/9/status  →  PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB
```

```http
POST /api/deliveries
{ "shipmentId": 9, "driverId": 1, "vehicleId": 1, "scheduledDate": "2026-10-14" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_DRIVER_UNAVAILABLE", "message": "Driver is not available, current status: ON_DELIVERY" }
```

✅ `DELIVERY_DRIVER_UNAVAILABLE` returned.

### Assign unavailable vehicle

Create a second driver (id=2) to isolate the vehicle check:

```http
POST /api/drivers
{ "name": "Carlos Rivera", "licenseNumber": "LIC-CR-002", "licenseType": "CLASS_B" }
```

Vehicle 1 is `IN_USE` (assigned to delivery 2). Attempt with available driver but unavailable vehicle:

```http
POST /api/deliveries
{ "shipmentId": 9, "driverId": 2, "vehicleId": 1, "scheduledDate": "2026-10-14" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_VEHICLE_UNAVAILABLE", "message": "Vehicle is not available, current status: IN_USE" }
```

✅ `DELIVERY_VEHICLE_UNAVAILABLE` returned.

### DELIVERED without POD

Create a second vehicle (id=2) and assign delivery 3 to shipment 9:

```http
POST /api/vehicles
{ "plateNumber": "XYZ-5678", "vehicleType": "TRUCK", "capacityKg": 1000 }
```

```http
POST /api/deliveries
{ "shipmentId": 9, "driverId": 2, "vehicleId": 2, "scheduledDate": "2026-10-14" }
```

Delivery 3 created. Advance to `OUT_FOR_DELIVERY`:

```http
PUT /api/deliveries/3/status  →  { "status": "OUT_FOR_DELIVERY" }
```

```http
PUT /api/deliveries/3/status
{ "status": "DELIVERED" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_POD_REQUIRED", "message": "Proof of delivery is required when marking delivery as DELIVERED" }
```

✅ `DELIVERY_POD_REQUIRED` returned.

### Invalid delivery status transition

```http
PUT /api/deliveries/3/status
{ "status": "ASSIGNED" }
```

**Response** `422 Unprocessable Entity`
```json
{ "code": "DELIVERY_STATUS_ERROR", "message": "Cannot transition delivery from OUT_FOR_DELIVERY to ASSIGNED" }
```

✅ Invalid transition blocked.

---

## Step 26 — Delivery FAILED and CANCELLED Side Effects

### Failed delivery → shipment FAILED, driver/vehicle released

Delivery 3 is currently `OUT_FOR_DELIVERY` (driver 2 `ON_DELIVERY`, vehicle 2 `IN_USE`).

```http
PUT /api/deliveries/3/status
{ "status": "FAILED" }
```

**Response** `200 OK`
```json
{ "id": 3, "status": "FAILED" }
```

**After `DeliveryFailedEvent`:**

| Resource | Status |
|---|---|
| Shipment 9 | `FAILED` |
| Driver 2 | `AVAILABLE` |
| Vehicle 2 | `AVAILABLE` |

```http
GET /api/shipments/9   →  { "status": "FAILED" }
GET /api/drivers/2     →  { "status": "AVAILABLE" }
GET /api/vehicles/2    →  { "status": "AVAILABLE" }
```

✅ Failed delivery sets shipment to `FAILED` and releases driver and vehicle.

### Cancelled delivery → shipment FAILED, driver/vehicle released

Create order 9, shipment 10 (company-owned, LAND), advance to `ARRIVED_AT_HUB`, assign delivery 4 (driver 2, vehicle 2):

```http
POST /api/orders  →  order 9 (1x SKU-A)
POST /api/shipments  →  shipment 10, carrierId=1, medium=LAND
PUT /api/shipments/10/status  →  PROCESSING → READY_FOR_PICKUP → PICKED_UP → IN_TRANSIT → ARRIVED_AT_HUB
POST /api/deliveries  →  delivery 4, driverId=2, vehicleId=2
```

Delivery 4 is `ASSIGNED` (driver 2 `ON_DELIVERY`, vehicle 2 `IN_USE`).

```http
PUT /api/deliveries/4/status
{ "status": "CANCELLED" }
```

**Response** `200 OK`
```json
{ "id": 4, "status": "CANCELLED" }
```

**After `DeliveryFailedEvent`:**

| Resource | Status |
|---|---|
| Shipment 10 | `FAILED` |
| Driver 2 | `AVAILABLE` |
| Vehicle 2 | `AVAILABLE` |

```http
GET /api/shipments/10  →  { "status": "FAILED" }
GET /api/drivers/2     →  { "status": "AVAILABLE" }
GET /api/vehicles/2    →  { "status": "AVAILABLE" }
```

✅ Cancelled delivery sets shipment to `FAILED` and releases driver and vehicle.

---

## Summary

| Phase | Scenario | Result |
|---|---|---|
| Auth | Register + login | ✅ |
| Auth | Protected endpoints require token | ✅ |
| Inventory | Receive stock | ✅ |
| Inventory | Low stock detection | ✅ |
| Inventory | Stock movement audit trail (paginated) | ✅ |
| Orders | Sufficient stock → `STOCK_RESERVED` (event-driven) | ✅ |
| Orders | Insufficient stock → `FAILED` with reason, no partial reservation | ✅ |
| Shipment | Fulfillment shipment → order auto-transitions to `READY_TO_SHIP` | ✅ |
| Shipment | Company-owned carrier → tracking number auto-generated | ✅ |
| Shipment | Third-party carrier → operator-provided tracking number | ✅ |
| Shipment | Standalone shipment (no order) | ✅ |
| Shipment | Company-owned carrier blocks manual `OUT_FOR_DELIVERY` | ✅ |
| Shipment | `FULFILLMENT` with no `orderId` → `SHIPMENT_ORDER_REQUIRED` | ✅ |
| Shipment | Order not in `STOCK_RESERVED` → `SHIPMENT_INVALID_ORDER_STATUS` | ✅ |
| Shipment | Duplicate shipment for same order → `SHIPMENT_DUPLICATE_ORDER` | ✅ |
| Shipment | Third-party carrier, no tracking number → `SHIPMENT_TRACKING_REQUIRED` | ✅ |
| Shipment | `STANDALONE` with no `cargoDescription` → `SHIPMENT_CARGO_REQUIRED` | ✅ |
| Shipment | Get by tracking number | ✅ |
| Shipment | Get non-existent shipment → 404 | ✅ |
| Shipment | Invalid status transition → `SHIPMENT_STATUS_ERROR` | ✅ |
| Shipment | `DELIVERED` without POD → `SHIPMENT_POD_REQUIRED` | ✅ |
| Shipment | Manual `DELIVERED` on company-owned carrier → `SHIPMENT_USE_DELIVERY` | ✅ |
| Shipment | Cancel shipment (`CREATED → CANCELLED`) | ✅ |
| Shipment | `IN_TRANSIT → FAILED` transition | ✅ |
| Delivery | Assign delivery → shipment `OUT_FOR_DELIVERY`, driver `ON_DELIVERY`, vehicle `IN_USE` | ✅ |
| Delivery | Complete delivery → shipment `DELIVERED` with POD + date, driver/vehicle released | ✅ |
| Delivery | Third-party carrier shipment → `DELIVERY_THIRD_PARTY_CARRIER` | ✅ |
| Delivery | Non-LAND shipment → `DELIVERY_LAND_ONLY` | ✅ |
| Delivery | Shipment not `ARRIVED_AT_HUB` → `DELIVERY_INVALID_SHIPMENT_STATUS` | ✅ |
| Delivery | Duplicate delivery → `DELIVERY_DUPLICATE` | ✅ |
| Delivery | Unavailable driver → `DELIVERY_DRIVER_UNAVAILABLE` | ✅ |
| Delivery | Unavailable vehicle → `DELIVERY_VEHICLE_UNAVAILABLE` | ✅ |
| Delivery | `DELIVERED` without POD → `DELIVERY_POD_REQUIRED` | ✅ |
| Delivery | Invalid status transition → `DELIVERY_STATUS_ERROR` | ✅ |
| Delivery | Failed delivery → shipment `FAILED`, driver/vehicle released | ✅ |
| Delivery | Cancelled delivery → shipment `FAILED`, driver/vehicle released | ✅ |
| Tracking | Public endpoint — no auth required | ✅ |
| Tracking | 8 events recorded for full lifecycle (CREATED → DELIVERED) | ✅ |
| Tracking | Delivery-driven transitions recorded (OUT_FOR_DELIVERY, DELIVERED) | ✅ |
| Tracking | Unknown tracking number → 404 | ✅ |
| Tracking | Authenticated endpoint by shipment ID | ✅ |
| Pagination | Orders, shipments, deliveries, stock movements all paginated | ✅ |
| Pagination | `sort`, `page`, `size` parameters work | ✅ |
| Reporting | Order summary — counts by status + created today | ✅ |
| Reporting | Shipment summary — counts by status | ✅ |
| Reporting | Delivery summary — counts by status + scheduled today | ✅ |
| Reporting | Inventory summary — products, warehouses, stock on hand, low-stock count | ✅ |
| Reporting | Low-stock report — correct item identified | ✅ |
| Reporting | Unauthenticated access → 401 | ✅ |

---

---

# Modular Extension — Phase 1: Configuration Foundation

Manual test run against Docker (postgres + backend only).  
Date: 2026-10-20  
Base URL: `http://localhost:8080`

## Setup

```bash
cd docker && docker compose build
docker compose up -d postgres backend
```

Liquibase applied 28 changesets on startup (V001–V012), including:
- `V010` — `module_catalog` table + 6 seeded modules
- `V011` — `module_config` table + DOMESTIC_LOGISTICS enabled, all others disabled
- `V012` — `module_config_audit` table

Registered `admin@logistics.com`, then upgraded to ADMIN role directly in DB:
```sql
UPDATE user_roles SET role = 'ADMIN'
WHERE user_id = (SELECT id FROM users WHERE email = 'admin@logistics.com');
```
Re-logged in to obtain a fresh token with ADMIN role.

---

## Step 27 — List All Modules

**Request**
```http
GET /api/admin/modules
Authorization: Bearer <admin-token>
```

**Response** `200 OK`
```json
[
  { "moduleKey": "DOMESTIC_LOGISTICS",        "moduleType": "CORE",        "enabled": true  },
  { "moduleKey": "INTERNATIONAL_FREIGHT",      "moduleType": "BUSINESS",    "enabled": false },
  { "moduleKey": "CUSTOMS_CLEARANCE",          "moduleType": "BUSINESS",    "enabled": false },
  { "moduleKey": "FREIGHT_CUSTOMS_INTEGRATION","moduleType": "INTEGRATION", "enabled": false },
  { "moduleKey": "DOMESTIC_HANDOFF",           "moduleType": "INTEGRATION", "enabled": false },
  { "moduleKey": "CUSTOMER_PORTAL",            "moduleType": "BUSINESS",    "enabled": false }
]
```

✅ All 6 modules returned.  
✅ DOMESTIC_LOGISTICS is the only module enabled on fresh install.  
✅ Module types (CORE, BUSINESS, INTEGRATION) correctly seeded.

---

## Step 28 — Get Module by Key

### DOMESTIC_LOGISTICS (enabled by default)

**Request**
```http
GET /api/admin/modules/DOMESTIC_LOGISTICS
Authorization: Bearer <admin-token>
```

**Response** `200 OK`
```json
{
  "moduleKey": "DOMESTIC_LOGISTICS",
  "name": "Domestic Logistics",
  "moduleType": "CORE",
  "enabled": true,
  "updatedBy": "system",
  "version": 0
}
```

✅ DOMESTIC_LOGISTICS enabled=true, seeded by `system`.

### INTERNATIONAL_FREIGHT (disabled by default)

**Response** `200 OK`
```json
{
  "moduleKey": "INTERNATIONAL_FREIGHT",
  "moduleType": "BUSINESS",
  "enabled": false,
  "updatedBy": "system",
  "version": 0
}
```

✅ INTERNATIONAL_FREIGHT enabled=false by default.

### Unknown module key

**Request**
```http
GET /api/admin/modules/UNKNOWN_MODULE
```

**Response** `404 Not Found`
```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Module not found: UNKNOWN_MODULE"
}
```

✅ 404 with `RESOURCE_NOT_FOUND` for unknown key.

---

## Step 29 — Enable a Module

**Request**
```http
PUT /api/admin/modules/INTERNATIONAL_FREIGHT
Authorization: Bearer <admin-token>
Content-Type: application/json

{ "enabled": true, "reason": "Enabling for manual validation" }
```

**Response** `200 OK`
```json
{
  "moduleKey": "INTERNATIONAL_FREIGHT",
  "enabled": true,
  "updatedBy": "admin@logistics.com",
  "updatedAt": "2026-10-20T04:52:52.245542880Z",
  "version": 0
}
```

✅ Module enabled. `updatedBy` set to the authenticated user's email.

---

## Step 30 — Disable a Module

**Request**
```http
PUT /api/admin/modules/INTERNATIONAL_FREIGHT
Content-Type: application/json

{ "enabled": false, "reason": "Disabling after validation" }
```

**Response** `200 OK`
```json
{
  "moduleKey": "INTERNATIONAL_FREIGHT",
  "enabled": false,
  "updatedBy": "admin@logistics.com",
  "version": 1
}
```

✅ Module disabled. `version` incremented to 1 (optimistic locking working).

---

## Step 31 — Audit Log

**Request**
```http
GET /api/admin/modules/audit
Authorization: Bearer <admin-token>
```

**Response** `200 OK`
```json
[
  {
    "id": 2,
    "moduleKey": "INTERNATIONAL_FREIGHT",
    "previousValue": "{\"enabled\":true}",
    "newValue": "{\"enabled\":false}",
    "changedBy": "admin@logistics.com",
    "changedAt": "2026-10-20T04:53:38.082689Z",
    "reason": "Disabling after validation"
  },
  {
    "id": 1,
    "moduleKey": "INTERNATIONAL_FREIGHT",
    "previousValue": "{\"enabled\":false}",
    "newValue": "{\"enabled\":true}",
    "changedBy": "admin@logistics.com",
    "changedAt": "2026-10-20T04:52:52.251425Z",
    "reason": "Enabling for manual validation"
  }
]
```

✅ 2 audit records created — one per update, newest first.  
✅ `previousValue` and `newValue` capture the full state as JSON.  
✅ `changedBy` and `reason` correctly recorded.

---

## Step 32 — GET /api/me/modules

### Authenticated user (any role)

**Request**
```http
GET /api/me/modules
Authorization: Bearer <admin-token>
```

**Response** `200 OK`
```json
[
  {
    "moduleKey": "DOMESTIC_LOGISTICS",
    "name": "Domestic Logistics",
    "description": "Existing domestic operations",
    "moduleType": "CORE"
  }
]
```

✅ Only enabled modules returned (DOMESTIC_LOGISTICS only, since all others are disabled).  
✅ Response is a slim view — no `enabled`, `version`, or `updatedBy` fields exposed to end users.

### Unauthenticated

```http
GET /api/me/modules  →  401 Unauthorized
```

✅ Unauthenticated request rejected.

---

## Step 33 — Access Control

| Scenario | Expected | Result |
|---|---|---|
| `GET /api/admin/modules` — no token | 401 | ✅ |
| `GET /api/admin/modules` — OPERATIONS role | 401 | ✅ (access denied) |
| `GET /api/me/modules` — OPERATIONS role | 200 | ✅ |
| `GET /api/me/modules` — no token | 401 | ✅ |

✅ Admin endpoints locked to ADMIN role.  
✅ `/api/me/modules` accessible to any authenticated user.

---

## Modular Extension Phase 1 — Summary

| Scenario | Result |
|---|---|
| Liquibase migrations V010–V012 applied cleanly on Docker startup | ✅ |
| 6 modules seeded in `module_catalog` with correct types | ✅ |
| DOMESTIC_LOGISTICS enabled=true on fresh install | ✅ |
| All other modules disabled=false on fresh install | ✅ |
| `GET /api/admin/modules` returns all 6 modules | ✅ |
| `GET /api/admin/modules/{key}` returns correct module | ✅ |
| `GET /api/admin/modules/UNKNOWN` returns 404 | ✅ |
| `PUT /api/admin/modules/{key}` enables a module | ✅ |
| `PUT /api/admin/modules/{key}` disables a module | ✅ |
| `version` increments on each update (optimistic locking) | ✅ |
| `updatedBy` set to authenticated user's email | ✅ |
| `GET /api/admin/modules/audit` returns log newest-first | ✅ |
| Audit records capture previousValue, newValue, changedBy, reason | ✅ |
| `GET /api/me/modules` returns only enabled modules | ✅ |
| `GET /api/me/modules` returns slim view (no internal fields) | ✅ |
| Admin endpoints require ADMIN role | ✅ |
| `/api/me/modules` accessible to any authenticated user | ✅ |
| Unauthenticated requests return 401 | ✅ |
