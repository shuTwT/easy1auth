import request from '../utils/request'
import type {
  AuditLog,
  AuditLogQueryDto,
  AuditLogListResponse,
  AuditLogStats
} from '../types/audit'

export const auditApi = {
  getList(query: AuditLogQueryDto): Promise<{ status: string; data: AuditLogListResponse }> {
    return request.get('/audit-logs', { params: query })
  },

  getById(id: string): Promise<{ status: string; data: AuditLog }> {
    return request.get(`/audit-logs/${id}`)
  },

  getStats(): Promise<{ status: string; data: AuditLogStats }> {
    return request.get('/audit-logs/stats')
  },

  export(format: 'csv' | 'json' = 'json', query?: Partial<AuditLogQueryDto>): Promise<Blob> {
    return request.get('/audit-logs/export', {
      params: { format, ...query },
      responseType: 'blob'
    })
  },

  cleanup(days: number = 90): Promise<{ status: string; message: string }> {
    return request.delete(`/audit-logs/cleanup?days=${days}`)
  }
}
