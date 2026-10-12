import { vehiclesApi } from '@/api/delivery'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import type { VehicleStatus } from '@/types'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

const statusVariant: Record<VehicleStatus, 'success' | 'warning' | 'secondary'> = {
  AVAILABLE: 'success', IN_USE: 'warning', MAINTENANCE: 'secondary',
}

export function VehiclesPage() {
  const qc = useQueryClient()
  const { data: vehicles, isLoading } = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesApi.list })
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ plateNumber: '', vehicleType: 'VAN', capacityKg: '' })
  const [error, setError] = useState('')

  const create = useMutation({
    mutationFn: () => vehiclesApi.create({
      plateNumber: form.plateNumber,
      vehicleType: form.vehicleType as any,
      capacityKg: form.capacityKg ? Number(form.capacityKg) : undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['vehicles'] }); setShowForm(false); setError('') },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to create vehicle'),
  })

  const set = (field: string, value: string) => setForm((f) => ({ ...f, [field]: value }))

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Vehicles</h1>
        <Button onClick={() => setShowForm(!showForm)}>+ New Vehicle</Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader><CardTitle>New Vehicle</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1"><Label>Plate Number</Label><Input value={form.plateNumber} onChange={(e) => set('plateNumber', e.target.value)} /></div>
              <div className="space-y-1">
                <Label>Type</Label>
                <Select value={form.vehicleType} onChange={(e) => set('vehicleType', e.target.value)}>
                  <option value="MOTORCYCLE">Motorcycle</option>
                  <option value="VAN">Van</option>
                  <option value="TRUCK">Truck</option>
                </Select>
              </div>
              <div className="space-y-1"><Label>Capacity (kg)</Label><Input type="number" value={form.capacityKg} onChange={(e) => set('capacityKg', e.target.value)} /></div>
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button onClick={() => create.mutate()} disabled={create.isPending}>Save</Button>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="p-0">
          <table className="w-full text-sm">
            <thead className="border-b bg-gray-50">
              <tr>
                {['ID', 'Plate', 'Type', 'Capacity (kg)', 'Status'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
              {vehicles?.map((v) => (
                <tr key={v.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">{v.id}</td>
                  <td className="px-4 py-3 font-mono">{v.plateNumber}</td>
                  <td className="px-4 py-3">{v.vehicleType}</td>
                  <td className="px-4 py-3">{v.capacityKg ?? '—'}</td>
                  <td className="px-4 py-3"><Badge variant={statusVariant[v.status]}>{v.status.replace(/_/g, ' ')}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}
