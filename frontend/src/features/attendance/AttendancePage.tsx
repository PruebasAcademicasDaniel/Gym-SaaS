import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listMembers } from '@/features/members/api'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { SelectField } from '@/shared/ui/SelectField'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { AttendanceSection } from './AttendanceSection'

export function AttendancePage() {
  const membersQuery = useQuery({ queryKey: ['members'], queryFn: listMembers })
  const [selectedMemberId, setSelectedMemberId] = useState('')

  const activeMembers = membersQuery.data?.filter((member) => member.active) ?? []

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Asistencia" description="Check-in rápido de un socio." />

      <Card className="max-w-md p-5">
        {membersQuery.isPending && <Spinner />}
        {membersQuery.error && <ErrorBanner error={membersQuery.error} />}
        {membersQuery.data && (
          <SelectField label="Socio" value={selectedMemberId} onChange={(event) => setSelectedMemberId(event.target.value)}>
            <option value="">Elegir socio…</option>
            {activeMembers.map((member) => (
              <option key={member.id} value={member.id}>
                {member.firstName} {member.lastName}
              </option>
            ))}
          </SelectField>
        )}
      </Card>

      {selectedMemberId && <AttendanceSection memberId={selectedMemberId} />}
    </div>
  )
}
