import request from '@/utils/request'
import type { TenantInfo } from '@/types/auth'
import type { Tenant, CreateTenantDto, UpdateTenantDto, TenantQueryDto, TenantListResponse } from '@/types/tenant'

export interface CreateTenantRequest {
  name: string
  plan?: string
}

export interface CreateTenantResponse {
  status: string
  message: string
  tenant: TenantInfo
}

export interface GetTenantsResponse {
  status: string
  tenants: TenantInfo[]
}

export interface GetCurrentTenantResponse {
  status: string
  tenant: TenantInfo
}

export const tenantApi = {
  getTenants(): Promise<GetTenantsResponse> {
    return request.get('/tenants/list')
  },

  createTenant(data: CreateTenantRequest): Promise<CreateTenantResponse> {
    return request.post('/tenants/create', data)
  },

  getCurrentTenant(): Promise<GetCurrentTenantResponse> {
    return request.get('/tenants/current')
  },

  getList(params: TenantQueryDto): Promise<TenantListResponse> {
    return request.get('/tenants/list', { params })
  },

  create(data: CreateTenantDto): Promise<{ status: string; message: string; tenant: Tenant }> {
    return request.post('/tenants/create', data)
  },

  update(id: string, data: UpdateTenantDto): Promise<{ status: string; message: string; tenant: Tenant }> {
    return request.put(`/tenants/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/tenants/${id}`)
  },

  updateStatus(id: string, status: string): Promise<{ status: string; message: string; tenant: Tenant }> {
    return request.put(`/tenants/${id}/status`, { status })
  }
}
