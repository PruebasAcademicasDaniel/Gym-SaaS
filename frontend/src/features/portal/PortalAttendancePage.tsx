import { useQuery } from '@tanstack/react-query'
import { getMyAttendance } from './api'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { EmptyState } from '@/shared/ui/EmptyState'
import { formatDateTime } from '@/shared/ui/formatters'

export function PortalAttendancePage() {
  const { data, isPending, error } = useQuery({ queryKey: ['portal', 'attendance'], queryFn: getMyAttendance })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Mi asistencia" description="Historial de check-ins registrados por el gimnasio." />

      <Card className="p-5">
        {isPending && <Spinner />}
        {error && <ErrorBanner error={error} />}
        {data && data.length === 0 && <EmptyState title="Sin check-ins registrados" />}
        {data && data.length > 0 && (
          <ul className="flex flex-col gap-1.5 text-sm text-stone-600">
            {data.map((entry) => (
              <li key={entry.id}>{formatDateTime(entry.checkedInAt)}</li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
