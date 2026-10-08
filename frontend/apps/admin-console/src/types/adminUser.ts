// Frontend types mirroring backend admin-user DTOs.
// Keep aligned with the Java admin API contract.
// These types are intentionally separate from the normal User types; admin users
// are a distinct domain with their own endpoints (/admin-users/*).
//
// Backend AdminMemberView is account-level: one row per admin account, carrying
// a `userPools` list of AdminMembershipView describing each tenant membership.
// The top-level userPoolId/tenantRole/roles are legacy compatibility fields
// derived from the account's selected membership and may be null when the
// account has no memberships. Membership mutations (assign roles, remove from
// tenant) require an explicit userPoolId targeting the currently selected tenant;
// do not use the legacy fields or the request interceptor's user-pool-id header as
// mutation authority.

export type AdminStatus = 'active' | 'disabled'

/** Summary of an admin role assigned within a tenant. */
export interface AdminRoleSummary {
  id: string
  name: string
  code: string
  permissions: string[]
}

/**
 * A single tenant membership of an admin account.
 * Mirrors backend AdminMembershipView: the userPoolId, the membership role
 * (e.g. 'owner'), and the admin roles assigned within that tenant.
 */
export interface AdminMembership {
  readonly userPoolId: string
  readonly tenantRole: string
  readonly roles: AdminRoleSummary[]
}

/**
 * Admin user as returned by the admin-management API.
 * Mirrors backend AdminMemberView: an account-level projection (one row per
 * admin account) carrying a `userPools` list of memberships. The top-level
 * userPoolId/tenantRole/roles are legacy compatibility fields derived from the
 * account's selected membership and may be null when the account has no
 * memberships; prefer the `userPools` list for membership-specific logic.
 */
export interface AdminUser {
  id: string
  /** Legacy compat: userPoolId of the selected membership, null when no membership. */
  userPoolId: string | null
  /** Legacy compat: role within the selected membership, null when no membership. */
  tenantRole: string | null
  currentUserPoolId: string | null
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
  /** Legacy compat: admin roles assigned within the selected membership. */
  roles: AdminRoleSummary[]
  /** All tenant memberships for this account. Source of truth for membership logic. */
  userPools: AdminMembership[]
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
  items: AdminUser[]
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

/** Body for PUT /admin-users/:id/roles. Mirrors backend RolesInput. */
export interface AssignRolesDto {
  userPoolId: string
  roleIds: string[]
}
