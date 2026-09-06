export function formatCurrency(amount: number | string): string {
  return `$${Number(amount).toFixed(2)}`
}

/**
 * Para fechas puras (LocalDate del backend: "2026-10-04", sin hora). Se
 * fuerza timeZone: 'UTC' porque `new Date('2026-10-04')` se interpreta
 * como medianoche UTC — sin esto, en cualquier huso horario detrás de UTC
 * la fecha se mostraría un día antes.
 */
const dateFormatter = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' })

export function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

/** Para timestamps reales (Instant del backend, con hora y offset) — acá sí conviene mostrar la hora local del navegador. */
const dateTimeFormatter = new Intl.DateTimeFormat('es-AR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatDateTime(value: string): string {
  return dateTimeFormatter.format(new Date(value))
}
