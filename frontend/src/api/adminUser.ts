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
// These endpoints require the Authorization header (added by the request
// interceptor) but are account-level: they do NOT depend on the tenant-id
// header for reads or account-level mutations. Membership mutations (assign
// roles, remove from tenant) take an explicit tenantId in the body / query.

export const adminUserApi = {
  getStats(): Promise<AdminUserStats> {
    return request.get('/admin-users/stats')
  },

  getList(query?: AdminUserQueryDto): Promise<AdminUserListResponse> {
    return request.get('/admin-users', { params: query })
  },

  getById(id: string): Promise<AdminUser> {
    return request.get(`/admin-users/${id}`)
  },

  update(id: string, data: UpdateAdminDto): Promise<AdminUser> {
    return request.put(`/admin-users/${id}`, data)
  },

  updateStatus(id: string, data: ChangeAdminStatusDto): Promise<AdminUser> {
    return request.put(`/admin-users/${id}/status`, data)
  },

  resetPassword(id: string, data: ResetPasswordDto): Promise<AdminUser> {
    return request.post(`/admin-users/${id}/reset-password`, data)
  },

  resetMfa(id: string): Promise<AdminUser> {
    return request.post(`/admin-users/${id}/reset-mfa`)
  },

  assignRoles(id: string, data: AssignRolesDto): Promise<AdminUser> {
    return request.put(`/admin-users/${id}/roles`, data)
  },

  removeFromTenant(id: string, tenantId: string): Promise<void> {
    return request.delete(`/admin-users/${id}/tenant`, { params: { tenantId } })
  }
}
