import request from '../utils/request'
import type {
  Application,
  CreateApplicationDto,
  UpdateApplicationDto,
  ApplicationQueryDto,
  ApplicationListResponse,
  ApplicationStats,
  RegenerateSecretResponse
} from '../types/application'

export const applicationApi = {
  getList(query: ApplicationQueryDto): Promise<{ status: string; data: ApplicationListResponse }> {
    return request.get('/applications', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: Application }> {
    return request.get(`/applications/${id}`)
  },

  create(data: CreateApplicationDto): Promise<{ status: string; message: string; data: Application }> {
    return request.post('/applications', data)
  },

  update(id: string, data: UpdateApplicationDto): Promise<{ status: string; message: string; data: Application }> {
    return request.put(`/applications/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/applications/${id}`)
  },

  updateStatus(id: string, status: string): Promise<{ status: string; message: string; data: Application }> {
    return request.put(`/applications/${id}/status`, { status })
  },

  regenerateSecret(id: string): Promise<{ status: string; message: string; data: RegenerateSecretResponse }> {
    return request.post(`/applications/${id}/regenerate-secret`)
  },

  getStats(): Promise<{ status: string; data: ApplicationStats }> {
    return request.get('/applications/stats')
  }
}
