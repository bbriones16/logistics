import { trackingApi } from '@/api/shipments'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import type { ShipmentStatus } from '@/types'
import { useState } from 'react'

const statusVariant: Record<ShipmentStatus, 'default' | 'success' | 'destructive' | 'warning' | 'secondary' | 'outline'> = {
  CREATED: 'secondary', PROCESSING: 'default', READY_FOR_PICKUP: 'default',
  PICKED_UP: 'default', IN_TRANSIT: 'default', ARRIVED_AT_HUB: 'warning',
  OUT_FOR_DELIVERY: 'warning', DELIVERED: 'success', FAILED: 'destructive', CANCELLED: 'outline',
}

export function TrackPage() {
  const [input, setInput] = useState('')
  const [trackingNumber, setTrackingNumber] = useState('')
  const [data, setData] = useState<Awaited<ReturnType<typeof trackingApi.getByTrackingNumber>> | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleTrack() {
    if (!input.trim()) return
    setLoading(true)
    setError('')
    setData(null)
    try {
      const result = await trackingApi.getByTrackingNumber(input.trim())
      setData(result)
      setTrackingNumber(input.trim())
    } catch {
      setError('Tracking number not found. Please check and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="border-b bg-white px-6 py-4">
        <div className="mx-auto max-w-2xl flex items-center justify-between">
          <span className="text-lg font-semibold text-blue-600">LogiTrack</span>
          <a href="/login" className="text-sm text-blue-600 hover:underline">Staff Login →</a>
        </div>
      </header>

      <div className="mx-auto max-w-2xl px-6 py-12 space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-3xl font-bold text-gray-900">Track Your Shipment</h1>
          <p className="text-gray-500">Enter your tracking number to see the latest status.</p>
        </div>

        <div className="flex gap-2">
          <Input
            placeholder="e.g. TRK-20261012-00001"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleTrack()}
            className="flex-1"
          />
          <Button onClick={handleTrack} disabled={loading}>
            {loading ? 'Searching...' : 'Track'}
          </Button>
        </div>

        {error && (
          <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {data && (
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="font-mono text-base">{trackingNumber}</CardTitle>
                <Badge variant={statusVariant[data.currentStatus]}>{data.currentStatus.replace(/_/g, ' ')}</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <ol className="relative border-l border-gray-200 space-y-5 ml-2">
                {[...data.events].reverse().map((event, i) => (
                  <li key={i} className="ml-4">
                    <div className={`absolute -left-1.5 mt-1 h-3 w-3 rounded-full border-2 border-white ${i === 0 ? 'bg-blue-500' : 'bg-gray-300'}`} />
                    <div className="flex items-center gap-2">
                      <Badge variant={statusVariant[event.status]}>{event.status.replace(/_/g, ' ')}</Badge>
                    </div>
                    <p className="text-xs text-gray-400 mt-0.5">{new Date(event.occurredAt).toLocaleString()}</p>
                    {event.location && <p className="text-xs text-gray-500 mt-0.5">{event.location}</p>}
                    {event.notes && <p className="text-xs text-gray-400 italic mt-0.5">{event.notes}</p>}
                  </li>
                ))}
              </ol>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
