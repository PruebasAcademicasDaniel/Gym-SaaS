import { useQuery } from '@tanstack/react-query'
import { getMyProfile } from './api'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { Badge } from '@/shared/ui/Badge'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'

export function PortalProfilePage() {
  const { data, isPending, error } = useQuery({ queryKey: ['portal', 'me'], queryFn: getMyProfile })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Mi perfil" description="Tus datos como socio de este gimnasio." />

      {isPending && <Spinner />}
      {error && <ErrorBanner error={error} />}
      {data && (
        <Card className="max-w-xl p-5">
          <div className="mb-3">
            <Badge tone={data.active ? 'success' : 'neutral'}>{data.active ? 'Activo' : 'Inactivo'}</Badge>
          </div>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div>
              <dt className="text-stone-500">Nombre</dt>
              <dd className="text-stone-900">
                {data.firstName} {data.lastName}
              </dd>
            </div>
            <div>
              <dt className="text-stone-500">Email</dt>
              <dd className="text-stone-900">{data.email ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-stone-500">Teléfono</dt>
              <dd className="text-stone-900">{data.phone ?? '—'}</dd>
            </div>
          </dl>
        </Card>
      )}
    </div>
  )
}
