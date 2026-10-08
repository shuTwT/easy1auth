import request from '@/utils/request'
import type {
  ManagementPermission,
  UserPoolPackage,
  UserPoolPackageMutation,
  UserPoolPackageStatus,
} from '@/types/userPoolPackage'

export const userPoolPackageApi = {
  list(): Promise<UserPoolPackage[]> {
    return request.get('/platform/user-pool-packages')
  },

  get(id: number): Promise<UserPoolPackage> {
    return request.get(`/platform/user-pool-packages/${id}`)
  },

  create(data: UserPoolPackageMutation): Promise<UserPoolPackage> {
    return request.post('/platform/user-pool-packages', data)
  },

  update(id: number, data: UserPoolPackageMutation): Promise<UserPoolPackage> {
    return request.put(`/platform/user-pool-packages/${id}`, data)
  },

  updateStatus(id: number, status: UserPoolPackageStatus): Promise<UserPoolPackage> {
    return request.put(`/platform/user-pool-packages/${id}/status`, { status })
  },

  replacePermissions(id: number, permissionCodes: string[]): Promise<UserPoolPackage> {
    return request.put(`/platform/user-pool-packages/${id}/permissions`, { permissionCodes })
  },

  remove(id: number): Promise<void> {
    return request.delete(`/platform/user-pool-packages/${id}`)
  },

  permissionCatalog(): Promise<ManagementPermission[]> {
    return request.get('/platform/permissions')
  },
}
