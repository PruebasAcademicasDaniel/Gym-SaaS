import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { listPaymentsByMembership, registerPayment, type PaymentMethod } from './api'
import { Button } from '@/shared/ui/Button'
import { TextField } from '@/shared/ui/TextField'
import { SelectField } from '@/shared/ui/SelectField'
import { Spinner } from '@/shared/ui/Spinner'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'
import { EmptyState } from '@/shared/ui/EmptyState'
import { formatCurrency, formatDate } from '@/shared/ui/formatters'

const methodLabel: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  CARD: 'Tarjeta',
  TRANSFER: 'Transferencia',
  OTHER: 'Otro',
}

/** Solo se monta cuando el actor es GYM_ADMIN (ver MembershipsSection) — Pagos no tiene ningún acceso de TRAINER en el backend. */
export function PaymentsSection({ membershipId }: { membershipId: string }) {
  const queryClient = useQueryClient()
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState<PaymentMethod>('CASH')

  const paymentsQuery = useQuery({
    queryKey: ['payments', membershipId],
    queryFn: () => listPaymentsByMembership(membershipId),
  })

  const registerMutation = useMutation({
    mutationFn: () => registerPayment({ membershipId, amount: Number(amount), method }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments', membershipId] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      setAmount('')
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (Number(amount) > 0) registerMutation.mutate()
  }

  return (
    <div className="flex flex-col gap-3">
      <form className="flex flex-wrap items-end gap-3" onSubmit={handleSubmit}>
        <div className="w-32">
          <TextField
            label="Monto"
            type="number"
            min="0.01"
            step="0.01"
            required
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
          />
        </div>
        <div className="w-40">
          <SelectField label="Método" value={method} onChange={(event) => setMethod(event.target.value as PaymentMethod)}>
            {Object.entries(methodLabel).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </SelectField>
        </div>
        <Button type="submit" disabled={registerMutation.isPending}>
          {registerMutation.isPending ? 'Registrando…' : 'Registrar pago'}
        </Button>
      </form>
      {registerMutation.isError && <ErrorBanner error={registerMutation.error} />}

      {paymentsQuery.isPending && <Spinner />}
      {paymentsQuery.error && <ErrorBanner error={paymentsQuery.error} />}
      {paymentsQuery.data && paymentsQuery.data.length === 0 && <EmptyState title="Sin pagos registrados" />}
      {paymentsQuery.data && paymentsQuery.data.length > 0 && (
        <table className="w-full text-sm">
          <thead className="text-left text-xs font-medium uppercase tracking-wide text-stone-500">
            <tr>
              <th className="py-1.5 pr-4">Fecha</th>
              <th className="py-1.5 pr-4">Monto</th>
              <th className="py-1.5">Método</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {paymentsQuery.data.map((payment) => (
              <tr key={payment.id}>
                <td className="py-1.5 pr-4 text-stone-600">{formatDate(payment.paymentDate)}</td>
                <td className="py-1.5 pr-4 font-medium text-stone-900">{formatCurrency(payment.amount)}</td>
                <td className="py-1.5 text-stone-600">{methodLabel[payment.method]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
