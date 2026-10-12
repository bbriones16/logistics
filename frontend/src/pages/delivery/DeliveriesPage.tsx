import { deliveriesApi, driversApi, vehiclesApi } from '@/api/delivery'
import { shipmentsApi } from '@/api/shipments'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import type { DeliveryStatus } from '@/types'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

const statusVariant: Record<DeliveryStatus, 'default' | 'success' | 'destructive' | 'warning' | 'secondary' | 'outline'> = {
  ASSIGNED: 'default', OUT_FOR_DELIVERY: 'warning', DELIVERED: 'success',
  FAILED: 'destructive', CANCELLED: 'outline',
}

const TRANSITIONS: Record<DeliveryStatus, DeliveryStatus[]> = {
  ASSIGNED: ['OUT_FOR_DELIVERY', 'CANCELLED'],
  OUT_FOR_DELIVERY: ['DELIVERED', 'FAILED'],
  DELIVERED: [], FAILED: [], CANCELLED: [],
}

export function DeliveriesPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ shipmentId: '', driverId: '', vehicleId: '', scheduledDate: '', notes: '' })
  const [statusUpdates, setStatusUpdates] = useState<Record<number, { status: string; pod: string }>>({})

  const { data, isLoading } = useQuery({ queryKey: ['deliveries', page], queryFn: () => deliveriesApi.list(page) })
  const { data: drivers } = useQuery({ queryKey: ['drivers', 'available'], queryFn: driversApi.available })
  const { data: vehicles } = useQuery({ queryKey: ['vehicles', 'available'], queryFn: vehiclesApi.available })
  const { data: shipments } = useQuery({ queryKey: ['shipments', 0], queryFn: () => shipmentsApi.list(0, 100) })

  const create = useMutation({
    mutationFn: () => deliveriesApi.create({
      shipmentId: Number(form.shipmentId), driverId: Number(form.driverId),
      vehicleId: Number(form.vehicleId), scheduledDate: form.scheduledDate, notes: form.notes,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['deliveries'] }); setShowForm(false); setError('') },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to assign delivery'),
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, status, pod }: { id: number; status: string; pod: string }) =>
      deliveriesApi.updateStatus(id, { status: status as DeliveryStatus, proofOfDelivery: pod || undefined }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['deliveries'] }); qc.invalidateQueries({ queryKey: ['shipments'] }) },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to update status'),
  })

  const set = (field: string, value: string) => setForm((f) => ({ ...f, [field]: value }))

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Deliveries</h1>
        <Button onClick={() => setShowForm(!showForm)}>+ Assign Delivery</Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader><CardTitle>Assign Delivery</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1">
                <Label>Shipment (ARRIVED_AT_HUB)</Label>
                <Select value={form.shipmentId} onChange={(e) => set('shipmentId', e.target.value)}>
                  <option value="">Select shipment...</option>
                  {shipments?.content.filter((s) => s.status === 'ARRIVED_AT_HUB').map((s) => (
                    <option key={s.id} value={s.id}>#{s.id} — {s.trackingNumber}</option>
                  ))}
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Driver</Label>
                <Select value={form.driverId} onChange={(e) => set('driverId', e.target.value)}>
                  <option value="">Select driver...</option>
                  {drivers?.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Vehicle</Label>
                <Select value={form.vehicleId} onChange={(e) => set('vehicleId', e.target.value)}>
                  <option value="">Select vehicle...</option>
                  {vehicles?.map((v) => <option key={v.id} value={v.id}>{v.plateNumber} ({v.vehicleType})</option>)}
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Scheduled Date</Label>
                <Input type="date" value={form.scheduledDate} onChange={(e) => set('scheduledDate', e.target.value)} />
              </div>
              <div className="space-y-1">
                <Label>Notes</Label>
                <Input value={form.notes} onChange={(e) => set('notes', e.target.value)} />
              </div>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button onClick={() => create.mutate()} disabled={create.isPending}>Assign</Button>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead className="border-b bg-gray-50">
              <tr>
                {['ID', 'Shipment', 'Driver', 'Vehicle', 'Status', 'Scheduled', 'Action'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
              {data?.content.map((d) => {
                const allowed = TRANSITIONS[d.status] ?? []
                const upd = statusUpdates[d.id] ?? { status: '', pod: '' }
                return (
                  <tr key={d.id} className="border-b last:border-0 hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-500">#{d.id}</td>
                    <td className="px-4 py-3 font-mono text-xs">#{d.shipmentId}</td>
                    <td className="px-4 py-3">{d.driverName}</td>
                    <td className="px-4 py-3">{d.vehiclePlate}</td>
                    <td className="px-4 py-3"><Badge variant={statusVariant[d.status]}>{d.status.replace(/_/g, ' ')}</Badge></td>
                    <td className="px-4 py-3">{d.scheduledDate}</td>
                    <td className="px-4 py-3">
                      {allowed.length > 0 && (
                        <div className="flex items-center gap-1">
                          <Select
                            className="h-7 text-xs w-36"
                            value={upd.status}
                            onChange={(e) => setStatusUpdates((prev) => ({ ...prev, [d.id]: { ...upd, status: e.target.value } }))}
                          >
                            <option value="">Status...</option>
                            {allowed.map((s) => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                          </Select>
                          {upd.status === 'DELIVERED' && (
                            <Input
                              className="h-7 text-xs w-28"
                              placeholder="POD"
                              value={upd.pod}
                              onChange={(e) => setStatusUpdates((prev) => ({ ...prev, [d.id]: { ...upd, pod: e.target.value } }))}
                            />
                          )}
                          <Button
                            size="sm"
                            className="h-7 text-xs"
                            disabled={!upd.status || updateStatus.isPending}
                            onClick={() => updateStatus.mutate({ id: d.id, status: upd.status, pod: upd.pod })}
                          >
                            Update
                          </Button>
                        </div>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </CardContent>
      </Card>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-gray-500">
          <span>Page {data.number + 1} of {data.totalPages} ({data.totalElements} total)</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
            <Button variant="outline" size="sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)}>Next</Button>
          </div>
        </div>
      )}
    </div>
  )
}
