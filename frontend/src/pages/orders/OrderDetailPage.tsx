import { ordersApi } from '@/api/orders'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { OrderStatus } from '@/types'
import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'

const statusVariant: Record<OrderStatus, 'default' | 'success' | 'destructive' | 'warning' | 'secondary' | 'outline'> = {
  CREATED: 'secondary', CONFIRMED: 'default', STOCK_RESERVED: 'default',
  PROCESSING: 'default', READY_TO_SHIP: 'warning', SHIPPED: 'default',
  DELIVERED: 'success', CANCELLED: 'outline', FAILED: 'destructive', RETURNED: 'secondary',
}

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: order, isLoading } = useQuery({
    queryKey: ['orders', id],
    queryFn: () => ordersApi.get(Number(id)),
  })

  if (isLoading) return <div className="p-6 text-gray-400">Loading...</div>
  if (!order) return <div className="p-6 text-gray-400">Order not found.</div>

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center gap-3">
        <Link to="/orders" className="text-sm text-blue-600 hover:underline">← Orders</Link>
        <h1 className="text-2xl font-semibold">Order #{order.id}</h1>
        <Badge variant={statusVariant[order.status]}>{order.status.replace(/_/g, ' ')}</Badge>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Details</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <Row label="Customer" value={order.customerName} />
            <Row label="Total Amount" value={`$${order.items.reduce((sum, i) => sum + i.quantity * i.unitPrice, 0).toFixed(2)}`} />
            <Row label="Notes" value={order.notes ?? '—'} />
            <Row label="Created" value={new Date(order.createdAt).toLocaleString()} />
            {order.failureReason && <Row label="Failure Reason" value={order.failureReason} />}
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Items</CardTitle></CardHeader>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="border-b bg-gray-50">
                <tr>
                  {['SKU', 'Product', 'Qty', 'Unit Price', 'Total'].map((h) => (
                    <th key={h} className="px-4 py-2 text-left font-medium text-gray-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {order.items.map((item) => (
                  <tr key={item.id} className="border-b last:border-0">
                    <td className="px-4 py-2 font-mono text-xs">{item.sku}</td>
                    <td className="px-4 py-2">{item.productName}</td>
                    <td className="px-4 py-2">{item.quantity}</td>
                    <td className="px-4 py-2">${item.unitPrice.toFixed(2)}</td>
                    <td className="px-4 py-2">${(item.quantity * item.unitPrice).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
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
