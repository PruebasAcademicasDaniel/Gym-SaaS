import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listPlans, createPlan, deactivatePlan } from './api'
import { useAuth } from '@/shared/auth/AuthContext'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { TextField } from '@/shared/ui/TextField'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { EmptyState } from '@/shared/ui/EmptyState'
import { formatCurrency } from '@/shared/ui/formatters'

export function PlansPage() {
  const { user } = useAuth()
  const canManage = user?.role === 'GYM_ADMIN'
  const queryClient = useQueryClient()
  const plansQuery = useQuery({ queryKey: ['plans'], queryFn: listPlans })

  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [price, setPrice] = useState('')
  const [durationDays, setDurationDays] = useState('30')

  const createMutation = useMutation({
    mutationFn: () =>
      createPlan({ name, description: description || undefined, price: Number(price), durationDays: Number(durationDays) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plans'] })
      setName('')
      setDescription('')
      setPrice('')
      setDurationDays('30')
      setShowForm(false)
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deactivatePlan(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['plans'] }),
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    createMutation.mutate()
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Planes"
        description="Catálogo de planes del gimnasio."
        actions={
          canManage ? (
            <Button onClick={() => setShowForm((value) => !value)} variant={showForm ? 'secondary' : 'primary'}>
              {showForm ? 'Cancelar' : 'Nuevo plan'}
            </Button>
          ) : undefined
        }
      />

      {canManage && showForm && (
        <Card className="max-w-lg p-5">
          <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
            <TextField label="Nombre" required value={name} onChange={(event) => setName(event.target.value)} />
            <TextField label="Descripción" value={description} onChange={(event) => setDescription(event.target.value)} />
            <div className="grid grid-cols-2 gap-3">
              <TextField
                label="Precio"
                type="number"
                min="0.01"
                step="0.01"
                required
                value={price}
                onChange={(event) => setPrice(event.target.value)}
              />
              <TextField
                label="Duración (días)"
                type="number"
                min="1"
                required
                value={durationDays}
                onChange={(event) => setDurationDays(event.target.value)}
              />
            </div>
            {createMutation.isError && <ErrorBanner error={createMutation.error} />}
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Guardando…' : 'Guardar'}
            </Button>
          </form>
        </Card>
      )}

      <Card>
        {plansQuery.isPending && (
          <div className="p-6">
            <Spinner />
          </div>
        )}
        {plansQuery.error && (
          <div className="p-4">
            <ErrorBanner error={plansQuery.error} />
          </div>
        )}
        {plansQuery.data && plansQuery.data.length === 0 && (
          <div className="p-4">
            <EmptyState title="Sin planes" description="Todavía no se creó ningún plan." />
          </div>
        )}
        {plansQuery.data && plansQuery.data.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-stone-50 text-left text-xs font-medium uppercase tracking-wide text-stone-500">
                <tr>
                  <th className="px-4 py-2.5">Nombre</th>
                  <th className="px-4 py-2.5">Precio</th>
                  <th className="px-4 py-2.5">Duración</th>
                  <th className="px-4 py-2.5">Estado</th>
                  {canManage && <th className="px-4 py-2.5" />}
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {plansQuery.data.map((plan) => (
                  <tr key={plan.id}>
                    <td className="px-4 py-2.5">
                      <p className="font-medium text-stone-900">{plan.name}</p>
                      {plan.description && <p className="text-xs text-stone-500">{plan.description}</p>}
                    </td>
                    <td className="px-4 py-2.5 text-stone-600">{formatCurrency(plan.price)}</td>
                    <td className="px-4 py-2.5 text-stone-600">{plan.durationDays} días</td>
                    <td className="px-4 py-2.5">
                      <Badge tone={plan.active ? 'success' : 'neutral'}>{plan.active ? 'Activo' : 'Inactivo'}</Badge>
                    </td>
                    {canManage && (
                      <td className="px-4 py-2.5 text-right">
                        {plan.active && (
                          <Button
                            variant="ghost"
                            onClick={() => deactivateMutation.mutate(plan.id)}
                            disabled={deactivateMutation.isPending}
                          >
                            Desactivar
                          </Button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
