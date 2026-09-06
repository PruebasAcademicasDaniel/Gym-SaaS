import { forwardRef, type SelectHTMLAttributes } from 'react'

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
}

export const SelectField = forwardRef<HTMLSelectElement, SelectFieldProps>(function SelectField(
  { label, error, id, className = '', children, ...props },
  ref,
) {
  const selectId = id ?? props.name
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={selectId} className="text-sm font-medium text-stone-700">
        {label}
      </label>
      <select
        ref={ref}
        id={selectId}
        className={`rounded-md border px-3 py-2 text-sm text-stone-900 shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
          error ? 'border-red-400' : 'border-stone-300'
        } ${className}`}
        {...props}
      >
        {children}
      </select>
      {error && <span className="text-xs text-red-600">{error}</span>}
    </div>
  )
})
