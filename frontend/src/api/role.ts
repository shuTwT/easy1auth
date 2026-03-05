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
  getStats() {
    return request.get<RoleStats>('/roles/stats')
  },

  getTree() {
    return request.get<RoleTree[]>('/roles/tree')
  },

  getList(params?: { search?: string; type?: string }) {
    return request.get<Role[]>('/roles', { params })
  },

  getById(id: string) {
    return request.get<Role>(`/roles/${id}`)
  },

  create(data: CreateRoleDto) {
    return request.post<Role>('/roles', data)
  },

  update(id: string, data: UpdateRoleDto) {
    return request.put<Role>(`/roles/${id}`, data)
  },

  delete(id: string) {
    return request.delete(`/roles/${id}`)
  },

  getRoleUsers(roleId: string, params?: { search?: string }) {
    return request.get<RoleUsersResponse>(`/roles/${roleId}/users`, { params })
  },

  assignUsers(roleId: string, data: AssignRoleDto) {
    return request.post(`/roles/${roleId}/users`, data)
  },

  removeUsers(roleId: string, data: RemoveRoleDto) {
    return request.delete(`/roles/${roleId}/users`, { data })
  },

  getUserRoles(userId: string) {
    return request.get<Role[]>(`/roles/user/${userId}`)
  },

  assignRolesToUser(userId: string, roleIds: string[]) {
    return request.post(`/roles/user/${userId}`, { roleIds })
  },
}
