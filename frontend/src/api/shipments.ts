import type { Carrier, CreateShipmentRequest, Page, Shipment, TrackingHistory, UpdateShipmentStatusRequest } from '@/types'
import api from './client'

export const carriersApi = {
  list: () => api.get<Carrier[]>('/carriers').then((r) => r.data),
  get: (id: number) => api.get<Carrier>(`/carriers/${id}`).then((r) => r.data),
  create: (data: Omit<Carrier, 'id'>) =>
    api.post<Carrier>('/carriers', data).then((r) => r.data),
}

export const shipmentsApi = {
  list: (page = 0, size = 20) =>
    api.get<Page<Shipment>>('/shipments', { params: { page, size, sort: 'createdAt,desc' } }).then((r) => r.data),
  get: (id: number) => api.get<Shipment>(`/shipments/${id}`).then((r) => r.data),
  getByTrackingNumber: (trackingNumber: string) =>
    api.get<Shipment>(`/shipments/tracking/${trackingNumber}`).then((r) => r.data),
  create: (data: CreateShipmentRequest) =>
    api.post<Shipment>('/shipments', data).then((r) => r.data),
  updateStatus: (id: number, data: UpdateShipmentStatusRequest) =>
    api.put<Shipment>(`/shipments/${id}/status`, data).then((r) => r.data),
  tracking: (id: number) =>
    api.get<TrackingHistory>(`/shipments/${id}/tracking`).then((r) => r.data),
}

export const trackingApi = {
  getByTrackingNumber: (trackingNumber: string) =>
    api.get<TrackingHistory>(`/tracking/${trackingNumber}`).then((r) => r.data),
}
