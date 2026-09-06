import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { useAuth } from '@/shared/auth/AuthContext'
import { Button } from '@/shared/ui/Button'
import { TextField } from '@/shared/ui/TextField'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'

export function LoginPage() {
  const { login, status } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  if (status === 'authenticated') {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from ?? '/admin'} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      navigate('/admin', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-stone-50 px-4">
      <div className="w-full max-w-sm rounded-xl border border-stone-200 bg-white p-8 shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-wider text-emerald-700">GymFlow</p>
        <h1 className="mt-2 text-xl font-semibold text-stone-900">Portal administrativo</h1>
        <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit}>
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="username"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <TextField
            label="Contraseña"
            type="password"
            name="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          {error !== null && <ErrorBanner error={error} />}
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Ingresando…' : 'Ingresar'}
          </Button>
        </form>
      </div>
    </main>
  )
}
