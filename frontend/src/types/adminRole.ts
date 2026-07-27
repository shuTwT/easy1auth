// Frontend types mirroring backend admin-role DTOs.
// Source of truth: backend/src/types/admin-role.types.ts
// These types are intentionally separate from the normal Role types; admin roles
// are a distinct domain with their own endpoints (/admin-roles/*).

/**
 * Admin role as returned by the admin-management API.
 * Mirrors backend AdminRoleResponse = AdminRole & { adminCount }.
 * permissions are stored as Json in the DB but treated as string[] by the
 * admin-role service (each entry is an admin permission code or '*').
 */
export interface AdminRole {
  id: string
  tenantId: string
  name: string
  description: string | null
  permissions: string[]
  isSystem: boolean
  adminCount: number
  createdAt: string
  updatedAt: string
}

/** Query parameters for GET /admin-roles. */
export interface AdminRoleQueryDto {
  page?: number
  pageSize?: number
  name?: string
  isSystem?: boolean
}

/** Response shape for GET /admin-roles (list). Mirrors backend AdminRoleListResponse. */
export interface AdminRoleListResponse {
  roles: AdminRole[]
  total: number
  page: number
  pageSize: number
}

/** Response shape for GET /admin-roles/stats. Mirrors backend AdminRoleStats. */
export interface AdminRoleStats {
  totalRoles: number
  systemRoles: number
  customRoles: number
  totalAdmins: number
}

/** Body for POST /admin-roles. */
export interface CreateAdminRoleDto {
  name: string
  description?: string
  permissions: string[]
}

/** Body for PUT /admin-roles/:id. */
export interface UpdateAdminRoleDto {
  name?: string
  description?: string
  permissions?: string[]
}

/**
 * A single admin permission entry for display purposes.
 * The backend currently returns a flat string[] of permission codes; the
 * frontend page can derive PermissionCatalogItem[] from that list to render
 * grouped/categorized permission selectors.
 */
export interface PermissionCatalogItem {
  code: string
  name: string
  description: string
  category: string
}

/**
 * Response shape for GET /admin-roles/permissions/catalog.
 * Mirrors backend PermissionCatalogResponse: a flat list of the 12 admin
 * permission codes. The frontend may group these into categories for display.
 */
export interface PermissionCatalogResponse {
  permissions: string[]
}