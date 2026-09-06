import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '@/shared/auth/AuthContext'
import { FullPageSpinner } from '@/shared/ui/Spinner'

export function ProtectedRoute() {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') return <FullPageSpinner />
  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}
