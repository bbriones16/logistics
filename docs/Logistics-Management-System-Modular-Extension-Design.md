# Logistics Management System — Modular Extension Design

## 1. Objective

Extend the existing Logistics Management System (LMS) by introducing configurable modules for:

1. Feature Toggle and Module Configuration
2. International Freight Forwarding
3. Customs Clearance

The existing Domestic Logistics module remains intact and operational.

The system must support clients that use domestic logistics, international freight forwarding, customs clearance, or any permitted combination of these modules.

### Design principles

- Backward compatibility with existing domestic logistics functionality.
- Independent activation and deactivation of each business module.
- Shared platform services where appropriate.
- Role-based authorization per module.
- Optional integration between freight, customs and domestic delivery.
- Incremental implementation without requiring a microservices architecture.
- Auditability of configuration changes and shipment operations.

---

## Confirmed Design Decisions

The following decisions were confirmed before implementation began:

| Decision | Choice | Notes |
|---|---|---|
| Deployment model | Single-tenant | One deployment per client. No multi-tenant architecture needed. |
| New roles | `FREIGHT_OPERATOR`, `CUSTOMS_OFFICER`, `FINANCE_USER` | Added to existing `ADMIN`, `OPERATIONS`, `DISPATCHER`, `WAREHOUSE_USER` roles. |
| Document storage (local dev) | MinIO | Runs as a Docker container alongside the existing stack. S3-compatible API — same SDK and code as production. |
| Document storage (production) | AWS S3 | Different bucket, same code. Configured via environment variables. |
| Notifications | Deferred to Phase 4 | Not in scope for Phases 1–3. |
| Implementation approach | Backend-first per phase | Complete and test backend before building frontend for each phase. |
| Customs filing | Manual tracking only (MVP) | No direct electronic customs declaration filing in initial scope. |
| Module enablement control | ADMIN role | Platform administrator controls module catalog and configuration. |

## 2. Scope

### In scope

- Deployment-level module configuration (single-tenant).
- Feature entitlements and runtime authorization.
- Freight quotation and booking management.
- Import and export shipment management.
- Shipment transport legs and milestones.
- Customs clearance job management.
- Customs documentation and status tracking.
- Optional links between freight shipments and clearance jobs.
- Optional handoff from freight/customs to domestic delivery.
- Audit logs, permissions and operational reporting.

### Out of scope for the initial MVP

- Direct electronic customs declaration filing.
- Automatic tariff classification.
- Automatic calculation of legally payable duties and taxes.
- Full accounting or ERP replacement.
- Live integrations with every shipping carrier.
- Complex international trade compliance automation.

These can be introduced later based on client requirements, jurisdiction, available APIs and regulatory authorization.

## 3. High-Level Architecture

```text
                    LMS PLATFORM
                         |
          +--------------+--------------+
          |                             |
   Shared Platform Services      Module Configuration
          |                             |
          |                    +--------+---------+
          |                    |        |         |
          |                 Domestic  Freight   Customs
          |                 Logistics Forwarding Clearance
          |                    |        |         |
          +--------------------+--------+---------+
                               |
                    Optional Integration Layer
                               |
                   Freight <-> Customs
                               |
                    Freight/Customs -> Domestic
                               |
                      External Integrations
                 Carriers / Customs / Notifications
```

### Shared platform services

Reuse existing capabilities where they already exist:

- Authentication and user management.
- Tenant/customer management.
- Roles and permissions.
- Document storage.
- Notifications.
- Audit logging.
- Common reference data.
- Existing domestic delivery services.

Avoid duplicating existing customer, user or notification functionality inside the new modules.

## 4. Feature Toggle and Module Configuration

### 4.1 Purpose

The configuration module determines which business capabilities are available for this deployment.

Since the system is single-tenant (one deployment per client), there is no tenant scoping on module configuration. Each deployment has its own module configuration managed by the ADMIN role.

A module being disabled must mean that its functionality is unavailable through both the user interface and the backend APIs.

Feature toggles must not replace authorization. A module can be enabled while individual users still lack permission to access it.

### 4.2 Initial module catalog

| Module key | Description | Initial status |
|---|---|---|
| DOMESTIC_LOGISTICS | Existing domestic operations | Existing |
| INTERNATIONAL_FREIGHT | Freight forwarding and shipment management | New |
| CUSTOMS_CLEARANCE | Customs clearance jobs and documents | New |
| FREIGHT_CUSTOMS_INTEGRATION | Link freight shipments and clearance jobs | New |
| DOMESTIC_HANDOFF | Create domestic delivery jobs from international shipments | New |
| CUSTOMER_PORTAL | Customer-facing tracking and documents | Existing or configurable |

Module keys should be stable identifiers, not display names. Display names and descriptions should be maintained separately.

The integration toggles should only be effective when the required modules are enabled.

For example:

```text
INTERNATIONAL_FREIGHT = ON
CUSTOMS_CLEARANCE = ON
FREIGHT_CUSTOMS_INTEGRATION = ON
DOMESTIC_LOGISTICS = OFF
DOMESTIC_HANDOFF = OFF
```

This configuration permits freight and customs workflows without requiring domestic logistics.

### 4.3 Recommended data model

#### `module_catalog`

| Field | Purpose |
|---|---|
| id | Primary key |
| module_key | Unique stable identifier |
| name | Display name |
| description | Module description |
| module_type | CORE, BUSINESS or INTEGRATION |
| active | Whether the module is available in the product |
| created_at | Creation timestamp |
| updated_at | Last modification timestamp |

#### `module_config`

Since the system is single-tenant, `tenant_id` is not needed. Each deployment has one set of module configurations.

| Field | Purpose |
|---|---|
| id | Primary key |
| module_id | Reference to module catalog |
| enabled | Whether the module is enabled for this deployment |
| configuration_json | Optional module-specific settings |
| effective_from | Optional activation date |
| effective_until | Optional expiration date |
| updated_by | User who last changed the configuration |
| updated_at | Last modification timestamp |
| version | Configuration version for concurrency control |

Add a unique constraint on `module_id`.

#### `module_config_audit`

| Field | Purpose |
|---|---|
| id | Primary key |
| module_key | Affected module |
| previous_value | Previous enabled state/configuration |
| new_value | New enabled state/configuration |
| changed_by | Actor |
| changed_at | Timestamp |
| reason | Optional change reason |

Store audit records as immutable events. Do not overwrite historical configuration changes.

### 4.4 Configuration hierarchy

Use the following logical checks:

1. The module exists and is globally available.
2. The module is enabled for this deployment.
3. The user is authorized for the requested operation.
4. Any module dependencies are satisfied.
5. The requested action is permitted by the module's current state.

Conceptually:

```java
boolean canAccessModule(
        String moduleKey,
        User user) {

    return moduleCatalog.isAvailable(moduleKey)
        && moduleConfig.isEnabled(moduleKey)
        && authorizationService.hasModuleAccess(user, moduleKey);
}
```

This is illustrative Java pseudocode. Adapt it to the existing JWT + Spring Security authorization framework.

### 4.5 API endpoints

Since the system is single-tenant, there is no `tenantId` path parameter.

```http
GET    /api/admin/modules
GET    /api/admin/modules/{moduleKey}
PUT    /api/admin/modules/{moduleKey}
GET    /api/admin/modules/audit
GET    /api/me/modules
```

`GET /api/me/modules` returns the enabled modules the current user is allowed to access. Use it to construct the frontend navigation.

All `/api/admin/modules` endpoints require the `ADMIN` role.

### 4.6 Toggle behavior

When a module is disabled:

- Remove its navigation items.
- Reject its API operations.
- Hide its module-specific actions.
- Prevent new business transactions in that module.
- Preserve historical records and documents.
- Continue to expose shared records only where authorized.
- Record the configuration change.

Do not delete records when disabling a module.

Define a separate policy for in-progress jobs. The recommended default is to preserve access to authorized read-only views of existing jobs while blocking new jobs and ordinary modifications. Allow explicit, audited administrative exceptions where operational continuity requires them.

## 5. International Freight Forwarding Module

### 5.1 Objective

Manage international freight forwarding from quotation and booking through transport, shipment tracking and handoff to the destination operation.

### 5.2 Functional components

#### A. Freight quotation

Capture:

- Customer.
- Shipment direction: import, export or cross-trade.
- Origin and destination.
- Transport mode: air, sea or land.
- Cargo description.
- Packages, weight and dimensions.
- Chargeable weight or volume where applicable.
- Freight charges and surcharges.
- Currency and quotation validity.
- Terms, exclusions and notes.

Quotation lifecycle:

```text
DRAFT -> SENT -> ACCEPTED
                 |
                 +-> REJECTED
                 |
                 +-> EXPIRED
```

Only an accepted quotation should be eligible for conversion into a confirmed booking, subject to the client's workflow.

#### B. Booking and shipment job

Generate a unique shipment reference.

Capture:

- Customer and relevant contacts.
- Shipper and consignee.
- Origin and destination locations.
- Transport mode.
- Incoterms and named place, where applicable.
- Cargo and package details.
- Carrier and service provider.
- Booking reference.
- Estimated departure and arrival.
- Related quotation.

Recommended shipment statuses:

```text
BOOKED
  -> CARGO_RECEIVED
  -> DEPARTED
  -> IN_TRANSIT
  -> ARRIVED
  -> DELIVERED_OR_COLLECTED
  -> COMPLETED
```

Use milestone-based tracking rather than assuming every shipment follows the same linear lifecycle. Exceptions, cancellations, customs holds and partial shipments should be modeled separately.

#### C. Transport legs

A shipment may contain multiple legs, such as a truck movement to a port, an ocean voyage and a destination truck movement.

Each transport leg should record:

- Sequence number.
- Mode of transport.
- Origin and destination.
- Carrier.
- Vessel, voyage, flight or vehicle reference where applicable.
- Departure and arrival estimates and actuals.
- Booking or tracking references.
- Leg status.

#### D. Cargo and container management

Support:

- Multiple cargo items per shipment.
- Package counts and packaging types.
- Gross and net weight where applicable.
- Dimensions and volume.
- Container type, container number and seal number.
- Container-to-shipment associations.
- Partial loads and multiple containers where needed.

Avoid assuming that every shipment is containerized. Air freight and consolidated cargo require different data.

#### E. Shipment milestones

Provide a configurable milestone history, including:

- Booking confirmed.
- Cargo received.
- Export clearance completed.
- Departed origin.
- Arrived at transshipment location.
- Arrived at destination.
- Import clearance released.
- Available for collection or delivery.
- Final delivery or collection completed.

Each milestone should capture its timestamp, source, location, notes and actor or integration.

#### F. Freight charges

For the MVP, record estimated and actual charges:

- Freight.
- Origin handling.
- Destination handling.
- Fuel and security surcharges.
- Storage and other service charges.
- Third-party costs.

Keep charge currency, transaction currency and exchange-rate information explicit. Do not silently convert or overwrite original transaction amounts.

### 5.3 Freight data model

| Entity | Responsibility |
|---|---|
| `freight_quotation` | Customer quotation and quoted terms |
| `freight_booking` | Accepted booking details |
| `freight_shipment` | Master shipment job |
| `freight_transport_leg` | Individual movement segments |
| `freight_cargo_item` | Cargo, packages, dimensions and weights |
| `freight_container` | Container details and associations |
| `freight_milestone` | Shipment and transport progress |
| `freight_charge` | Customer charges and operational costs |
| `freight_party` | Shipper, consignee and other shipment parties, or references to existing party records |

Use existing customer and contact entities when appropriate rather than creating duplicate master data.

## 6. Customs Clearance Module

### 6.1 Objective

Manage the operational clearance process, including clearance jobs, documentation, declarations, status updates, exceptions and release records.

The module should initially manage and track customs work. It should not claim to perform official customs filing unless an authorized integration is implemented.

### 6.2 Functional components

#### A. Clearance job

A clearance job may be created:

- From an international freight shipment.
- Independently for a customer who only uses customs services.
- Manually by an authorized user.

Capture:

- Clearance reference.
- Customer and importer/exporter.
- Direction: import or export.
- Country and customs office.
- Related freight shipment, if any.
- Assigned clearance officer.
- Arrival or departure information.
- Declaration reference and submission details.
- Current clearance status.
- Required completion dates.

#### B. Clearance lifecycle

Suggested lifecycle:

```text
DRAFT
  -> DOCUMENTS_PENDING
  -> READY_FOR_SUBMISSION
  -> SUBMITTED
  -> UNDER_CUSTOMS_REVIEW
  -> EXAMINATION_REQUIRED (if applicable)
  -> DUTIES_OR_FEES_PENDING (if applicable)
  -> RELEASED
  -> CLOSED
```

Additional exception states should include rejected, on hold and cancelled.

Not all shipments will pass through every state. Store status history and require appropriate permissions and evidence for critical transitions.

#### C. Document management

Support configurable document requirements by shipment direction, country and clearance type.

Typical documents may include:

- Commercial invoice.
- Packing list.
- Bill of lading or air waybill.
- Import/export permits where applicable.
- Certificate of origin where applicable.
- Customs declaration and assessment records.
- Supporting permits or certificates.

For each document, store:

- Document type.
- File reference and secure storage location.
- Version.
- Uploading user and timestamp.
- Verification status.
- Expiry date where relevant.
- Related clearance job.
- Notes or rejection reason.

Do not hard-code one universal document checklist. Requirements differ by jurisdiction, goods and transaction.

#### D. Declaration and assessment records

Capture reference information for declarations, customs responses, assessments, inspections and release notices.

Where appropriate, record:

- Declaration reference.
- Submission timestamp.
- Assessment reference.
- Duty and tax amounts as provided by an authorized source.
- Currency.
- Payment status and supporting reference.
- Examination results.
- Release reference and timestamp.

These records should distinguish user-entered information from information returned by an official integration.

#### E. Exception handling

Support:

- Missing documents.
- Customs queries.
- Failed or rejected submissions.
- Examination requests.
- Unpaid duties or fees.
- Permit issues.
- Clearance delays.
- Holds and release notifications.

Each exception should have an owner, timestamp, notes and resolution history.

### 6.3 Customs data model

| Entity | Responsibility |
|---|---|
| `customs_clearance_job` | Main clearance case |
| `customs_document_requirement` | Required document types and rules |
| `customs_job_document` | Documents associated with a clearance job |
| `customs_declaration` | Declaration references and status |
| `customs_assessment` | Duty, tax and assessment records |
| `customs_event` | Status changes, inspections, holds and releases |
| `customs_exception` | Issues requiring action |
| `customs_payment_reference` | Payment references and recorded payment status, not necessarily payment processing |

A clearance job should support independent operation when no freight shipment exists.

## 7. Integration Design

### 7.1 Freight-to-customs integration

When both modules are enabled, an authorized user can create a clearance job from a freight shipment.

The operation should:

1. Validate tenant access and module configuration.
2. Confirm that freight and customs are both enabled.
3. Verify that the shipment is eligible for clearance.
4. Create the clearance job.
5. Link the job to the shipment.
6. Record an audit event.
7. Update shipment tracking when relevant clearance milestones occur.

Avoid automatically creating duplicate clearance jobs when the operation is retried.

Use a unique relationship or idempotency key where appropriate.

### 7.2 Customs-to-freight status updates

When a clearance job reaches a significant milestone, publish a domain event such as:

```text
CustomsClearanceReleased
CustomsClearanceOnHold
CustomsDocumentsRejected
```

The freight module may consume these events and update shipment milestones.

If customs is disabled, freight operations must still function. If freight is disabled, standalone customs jobs must still function.

### 7.3 Freight/customs-to-domestic integration

When the domestic module and handoff feature are enabled, an authorized user can initiate a domestic delivery job after the shipment reaches an eligible state.

Recommended workflow:

```text
International Shipment Arrives
             |
      Customs Released
             |
   Delivery Eligible?
             |
            Yes
             |
   Create Domestic Delivery
             |
 Link Shipment and Delivery IDs
             |
   Track Final-Mile Delivery
```

This must be optional and should reuse the existing domestic delivery creation process rather than duplicating its logic.

Use an explicit handoff operation initially; automatic handoff can be introduced after the business rules are validated.

### 7.4 Integration rules

- Never require a disabled module for a core workflow.
- Use stable identifiers to link records.
- Validate tenant ownership for every linked entity.
- Record integration failures and allow safe retries.
- Make event consumers idempotent.
- Preserve source records if a downstream operation fails.
- Do not treat a shipment as customs-released without an authorized user action or trusted integration event.

## 8. Security and Data Isolation

### Required controls

- Tenant-scoped data access.
- Role-based permissions.
- Server-side module entitlement checks.
- Fine-grained access to documents and financial data.
- Audit trails for configuration, status and financial changes.
- Secure document storage and controlled download links.
- Input validation and protection against unauthorized cross-tenant references.
- Secrets managed outside source code.
- Explicit permissions for configuration changes and operational overrides.

### Roles

The following roles are confirmed for this system:

| Role | Access |
|---|---|
| `ADMIN` | Full access — module configuration, all modules, user management |
| `OPERATIONS` | Domestic logistics — orders, inventory, shipments, deliveries, tracking, reports |
| `WAREHOUSE_USER` | Products, warehouses, inventory |
| `DISPATCHER` | Carriers, shipments, drivers, vehicles, deliveries, tracking |
| `FREIGHT_OPERATOR` | Freight quotations, bookings, shipment jobs, cargo, transport legs, charges |
| `CUSTOMS_OFFICER` | Customs clearance jobs, documents, declarations, assessments, exceptions |
| `FINANCE_USER` | Freight charges, customs assessments, cost and billing records |

## 9. Backend Package Structure

Extending the existing `com.logistics.system` package structure:

```text
com.logistics.system
├── common/                     # Existing — shared exceptions, events, error handling
│   ├── config/
│   ├── event/
│   ├── exception/
│   └── storage/                # New — S3/MinIO document storage service
├── configuration/              # New — module catalog and feature toggles
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
├── order/                      # Existing
├── inventory/                  # Existing
├── warehouse/                  # Existing
├── shipment/                   # Existing
├── delivery/                   # Existing
├── tracking/                   # Existing
├── reporting/                  # Existing
├── security/                   # Existing — JWT, roles (FREIGHT_OPERATOR, CUSTOMS_OFFICER, FINANCE_USER added)
├── freight/                    # New — Phase 2
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
├── customs/                    # New — Phase 3
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
└── integration/                # New — Phase 4
    ├── freightcustoms/
    └── domestic/
```

## 10. Suggested Frontend Navigation

The navigation should be driven by the effective module configuration and the current user's permissions.

```text
Dashboard

Domestic Logistics          [when enabled]
  Orders
  Inventory
  Dispatch
  Deliveries

International Freight      [when enabled]
  Quotations
  Bookings
  Shipments
  Cargo & Containers
  Tracking
  Charges

Customs Clearance           [when enabled]
  Clearance Jobs
  Documents
  Declarations
  Assessments
  Exceptions

Administration              [with permission]
  Users & Roles
  Module Configuration
  Audit History
```

The user interface is not the security boundary. Every operation must still be checked by the backend.

## 11. Implementation Plan

### Phase 1 — Configuration foundation (Backend)

- Add `FREIGHT_OPERATOR`, `CUSTOMS_OFFICER`, `FINANCE_USER` to the `Role` enum.
- Create `module_catalog` and `module_config` Liquibase migrations.
- Create `module_config_audit` Liquibase migration.
- Implement `ModuleCatalog`, `ModuleConfig`, `ModuleConfigAudit` domain entities.
- Implement `ModuleConfigService` with enable/disable and access check logic.
- Implement admin API endpoints (`/api/admin/modules`).
- Implement `GET /api/me/modules` for frontend navigation.
- Add module access guard for use in Phase 2+ controllers.
- Add tests for disabled modules and unauthorized access.
- Preserve all existing domestic behavior through regression tests.

### Phase 1 — Configuration foundation (Frontend)

- Drive sidebar navigation from `GET /api/me/modules` response.
- Add Module Configuration page under Administration (ADMIN only).
- Show/hide module sections based on enabled modules.

### Phase 2 — Freight MVP (Backend)

- S3/MinIO document storage service (shared, used by freight and customs).
- Freight quotation — create, send, accept, reject, expire.
- Booking conversion from accepted quotation.
- Freight shipment job creation and lifecycle.
- Cargo items and container management.
- Transport legs.
- Shipment milestones.
- Basic freight charge recording.
- Freight shipment search and reporting.

### Phase 2 — Freight MVP (Frontend)

- Quotations, Bookings, Shipments, Cargo & Containers, Tracking, Charges pages.

### Phase 3 — Customs MVP (Backend)

- Standalone clearance jobs.
- Clearance jobs linked to freight shipments.
- Document requirements and attachments (uses shared S3/MinIO storage).
- Declaration and assessment references.
- Clearance lifecycle and exception management.
- Release records and audit history.

### Phase 3 — Customs MVP (Frontend)

- Clearance Jobs, Documents, Declarations, Assessments, Exceptions pages.

### Phase 4 — Integrations and notifications (Backend + Frontend)

- Freight/customs job linking.
- Customs status propagation to freight milestones.
- Optional domestic delivery handoff.
- Notifications (email or in-app).
- External carrier and customs integrations as required.

## 12. Acceptance Criteria

The extension is ready for initial release when:

1. Existing domestic operations continue to work without regression.
2. Administrators can enable and disable modules per tenant.
3. Disabled modules cannot be accessed through their APIs by unauthorized clients.
4. Freight-only clients can create and complete freight jobs without customs or domestic modules.
5. Customs-only clients can create and complete standalone clearance jobs without freight or domestic modules.
6. Freight and customs can be enabled together and linked correctly.
7. Domestic delivery handoff works only when the relevant modules and permissions are enabled.
8. Historical data remains available under the defined read-only and authorization rules when a module is disabled.
9. Configuration changes and critical business transitions are auditable.
10. Document access is tenant-scoped and authorized.
11. Retried integration operations do not create duplicate jobs.
12. Automated tests cover all supported module combinations and relevant dependency rules.

## 13. Resolved Design Decisions

All outstanding design decisions have been confirmed:

| Question | Answer |
|---|---|
| Single-tenant or multi-tenant? | Single-tenant — one deployment per client. No `tenant_id` needed. |
| Backend framework? | Java 21, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Liquibase |
| Frontend framework? | React 19, TypeScript, Vite 5, TanStack Query v5, React Router v7, Tailwind CSS v4 |
| Existing reusable services? | Auth and user management ✅. Documents and audit logging are new. |
| Document storage? | MinIO (local dev via Docker) + AWS S3 (production). Same SDK, configured via environment variables. |
| Customs filing? | Manual tracking only for MVP. No direct electronic filing. |
| Module enablement control? | ADMIN role controls module catalog and configuration. |
| Notifications? | Deferred to Phase 4. |
| Implementation approach? | Backend-first per phase. |}
