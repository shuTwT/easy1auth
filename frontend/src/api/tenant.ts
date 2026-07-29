import request from '@/utils/request'
import type { TenantInfo } from '@/types/auth'
import type { Tenant, CreateTenantDto, UpdateTenantDto, TenantQueryDto, TenantListResponse } from '@/types/tenant'

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

  create(data: CreateTenantDto): Promise<Tenant> {
    return request.post('/tenants/create', data)
  },

  update(id: string, data: UpdateTenantDto): Promise<Tenant> {
    return request.put(`/tenants/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/tenants/${id}`)
  },

  updateStatus(id: string, status: string): Promise<Tenant> {
    return request.put(`/tenants/${id}/status`, { status })
  }
}
