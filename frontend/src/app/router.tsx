import { createBrowserRouter, Navigate } from 'react-router'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { RequireRole } from '@/routes/RequireRole'
import { AdminLayout } from '@/routes/AdminLayout'
import { PortalLayout } from '@/routes/PortalLayout'
import { DefaultAdminRedirect } from '@/routes/DefaultAdminRedirect'
import { NotFoundPage } from '@/routes/NotFoundPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { MembersListPage } from '@/features/members/MembersListPage'
import { NewMemberPage } from '@/features/members/NewMemberPage'
import { MemberDetailPage } from '@/features/members/MemberDetailPage'
import { PlansPage } from '@/features/plans/PlansPage'
import { AttendancePage } from '@/features/attendance/AttendancePage'
import { PortalProfilePage } from '@/features/portal/PortalProfilePage'
import { PortalMembershipPage } from '@/features/portal/PortalMembershipPage'
import { PortalPaymentsPage } from '@/features/portal/PortalPaymentsPage'
import { PortalAttendancePage } from '@/features/portal/PortalAttendancePage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/admin" replace /> },
  { path: '/login', element: <LoginPage /> },
  {
    path: '/admin',
    element: <ProtectedRoute />,
    children: [
      {
        // MEMBER no tiene nada que hacer en el portal administrativo — lo manda a /portal en vez de mostrarle
        // una pantalla que el backend le va a rechazar con 403 igual.
        element: <RequireRole roles={['GYM_ADMIN', 'TRAINER']} redirectTo="/portal" />,
        children: [
          {
            element: <AdminLayout />,
            children: [
              { index: true, element: <DefaultAdminRedirect /> },
              {
                element: <RequireRole roles={['GYM_ADMIN']} />,
                children: [
                  { path: 'dashboard', element: <DashboardPage /> },
                  { path: 'members/new', element: <NewMemberPage /> },
                ],
              },
              { path: 'members', element: <MembersListPage /> },
              { path: 'members/:memberId', element: <MemberDetailPage /> },
              { path: 'plans', element: <PlansPage /> },
              { path: 'attendance', element: <AttendancePage /> },
            ],
          },
        ],
      },
    ],
  },
  {
    path: '/portal',
    element: <ProtectedRoute />,
    children: [
      {
        element: <RequireRole roles={['MEMBER']} redirectTo="/admin" />,
        children: [
          {
            element: <PortalLayout />,
            children: [
              { index: true, element: <Navigate to="/portal/profile" replace /> },
              { path: 'profile', element: <PortalProfilePage /> },
              { path: 'membership', element: <PortalMembershipPage /> },
              { path: 'payments', element: <PortalPaymentsPage /> },
              { path: 'attendance', element: <PortalAttendancePage /> },
            ],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
