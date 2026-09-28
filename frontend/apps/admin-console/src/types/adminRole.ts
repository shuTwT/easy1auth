// Frontend types mirroring backend admin-role DTOs.
// Keep aligned with the Java admin API contract.
// These types are intentionally separate from the normal Role types; admin roles
// are a distinct domain with their own endpoints (/admin-roles/*).
import type { ManagementPermission } from './tenantPackage'

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
  code: string
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
  items: AdminRole[]
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
  code: string
  description?: string
  permissions: string[]
}

/** Body for PUT /admin-roles/:id. */
export interface UpdateAdminRoleDto {
  name?: string
  description?: string
  permissions?: string[]
}

/** One permission entry returned by the management permission catalog. */
export type PermissionCatalogItem = ManagementPermission

/**
 * Response shape for GET /admin-roles/permissions/catalog.
 * Mirrors backend PermissionCatalogResponse: permission metadata objects.
 */
export interface PermissionCatalogResponse {
  permissions: PermissionCatalogItem[]
}
