import { Request, Response, NextFunction } from 'express'
import auditService from '../services/audit.service'
import { AuthRequest } from './auth'

interface AuditConfig {
  excludePaths?: string[]
  excludeMethods?: string[]
}

const defaultConfig: AuditConfig = {
  excludePaths: ['/health', '/oauth2', '/api/auth/login', '/api/auth/register'],
  excludeMethods: ['OPTIONS']
}

export function auditMiddleware(config: AuditConfig = defaultConfig) {
  return async (req: Request, res: Response, next: NextFunction) => {
    if (config.excludeMethods?.includes(req.method)) {
      return next()
    }

    if (config.excludePaths?.some(path => req.path.startsWith(path))) {
      return next()
    }

    const startTime = Date.now()

    const originalEnd = res.end
    res.end = function(chunk?: any, encoding?: any, cb?: any): any {
      const responseTime = Date.now() - startTime

      const authReq = req as AuthRequest
      const tenantId = authReq.tenantId
      const userId = authReq.userId

      if (tenantId && req.path.startsWith('/api/')) {
        const auditData = {
          userId,
          username: (authReq as any).user?.username,
          type: getAuditType(req.path) as any,
          action: getAuditAction(req.method, req.path),
          resource: getResource(req.path),
          resourceId: getResourceId(req) as string | undefined,
          method: req.method,
          ip: getClientIp(req),
          userAgent: req.headers['user-agent'],
          status: res.statusCode < 400 ? 'success' as const : 'failed' as const,
          errorMessage: res.statusCode >= 400 ? (res as any).statusMessage : undefined,
          changes: extractChanges(req)
        }

        auditService.create(tenantId, auditData).catch(err => {
          console.error('Failed to create audit log:', err)
        })
      }

      return originalEnd.call(this, chunk, encoding, cb)
    }

    next()
  }
}

function getAuditType(path: string): string {
  if (path.includes('/auth')) return 'auth'
  if (path.includes('/users')) return 'user'
  if (path.includes('/applications')) return 'application'
  if (path.includes('/tenants')) return 'tenant'
  if (path.includes('/roles')) return 'role'
  if (path.includes('/groups')) return 'group'
  return 'system'
}

function getAuditAction(method: string, path: string): string {
  if (path.includes('/login')) return 'login'
  if (path.includes('/logout')) return 'logout'
  if (path.includes('/password')) return 'password_change'

  switch (method) {
    case 'POST':
      return 'create'
    case 'PUT':
    case 'PATCH':
      return 'update'
    case 'DELETE':
      return 'delete'
    case 'GET':
      return 'view'
    default:
      return 'unknown'
  }
}

function getResource(path: string): string {
  const parts = path.split('/').filter(Boolean)
  if (parts.length >= 2) {
    return parts[1]
  }
  return 'unknown'
}

function getResourceId(req: Request): string | undefined {
  const id = req.params.id || req.params.userId || req.params.applicationId
  return Array.isArray(id) ? id[0] : id
}

function getClientIp(req: Request): string {
  return (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim() ||
         req.headers['x-real-ip'] as string ||
         req.connection.remoteAddress ||
         req.socket.remoteAddress ||
         'unknown'
}

function extractChanges(req: Request): Record<string, any> | undefined {
  if (req.method === 'GET') return undefined

  const body = req.body
  if (!body || Object.keys(body).length === 0) return undefined

  const sensitiveFields = ['password', 'clientSecret', 'secret', 'token']
  const sanitized: Record<string, any> = {}

  for (const [key, value] of Object.entries(body)) {
    if (sensitiveFields.some(field => key.toLowerCase().includes(field))) {
      sanitized[key] = '******'
    } else {
      sanitized[key] = value
    }
  }

  return sanitized
}

export async function createAuditLog(
  tenantId: string,
  data: {
    userId?: string
    username?: string
    type: string
    action: string
    resource: string
    resourceId?: string
    method?: string
    ip: string
    userAgent?: string
    status: 'success' | 'failed'
    errorMessage?: string
    changes?: Record<string, any>
  }
): Promise<void> {
  try {
    await auditService.create(tenantId, {
      ...data,
      type: data.type as any,
      resource: data.resource
    })
  } catch (error) {
    console.error('Failed to create audit log:', error)
  }
}
