import request from '@/utils/request'
import type { TenantInfo } from '@/types/auth'
import type { Tenant, CreateTenantDto, TenantControl, TenantControlListResponse, TenantQueryDto, TenantListResponse, TenantUpdateDto } from '@/types/tenant'

export interface CreateTenantRequest {
  name: string
  plan?: string
}

export interface TenantPageResponse {
  items: TenantInfo[]
  page: number
  pageSize: number
  total: number
}

export const tenantApi = {
  getTenants(): Promise<TenantPageResponse> {
    return request.get('/tenants/list')
  },

  createTenant(data: CreateTenantRequest): Promise<TenantInfo> {
    return request.post('/tenants/create', data)
  },

  getCurrentTenant(): Promise<TenantInfo> {
    return request.get('/tenants/current')
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
