import type { AdminRole } from '@prisma/client'

export interface CreateAdminRoleDto {
  name: string
  description?: string
  permissions: string[]
}

export interface UpdateAdminRoleDto {
  name?: string
  description?: string
  permissions?: string[]
}

export interface AdminRoleQueryDto {
  page?: number
  pageSize?: number
  name?: string
  isSystem?: boolean
}

export type AdminRoleResponse = AdminRole & {
  adminCount: number
}

export interface AdminRoleListResponse {
  roles: AdminRoleResponse[]
  total: number
  page: number
  pageSize: number
}

export interface AdminRoleStats {
  totalRoles: number
  systemRoles: number
  customRoles: number
  totalAdmins: number
}

export interface PermissionCatalogResponse {
  permissions: readonly string[]
}
