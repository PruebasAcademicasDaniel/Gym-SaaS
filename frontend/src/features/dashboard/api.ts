import { apiRequest } from '@/shared/api/httpClient'

export interface DashboardSummary {
  activeMembers: number
  membershipsExpiringSoon: number
  revenueThisMonth: number
}

export function getDashboardSummary(): Promise<DashboardSummary> {
  return apiRequest<DashboardSummary>('/api/v1/dashboard')
}
