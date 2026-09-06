import { useQuery } from '@tanstack/react-query'
import { getMyMemberships } from './api'
import type { MembershipStatus } from '@/features/memberships/api'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { Badge } from '@/shared/ui/Badge'
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

export function PortalMembershipPage() {
  const { data, isPending, error } = useQuery({ queryKey: ['portal', 'memberships'], queryFn: getMyMemberships })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Mi membresía" description="Historial completo de tus planes contratados." />

      <Card className="p-5">
        {isPending && <Spinner />}
        {error && <ErrorBanner error={error} />}
        {data && data.length === 0 && (
          <EmptyState title="Sin membresías" description="Todavía no contrataste ningún plan." />
        )}
        {data && data.length > 0 && (
          <ul className="flex flex-col gap-3">
            {data.map((membership) => (
              <li key={membership.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-stone-200 p-3">
                <div>
                  <p className="text-sm font-medium text-stone-900">{membership.planName}</p>
                  <p className="text-xs text-stone-500">
                    {formatDate(membership.startDate)} — {formatDate(membership.endDate)}
                  </p>
                </div>
                <Badge tone={statusTone[membership.status]}>{statusLabel[membership.status]}</Badge>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
