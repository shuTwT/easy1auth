import request from '@/utils/request'
import type {
  User,
  CreateUserDto,
  UpdateUserDto,
  UserQueryDto,
  UserListResponse,
  UserStats,
  ChangePasswordDto,
  AssignRolesDto,
  AssignGroupsDto
} from '@/types/user'

export const userApi = {
  getList(query?: UserQueryDto): Promise<UserListResponse> {
    return request.get('/users', { params: query })
  },

  getById(id: string): Promise<User> {
    return request.get(`/users/${id}`)
  },

  create(data: CreateUserDto): Promise<User> {
    return request.post('/users', data)
  },

  update(id: string, data: UpdateUserDto): Promise<User> {
    return request.put(`/users/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/users/${id}`)
  },

  updateStatus(id: string, status: string): Promise<User> {
    return request.put(`/users/${id}/status`, { status })
  },

  getStats(): Promise<UserStats> {
    return request.get('/users/stats')
  },

  resetPassword(id: string, newPassword: string): Promise<void> {
    return request.post(`/users/${id}/reset-password`, { newPassword })
  },

  changePassword(id: string, data: ChangePasswordDto): Promise<void> {
    return request.post(`/users/${id}/change-password`, data)
  },

  getRoles(id: string): Promise<any[]> {
    return request.get(`/users/${id}/roles`)
  },

  assignRoles(id: string, data: AssignRolesDto): Promise<void> {
    return request.post(`/users/${id}/roles`, data)
  },

  getGroups(id: string): Promise<any[]> {
    return request.get(`/users/${id}/groups`)
  },

  assignGroups(id: string, data: AssignGroupsDto): Promise<void> {
    return request.post(`/users/${id}/groups`, data)
  }
}
