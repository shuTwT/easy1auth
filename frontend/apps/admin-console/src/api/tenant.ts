import request from '@/utils/request'
import type { TenantInfo } from '@/types/auth'
import type { Tenant, CreateTenantDto, TenantControl, TenantControlListResponse, TenantQueryDto, TenantListResponse, TenantUpdateDto } from '@/types/tenant'

export interface CreateTenantRequest {
  name: string
  packageId: number
}

export interface TenantPackageOption {
  id: number
  name: string
  maxUsers: number
  maxApps: number
}

export const tenantApi = {
  getSimpleList(): Promise<TenantInfo[]> {
    return request.get('/tenants/simple-list')
  },

  getCurrentSimpleList(): Promise<TenantInfo[]> {
    return request.get('/tenants/simple-slist/current')
  },

  createTenant(data: CreateTenantRequest): Promise<TenantInfo> {
    return request.post('/tenants/create', data)
  },

  getCurrentTenant(): Promise<TenantInfo> {
    return request.get('/tenants/current')
  },

  getAvailablePackages(): Promise<TenantPackageOption[]> {
    return request.get('/tenants/available-packages')
  },

  getList(params: TenantQueryDto): Promise<TenantListResponse> {
    return request.get('/tenants/list', { params })
  },

  getManagedList(): Promise<TenantControlListResponse> {
    return request.get('/tenants/managed')
  },

  create(data: CreateTenantDto): Promise<Tenant> {
    return request.post('/tenants/create', data)
  },

  update(id: string, data: TenantUpdateDto): Promise<TenantControl> {
    return request.put(`/tenants/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/tenants/${id}`)
  },

  updateStatus(id: string, status: 'active' | 'suspended'): Promise<TenantControl> {
    return request.put(`/tenants/${id}/status`, { status })
  },

  transferAdministrator(id: string, administratorAccountId: string): Promise<TenantControl> {
    return request.put(`/tenants/${id}/administrator`, { administratorAccountId })
  }
}
