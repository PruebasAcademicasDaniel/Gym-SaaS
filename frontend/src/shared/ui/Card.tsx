import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-lg border border-stone-200 bg-white shadow-sm ${className}`}>{children}</div>
}
