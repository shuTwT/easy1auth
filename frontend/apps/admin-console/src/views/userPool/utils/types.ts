import type { UserPool, UserPoolStatus } from '@/types/userPool'

export interface SelectOption<T extends string | number = string> {
  value: T
  label: string
}

export interface UserPoolEditForm {
  name: string
  packageId: number | undefined
}

export type UserPoolStatusOption = SelectOption<UserPoolStatus>

export interface UserPoolEditDialogExpose {
  open: (userPool: UserPool) => void
}

export interface TransferAdministratorDialogExpose {
  open: (userPool: UserPool) => void
}

export interface UserPoolDeleteDialogExpose {
  open: (userPool: UserPool) => void
}
