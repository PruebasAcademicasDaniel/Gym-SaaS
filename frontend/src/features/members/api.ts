import { apiRequest } from '@/shared/api/httpClient'

export interface Member {
  id: string
  firstName: string
  lastName: string
  email: string | null
  phone: string | null
  active: boolean
  createdAt: string
}

export interface MemberInput {
  firstName: string
  lastName: string
  email?: string
  phone?: string
}

export function listMembers(): Promise<Member[]> {
  return apiRequest<Member[]>('/api/v1/members')
}

export function getMember(id: string): Promise<Member> {
  return apiRequest<Member>(`/api/v1/members/${id}`)
}

export function createMember(input: MemberInput): Promise<Member> {
  return apiRequest<Member>('/api/v1/members', { method: 'POST', body: input })
}

export function updateMember(id: string, input: MemberInput): Promise<Member> {
  return apiRequest<Member>(`/api/v1/members/${id}`, { method: 'PATCH', body: input })
}

export function deactivateMember(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/members/${id}/deactivate`, { method: 'PATCH' })
}
