import request from '@/utils/request'
import type {
  User,
  CreateUserDto,
  UpdateUserDto,
  UserQueryDto,
  UserListResponse,
  UserStats,
  ChangePasswordDto,
  ResetPasswordDto,
  AssignRolesDto,
  AssignGroupsDto
} from '@/types/user'

export const userApi = {
  getList(query: UserQueryDto): Promise<{ status: string; data: UserListResponse }> {
    return request.get('/users', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: User }> {
    return request.get(`/users/${id}`)
  },

  create(data: CreateUserDto): Promise<{ status: string; message: string; data: User }> {
    return request.post('/users', data)
  },

  update(id: string, data: UpdateUserDto): Promise<{ status: string; message: string; data: User }> {
    return request.put(`/users/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/users/${id}`)
  },

  updateStatus(id: string, status: string): Promise<{ status: string; message: string; data: User }> {
    return request.put(`/users/${id}/status`, { status })
  },

  getStats(): Promise<{ status: string; data: UserStats }> {
    return request.get('/users/stats')
  },

  resetPassword(id: string, newPassword: string): Promise<{ status: string; message: string }> {
    return request.post(`/users/${id}/reset-password`, { newPassword })
  },

  changePassword(id: string, data: ChangePasswordDto): Promise<{ status: string; message: string }> {
    return request.post(`/users/${id}/change-password`, data)
  },

  getRoles(id: string): Promise<{ status: string; data: any[] }> {
    return request.get(`/users/${id}/roles`)
  },

  assignRoles(id: string, data: AssignRolesDto): Promise<{ status: string; message: string }> {
    return request.post(`/users/${id}/roles`, data)
  },

  getGroups(id: string): Promise<{ status: string; data: any[] }> {
    return request.get(`/users/${id}/groups`)
  },

  assignGroups(id: string, data: AssignGroupsDto): Promise<{ status: string; message: string }> {
    return request.post(`/users/${id}/groups`, data)
  }
}
