import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listMembershipsByMember, createMembership, cancelMembership, type MembershipStatus } from './api'
import { listPlans } from '@/features/plans/api'
import { PaymentsSection } from '@/features/payments/PaymentsSection'
import { useAuth } from '@/shared/auth/AuthContext'
import { Card } from '@/shared/ui/Card'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { SelectField } from '@/shared/ui/SelectField'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { EmptyState } from '@/shared/ui/EmptyState'
import { formatDate } from '@/shared/ui/formatters'

const statusTone: Record<MembershipStatus, 'success' | 'warning' | 'neutral'> = {
  ACTIVE: 'success',
  EXPIRED: 'warning',
  CANCELLED: 'neutral',
}

const statusLabel: Record<MembershipStatus, string> = {
  ACTIVE: 'Activa',
  EXPIRED: 'Vencida',
  CANCELLED: 'Cancelada',
}

export function MembershipsSection({ memberId }: { memberId: string }) {
  const { user } = useAuth()
  const canManage = user?.role === 'GYM_ADMIN'
  const queryClient = useQueryClient()
  const [selectedPlanId, setSelectedPlanId] = useState('')
  const [expandedId, setExpandedId] = useState<string | null>(null)

  const membershipsQuery = useQuery({
    queryKey: ['memberships', memberId],
    queryFn: () => listMembershipsByMember(memberId),
  })
  // Sin gate por rol: TRAINER también puede leer /api/v1/plans (para ver el nombre del plan en cada membresía), solo no puede contratar.
  const plansQuery = useQuery({ queryKey: ['plans'], queryFn: listPlans })

  const contractMutation = useMutation({
    mutationFn: () => createMembership(memberId, selectedPlanId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['memberships', memberId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      setSelectedPlanId('')
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (id: string) => cancelMembership(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['memberships', memberId] }),
  })

  const activePlans = plansQuery.data?.filter((plan) => plan.active) ?? []
  const planName = (planId: string) => plansQuery.data?.find((plan) => plan.id === planId)?.name ?? planId

  return (
    <Card className="p-5">
      <h2 className="text-base font-semibold text-stone-900">Membresías</h2>

      {canManage && (
        <form
          className="mt-4 flex flex-wrap items-end gap-3"
          onSubmit={(event) => {
            event.preventDefault()
            if (selectedPlanId) contractMutation.mutate()
          }}
        >
          <div className="min-w-[200px]">
            <SelectField label="Contratar plan" value={selectedPlanId} onChange={(event) => setSelectedPlanId(event.target.value)}>
              <option value="">Elegir plan…</option>
              {activePlans.map((plan) => (
                <option key={plan.id} value={plan.id}>
                  {plan.name}
                </option>
              ))}
            </SelectField>
          </div>
          <Button type="submit" disabled={!selectedPlanId || contractMutation.isPending}>
            {contractMutation.isPending ? 'Contratando…' : 'Contratar'}
          </Button>
        </form>
      )}
      {contractMutation.isError && (
        <div className="mt-3">
          <ErrorBanner error={contractMutation.error} />
        </div>
      )}

      <div className="mt-4">
        {membershipsQuery.isPending && <Spinner />}
        {membershipsQuery.error && <ErrorBanner error={membershipsQuery.error} />}
        {membershipsQuery.data && membershipsQuery.data.length === 0 && (
          <EmptyState title="Sin membresías" description="Este socio todavía no contrató ningún plan." />
        )}
        <ul className="flex flex-col gap-3">
          {membershipsQuery.data?.map((membership) => (
            <li key={membership.id} className="rounded-md border border-stone-200 p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="text-sm font-medium text-stone-900">{planName(membership.planId)}</p>
                  <p className="text-xs text-stone-500">
                    {formatDate(membership.startDate)} — {formatDate(membership.endDate)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge tone={statusTone[membership.status]}>{statusLabel[membership.status]}</Badge>
                  {canManage && (
                    <Button
                      variant="ghost"
                      onClick={() => setExpandedId((id) => (id === membership.id ? null : membership.id))}
                    >
                      Pagos
                    </Button>
                  )}
                  {canManage && membership.status === 'ACTIVE' && (
                    <Button
                      variant="danger"
                      disabled={cancelMutation.isPending}
                      onClick={() => cancelMutation.mutate(membership.id)}
                    >
                      Cancelar
                    </Button>
                  )}
                </div>
              </div>
              {canManage && expandedId === membership.id && (
                <div className="mt-3 border-t border-stone-100 pt-3">
                  <PaymentsSection membershipId={membership.id} />
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>
    </Card>
  )
}
