import { carriersApi, shipmentsApi } from '@/api/shipments'
import { ordersApi } from '@/api/orders'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import type { ShipmentStatus } from '@/types'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'

const statusVariant: Record<ShipmentStatus, 'default' | 'success' | 'destructive' | 'warning' | 'secondary' | 'outline'> = {
  CREATED: 'secondary', PROCESSING: 'default', READY_FOR_PICKUP: 'default',
  PICKED_UP: 'default', IN_TRANSIT: 'default', ARRIVED_AT_HUB: 'warning',
  OUT_FOR_DELIVERY: 'warning', DELIVERED: 'success', FAILED: 'destructive', CANCELLED: 'outline',
}

export function ShipmentsPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    shipmentType: 'FULFILLMENT', orderId: '', carrierId: '', medium: 'LAND',
    trackingNumber: '', cargoDescription: '', shipToAddress: '', weightKg: '', freightCost: '',
  })

  const { data, isLoading } = useQuery({ queryKey: ['shipments', page], queryFn: () => shipmentsApi.list(page) })
  const { data: carriers } = useQuery({ queryKey: ['carriers'], queryFn: carriersApi.list })
  const { data: orders } = useQuery({ queryKey: ['orders', 0], queryFn: () => ordersApi.list(0, 100) })

  const create = useMutation({
    mutationFn: () => shipmentsApi.create({
      shipmentType: form.shipmentType as any,
      orderId: form.orderId ? Number(form.orderId) : undefined,
      carrierId: Number(form.carrierId),
      medium: form.medium as any,
      trackingNumber: form.trackingNumber || undefined,
      cargoDescription: form.cargoDescription || undefined,
      shipToAddress: form.shipToAddress || undefined,
      weightKg: form.weightKg ? Number(form.weightKg) : undefined,
      freightCost: form.freightCost ? Number(form.freightCost) : undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['shipments'] }); setShowForm(false); setError('') },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to create shipment'),
  })

  const set = (field: string, value: string) => setForm((f) => ({ ...f, [field]: value }))

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Shipments</h1>
        <Button onClick={() => setShowForm(!showForm)}>+ New Shipment</Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader><CardTitle>New Shipment</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1">
                <Label>Type</Label>
                <Select value={form.shipmentType} onChange={(e) => set('shipmentType', e.target.value)}>
                  <option value="FULFILLMENT">Fulfillment</option>
                  <option value="STANDALONE">Standalone</option>
                </Select>
              </div>
              {form.shipmentType === 'FULFILLMENT' && (
                <div className="space-y-1">
                  <Label>Order</Label>
                  <Select value={form.orderId} onChange={(e) => set('orderId', e.target.value)}>
                    <option value="">Select order...</option>
                    {orders?.content.filter((o) => o.status === 'STOCK_RESERVED').map((o) => (
                      <option key={o.id} value={o.id}>#{o.id} — {o.customerName}</option>
                    ))}
                  </Select>
                </div>
              )}
              <div className="space-y-1">
                <Label>Carrier</Label>
                <Select value={form.carrierId} onChange={(e) => set('carrierId', e.target.value)}>
                  <option value="">Select carrier...</option>
                  {carriers?.map((c) => <option key={c.id} value={c.id}>{c.name} {c.companyOwned ? '(Own)' : '(3PL)'}</option>)}
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Medium</Label>
                <Select value={form.medium} onChange={(e) => set('medium', e.target.value)}>
                  <option value="LAND">Land</option>
                  <option value="AIR">Air</option>
                  <option value="SEA">Sea</option>
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Tracking Number</Label>
                <Input placeholder="Required for 3PL" value={form.trackingNumber} onChange={(e) => set('trackingNumber', e.target.value)} />
              </div>
              <div className="space-y-1">
                <Label>Ship To Address</Label>
                <Input value={form.shipToAddress} onChange={(e) => set('shipToAddress', e.target.value)} />
              </div>
              {form.shipmentType === 'STANDALONE' && (
                <div className="space-y-1">
                  <Label>Cargo Description</Label>
                  <Input value={form.cargoDescription} onChange={(e) => set('cargoDescription', e.target.value)} />
                </div>
              )}
              <div className="space-y-1">
                <Label>Weight (kg)</Label>
                <Input type="number" value={form.weightKg} onChange={(e) => set('weightKg', e.target.value)} />
              </div>
              <div className="space-y-1">
                <Label>Freight Cost</Label>
                <Input type="number" value={form.freightCost} onChange={(e) => set('freightCost', e.target.value)} />
              </div>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button onClick={() => create.mutate()} disabled={create.isPending}>Create Shipment</Button>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead className="border-b bg-gray-50">
              <tr>
                {['ID', 'Tracking #', 'Type', 'Carrier', 'Status', 'Order', 'Created', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
              {data?.content.map((s) => (
                <tr key={s.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">#{s.id}</td>
                  <td className="px-4 py-3 font-mono text-xs">{s.trackingNumber}</td>
                  <td className="px-4 py-3">{s.shipmentType}</td>
                  <td className="px-4 py-3">{s.carrierName}</td>
                  <td className="px-4 py-3"><Badge variant={statusVariant[s.status]}>{s.status.replace(/_/g, ' ')}</Badge></td>
                  <td className="px-4 py-3">{s.orderId ? `#${s.orderId}` : '—'}</td>
                  <td className="px-4 py-3 text-gray-500">{new Date(s.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3"><Link to={`/shipments/${s.id}`} className="text-blue-600 hover:underline text-xs">View</Link></td>
                </tr>
              ))}
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
