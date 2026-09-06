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

export interface GrantPortalAccessInput {
  email: string
  password: string
}

/**
 * Da de alta un login de portal (role MEMBER) para este socio — Fase 13.
 * Pega contra /api/v1/users (no /api/v1/members) porque el login vive en
 * el módulo auth del backend; memberId es lo que lo vincula a este socio.
 * Si el socio ya tiene un acceso, el backend responde 409 (se muestra
 * como cualquier otro error de API, sin manejo especial acá).
 */
export function grantPortalAccess(memberId: string, input: GrantPortalAccessInput): Promise<void> {
  return apiRequest<void>('/api/v1/users', {
    method: 'POST',
    body: { email: input.email, password: input.password, role: 'MEMBER', memberId },
  })
}
