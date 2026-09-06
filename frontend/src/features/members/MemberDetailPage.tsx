import { useState } from 'react'
import { useParams, useNavigate } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getMember, updateMember, deactivateMember } from './api'
import { useAuth } from '@/shared/auth/AuthContext'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { Badge } from '@/shared/ui/Badge'
import { Button } from '@/shared/ui/Button'
import { TextField } from '@/shared/ui/TextField'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { MembershipsSection } from '@/features/memberships/MembershipsSection'
import { AttendanceSection } from '@/features/attendance/AttendanceSection'

export function MemberDetailPage() {
  const { memberId } = useParams<{ memberId: string }>()
  const { user } = useAuth()
  const canManage = user?.role === 'GYM_ADMIN'
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '' })

  const memberQuery = useQuery({
    queryKey: ['member', memberId],
    queryFn: () => getMember(memberId as string),
    enabled: !!memberId,
  })

  function startEditing() {
    if (!memberQuery.data) return
    setForm({
      firstName: memberQuery.data.firstName,
      lastName: memberQuery.data.lastName,
      email: memberQuery.data.email ?? '',
      phone: memberQuery.data.phone ?? '',
    })
    setEditing(true)
  }

  const updateMutation = useMutation({
    mutationFn: () =>
      updateMember(memberId as string, {
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email || undefined,
        phone: form.phone || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['member', memberId] })
      queryClient.invalidateQueries({ queryKey: ['members'] })
      setEditing(false)
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: () => deactivateMember(memberId as string),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['member', memberId] })
      queryClient.invalidateQueries({ queryKey: ['members'] })
    },
  })

  if (memberQuery.isPending) return <Spinner />
  if (memberQuery.error) return <ErrorBanner error={memberQuery.error} />

  const member = memberQuery.data
  if (!member) return null

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={`${member.firstName} ${member.lastName}`}
        actions={
          <>
            <Button variant="secondary" onClick={() => navigate('/admin/members')}>
              Volver
            </Button>
            {canManage && !editing && (
              <Button variant="secondary" onClick={startEditing}>
                Editar
              </Button>
            )}
            {canManage && member.active && (
              <Button variant="danger" onClick={() => deactivateMutation.mutate()} disabled={deactivateMutation.isPending}>
                Dar de baja
              </Button>
            )}
          </>
        }
      />

      <Card className="max-w-xl p-5">
        <div className="mb-3">
          <Badge tone={member.active ? 'success' : 'neutral'}>{member.active ? 'Activo' : 'Inactivo'}</Badge>
        </div>

        {!editing ? (
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-stone-500">Email</dt>
              <dd className="text-stone-900">{member.email ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-stone-500">Teléfono</dt>
              <dd className="text-stone-900">{member.phone ?? '—'}</dd>
            </div>
          </dl>
        ) : (
          <form
            className="flex flex-col gap-4"
            onSubmit={(event) => {
              event.preventDefault()
              updateMutation.mutate()
            }}
          >
            <div className="grid grid-cols-2 gap-3">
              <TextField
                label="Nombre"
                required
                value={form.firstName}
                onChange={(event) => setForm({ ...form, firstName: event.target.value })}
              />
              <TextField
                label="Apellido"
                required
                value={form.lastName}
                onChange={(event) => setForm({ ...form, lastName: event.target.value })}
              />
            </div>
            <TextField label="Email" type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
            <TextField label="Teléfono" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
            {updateMutation.isError && <ErrorBanner error={updateMutation.error} />}
            <div className="flex gap-2">
              <Button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending ? 'Guardando…' : 'Guardar'}
              </Button>
              <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
                Cancelar
              </Button>
            </div>
          </form>
        )}
        {deactivateMutation.isError && (
          <div className="mt-3">
            <ErrorBanner error={deactivateMutation.error} />
          </div>
        )}
      </Card>

      <MembershipsSection memberId={member.id} />
      <AttendanceSection memberId={member.id} />
    </div>
  )
}
