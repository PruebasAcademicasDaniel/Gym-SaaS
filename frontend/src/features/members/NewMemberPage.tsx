import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { createMember } from './api'
import { PageHeader } from '@/shared/ui/PageHeader'
import { Card } from '@/shared/ui/Card'
import { TextField } from '@/shared/ui/TextField'
import { Button } from '@/shared/ui/Button'
import { ErrorBanner } from '@/shared/ui/ErrorBanner'

export function NewMemberPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')

  const mutation = useMutation({
    mutationFn: () => createMember({ firstName, lastName, email: email || undefined, phone: phone || undefined }),
    onSuccess: (member) => {
      queryClient.invalidateQueries({ queryKey: ['members'] })
      navigate(`/admin/members/${member.id}`)
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    mutation.mutate()
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Nuevo socio" />
      <Card className="max-w-lg p-6">
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <TextField label="Nombre" required value={firstName} onChange={(event) => setFirstName(event.target.value)} />
          <TextField label="Apellido" required value={lastName} onChange={(event) => setLastName(event.target.value)} />
          <TextField label="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
          <TextField label="Teléfono" value={phone} onChange={(event) => setPhone(event.target.value)} />
          {mutation.isError && <ErrorBanner error={mutation.error} />}
          <div className="flex gap-2">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Guardando…' : 'Guardar'}
            </Button>
            <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
              Cancelar
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
