import type { Admin } from '@prisma/client'

export type AdminStatus = 'active' | 'disabled'

export interface AdminQueryDto {
  page?: number
  pageSize?: number
  username?: string
  email?: string
  phone?: string
  status?: AdminStatus
  roleId?: string
}

export interface AdminListResponse {
  admins: Admin[]
  total: number
  page: number
  pageSize: number
}

export interface AdminStats {
  totalAdmins: number
  activeAdmins: number
  disabledAdmins: number
  mfaEnabledAdmins: number
  ownerCount: number
}

export interface UpdateAdminDto {
  username?: string
  email?: string
  phone?: string
}

export interface ChangeAdminStatusDto {
  status: AdminStatus
}

export interface ResetPasswordDto {
  newPassword: string
}

export interface ResetMfaDto {}

export interface AssignRolesDto {
  roleIds: string[]
}

export interface RemoveFromTenantDto {}
