import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import * as authApi from '@/shared/auth/api'
import type { MeResponse } from '@/shared/auth/api'
import { clearTokens, loadTokens, saveTokens } from '@/shared/auth/tokenStorage'
import { setSessionExpiredHandler } from '@/shared/api/httpClient'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

interface AuthContextValue {
  user: MeResponse | null
  status: AuthStatus
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null)
  // Lazy init en vez de arrancar en 'loading' + setState síncrono en el effect:
  // sin tokens guardados no hay nada que validar contra el backend.
  const [status, setStatus] = useState<AuthStatus>(() => (loadTokens() ? 'loading' : 'unauthenticated'))

  const forceLogout = useCallback(() => {
    clearTokens()
    setUser(null)
    setStatus('unauthenticated')
  }, [])

  useEffect(() => {
    setSessionExpiredHandler(forceLogout)
    return () => setSessionExpiredHandler(null)
  }, [forceLogout])

  useEffect(() => {
    const tokens = loadTokens()
    if (!tokens) return
    authApi
      .me()
      .then((profile) => {
        setUser(profile)
        setStatus('authenticated')
      })
      .catch(() => {
        clearTokens()
        setStatus('unauthenticated')
      })
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password)
    saveTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken })
    const profile = await authApi.me()
    setUser(profile)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(() => {
    const tokens = loadTokens()
    if (tokens) {
      authApi.logout(tokens.refreshToken).catch(() => {
        // best effort: si esta llamada falla igual limpiamos la sesión local
      })
    }
    forceLogout()
  }, [forceLogout])

  return <AuthContext.Provider value={{ user, status, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  }
  return context
}
