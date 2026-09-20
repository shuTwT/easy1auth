import request from '@/utils/request'
import type {
  Permission,
  PermissionTree,
  PermissionStats,
  CreatePermissionDto,
  UpdatePermissionDto,
  PermissionQueryDto,
  PermissionListResponse,
  PermissionSpace,
  PermissionSpaceInput,
} from '@/types/permission'

export const permissionApi = {
  getList(query: PermissionQueryDto): Promise<PermissionListResponse> {
    return request.get('/permissions', { params: query })
  },

  getTree(): Promise<PermissionTree[]> {
    return request.get('/permissions/tree')
  },

  getStats(): Promise<PermissionStats> {
    return request.get('/permissions/stats')
  },

  getById(id: string): Promise<Permission> {
    return request.get(`/permissions/${id}`)
  },

  create(data: CreatePermissionDto): Promise<Permission> {
    return request.post('/permissions', data)
  },

  update(id: string, data: UpdatePermissionDto): Promise<Permission> {
    return request.put(`/permissions/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/permissions/${id}`)
  },

  getSpaces(search?: string): Promise<PermissionSpace[]> {
    return request.get('/permission-spaces', { params: { search: search || undefined } })
  },

  createSpace(data: PermissionSpaceInput): Promise<PermissionSpace> {
    return request.post('/permission-spaces', data)
  },

  updateSpace(id: string, data: PermissionSpaceInput): Promise<PermissionSpace> {
    return request.put(`/permission-spaces/${id}`, data)
  },

  deleteSpace(id: string): Promise<void> {
    return request.delete(`/permission-spaces/${id}`)
  },
}
