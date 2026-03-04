import request from '@/utils/request'
import type { 
  Tenant, 
  CreateTenantDto, 
  UpdateTenantDto, 
  TenantQueryDto, 
  TenantListResponse,
  TenantStats 
} from '@/types/tenant'

export const tenantApi = {
  getList(params?: TenantQueryDto): Promise<TenantListResponse> {
    return request.get('/tenants', { params })
  },

  getById(id: string): Promise<Tenant> {
    return request.get(`/tenants/${id}`)
  },

  create(data: CreateTenantDto): Promise<Tenant> {
    return request.post('/tenants', data)
  },

  update(id: string, data: UpdateTenantDto): Promise<Tenant> {
    return request.put(`/tenants/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/tenants/${id}`)
  },

  updateStatus(id: string, status: string): Promise<Tenant> {
    return request.patch(`/tenants/${id}/status`, { status })
  },

  getStats(id: string): Promise<TenantStats> {
    return request.get(`/tenants/${id}/stats`)
  },

  checkDomain(domain: string): Promise<{ available: boolean }> {
    return request.get('/tenants/check-domain', { params: { domain } })
  },

  validateLimits(id: string): Promise<{
    canAddUser: boolean
    canAddApp: boolean
    currentUsers: number
    currentApps: number
  }> {
    return request.get(`/tenants/${id}/validate-limits`)
  }
}
