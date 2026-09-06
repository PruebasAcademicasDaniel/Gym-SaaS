import { Navigate, Outlet } from 'react-router'
import { useAuth } from '@/shared/auth/AuthContext'
import type { Role } from '@/shared/api/types'

/** El backend es la autoridad real (@PreAuthorize) — esto solo evita mostrarle a alguien una pantalla que igual le va a devolver 403. */
export function RequireRole({ roles, redirectTo = '/admin' }: { roles: Role[]; redirectTo?: string }) {
  const { user } = useAuth()

  if (!user || !roles.includes(user.role)) {
    return <Navigate to={redirectTo} replace />
  }

  return <Outlet />
}
