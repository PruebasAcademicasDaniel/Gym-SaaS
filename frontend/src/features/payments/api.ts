import { apiRequest } from '@/shared/api/httpClient'

export type PaymentMethod = 'CASH' | 'CARD' | 'TRANSFER' | 'OTHER'

export interface Payment {
  id: string
  membershipId: string
  amount: number
  method: PaymentMethod
  paymentDate: string
  createdAt: string
}

export interface PaymentInput {
  membershipId: string
  amount: number
  method: PaymentMethod
}

export function listPaymentsByMembership(membershipId: string): Promise<Payment[]> {
  return apiRequest<Payment[]>(`/api/v1/memberships/${membershipId}/payments`)
}

export function registerPayment(input: PaymentInput): Promise<Payment> {
  return apiRequest<Payment>('/api/v1/payments', { method: 'POST', body: input })
}
