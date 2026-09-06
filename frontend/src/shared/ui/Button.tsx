import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

const variantClasses: Record<Variant, string> = {
  primary: 'bg-emerald-600 text-white hover:bg-emerald-700 disabled:bg-emerald-300',
  secondary: 'border border-stone-300 bg-white text-stone-700 hover:bg-stone-50 disabled:text-stone-400',
  danger: 'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300',
  ghost: 'text-stone-600 hover:bg-stone-100 disabled:text-stone-300',
}

/** Para estilar un <Link> de react-router como si fuera un botón — no puede ser un <button> real porque necesita navegar. */
export function buttonClasses(variant: Variant = 'primary', className = ''): string {
  return `inline-flex items-center justify-center gap-1.5 rounded-md px-3.5 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed ${variantClasses[variant]} ${className}`
}

export function Button({ variant = 'primary', className = '', ...props }: ButtonProps) {
  return <button className={buttonClasses(variant, className)} {...props} />
}
