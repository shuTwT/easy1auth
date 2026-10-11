import type { Role, RoleTree } from '@/types/role'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface RoleEditForm {
  name: string
  code: string
  description: string
  type: string
  dataScope: string
  parentId: string | undefined
}

export interface AssignableUser {
  id: string
  name: string
  disabled?: boolean
}

export interface RoleEditDialogExpose {
  open: (row?: Role) => void
}

export interface RoleUsersDialogExpose {
  open: (row: Role | RoleTree) => void
}

export interface AssignUsersDialogExpose {
  open: (roleId: string, existingUserIds: string[]) => void
}
