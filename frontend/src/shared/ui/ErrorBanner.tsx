import { ApiError } from '@/shared/api/httpClient'

/** Centraliza cómo se ve un error de API en toda la app — nunca se muestra un stack trace ni el mensaje crudo de fetch. */
export function messageFor(error: unknown): string {
  if (error instanceof ApiError) {
    const fieldErrors = error.fieldErrors?.length ? ` (${error.fieldErrors.join(', ')})` : ''
    return `${error.message}${fieldErrors}`
  }
  return 'Ocurrió un error inesperado. Probá de nuevo.'
}

export function ErrorBanner({ error }: { error: unknown }) {
  return (
    <div className="rounded-md border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700" role="alert">
      {messageFor(error)}
    </div>
  )
}
