import { NavLink, Outlet } from 'react-router'
import { useAuth } from '@/shared/auth/AuthContext'
import { Button } from '@/shared/ui/Button'

function navItemClass({ isActive }: { isActive: boolean }) {
  return `rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-emerald-50 text-emerald-700' : 'text-stone-600 hover:bg-stone-100'
  }`
}

export function AdminLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-stone-50">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <div className="flex flex-wrap items-center gap-6">
            <span className="text-sm font-semibold uppercase tracking-wider text-emerald-700">GymFlow</span>
            <nav className="flex flex-wrap gap-1">
              {user?.role === 'GYM_ADMIN' && (
                <NavLink to="/admin/dashboard" className={navItemClass}>
                  Dashboard
                </NavLink>
              )}
              <NavLink to="/admin/members" className={navItemClass}>
                Socios
              </NavLink>
              <NavLink to="/admin/plans" className={navItemClass}>
                Planes
              </NavLink>
              <NavLink to="/admin/attendance" className={navItemClass}>
                Asistencia
              </NavLink>
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-stone-500">{user?.email}</span>
            <Button variant="secondary" onClick={logout}>
              Cerrar sesión
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
