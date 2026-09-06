import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 bg-stone-50 text-center">
      <p className="text-4xl font-semibold text-stone-300">404</p>
      <p className="text-stone-600">Esta página no existe.</p>
      <Link to="/admin" className="text-sm font-medium text-emerald-700 hover:underline">
        Volver al panel
      </Link>
    </main>
  )
}
