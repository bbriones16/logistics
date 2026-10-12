import { carriersApi } from '@/api/shipments'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

export function CarriersPage() {
  const qc = useQueryClient()
  const { data: carriers, isLoading } = useQuery({ queryKey: ['carriers'], queryFn: carriersApi.list })
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ code: '', name: '', companyOwned: false, contactEmail: '', contactPhone: '' })
  const [error, setError] = useState('')

  const create = useMutation({
    mutationFn: () => carriersApi.create(form),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['carriers'] }); setShowForm(false); setError('') },
    onError: (e: any) => setError(e.response?.data?.message ?? 'Failed to create carrier'),
  })

  const set = (field: string, value: string | boolean) => setForm((f) => ({ ...f, [field]: value }))

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Carriers</h1>
        <Button onClick={() => setShowForm(!showForm)}>+ New Carrier</Button>
      </div>

      {showForm && (
        <Card>
          <CardHeader><CardTitle>New Carrier</CardTitle></CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1"><Label>Code</Label><Input value={form.code} onChange={(e) => set('code', e.target.value)} /></div>
              <div className="space-y-1"><Label>Name</Label><Input value={form.name} onChange={(e) => set('name', e.target.value)} /></div>
              <div className="space-y-1"><Label>Contact Email</Label><Input type="email" value={form.contactEmail} onChange={(e) => set('contactEmail', e.target.value)} /></div>
              <div className="space-y-1"><Label>Contact Phone</Label><Input value={form.contactPhone} onChange={(e) => set('contactPhone', e.target.value)} /></div>
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={form.companyOwned} onChange={(e) => set('companyOwned', e.target.checked)} />
              Company-owned carrier
            </label>
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
                {['ID', 'Code', 'Name', 'Type', 'Email', 'Phone'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left font-medium text-gray-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>}
              {carriers?.map((c) => (
                <tr key={c.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500">{c.id}</td>
                  <td className="px-4 py-3 font-mono text-xs">{c.code}</td>
                  <td className="px-4 py-3 font-medium">{c.name}</td>
                  <td className="px-4 py-3"><Badge variant={c.companyOwned ? 'default' : 'secondary'}>{c.companyOwned ? 'Own Fleet' : '3PL'}</Badge></td>
                  <td className="px-4 py-3">{c.contactEmail ?? '—'}</td>
                  <td className="px-4 py-3">{c.contactPhone ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}
