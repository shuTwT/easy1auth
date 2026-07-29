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
  getList(query: PositionQueryDto): Promise<PositionListResponse> {
    return request.get('/positions', { params: query })
  },

  getById(id: string): Promise<Position> {
    return request.get(`/positions/${id}`)
  },

  create(data: CreatePositionDto): Promise<Position> {
    return request.post('/positions', data)
  },

  update(id: string, data: UpdatePositionDto): Promise<Position> {
    return request.put(`/positions/${id}`, data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/positions/${id}`)
  },

  getStats(): Promise<PositionStats> {
    return request.get('/positions/stats')
  }
}
