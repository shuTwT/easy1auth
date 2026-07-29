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
  getStats(): Promise<AdminRoleStats> {
    return request.get('/admin-roles/stats')
  },

  getList(query?: AdminRoleQueryDto): Promise<AdminRoleListResponse> {
    return request.get('/admin-roles', { params: query })
  },

  getById(id: string): Promise<AdminRole> {
    return request.get(`/admin-roles/${id}`)
  },

  create(data: CreateAdminRoleDto): Promise<AdminRole> {
    return request.post('/admin-roles', data)
  },

  update(id: string, data: UpdateAdminRoleDto): Promise<AdminRole> {
    return request.put(`/admin-roles/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/admin-roles/${id}`)
  },

  getPermissionCatalog(): Promise<PermissionCatalogResponse> {
    return request.get('/admin-roles/permissions/catalog')
  }
}
