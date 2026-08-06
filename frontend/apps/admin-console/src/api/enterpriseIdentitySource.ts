import request from '@/utils/request'
import type { EnterpriseIdentitySource, EnterpriseIdentitySourceInput, EnterpriseIdentityTask } from '@/types/enterpriseIdentitySource'

export const enterpriseIdentitySourceApi = {
  getStats: async (): Promise<any> => request.get('/enterprise-identity-sources/stats') as any,
  getList: async (params: { page?: number; pageSize?: number; search?: string; status?: string }): Promise<any> => request.get('/enterprise-identity-sources', { params }) as any,
  create: async (input: EnterpriseIdentitySourceInput): Promise<EnterpriseIdentitySource> => request.post('/enterprise-identity-sources', input) as any,
  update: async (id: string, input: EnterpriseIdentitySourceInput): Promise<EnterpriseIdentitySource> => request.put(`/enterprise-identity-sources/${id}`, input) as any,
  delete: async (id: string): Promise<void> => request.delete(`/enterprise-identity-sources/${id}`) as any,
  sync: async (id: string): Promise<EnterpriseIdentityTask> => request.post(`/enterprise-identity-sources/${id}/sync`) as any,
  tasks: async (id: string): Promise<EnterpriseIdentityTask[]> => request.get(`/enterprise-identity-sources/${id}/tasks`) as any,
}
