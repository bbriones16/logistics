import type { CreateDeliveryRequest, Delivery, Driver, Page, UpdateDeliveryStatusRequest, Vehicle } from '@/types'
import api from './client'

export const driversApi = {
  list: () => api.get<Driver[]>('/drivers').then((r) => r.data),
  available: () => api.get<Driver[]>('/drivers/available').then((r) => r.data),
  get: (id: number) => api.get<Driver>(`/drivers/${id}`).then((r) => r.data),
  create: (data: Omit<Driver, 'id' | 'status'>) =>
    api.post<Driver>('/drivers', data).then((r) => r.data),
}

export const vehiclesApi = {
  list: () => api.get<Vehicle[]>('/vehicles').then((r) => r.data),
  available: () => api.get<Vehicle[]>('/vehicles/available').then((r) => r.data),
  get: (id: number) => api.get<Vehicle>(`/vehicles/${id}`).then((r) => r.data),
  create: (data: Omit<Vehicle, 'id' | 'status'>) =>
    api.post<Vehicle>('/vehicles', data).then((r) => r.data),
}

export const deliveriesApi = {
  list: (page = 0, size = 20) =>
    api.get<Page<Delivery>>('/deliveries', { params: { page, size, sort: 'createdAt,desc' } }).then((r) => r.data),
  get: (id: number) => api.get<Delivery>(`/deliveries/${id}`).then((r) => r.data),
  create: (data: CreateDeliveryRequest) =>
    api.post<Delivery>('/deliveries', data).then((r) => r.data),
  updateStatus: (id: number, data: UpdateDeliveryStatusRequest) =>
    api.put<Delivery>(`/deliveries/${id}/status`, data).then((r) => r.data),
}
