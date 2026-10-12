// --- Auth ---
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
}

// --- Common ---
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
}

// --- Orders ---
export type OrderStatus =
  | 'CREATED' | 'CONFIRMED' | 'STOCK_RESERVED' | 'PROCESSING'
  | 'READY_TO_SHIP' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'FAILED' | 'RETURNED'

export interface Customer {
  id: number
  name: string
  email: string
  phone?: string
  address?: string
}

export interface OrderItem {
  id: number
  sku: string
  productName: string
  quantity: number
  unitPrice: number
}

export interface Order {
  id: number
  customerId: number
  customerName: string
  status: OrderStatus
  failureReason?: string
  notes?: string
  items: OrderItem[]
  createdAt: string
}

export interface CreateOrderRequest {
  customerId: number
  notes?: string
  items: { sku: string; productName: string; quantity: number; unitPrice: number }[]
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus
}

// --- Inventory ---
export type MovementType = 'RECEIVE' | 'RESERVE' | 'RESERVE_RELEASE' | 'PICK' | 'PACK' | 'ADJUSTMENT'

export interface Product {
  id: number
  sku: string
  name: string
  description?: string
  unitOfMeasure?: string
  lowStockThreshold: number
}

export interface Warehouse {
  id: number
  code: string
  name: string
  address?: string
  active: boolean
}

export interface InventoryItem {
  productId: number
  productSku: string
  productName: string
  warehouseId: number
  warehouseName: string
  quantityOnHand: number
  quantityReserved: number
  availableQuantity: number
  lowStockThreshold: number
  belowThreshold: boolean
}

export interface StockMovement {
  id: number
  productId: number
  productSku: string
  warehouseId: number
  movementType: MovementType
  quantity: number
  referenceId?: number
  notes?: string
  createdAt: string
}

export interface ReceiveStockRequest {
  productId: number
  warehouseId: number
  quantity: number
  notes?: string
}

// --- Shipments ---
export type ShipmentStatus =
  | 'CREATED' | 'PROCESSING' | 'READY_FOR_PICKUP' | 'PICKED_UP'
  | 'IN_TRANSIT' | 'ARRIVED_AT_HUB' | 'OUT_FOR_DELIVERY' | 'DELIVERED'
  | 'FAILED' | 'CANCELLED'

export type ShipmentType = 'FULFILLMENT' | 'STANDALONE'
export type ShipmentMedium = 'LAND' | 'AIR' | 'SEA'

export interface Carrier {
  id: number
  code: string
  name: string
  companyOwned: boolean
  contactEmail?: string
  contactPhone?: string
}

export interface Shipment {
  id: number
  shipmentType: ShipmentType
  orderId?: number
  carrierId: number
  carrierName: string
  medium: ShipmentMedium
  trackingNumber: string
  status: ShipmentStatus
  cargoDescription?: string
  quantity?: number
  weightKg?: number
  freightCost?: number
  shipToAddress?: string
  estimatedDeliveryDate?: string
  actualDeliveryDate?: string
  proofOfDelivery?: string
  notes?: string
  createdAt: string
}

export interface CreateShipmentRequest {
  shipmentType: ShipmentType
  orderId?: number
  carrierId: number
  medium: ShipmentMedium
  trackingNumber?: string
  cargoDescription?: string
  quantity?: number
  weightKg?: number
  freightCost?: number
  shipToAddress?: string
  estimatedDeliveryDate?: string
  notes?: string
}

export interface UpdateShipmentStatusRequest {
  status: ShipmentStatus
  proofOfDelivery?: string
}

// --- Delivery ---
export type DeliveryStatus = 'ASSIGNED' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'FAILED' | 'CANCELLED'
export type DriverStatus = 'AVAILABLE' | 'ON_DELIVERY' | 'OFF_DUTY'
export type VehicleStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE'
export type VehicleType = 'MOTORCYCLE' | 'VAN' | 'TRUCK'

export interface Driver {
  id: number
  name: string
  licenseNumber: string
  licenseType?: string
  phone?: string
  status: DriverStatus
}

export interface Vehicle {
  id: number
  plateNumber: string
  vehicleType: VehicleType
  capacityKg?: number
  status: VehicleStatus
}

export interface Delivery {
  id: number
  shipmentId: number
  driverId: number
  driverName: string
  vehicleId: number
  vehiclePlate: string
  status: DeliveryStatus
  scheduledDate: string
  notes?: string
  createdAt: string
}

export interface CreateDeliveryRequest {
  shipmentId: number
  driverId: number
  vehicleId: number
  scheduledDate: string
  notes?: string
}

export interface UpdateDeliveryStatusRequest {
  status: DeliveryStatus
  proofOfDelivery?: string
}

// --- Tracking ---
export interface TrackingEvent {
  status: ShipmentStatus
  location?: string
  notes?: string
  occurredAt: string
}

export interface TrackingHistory {
  shipmentId: number
  trackingNumber: string
  currentStatus: ShipmentStatus
  events: TrackingEvent[]
}

// --- Reporting ---
export interface OrderSummary {
  total: number
  createdToday: number
  byStatus: Record<OrderStatus, number>
}

export interface ShipmentSummary {
  total: number
  byStatus: Record<ShipmentStatus, number>
}

export interface DeliverySummary {
  total: number
  scheduledToday: number
  byStatus: Record<DeliveryStatus, number>
}

export interface InventorySummary {
  totalProducts: number
  totalWarehouses: number
  totalStockOnHand: number
  lowStockItemCount: number
}

export interface LowStockReport {
  count: number
  items: InventoryItem[]
}
