import { apiRequest } from '@/shared/api/httpClient'

export interface SendRemindersResult {
  sent: number
}

export function sendExpirationReminders(): Promise<SendRemindersResult> {
  return apiRequest<SendRemindersResult>('/api/v1/notifications/expiration-reminders', { method: 'POST' })
}
