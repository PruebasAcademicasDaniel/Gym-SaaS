import { useQuery } from '@tanstack/react-query'
import { getDashboardSummary } from './api'
import { Card } from '@/shared/ui/Card'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { formatCurrency } from '@/shared/ui/formatters'

export function DashboardPage() {
  const { data, isPending, error } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboardSummary })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Dashboard" description="Estado general del gimnasio." />
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
