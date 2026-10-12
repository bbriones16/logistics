import { inventoryApi, productsApi, warehousesApi } from '@/api/inventory'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

export function InventoryPage() {
  const qc = useQueryClient()
  const [selectedWarehouse, setSelectedWarehouse] = useState<number | null>(null)
  const [showReceive, setShowReceive] = useState(false)
  const [receiveProductId, setReceiveProductId] = useState('')
  const [receiveWarehouseId, setReceiveWarehouseId] = useState('')
  const [receiveQty, setReceiveQty] = useState('')
  const [receiveNotes, setReceiveNotes] = useState('')
  const [error, setError] = useState('')

  const { data: warehouses } = useQuery({ queryKey: ['warehouses'], queryFn: warehousesApi.list })
  const { data: products } = useQuery({ queryKey: ['products'], queryFn: productsApi.list })
  const { data: stock, isLoading } = useQuery({
    queryKey: ['inventory', 'warehouse', selectedWarehouse],
    queryFn: () => inventoryApi.byWarehouse(selectedWarehouse!),
    enabled: !!selectedWarehouse,
  })
  const { data: lowStock } = useQuery({ queryKey: ['inventory', 'low-stock'], queryFn: inventoryApi.lowStock })

  const receive = useMutation({
    mutationFn: () => inventoryApi.receive({
      productId: Number(receiveProductId),
      warehouseId: Number(receiveWarehouseId),
      quantity: Number(receiveQty),
      notes: receiveNotes,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventory'] })
      setShowReceive(false)
      setError('')
    },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to receive stock'),
  })

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Inventory</h1>
        <Button onClick={() => setShowReceive(!showReceive)}>+ Receive Stock</Button>
      </div>

      {showReceive && (
        <Card>
          <CardHeader><CardTitle>Receive Stock</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Product</Label>
                <Select value={receiveProductId} onChange={(e) => setReceiveProductId(e.target.value)}>
                  <option value="">Select product...</option>
                  {products?.map((p) => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Warehouse</Label>
                <Select value={receiveWarehouseId} onChange={(e) => setReceiveWarehouseId(e.target.value)}>
                  <option value="">Select warehouse...</option>
                  {warehouses?.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
                </Select>
              </div>
              <div className="space-y-1">
                <Label>Quantity</Label>
                <Input type="number" value={receiveQty} onChange={(e) => setReceiveQty(e.target.value)} />
              </div>
              <div className="space-y-1">
                <Label>Notes</Label>
                <Input value={receiveNotes} onChange={(e) => setReceiveNotes(e.target.value)} />
              </div>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button onClick={() => receive.mutate()} disabled={receive.isPending}>Receive</Button>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Low stock alert */}
        <Card>
          <CardHeader><CardTitle className="text-yellow-600">Low Stock ({lowStock?.length ?? 0})</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {lowStock?.length === 0 && <p className="text-sm text-gray-400">All levels healthy.</p>}
            {lowStock?.map((item) => (
              <div key={item.productId} className="flex justify-between text-sm">
                <span>{item.productName}</span>
                <Badge variant="warning">{item.availableQuantity}</Badge>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Stock by warehouse */}
        <div className="lg:col-span-2 space-y-3">
          <div className="space-y-1">
            <Label>View stock by warehouse</Label>
            <Select value={selectedWarehouse ?? ''} onChange={(e) => setSelectedWarehouse(Number(e.target.value))}>
              <option value="">Select warehouse...</option>
              {warehouses?.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
            </Select>
          </div>
          <Card>
            <CardContent className="p-0">
              <table className="w-full text-sm">
                <thead className="border-b bg-gray-50">
                  <tr>
                    {['SKU', 'Product', 'On Hand', 'Reserved', 'Available', ''].map((h) => (
                      <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {!selectedWarehouse && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Select a warehouse to view stock.</td></tr>}
                  {isLoading && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
                  {stock?.map((item) => (
                    <tr key={item.productId} className="border-b last:border-0 hover:bg-gray-50">
                      <td className="px-4 py-3 font-mono text-xs">{item.productSku}</td>
                      <td className="px-4 py-3 font-medium">{item.productName}</td>
                      <td className="px-4 py-3">{item.quantityOnHand}</td>
                      <td className="px-4 py-3">{item.quantityReserved}</td>
                      <td className="px-4 py-3">
                        <Badge variant={item.belowThreshold ? 'warning' : 'success'}>{item.availableQuantity}</Badge>
                      </td>
                      <td className="px-4 py-3 text-xs text-gray-400">threshold: {item.lowStockThreshold}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
