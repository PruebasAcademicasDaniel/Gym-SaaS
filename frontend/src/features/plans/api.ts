import { apiRequest } from '@/shared/api/httpClient'

export interface Plan {
  id: string
  name: string
  description: string | null
  price: number
  durationDays: number
  active: boolean
  createdAt: string
}

export interface PlanInput {
  name: string
  description?: string
  price: number
  durationDays: number
}

export function listPlans(): Promise<Plan[]> {
  return apiRequest<Plan[]>('/api/v1/plans')
}

export function createPlan(input: PlanInput): Promise<Plan> {
  return apiRequest<Plan>('/api/v1/plans', { method: 'POST', body: input })
}

export function deactivatePlan(id: string): Promise<void> {
  return apiRequest<void>(`/api/v1/plans/${id}/deactivate`, { method: 'PATCH' })
}
