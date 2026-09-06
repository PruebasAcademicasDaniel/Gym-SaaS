import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { checkIn, listAttendanceByMember } from './api'
import { Card } from '@/shared/ui/Card'
import { Button } from '@/shared/ui/Button'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { EmptyState } from '@/shared/ui/EmptyState'
import { formatDateTime } from '@/shared/ui/formatters'

/** GYM_ADMIN y TRAINER pueden registrar check-ins — el único módulo donde TRAINER escribe, no solo lee (Fase 9). */
export function AttendanceSection({ memberId }: { memberId: string }) {
  const queryClient = useQueryClient()
  const attendanceQuery = useQuery({
    queryKey: ['attendance', memberId],
    queryFn: () => listAttendanceByMember(memberId),
  })
  const checkInMutation = useMutation({
    mutationFn: () => checkIn(memberId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['attendance', memberId] }),
  })

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-base font-semibold text-stone-900">Asistencia</h2>
        <Button onClick={() => checkInMutation.mutate()} disabled={checkInMutation.isPending}>
          {checkInMutation.isPending ? 'Registrando…' : 'Registrar asistencia ahora'}
        </Button>
      </div>
      {checkInMutation.isError && (
        <div className="mt-3">
          <ErrorBanner error={checkInMutation.error} />
        </div>
      )}
      <div className="mt-4">
        {attendanceQuery.isPending && <Spinner />}
        {attendanceQuery.error && <ErrorBanner error={attendanceQuery.error} />}
        {attendanceQuery.data && attendanceQuery.data.length === 0 && <EmptyState title="Sin check-ins registrados" />}
        {attendanceQuery.data && attendanceQuery.data.length > 0 && (
          <ul className="flex max-h-56 flex-col gap-1.5 overflow-y-auto text-sm text-stone-600">
            {attendanceQuery.data.map((entry) => (
              <li key={entry.id}>{formatDateTime(entry.checkedInAt)}</li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  )
}
