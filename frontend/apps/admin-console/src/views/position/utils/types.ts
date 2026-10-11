import type { Position } from '@/types/position'

export interface SelectOption<T extends string | number = string> {
  value: T
  label: string
}

export interface PositionEditForm {
  name: string
  code: string
  description: string
  departmentId: string | undefined
  level: number
  sequence: string
  maxCount: number | undefined
}

export interface PositionEditDialogExpose {
  open: (row?: Position) => void
}
