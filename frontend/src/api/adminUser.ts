import request from '@/utils/request'
import type {
  AdminUser,
  AdminUserListResponse,
  AdminUserStats,
  AdminUserQueryDto,
  UpdateAdminDto,
  ChangeAdminStatusDto,
  ResetPasswordDto,
  AssignRolesDto
} from '@/types/adminUser'

// Admin-user API module. Calls /admin-users/* endpoints exclusively.
// The request interceptor adds the Authorization header and tenant-id header
// automatically; no tenant/user params are needed here.

export const adminUserApi = {
  getStats(): Promise<{ status: string; data: AdminUserStats }> {
    return request.get('/admin-users/stats')
  },

  getList(query?: AdminUserQueryDto): Promise<{ status: string; data: AdminUserListResponse }> {
    return request.get('/admin-users', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: AdminUser }> {
    return request.get(`/admin-users/${id}`)
  },

  update(id: string, data: UpdateAdminDto): Promise<{ status: string; message: string; data: AdminUser }> {
    return request.put(`/admin-users/${id}`, data)
  },

  updateStatus(id: string, data: ChangeAdminStatusDto): Promise<{ status: string; message: string; data: AdminUser }> {
    return request.put(`/admin-users/${id}/status`, data)
  },

  resetPassword(id: string, data: ResetPasswordDto): Promise<{ status: string; message: string; data: AdminUser }> {
    return request.post(`/admin-users/${id}/reset-password`, data)
  },

  resetMfa(id: string): Promise<{ status: string; message: string; data: AdminUser }> {
    return request.post(`/admin-users/${id}/reset-mfa`)
  },

  assignRoles(id: string, data: AssignRolesDto): Promise<{ status: string; message: string; data: AdminUser }> {
    return request.put(`/admin-users/${id}/roles`, data)
  },

  removeFromTenant(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/admin-users/${id}/tenant`)
  }
}