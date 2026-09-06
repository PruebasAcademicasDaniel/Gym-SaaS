import { apiRequest } from '@/shared/api/httpClient'
import type { Role } from '@/shared/api/types'

/**
 * Vive en shared, no en features/auth, porque AuthContext (compartido por
 * toda la app) lo necesita — features/auth solo tiene la pantalla de
 * login, que consume el contexto, no esto directamente.
 */
export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface MeResponse {
  userId: string
  email: string
  role: Role
  gymId: string | null
}

export function login(email: string, password: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: { email, password },
    skipAuth: true,
  })
}

export function me(): Promise<MeResponse> {
  return apiRequest<MeResponse>('/api/v1/auth/me')
}

export function logout(refreshToken: string): Promise<void> {
  return apiRequest<void>('/api/v1/auth/logout', {
    method: 'POST',
    body: { refreshToken },
    skipAuth: true,
  })
}
