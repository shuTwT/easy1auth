export function firstString(value: unknown): string | undefined {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && typeof value[0] === 'string') return value[0]
  return undefined
}

export function safeCssColor(value: string | undefined, fallback: string): string {
  return value && /^#[0-9a-f]{6}$/i.test(value) ? value : fallback
}
