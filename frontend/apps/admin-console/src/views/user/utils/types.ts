import type { User } from '@/types/user'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface UserEditForm {
  username: string
  email: string
  password: string
  phone: string
  name: string
  avatar: string
  department: string
  position: string
}

export interface ResetPasswordForm {
  newPassword: string
  confirmPassword: string
}

export interface ImportErrorItem {
  row: number
  username: string
  error: string
}

export interface ImportedUserItem {
  username: string
  email: string
  name: string
}

export interface ImportResult {
  total: number
  success: number
  failed: number
  errors?: ImportErrorItem[]
  importedUsers?: ImportedUserItem[]
}

export interface UserEditDialogExpose {
  open: (row?: User) => void
}

export interface ResetPasswordDialogExpose {
  open: (userId: string) => void
}

export interface AssignRoleDialogExpose {
  open: (row: User) => void
}

export interface ImportDialogExpose {
  open: () => void
}
