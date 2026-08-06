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
  getList(query: ApplicationQueryDto): Promise<ApplicationListResponse> {
    return request.get('/applications', { params: query })
  },

  getById(id: string): Promise<Application> {
    return request.get(`/applications/${id}`)
  },

  create(data: CreateApplicationDto): Promise<Application> {
    return request.post('/applications', data)
  },

  update(id: string, data: UpdateApplicationDto): Promise<Application> {
    return request.put(`/applications/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/applications/${id}`)
  },

  updateStatus(id: string, status: string): Promise<Application> {
    return request.put(`/applications/${id}/status`, { status })
  },

  regenerateSecret(id: string): Promise<RegenerateSecretResponse> {
    return request.post(`/applications/${id}/regenerate-secret`)
  },

  getStats(): Promise<ApplicationStats> {
    return request.get('/applications/stats')
  }
}
