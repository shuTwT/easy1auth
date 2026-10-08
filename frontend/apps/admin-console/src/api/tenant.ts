import request from '@/utils/request'
import type { Tenant, TenantInput, TenantList, TenantQuery } from '@/types/tenant'

export const tenantApi = {
  list(params: TenantQuery): Promise<TenantList> {
    return request.get('/pool-tenants', { params })
  },
  create(input: TenantInput): Promise<Tenant> {
    return request.post('/pool-tenants', input)
  },
  update(id: string, input: TenantInput): Promise<Tenant> {
    return request.put(`/pool-tenants/${id}`, input)
  },
  delete(id: string): Promise<void> {
    return request.delete(`/pool-tenants/${id}`)
  },
}
