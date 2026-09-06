import { apiRequest } from '@/shared/api/httpClient'

export interface SendRemindersResult {
  sent: number
}

export function sendExpirationReminders(): Promise<SendRemindersResult> {
  return apiRequest<SendRemindersResult>('/api/v1/notifications/expiration-reminders', { method: 'POST' })
}

export function sendRiskAlerts(): Promise<SendRemindersResult> {
  return apiRequest<SendRemindersResult>('/api/v1/notifications/risk-alerts', { method: 'POST' })
}
