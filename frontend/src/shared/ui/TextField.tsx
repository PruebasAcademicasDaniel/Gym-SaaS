import { forwardRef, type InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, id, className = '', ...props },
  ref,
) {
  const inputId = id ?? props.name
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={inputId} className="text-sm font-medium text-stone-700">
        {label}
      </label>
      <input
        ref={ref}
        id={inputId}
        className={`rounded-md border px-3 py-2 text-sm text-stone-900 shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
          error ? 'border-red-400' : 'border-stone-300'
        } ${className}`}
        {...props}
      />
      {error && <span className="text-xs text-red-600">{error}</span>}
    </div>
  )
})
