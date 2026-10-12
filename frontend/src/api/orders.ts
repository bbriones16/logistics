import type { Customer, CreateOrderRequest, Order, Page, UpdateOrderStatusRequest } from '@/types'
import api from './client'

export const customersApi = {
  list: () => api.get<Customer[]>('/customers').then((r) => r.data),
  get: (id: number) => api.get<Customer>(`/customers/${id}`).then((r) => r.data),
  create: (data: Omit<Customer, 'id'>) =>
    api.post<Customer>('/customers', data).then((r) => r.data),
}

export const ordersApi = {
  list: (page = 0, size = 20) =>
    api.get<Page<Order>>('/orders', { params: { page, size, sort: 'createdAt,desc' } }).then((r) => r.data),
  get: (id: number) => api.get<Order>(`/orders/${id}`).then((r) => r.data),
  create: (data: CreateOrderRequest) =>
    api.post<Order>('/orders', data).then((r) => r.data),
  updateStatus: (id: number, data: UpdateOrderStatusRequest) =>
    api.put<Order>(`/orders/${id}/status`, data).then((r) => r.data),
}
