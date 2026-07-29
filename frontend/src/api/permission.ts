import request from '@/utils/request'
import type {
  Permission,
  PermissionTree,
  PermissionStats,
  CreatePermissionDto,
  UpdatePermissionDto,
  PermissionQueryDto,
  PermissionListResponse,
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
}
