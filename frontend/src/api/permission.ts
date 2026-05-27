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
  getList(query: PermissionQueryDto): Promise<{ status: string; data: PermissionListResponse }> {
    return request.get('/permissions', { params: query })
  },

  getTree(): Promise<{ status: string; data: PermissionTree[] }> {
    return request.get('/permissions/tree')
  },

  getStats(): Promise<{ status: string; data: PermissionStats }> {
    return request.get('/permissions/stats')
  },

  getById(id: string): Promise<{ status: string; data: Permission }> {
    return request.get(`/permissions/${id}`)
  },

  create(data: CreatePermissionDto): Promise<{ status: string; data: Permission; message: string }> {
    return request.post('/permissions', data)
  },

  update(id: string, data: UpdatePermissionDto): Promise<{ status: string; data: Permission; message: string }> {
    return request.put(`/permissions/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/permissions/${id}`)
  },
}
