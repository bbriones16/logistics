# LogiTrack — Frontend

React 19 + TypeScript + Vite frontend for the Logistics Management System.

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| React | 19 | UI framework |
| TypeScript | 6 | Type safety |
| Vite | 5 | Dev server & bundler |
| React Router | 7 | Client-side routing |
| TanStack Query | 5 | Server state & caching |
| Axios | 1 | HTTP client |
| Tailwind CSS | 4 | Styling |
| Radix UI | — | Accessible UI primitives |
| React Hook Form | 7 | Form state management |
| Zod | 3 | Schema validation |
| Lucide React | — | Icons |

---

## Running locally

### Option A — Dev server (without Docker)

From the `frontend/` directory:

```bash
npm install
npm run dev
```

Frontend available at `http://localhost:5173`.  
API calls are proxied to `http://localhost:8080` via the Vite dev proxy.

The backend must be running before using the app. See the root `README.md` for backend setup.

### Option B — Docker (full stack)

From the `docker/` directory:

```bash
# Build all images
docker compose build

# Start all services
docker compose up -d
```

| Container | Port | Description |
|---|---|---|
| logistics-postgres | 5432 | PostgreSQL 16 |
| logistics-backend | 8080 | Spring Boot API |
| logistics-frontend | 80 | React app served by Nginx |

Frontend available at `http://localhost`.  
Nginx proxies `/api` requests from the frontend to the backend automatically.

```bash
# Stop all services
docker compose down

# Stop and remove the database volume
docker compose down -v
```

---

## Scripts

| Script | Description |
|---|---|
| `npm run dev` | Start Vite dev server with HMR |
| `npm run build` | Type-check and build for production |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | Run Oxlint |

---

## Project structure

```
src/
├── api/            # Axios API modules (one per domain)
├── components/     # Shared layout and UI components
│   └── ui/         # Primitive components (Button, Card, Badge, etc.)
├── context/        # AuthContext — JWT token state
├── lib/            # Utility functions (cn)
├── pages/          # Route-level page components
│   ├── auth/
│   ├── dashboard/
│   ├── delivery/
│   ├── inventory/
│   ├── orders/
│   ├── reporting/
│   ├── shipments/
│   └── tracking/
└── types/          # Shared TypeScript interfaces and enums
```

---

## Pages and routes

| Route | Page | Auth required |
|---|---|---|
| `/login` | Login | No |
| `/track` | Public shipment tracker | No |
| `/` | Dashboard | Yes |
| `/orders` | Orders list | Yes |
| `/orders/:id` | Order detail | Yes |
| `/customers` | Customers list | Yes |
| `/inventory` | Inventory (stock levels) | Yes |
| `/shipments` | Shipments list | Yes |
| `/shipments/:id` | Shipment detail + tracking events | Yes |
| `/carriers` | Carriers list | Yes |
| `/delivery` | Deliveries list | Yes |
| `/drivers` | Drivers list | Yes |
| `/vehicles` | Vehicles list | Yes |

---

## Authentication

JWT token is stored in `localStorage`. The Axios client automatically attaches it as a `Bearer` token on every request. A 401 response clears the token and redirects to `/login`.

The `/login` and `/track` routes are public. All other routes are wrapped in `ProtectedRoute`, which redirects unauthenticated users to `/login`.

---

## API modules

Each file in `src/api/` maps to a backend domain:

| File | Backend prefix |
|---|---|
| `auth.ts` | `/api/auth` |
| `orders.ts` | `/api/orders`, `/api/customers` |
| `inventory.ts` | `/api/inventory`, `/api/products`, `/api/warehouses` |
| `shipments.ts` | `/api/shipments`, `/api/carriers` |
| `delivery.ts` | `/api/deliveries`, `/api/drivers`, `/api/vehicles` |
| `reporting.ts` | `/api/reports` |

---

## Dashboard

The dashboard aggregates live data from all four reporting endpoints:

- Order count + created today + breakdown by status
- Shipment count + in-transit count
- Delivery count + scheduled today
- Inventory summary (total products, warehouses, stock on hand, low-stock count)
- Low-stock items alert table
