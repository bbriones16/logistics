import { reportingApi } from '@/api/reporting'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, Package, PackageCheck, Truck } from 'lucide-react'

export function DashboardPage() {
  const orders = useQuery({ queryKey: ['reports', 'orders'], queryFn: reportingApi.orders })
  const shipments = useQuery({ queryKey: ['reports', 'shipments'], queryFn: reportingApi.shipments })
  const deliveries = useQuery({ queryKey: ['reports', 'deliveries'], queryFn: reportingApi.deliveries })
  const inventory = useQuery({ queryKey: ['reports', 'inventory'], queryFn: reportingApi.inventorySummary })
  const lowStock = useQuery({ queryKey: ['reports', 'low-stock'], queryFn: reportingApi.lowStock })

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>

      {/* Summary cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-2 text-sm text-gray-500">
              <Package size={16} /> Orders
            </CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{orders.data?.total ?? '—'}</p>
            <p className="text-xs text-gray-500 mt-1">{orders.data?.createdToday ?? 0} created today</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-2 text-sm text-gray-500">
              <Truck size={16} /> Shipments
            </CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{shipments.data?.total ?? '—'}</p>
            <p className="text-xs text-gray-500 mt-1">
              {shipments.data?.byStatus?.IN_TRANSIT ?? 0} in transit
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-2 text-sm text-gray-500">
              <PackageCheck size={16} /> Deliveries
            </CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{deliveries.data?.total ?? '—'}</p>
            <p className="text-xs text-gray-500 mt-1">{deliveries.data?.scheduledToday ?? 0} scheduled today</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription className="flex items-center gap-2 text-sm text-gray-500">
              <AlertTriangle size={16} /> Low Stock
            </CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-3xl font-bold">{inventory.data?.lowStockItemCount ?? '—'}</p>
            <p className="text-xs text-gray-500 mt-1">{inventory.data?.totalProducts ?? 0} total products</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Order status breakdown */}
        <Card>
          <CardHeader>
            <CardTitle>Orders by Status</CardTitle>
          </CardHeader>
          <CardContent>
            {orders.data ? (
              <div className="space-y-2">
                {Object.entries(orders.data.byStatus)
                  .filter(([, count]) => count > 0)
                  .map(([status, count]) => (
                    <div key={status} className="flex items-center justify-between">
                      <span className="text-sm text-gray-600">{status.replace(/_/g, ' ')}</span>
                      <Badge variant="secondary">{count}</Badge>
                    </div>
                  ))}
              </div>
            ) : (
              <p className="text-sm text-gray-400">Loading...</p>
            )}
          </CardContent>
        </Card>

        {/* Low stock alert */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <AlertTriangle size={16} className="text-yellow-500" />
              Low Stock Items
            </CardTitle>
          </CardHeader>
          <CardContent>
            {lowStock.data?.items.length === 0 ? (
              <p className="text-sm text-gray-400">All stock levels are healthy.</p>
            ) : (
              <div className="space-y-2">
                {lowStock.data?.items.map((item) => (
                  <div key={item.productId} className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium">{item.productName}</p>
                      <p className="text-xs text-gray-500">{item.productSku}</p>
                    </div>
                    <Badge variant="warning">
                      {item.availableQuantity} / {item.lowStockThreshold}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Inventory summary */}
      <Card>
        <CardHeader>
          <CardTitle>Inventory Summary</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {[
              { label: 'Products', value: inventory.data?.totalProducts },
              { label: 'Warehouses', value: inventory.data?.totalWarehouses },
              { label: 'Total Stock On Hand', value: inventory.data?.totalStockOnHand },
              { label: 'Low Stock Items', value: inventory.data?.lowStockItemCount },
            ].map(({ label, value }) => (
              <div key={label} className="rounded-md bg-gray-50 p-3">
                <p className="text-xs text-gray-500">{label}</p>
                <p className="text-xl font-semibold">{value ?? '—'}</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function CardDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <p className={className} {...props} />
}
