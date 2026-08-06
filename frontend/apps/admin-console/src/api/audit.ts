import request from '../utils/request'
import type {
  AuditLog,
  AuditLogQueryDto,
  AuditLogListResponse,
  AuditLogStats
} from '../types/audit'

export const auditApi = {
  getList(query: AuditLogQueryDto): Promise<AuditLogListResponse> {
    return request.get('/audit-logs', { params: query })
  },

  getById(id: string): Promise<AuditLog> {
    return request.get(`/audit-logs/${id}`)
  },

  getStats(): Promise<AuditLogStats> {
    return request.get('/audit-logs/stats')
  },

  export(format: 'csv' | 'json' = 'json', query?: Partial<AuditLogQueryDto>): Promise<Blob> {
    return request.get('/audit-logs/export', {
      params: { format, ...query },
      responseType: 'blob'
    })
  },

  cleanup(days: number = 90): Promise<void> {
    return request.delete(`/audit-logs/cleanup?days=${days}`)
  }
}
