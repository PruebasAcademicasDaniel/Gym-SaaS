import { apiRequest } from '@/shared/api/httpClient'
import type { Member } from '@/features/members/api'
import type { Payment } from '@/features/payments/api'
import type { Attendance } from '@/features/attendance/api'
import type { MembershipStatus } from '@/features/memberships/api'

/**
 * Distinto de MembershipResponse (la del admin): trae planName en vez de
 * planId, porque un MEMBER no tiene acceso a GET /api/v1/plans para
 * resolverlo del lado del cliente — ver PortalMembershipResponse en el
 * backend.
 */
export interface PortalMembership {
  id: string
  planName: string
  startDate: string
  endDate: string
  status: MembershipStatus
}

/** Ninguna de estas rutas toma un id: el backend siempre resuelve "el socio actual" a partir del JWT, nunca de un parámetro. */
export function getMyProfile(): Promise<Member> {
  return apiRequest<Member>('/api/v1/portal/me')
}

export function getMyMemberships(): Promise<PortalMembership[]> {
  return apiRequest<PortalMembership[]>('/api/v1/portal/memberships')
}

export function getMyPayments(): Promise<Payment[]> {
  return apiRequest<Payment[]>('/api/v1/portal/payments')
}

export function getMyAttendance(): Promise<Attendance[]> {
  return apiRequest<Attendance[]>('/api/v1/portal/attendance')
}
