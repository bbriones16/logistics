import { shipmentsApi } from '@/api/shipments'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import type { ShipmentStatus } from '@/types'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle, MapPin } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

const statusVariant: Record<ShipmentStatus, 'default' | 'success' | 'destructive' | 'warning' | 'secondary' | 'outline'> = {
  CREATED: 'secondary', PROCESSING: 'default', READY_FOR_PICKUP: 'default',
  PICKED_UP: 'default', IN_TRANSIT: 'default', ARRIVED_AT_HUB: 'warning',
  OUT_FOR_DELIVERY: 'warning', DELIVERED: 'success', FAILED: 'destructive', CANCELLED: 'outline',
}

const TRANSITIONS: Record<ShipmentStatus, ShipmentStatus[]> = {
  CREATED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['READY_FOR_PICKUP', 'CANCELLED'],
  READY_FOR_PICKUP: ['PICKED_UP', 'CANCELLED'],
  PICKED_UP: ['IN_TRANSIT'],
  IN_TRANSIT: ['ARRIVED_AT_HUB', 'FAILED'],
  ARRIVED_AT_HUB: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED', 'FAILED'],
  DELIVERED: [], FAILED: [], CANCELLED: [],
}

export function ShipmentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const qc = useQueryClient()
  const [nextStatus, setNextStatus] = useState<ShipmentStatus | ''>('')
  const [pod, setPod] = useState('')
  const [error, setError] = useState('')

  const { data: shipment, isLoading } = useQuery({
    queryKey: ['shipments', id],
    queryFn: () => shipmentsApi.get(Number(id)),
  })
  const { data: tracking } = useQuery({
    queryKey: ['shipments', id, 'tracking'],
    queryFn: () => shipmentsApi.tracking(Number(id)),
    enabled: !!id,
  })

  const updateStatus = useMutation({
    mutationFn: () => shipmentsApi.updateStatus(Number(id), {
      status: nextStatus as ShipmentStatus,
      proofOfDelivery: pod || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['shipments', id] })
      qc.invalidateQueries({ queryKey: ['shipments', id, 'tracking'] })
      setNextStatus('')
      setPod('')
      setError('')
    },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to update status'),
  })

  if (isLoading) return <div className="p-6 text-gray-400">Loading...</div>
  if (!shipment) return <div className="p-6 text-gray-400">Shipment not found.</div>

  const allowed = TRANSITIONS[shipment.status] ?? []

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center gap-3">
        <Link to="/shipments" className="text-sm text-blue-600 hover:underline">← Shipments</Link>
        <h1 className="text-2xl font-semibold">Shipment #{shipment.id}</h1>
        <Badge variant={statusVariant[shipment.status]}>{shipment.status.replace(/_/g, ' ')}</Badge>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Details</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <Row label="Tracking #" value={shipment.trackingNumber} />
            <Row label="Type" value={shipment.shipmentType} />
            <Row label="Carrier" value={shipment.carrierName} />
            <Row label="Medium" value={shipment.medium} />
            {shipment.orderId && <Row label="Order" value={`#${shipment.orderId}`} />}
            {shipment.shipToAddress && <Row label="Ship To" value={shipment.shipToAddress} />}
            {shipment.weightKg && <Row label="Weight" value={`${shipment.weightKg} kg`} />}
            {shipment.freightCost && <Row label="Freight Cost" value={`$${shipment.freightCost}`} />}
            {shipment.estimatedDeliveryDate && <Row label="Est. Delivery" value={shipment.estimatedDeliveryDate} />}
            {shipment.actualDeliveryDate && <Row label="Actual Delivery" value={shipment.actualDeliveryDate} />}
            {shipment.proofOfDelivery && <Row label="POD" value={shipment.proofOfDelivery} />}
            {shipment.cargoDescription && <Row label="Cargo" value={shipment.cargoDescription} />}
          </CardContent>
        </Card>

        <div className="space-y-4">
          {/* Status update */}
          {allowed.length > 0 && (
            <Card>
              <CardHeader><CardTitle>Update Status</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                <div className="space-y-1">
                  <Label>New Status</Label>
                  <Select value={nextStatus} onChange={(e) => setNextStatus(e.target.value as ShipmentStatus)}>
                    <option value="">Select status...</option>
                    {allowed.map((s) => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                  </Select>
                </div>
                {nextStatus === 'DELIVERED' && (
                  <div className="space-y-1">
                    <Label>Proof of Delivery</Label>
                    <Input value={pod} onChange={(e) => setPod(e.target.value)} placeholder="e.g. Signed by John at door" />
                  </div>
                )}
                {error && <p className="text-sm text-red-600">{error}</p>}
                <Button onClick={() => updateStatus.mutate()} disabled={!nextStatus || updateStatus.isPending}>
                  Update
                </Button>
              </CardContent>
            </Card>
          )}

          {/* Tracking timeline */}
          <Card>
            <CardHeader><CardTitle className="flex items-center gap-2"><MapPin size={16} /> Tracking History</CardTitle></CardHeader>
            <CardContent>
              {!tracking?.events.length ? (
                <p className="text-sm text-gray-400">No tracking events yet.</p>
              ) : (
                <ol className="relative border-l border-gray-200 space-y-4 ml-2">
                  {[...tracking.events].reverse().map((event, i) => (
                    <li key={i} className="ml-4">
                      <div className="absolute -left-1.5 mt-1 h-3 w-3 rounded-full border border-white bg-blue-500" />
                      <div className="flex items-center gap-2">
                        <Badge variant={statusVariant[event.status]} className="text-xs">{event.status.replace(/_/g, ' ')}</Badge>
                        {i === 0 && <CheckCircle size={14} className="text-green-500" />}
                      </div>
                      <p className="text-xs text-gray-400 mt-0.5">{new Date(event.occurredAt).toLocaleString()}</p>
                      {event.location && <p className="text-xs text-gray-500">{event.location}</p>}
                    </li>
                  ))}
                </ol>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <span className="text-gray-500">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  )
}
