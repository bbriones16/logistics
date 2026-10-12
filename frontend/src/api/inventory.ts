import type { InventoryItem, Page, Product, ReceiveStockRequest, StockMovement, Warehouse } from '@/types'
import api from './client'

export const productsApi = {
  list: () => api.get<Product[]>('/products').then((r) => r.data),
  get: (id: number) => api.get<Product>(`/products/${id}`).then((r) => r.data),
  create: (data: Omit<Product, 'id'>) =>
    api.post<Product>('/products', data).then((r) => r.data),
}

export const warehousesApi = {
  list: () => api.get<Warehouse[]>('/warehouses').then((r) => r.data),
  get: (id: number) => api.get<Warehouse>(`/warehouses/${id}`).then((r) => r.data),
  create: (data: Omit<Warehouse, 'id' | 'active'>) =>
    api.post<Warehouse>('/warehouses', data).then((r) => r.data),
}

export const inventoryApi = {
  byWarehouse: (warehouseId: number) =>
    api.get<InventoryItem[]>(`/inventory/warehouse/${warehouseId}`).then((r) => r.data),
  byProduct: (productId: number) =>
    api.get<InventoryItem[]>(`/inventory/product/${productId}`).then((r) => r.data),
  lowStock: () => api.get<InventoryItem[]>('/inventory/low-stock').then((r) => r.data),
  receive: (data: ReceiveStockRequest) =>
    api.post('/inventory/receive', data).then((r) => r.data),
  movements: (productId: number, page = 0, size = 50) =>
    api
      .get<Page<StockMovement>>(`/inventory/product/${productId}/movements`, {
        params: { page, size },
      })
      .then((r) => r.data),
}
