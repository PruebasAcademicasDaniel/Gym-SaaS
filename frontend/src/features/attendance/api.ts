import { apiRequest } from '@/shared/api/httpClient'

export interface Attendance {
  id: string
  memberId: string
  checkedInAt: string
}

export function listAttendanceByMember(memberId: string): Promise<Attendance[]> {
  return apiRequest<Attendance[]>(`/api/v1/members/${memberId}/attendance`)
}

export function checkIn(memberId: string): Promise<Attendance> {
  return apiRequest<Attendance>('/api/v1/attendance', { method: 'POST', body: { memberId } })
}
