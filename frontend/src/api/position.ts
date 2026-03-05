import request from '@/utils/request'
import type {
  Position,
  CreatePositionDto,
  UpdatePositionDto,
  PositionQueryDto,
  PositionListResponse,
  PositionStats
} from '@/types/position'

export const positionApi = {
  getList(query: PositionQueryDto): Promise<{ status: string; data: PositionListResponse }> {
    return request.get('/positions', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: Position }> {
    return request.get(`/positions/${id}`)
  },

  create(data: CreatePositionDto): Promise<{ status: string; message: string; data: Position }> {
    return request.post('/positions', data)
  },

  update(id: string, data: UpdatePositionDto): Promise<{ status: string; message: string; data: Position }> {
    return request.put(`/positions/${id}`, data)
  },

  delete(id: string): Promise<{ status: string; message: string }> {
    return request.delete(`/positions/${id}`)
  },

  getStats(): Promise<{ status: string; data: PositionStats }> {
    return request.get('/positions/stats')
  }
}
