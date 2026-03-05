import { AuditLog } from '@prisma/client'

export type AuditLogType = 'auth' | 'user' | 'application' | 'tenant' | 'role' | 'group' | 'system'
export type AuditLogAction = 
  | 'login' | 'logout' | 'login_failed'
  | 'create' | 'update' | 'delete' | 'view'
  | 'enable' | 'disable' | 'lock' | 'unlock'
  | 'assign' | 'revoke'
  | 'password_change' | 'password_reset'
  | 'token_generate' | 'token_revoke'
  | 'config_change'

export type AuditLogStatus = 'success' | 'failed'

export interface CreateAuditLogDto {
  userId?: string
  username?: string
  type: AuditLogType
  action: AuditLogAction | string
  resource: string
  resourceId?: string
  method?: string
  ip: string
  userAgent?: string
  location?: string
  status: AuditLogStatus
  errorMessage?: string
  changes?: Record<string, any>
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

export interface AuditLogExportDto {
  format?: 'csv' | 'json'
  startDate?: string
  endDate?: string
  type?: AuditLogType
  userId?: string
}
