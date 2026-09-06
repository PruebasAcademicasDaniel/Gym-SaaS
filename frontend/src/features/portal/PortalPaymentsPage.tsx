import { useQuery } from '@tanstack/react-query'
import { getMyPayments } from './api'
import type { PaymentMethod } from '@/features/payments/api'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
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

export function PortalPaymentsPage() {
  const { data, isPending, error } = useQuery({ queryKey: ['portal', 'payments'], queryFn: getMyPayments })

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Mis pagos" description="Historial de pagos registrados, de todas tus membresías." />

      <Card className="p-5">
        {isPending && <Spinner />}
        {error && <ErrorBanner error={error} />}
        {data && data.length === 0 && <EmptyState title="Sin pagos registrados" />}
        {data && data.length > 0 && (
          <table className="w-full text-sm">
            <thead className="text-left text-xs font-medium uppercase tracking-wide text-stone-500">
              <tr>
                <th className="py-1.5 pr-4">Fecha</th>
                <th className="py-1.5 pr-4">Monto</th>
                <th className="py-1.5">Método</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {data.map((payment) => (
                <tr key={payment.id}>
                  <td className="py-1.5 pr-4 text-stone-600">{formatDate(payment.paymentDate)}</td>
                  <td className="py-1.5 pr-4 font-medium text-stone-900">{formatCurrency(payment.amount)}</td>
                  <td className="py-1.5 text-stone-600">{methodLabel[payment.method]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  )
}
