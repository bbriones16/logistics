import type { DeliverySummary, InventorySummary, LowStockReport, OrderSummary, ShipmentSummary } from '@/types'
import api from './client'

export const reportingApi = {
  orders: () => api.get<OrderSummary>('/reports/orders').then((r) => r.data),
  shipments: () => api.get<ShipmentSummary>('/reports/shipments').then((r) => r.data),
  deliveries: () => api.get<DeliverySummary>('/reports/deliveries').then((r) => r.data),
  inventorySummary: () => api.get<InventorySummary>('/reports/inventory/summary').then((r) => r.data),
  lowStock: () => api.get<LowStockReport>('/reports/inventory/low-stock').then((r) => r.data),
}
