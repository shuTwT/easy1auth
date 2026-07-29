// Frontend types mirroring backend admin-user DTOs.
// Keep aligned with the Java admin API contract.
// These types are intentionally separate from the normal User types; admin users
// are a distinct domain with their own endpoints (/admin-users/*).

export type AdminStatus = 'active' | 'disabled'

/** Summary of an admin role assigned within the current tenant. */
export interface AdminRoleSummary {
  id: string
  name: string
  permissions: string[]
}

/**
 * Admin user as returned by the admin-management API.
 * Mirrors backend AdminDto: the public projection of an Admin scoped to the
 * current tenant (no password, mfaSecret, or other credential fields).
 */
export interface AdminUser {
  id: string
  tenantId: string
  /** Role this admin holds within the current tenant (e.g. 'owner'). */
  tenantRole: string
  currentTenantId: string | null
  username: string
  email: string
  phone: string | null
  status: AdminStatus
  mfaEnabled: boolean
  /** MFA method type ('totp' | 'sms' | 'email'). Not returned by the current public DTO. */
  mfaType?: string | null
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
  /** Admin roles assigned within the current tenant. */
  roles: AdminRoleSummary[]
}

/** Tenant membership info for an admin user. */
export interface AdminTenantInfo {
  role: string
  joinedAt: string
}

/** Query parameters for GET /admin-users. */
export interface AdminUserQueryDto {
  page?: number
  pageSize?: number
  username?: string
  email?: string
  phone?: string
  status?: AdminStatus
  roleId?: string
}

/** Response shape for GET /admin-users (list). Mirrors backend AdminListResponse. */
export interface AdminUserListResponse {
  admins: AdminUser[]
  total: number
  page: number
  pageSize: number
}

/** Response shape for GET /admin-users/stats. Mirrors backend AdminStats. */
export interface AdminUserStats {
  totalAdmins: number
  activeAdmins: number
  disabledAdmins: number
  mfaEnabledAdmins: number
  /** Count of tenant owners. Not returned by the current backend stats endpoint. */
  ownerCount?: number
}

/** Body for PUT /admin-users/:id. */
export interface UpdateAdminDto {
  username?: string
  email?: string
  phone?: string
}

/** Body for PUT /admin-users/:id/status. */
export interface ChangeAdminStatusDto {
  status: AdminStatus
}

/** Body for POST /admin-users/:id/reset-password. */
export interface ResetPasswordDto {
  newPassword: string
}

/** Body for PUT /admin-users/:id/roles. */
export interface AssignRolesDto {
  roleIds: string[]
}

/** Body for DELETE /admin-users/:id/tenant (currently empty, sent as no body). */
export interface RemoveFromTenantDto {}
