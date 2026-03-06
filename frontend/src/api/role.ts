import request from '../utils/request'
import type {
  Role,
  RoleTree,
  RoleStats,
  CreateRoleDto,
  UpdateRoleDto,
  AssignRoleDto,
  RemoveRoleDto,
  RoleUsersResponse,
} from '../types/role'

export const roleApi = {
  getStats(): Promise<RoleStats> {
    return request.get('/roles/stats')
  },

  getTree(): Promise<RoleTree[]> {
    return request.get('/roles/tree')
  },

  getList(params?: { search?: string; type?: string }): Promise<Role[]> {
    return request.get('/roles', { params })
  },

  getById(id: string): Promise<Role> {
    return request.get(`/roles/${id}`)
  },

  create(data: CreateRoleDto): Promise<Role> {
    return request.post('/roles', data)
  },

  update(id: string, data: UpdateRoleDto): Promise<Role> {
    return request.put(`/roles/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/roles/${id}`)
  },

  getRoleUsers(roleId: string, params?: { search?: string }): Promise<RoleUsersResponse> {
    return request.get(`/roles/${roleId}/users`, { params })
  },

  assignUsers(roleId: string, data: AssignRoleDto): Promise<void> {
    return request.post(`/roles/${roleId}/users`, data)
  },

  removeUsers(roleId: string, data: RemoveRoleDto): Promise<void> {
    return request.delete(`/roles/${roleId}/users`, { data })
  },

  getUserRoles(userId: string): Promise<Role[]> {
    return request.get(`/roles/user/${userId}`)
  },

  assignRolesToUser(userId: string, roleIds: string[]): Promise<void> {
    return request.post(`/roles/user/${userId}`, { roleIds })
  },
}
