import type { AdminUser, AdminUserQueryDto } from '@/types/adminUser'
import type { AdminRole, CreateAdminRoleDto } from '@/types/adminRole'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface ApiErrorResponse {
  readonly msg?: string
}

export type AdminTabKey = 'admins' | 'roles'

export type RoleDialogMode = 'create' | 'edit'

export type AdminQueryForm = Required<Pick<AdminUserQueryDto, 'page' | 'pageSize'>> & AdminUserQueryDto

export type RoleForm = Omit<CreateAdminRoleDto, 'permissions'> & { id?: string }

export interface ResetPasswordForm {
  newPassword: string
  confirmPassword: string
}

export interface PermissionCategoryItem {
  code: string
  name: string
}

export interface PermissionCategory {
  code: string
  name: string
  items: PermissionCategoryItem[]
}

export interface AdminEditDialogExpose {
  open: (row: AdminUser) => void
}

export interface ResetPasswordDialogExpose {
  open: (row: AdminUser) => void
}

export interface ResetMfaDialogExpose {
  open: (row: AdminUser) => void
}

export interface AssignRolesDialogExpose {
  open: (row: AdminUser) => void
}

export interface RemoveFromPoolDialogExpose {
  open: (row: AdminUser) => void
}

export interface RoleEditDialogExpose {
  open: (mode: RoleDialogMode, row?: AdminRole) => void
}

export interface RolePermissionDialogExpose {
  open: (row: AdminRole) => void
}

export interface DeleteRoleDialogExpose {
  open: (row: AdminRole) => void
}
