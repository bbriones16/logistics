import { useAuth } from '@/context/AuthContext'
import {
  BarChart3,
  Box,
  LogOut,
  Package,
  PackageCheck,
  Truck,
  Users,
  Warehouse,
} from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'

const nav = [
  { to: '/', label: 'Dashboard', icon: BarChart3, end: true },
  { to: '/orders', label: 'Orders', icon: Package },
  { to: '/inventory', label: 'Inventory', icon: Warehouse },
  { to: '/shipments', label: 'Shipments', icon: Truck },
  { to: '/delivery', label: 'Deliveries', icon: PackageCheck },
  { to: '/drivers', label: 'Drivers', icon: Users },
  { to: '/vehicles', label: 'Vehicles', icon: Box },
  { to: '/carriers', label: 'Carriers', icon: Truck },
  { to: '/customers', label: 'Customers', icon: Users },
]

export function AppLayout() {
  const { logout } = useAuth()

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="flex w-56 flex-col border-r bg-white">
        <div className="flex h-14 items-center border-b px-4">
          <span className="text-base font-semibold text-blue-600">LogiTrack</span>
        </div>
        <nav className="flex-1 overflow-y-auto p-2">
          {nav.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              <Icon size={16} />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t p-2">
          <button
            onClick={logout}
            className="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900"
          >
            <LogOut size={16} />
            Logout
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
