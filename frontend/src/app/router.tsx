import { createBrowserRouter, Navigate } from 'react-router'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { RequireRole } from '@/routes/RequireRole'
import { AdminLayout } from '@/routes/AdminLayout'
import { DefaultAdminRedirect } from '@/routes/DefaultAdminRedirect'
import { NotFoundPage } from '@/routes/NotFoundPage'
import { LoginPage } from '@/features/auth/LoginPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { MembersListPage } from '@/features/members/MembersListPage'
import { NewMemberPage } from '@/features/members/NewMemberPage'
import { MemberDetailPage } from '@/features/members/MemberDetailPage'
import { PlansPage } from '@/features/plans/PlansPage'
import { AttendancePage } from '@/features/attendance/AttendancePage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/admin" replace /> },
  { path: '/login', element: <LoginPage /> },
  {
    path: '/admin',
    element: <ProtectedRoute />,
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
  { path: '*', element: <NotFoundPage /> },
])
