export type AuditLogType = 'auth' | 'user' | 'application' | 'tenant' | 'role' | 'group' | 'system'
export type AuditLogStatus = 'success' | 'failed'

export interface AuditLog {
  id: string
  tenantId: string
  userId: string | null
  username: string | null
  type: string
  action: string
  resource: string
  resourceId: string | null
  method: string | null
  ip: string
  userAgent: string | null
  location: string | null
  status: string
  errorMessage: string | null
  changes: any | null
  createdAt: string
}

export interface AuditLogQueryDto {
  page?: number
  pageSize?: number
  userId?: string
  username?: string
  type?: AuditLogType
  action?: string
  resource?: string
  resourceId?: string
  status?: AuditLogStatus
  startDate?: string
  endDate?: string
  ip?: string
}

export interface AuditLogListResponse {
  logs: AuditLog[]
  total: number
  page: number
  pageSize: number
}

export interface AuditLogStats {
  totalLogs: number
  successLogs: number
  failedLogs: number
  todayLogs: number
  weekLogs: number
  monthLogs: number
  topActions: Array<{
    action: string
    count: number
  }>
  topUsers: Array<{
    userId: string
    username: string
    count: number
  }>
  topIps: Array<{
    ip: string
    count: number
  }>
}
