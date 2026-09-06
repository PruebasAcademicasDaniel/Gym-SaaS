import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { getDashboardSummary } from './api'
import { listAtRiskMembers } from '@/features/risk/api'
import { sendExpirationReminders, sendRiskAlerts } from '@/features/notifications/api'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Button } from '@/shared/ui/Button'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { formatCurrency, formatDate } from '@/shared/ui/formatters'

export function DashboardPage() {
  const { data, isPending, error } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboardSummary })
  const riskQuery = useQuery({ queryKey: ['risk', 'members'], queryFn: listAtRiskMembers })
  const [lastResult, setLastResult] = useState<number | null>(null)

  const [lastRiskResult, setLastRiskResult] = useState<number | null>(null)

  const remindersMutation = useMutation({
    mutationFn: sendExpirationReminders,
    onSuccess: (result) => setLastResult(result.sent),
  })

  const riskAlertsMutation = useMutation({
    mutationFn: sendRiskAlerts,
    onSuccess: (result) => setLastRiskResult(result.sent),
  })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Dashboard"
        description="Estado general del gimnasio."
        actions={
          <Button variant="secondary" onClick={() => remindersMutation.mutate()} disabled={remindersMutation.isPending}>
            {remindersMutation.isPending ? 'Enviando…' : 'Enviar recordatorios de vencimiento'}
          </Button>
        }
      />

      {remindersMutation.isError && <ErrorBanner error={remindersMutation.error} />}
      {lastResult !== null && !remindersMutation.isPending && (
        <p className="text-sm text-stone-500">
          {lastResult === 0
            ? 'No había recordatorios pendientes de enviar.'
            : `Se enviaron ${lastResult} recordatorio${lastResult === 1 ? '' : 's'}.`}
        </p>
      )}

      {isPending && <Spinner />}
      {error && <ErrorBanner error={error} />}
      {data && (
        <div className="grid gap-4 sm:grid-cols-4">
          <StatCard label="Socios activos" value={data.activeMembers.toString()} />
          <StatCard label="Vencen en 7 días" value={data.membershipsExpiringSoon.toString()} />
          <StatCard label="Ingresos del mes" value={formatCurrency(data.revenueThisMonth)} />
          <StatCard label="Clientes en riesgo" value={data.membersAtRisk.toString()} warn={data.membersAtRisk > 0} />
        </div>
      )}

      {riskQuery.data && riskQuery.data.length > 0 && (
        <Card className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold text-stone-900">Clientes en riesgo</h2>
              <p className="mt-1 text-sm text-stone-500">
                Socios con membresía activa que no asisten hace varios días — vale la pena contactarlos antes de que
                no renueven.
              </p>
            </div>
            <Button variant="secondary" onClick={() => riskAlertsMutation.mutate()} disabled={riskAlertsMutation.isPending}>
              {riskAlertsMutation.isPending ? 'Enviando…' : 'Enviar alerta por email'}
            </Button>
          </div>
          {riskAlertsMutation.isError && (
            <div className="mt-3">
              <ErrorBanner error={riskAlertsMutation.error} />
            </div>
          )}
          {lastRiskResult !== null && !riskAlertsMutation.isPending && (
            <p className="mt-3 text-sm text-stone-500">
              {lastRiskResult === 0
                ? 'No había alertas pendientes de enviar (ya se habían notificado antes).'
                : `Se enviaron ${lastRiskResult} alerta${lastRiskResult === 1 ? '' : 's'}.`}
            </p>
          )}
          <ul className="mt-4 flex flex-col gap-2">
            {riskQuery.data.map((member) => (
              <li key={member.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-amber-200 bg-amber-50 px-3.5 py-2.5">
                <Link to={`/admin/members/${member.id}`} className="text-sm font-medium text-emerald-700 hover:underline">
                  {member.firstName} {member.lastName}
                </Link>
                <span className="text-xs text-stone-600">Última actividad: {formatDate(member.lastActivity)}</span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}

function StatCard({ label, value, warn = false }: { label: string; value: string; warn?: boolean }) {
  return (
    <Card className="p-5">
      <p className="text-sm text-stone-500">{label}</p>
      <p className={`mt-2 text-2xl font-semibold ${warn ? 'text-amber-600' : 'text-stone-900'}`}>{value}</p>
    </Card>
  )
}
