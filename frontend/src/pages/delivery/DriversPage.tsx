import { driversApi } from '@/api/delivery'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { DriverStatus } from '@/types'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

const statusVariant: Record<DriverStatus, 'success' | 'warning' | 'secondary'> = {
  AVAILABLE: 'success', ON_DELIVERY: 'warning', OFF_DUTY: 'secondary',
}

export function DriversPage() {
  const qc = useQueryClient()
  const { data: drivers, isLoading } = useQuery({ queryKey: ['drivers'], queryFn: driversApi.list })
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', licenseNumber: '', licenseType: '', phone: '' })
  const [error, setError] = useState('')

  const create = useMutation({
    mutationFn: () => driversApi.create(form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['drivers'] }); setShowForm(false); setError('') },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to create driver'),
  })

  const set = (field: string, value: string) => setForm((f) => ({ ...f, [field]: value }))

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Drivers</h1>
        <Button onClick={() => setShowForm(!showForm)}>+ New Driver</Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader><CardTitle>New Driver</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Name</Label><Input value={form.name} onChange={(e) => set('name', e.target.value)} /></div>
              <div className="space-y-1"><Label>License Number</Label><Input value={form.licenseNumber} onChange={(e) => set('licenseNumber', e.target.value)} /></div>
              <div className="space-y-1"><Label>License Type</Label><Input value={form.licenseType} onChange={(e) => set('licenseType', e.target.value)} /></div>
              <div className="space-y-1"><Label>Phone</Label><Input value={form.phone} onChange={(e) => set('phone', e.target.value)} /></div>
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
                {['ID', 'Name', 'License #', 'License Type', 'Phone', 'Status'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
              {drivers?.map((d) => (
                <tr key={d.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">{d.id}</td>
                  <td className="px-4 py-3 font-medium">{d.name}</td>
                  <td className="px-4 py-3 font-mono text-xs">{d.licenseNumber}</td>
                  <td className="px-4 py-3">{d.licenseType ?? '—'}</td>
                  <td className="px-4 py-3">{d.phone ?? '—'}</td>
                  <td className="px-4 py-3"><Badge variant={statusVariant[d.status]}>{d.status.replace(/_/g, ' ')}</Badge></td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}
