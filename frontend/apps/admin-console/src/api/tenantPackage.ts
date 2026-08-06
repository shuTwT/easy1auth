import request from '@/utils/request'
import type {
  ManagementPermission,
  TenantPackage,
  TenantPackageMutation,
  TenantPackageStatus,
} from '@/types/tenantPackage'

export const tenantPackageApi = {
  list(): Promise<TenantPackage[]> {
    return request.get('/platform/tenant-packages')
  },

  get(id: number): Promise<TenantPackage> {
    return request.get(`/platform/tenant-packages/${id}`)
  },

  create(data: TenantPackageMutation): Promise<TenantPackage> {
    return request.post('/platform/tenant-packages', data)
  },

  update(id: number, data: TenantPackageMutation): Promise<TenantPackage> {
    return request.put(`/platform/tenant-packages/${id}`, data)
  },

  updateStatus(id: number, status: TenantPackageStatus): Promise<TenantPackage> {
    return request.put(`/platform/tenant-packages/${id}/status`, { status })
  },

  replacePermissions(id: number, permissionCodes: string[]): Promise<TenantPackage> {
    return request.put(`/platform/tenant-packages/${id}/permissions`, { permissionCodes })
  },

  remove(id: number): Promise<void> {
    return request.delete(`/platform/tenant-packages/${id}`)
  },

  permissionCatalog(): Promise<ManagementPermission[]> {
    return request.get('/platform/permissions')
  },
}
