import { apiRequest } from '@/shared/api/httpClient'

export type MembershipStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED'

export interface Membership {
  id: string
  memberId: string
  planId: string
  startDate: string
  endDate: string
  status: MembershipStatus
  createdAt: string
}

export function listMembershipsByMember(memberId: string): Promise<Membership[]> {
  return apiRequest<Membership[]>(`/api/v1/members/${memberId}/memberships`)
}

export function createMembership(memberId: string, planId: string): Promise<Membership> {
  return apiRequest<Membership>('/api/v1/memberships', { method: 'POST', body: { memberId, planId } })
}

export function cancelMembership(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/memberships/${id}/cancel`, { method: 'PATCH' })
}
