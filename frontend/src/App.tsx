import { AppLayout } from '@/components/AppLayout'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { AuthProvider } from '@/context/AuthContext'
import { LoginPage } from '@/pages/auth/LoginPage'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { CustomersPage } from '@/pages/orders/CustomersPage'
import { OrderDetailPage } from '@/pages/orders/OrderDetailPage'
import { OrdersPage } from '@/pages/orders/OrdersPage'
import { InventoryPage } from '@/pages/inventory/InventoryPage'
import { ShipmentsPage } from '@/pages/shipments/ShipmentsPage'
import { ShipmentDetailPage } from '@/pages/shipments/ShipmentDetailPage'
import { CarriersPage } from '@/pages/shipments/CarriersPage'
import { DeliveriesPage } from '@/pages/delivery/DeliveriesPage'
import { DriversPage } from '@/pages/delivery/DriversPage'
import { VehiclesPage } from '@/pages/delivery/VehiclesPage'
import { TrackPage } from '@/pages/tracking/TrackPage'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/track" element={<TrackPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/orders" element={<OrdersPage />} />
                <Route path="/orders/:id" element={<OrderDetailPage />} />
                <Route path="/customers" element={<CustomersPage />} />
                <Route path="/inventory" element={<InventoryPage />} />
                <Route path="/shipments" element={<ShipmentsPage />} />
                <Route path="/shipments/:id" element={<ShipmentDetailPage />} />
                <Route path="/carriers" element={<CarriersPage />} />
                <Route path="/delivery" element={<DeliveriesPage />} />
                <Route path="/drivers" element={<DriversPage />} />
                <Route path="/vehicles" element={<VehiclesPage />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
