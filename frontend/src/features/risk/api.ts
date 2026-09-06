import { apiRequest } from '@/shared/api/httpClient'

export interface AtRiskMember {
  id: string
  firstName: string
  lastName: string
  email: string | null
  phone: string | null
  lastActivity: string
}

export function listAtRiskMembers(): Promise<AtRiskMember[]> {
  return apiRequest<AtRiskMember[]>('/api/v1/risk/members')
}
