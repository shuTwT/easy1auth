import request from '@/utils/request'
import type { TenantInfo } from '@/types/auth'

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
  }
}
