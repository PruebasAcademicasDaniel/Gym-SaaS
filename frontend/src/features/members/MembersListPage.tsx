import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listMembers } from './api'
import { useAuth } from '@/shared/auth/AuthContext'
import { PageHeader } from '@/shared/ui/PageHeader'
import { buttonClasses } from '@/shared/ui/Button'
import { Card } from '@/shared/ui/Card'
import { Badge } from '@/shared/ui/Badge'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { EmptyState } from '@/shared/ui/EmptyState'
import { TextField } from '@/shared/ui/TextField'

export function MembersListPage() {
  const { user } = useAuth()
  const { data, isPending, error } = useQuery({ queryKey: ['members'], queryFn: listMembers })
  const [search, setSearch] = useState('')

  const filtered = useMemo(() => {
    if (!data) return []
    const term = search.trim().toLowerCase()
    if (!term) return data
    return data.filter((member) => `${member.firstName} ${member.lastName}`.toLowerCase().includes(term))
  }, [data, search])

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Socios"
        description="Altas, edición y baja de socios del gimnasio."
        actions={
          user?.role === 'GYM_ADMIN' ? (
            <Link to="/admin/members/new" className={buttonClasses('primary')}>
              Nuevo socio
            </Link>
          ) : undefined
        }
      />

      <Card>
        <div className="border-b border-stone-200 p-4">
          <TextField
            label="Buscar"
            placeholder="Nombre o apellido…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        {isPending && (
          <div className="p-6">
            <Spinner />
          </div>
        )}
        {error && (
          <div className="p-4">
            <ErrorBanner error={error} />
          </div>
        )}
        {data && filtered.length === 0 && (
          <div className="p-4">
            <EmptyState
              title="No hay socios"
              description={search ? 'Ningún socio coincide con la búsqueda.' : 'Todavía no se dio de alta ningún socio.'}
            />
          </div>
        )}
        {filtered.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-stone-50 text-left text-xs font-medium uppercase tracking-wide text-stone-500">
                <tr>
                  <th className="px-4 py-2.5">Nombre</th>
                  <th className="px-4 py-2.5">Email</th>
                  <th className="px-4 py-2.5">Teléfono</th>
                  <th className="px-4 py-2.5">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {filtered.map((member) => (
                  <tr key={member.id} className="hover:bg-stone-50">
                    <td className="px-4 py-2.5">
                      <Link to={`/admin/members/${member.id}`} className="font-medium text-emerald-700 hover:underline">
                        {member.firstName} {member.lastName}
                      </Link>
                    </td>
                    <td className="px-4 py-2.5 text-stone-600">{member.email ?? '—'}</td>
                    <td className="px-4 py-2.5 text-stone-600">{member.phone ?? '—'}</td>
                    <td className="px-4 py-2.5">
                      <Badge tone={member.active ? 'success' : 'neutral'}>{member.active ? 'Activo' : 'Inactivo'}</Badge>
                    </td>
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
