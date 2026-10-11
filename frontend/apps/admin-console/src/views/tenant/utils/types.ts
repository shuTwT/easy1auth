import type { Tenant } from '@/types/tenant'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface TenantEditDialogExpose {
  open: (row?: Tenant) => void
  close: () => void
}

export interface TenantDeleteDialogExpose {
  open: (row: Tenant) => void
  close: () => void
}
