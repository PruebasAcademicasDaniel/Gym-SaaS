export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div className="rounded-lg border border-dashed border-stone-300 px-6 py-10 text-center">
      <p className="text-sm font-medium text-stone-700">{title}</p>
      {description && <p className="mt-1 text-sm text-stone-500">{description}</p>}
    </div>
  )
}
