import { customersApi, ordersApi } from '@/api/orders'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import type { OrderStatus } from '@/types'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'

const statusVariant: Record<OrderStatus, 'default' | 'success' | 'destructive' | 'warning' | 'secondary' | 'outline'> = {
  CREATED: 'secondary', CONFIRMED: 'default', STOCK_RESERVED: 'default',
  PROCESSING: 'default', READY_TO_SHIP: 'warning', SHIPPED: 'default',
  DELIVERED: 'success', CANCELLED: 'outline', FAILED: 'destructive', RETURNED: 'secondary',
}

export function OrdersPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [customerId, setCustomerId] = useState('')
  const [items, setItems] = useState([{ sku: '', productName: '', quantity: 1, unitPrice: 0 }])
  const [error, setError] = useState('')

  const { data, isLoading } = useQuery({
    queryKey: ['orders', page],
    queryFn: () => ordersApi.list(page),
  })
  const { data: customers } = useQuery({ queryKey: ['customers'], queryFn: customersApi.list })

  const create = useMutation({
    mutationFn: () => ordersApi.create({ customerId: Number(customerId), items }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['orders'] }); setShowForm(false); setError('') },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to create order'),
  })

  function addItem() {
    setItems([...items, { sku: '', productName: '', quantity: 1, unitPrice: 0 }])
  }

  function updateItem(i: number, field: string, value: string | number) {
    setItems(items.map((item, idx) => idx === i ? { ...item, [field]: value } : item))
  }

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Orders</h1>
        <Button onClick={() => setShowForm(!showForm)}>+ New Order</Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader><CardTitle>New Order</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1">
              <Label>Customer</Label>
              <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                <option value="">Select customer...</option>
                {customers?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Items</Label>
              {items.map((item, i) => (
                <div key={i} className="grid grid-cols-4 gap-2">
                  <Input placeholder="SKU" value={item.sku} onChange={(e) => updateItem(i, 'sku', e.target.value)} />
                  <Input placeholder="Product name" value={item.productName} onChange={(e) => updateItem(i, 'productName', e.target.value)} />
                  <Input type="number" placeholder="Qty" value={item.quantity} onChange={(e) => updateItem(i, 'quantity', Number(e.target.value))} />
                  <Input type="number" placeholder="Unit price" value={item.unitPrice} onChange={(e) => updateItem(i, 'unitPrice', Number(e.target.value))} />
                </div>
              ))}
              <Button variant="outline" size="sm" onClick={addItem}>+ Add Item</Button>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button onClick={() => create.mutate()} disabled={create.isPending}>Create Order</Button>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead className="border-b bg-gray-50">
              <tr>
                {['ID', 'Customer', 'Status', 'Items', 'Total', 'Created', ''].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
              {data?.content.map((o) => (
                <tr key={o.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">#{o.id}</td>
                  <td className="px-4 py-3 font-medium">{o.customerName}</td>
                  <td className="px-4 py-3"><Badge variant={statusVariant[o.status]}>{o.status.replace(/_/g, ' ')}</Badge></td>
                  <td className="px-4 py-3">{o.items.length}</td>
                  <td className="px-4 py-3">${o.items.reduce((sum, i) => sum + i.quantity * i.unitPrice, 0).toFixed(2)}</td>
                  <td className="px-4 py-3 text-gray-500">{new Date(o.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3"><Link to={`/orders/${o.id}`} className="text-blue-600 hover:underline text-xs">View</Link></td>
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
