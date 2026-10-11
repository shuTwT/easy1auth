import type { AuditLog } from '@/types/audit'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface DetailDialogExpose {
  open: (row: AuditLog) => void
}
