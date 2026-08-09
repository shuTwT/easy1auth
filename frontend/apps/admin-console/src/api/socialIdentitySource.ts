import request from '../utils/request'
import type {
  SocialIdentitySource,
  SocialIdentitySourceListResponse,
  CreateSocialIdentitySourceDto,
  UpdateSocialIdentitySourceDto,
  SocialIdentitySourceStats,
} from '../types/socialIdentitySource'

export const socialIdentitySourceApi = {
  async getStats(): Promise<SocialIdentitySourceStats> {
    const response: any = await request.get('/social-identity-sources/stats')
    return response
  },

  async getList(params?: { type?: string; status?: string; search?: string; page?: number; pageSize?: number }): Promise<SocialIdentitySourceListResponse> {
    const response: any = await request.get('/social-identity-sources', { params })
    return response
  },

  async getById(id: string): Promise<SocialIdentitySource> {
    const response: any = await request.get(`/social-identity-sources/${id}`)
    return response
  },

  async create(data: CreateSocialIdentitySourceDto): Promise<SocialIdentitySource> {
    const response: any = await request.post('/social-identity-sources', data)
    return response
  },

  async update(id: string, data: UpdateSocialIdentitySourceDto): Promise<SocialIdentitySource> {
    const response: any = await request.put(`/social-identity-sources/${id}`, data)
    return response
  },

  delete(id: string): Promise<void> {
    return request.delete(`/social-identity-sources/${id}`)
  },
}
