import request from '@/utils/request'
import type {
  AdminRole,
  AdminRoleListResponse,
  AdminRoleStats,
  AdminRoleQueryDto,
  CreateAdminRoleDto,
  UpdateAdminRoleDto,
  PermissionCatalogResponse
} from '@/types/adminRole'

// Admin-role API module. Calls /admin-roles/* endpoints exclusively.
// The request interceptor adds the Authorization header and tenant-id header
// automatically; no tenant params are needed here.

export const adminRoleApi = {
  getStats(): Promise<{ status: string; data: AdminRoleStats }> {
    return request.get('/admin-roles/stats')
  },

  getList(query?: AdminRoleQueryDto): Promise<{ status: string; data: AdminRoleListResponse }> {
    return request.get('/admin-roles', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: AdminRole }> {
    return request.get(`/admin-roles/${id}`)
  },

  create(data: CreateAdminRoleDto): Promise<{ status: string; message: string; data: AdminRole }> {
    return request.post('/admin-roles', data)
  },

  update(id: string, data: UpdateAdminRoleDto): Promise<{ status: string; message: string; data: AdminRole }> {
    return request.put(`/admin-roles/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/admin-roles/${id}`)
  },

  getPermissionCatalog(): Promise<{ status: string; data: PermissionCatalogResponse }> {
    return request.get('/admin-roles/permissions/catalog')
  }
}