import { clearTokens, loadTokens, saveTokens } from '@/shared/auth/tokenStorage'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  title?: string
  fieldErrors?: string[]

  constructor(status: number, message: string, title?: string, fieldErrors?: string[]) {
    super(message)
    this.status = status
    this.title = title
    this.fieldErrors = fieldErrors
  }
}

/** El AuthContext se engancha acá para reaccionar cuando el refresh falla — sin esto, un 401 con sesión vencida no tendría forma de avisarle a la UI que redirija a /login. */
let onSessionExpired: (() => void) | null = null

export function setSessionExpiredHandler(handler: (() => void) | null): void {
  onSessionExpired = handler
}

interface RefreshResponse {
  accessToken: string
  refreshToken: string
}

let refreshInFlight: Promise<string | null> | null = null

/** Deduplicado: si dos requests pisan un 401 al mismo tiempo, solo se refresca una vez. */
async function refreshAccessToken(): Promise<string | null> {
  const tokens = loadTokens()
  if (!tokens) return null

  if (!refreshInFlight) {
    refreshInFlight = fetch(`${BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: tokens.refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) return null
        const data = (await response.json()) as RefreshResponse
        saveTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken })
        return data.accessToken
      })
      .catch(() => null)
      .finally(() => {
        refreshInFlight = null
      })
  }

  return refreshInFlight
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  /** Para /auth/login y /auth/refresh — no tiene sentido adjuntar un token todavía. */
  skipAuth?: boolean
}

interface ProblemDetail {
  title?: string
  detail?: string
  errors?: string[]
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, skipAuth = false } = options

  const doFetch = (accessToken: string | null) => {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`
    return fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  }

  const tokens = loadTokens()
  let response = await doFetch(skipAuth ? null : (tokens?.accessToken ?? null))

  if (response.status === 401 && !skipAuth && tokens) {
    const newAccessToken = await refreshAccessToken()
    if (newAccessToken) {
      response = await doFetch(newAccessToken)
    } else {
      clearTokens()
      onSessionExpired?.()
      throw new ApiError(401, 'La sesión venció. Iniciá sesión de nuevo.')
    }
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const isJson = contentType.includes('json')
  const payload: ProblemDetail | T | null = isJson ? await response.json().catch(() => null) : null

  if (!response.ok) {
    const problem = payload as ProblemDetail | null
    throw new ApiError(
      response.status,
      problem?.detail ?? 'Ocurrió un error inesperado.',
      problem?.title,
      problem?.errors,
    )
  }

  return payload as T
}
