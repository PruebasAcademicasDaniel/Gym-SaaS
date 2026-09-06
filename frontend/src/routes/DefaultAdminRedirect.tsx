import { Navigate } from 'react-router'
import { useAuth } from '@/shared/auth/AuthContext'

/** GYM_ADMIN aterriza en el dashboard; TRAINER no tiene acceso ahí (Fase 10), así que aterriza en Socios. */
export function DefaultAdminRedirect() {
  const { user } = useAuth()
  const target = user?.role === 'GYM_ADMIN' ? '/admin/dashboard' : '/admin/members'
  return <Navigate to={target} replace />
}
