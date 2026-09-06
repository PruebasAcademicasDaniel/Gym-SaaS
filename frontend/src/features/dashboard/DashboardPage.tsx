import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { getDashboardSummary } from './api'
import { sendExpirationReminders } from '@/features/notifications/api'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Button } from '@/shared/ui/Button'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { formatCurrency } from '@/shared/ui/formatters'

export function DashboardPage() {
  const { data, isPending, error } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboardSummary })
  const [lastResult, setLastResult] = useState<number | null>(null)

  const remindersMutation = useMutation({
    mutationFn: sendExpirationReminders,
    onSuccess: (result) => setLastResult(result.sent),
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
        <div className="grid gap-4 sm:grid-cols-3">
          <StatCard label="Socios activos" value={data.activeMembers.toString()} />
          <StatCard label="Vencen en 7 días" value={data.membershipsExpiringSoon.toString()} />
          <StatCard label="Ingresos del mes" value={formatCurrency(data.revenueThisMonth)} />
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <Card className="p-5">
      <p className="text-sm text-stone-500">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-stone-900">{value}</p>
    </Card>
  )
}
