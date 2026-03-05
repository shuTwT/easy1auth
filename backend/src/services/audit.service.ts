import { AuditLog, Prisma } from '@prisma/client'
import prisma from '../lib/prisma'
import { 
  CreateAuditLogDto, 
  AuditLogQueryDto, 
  AuditLogListResponse,
  AuditLogStats
} from '../types/audit.types'
import { v4 as uuidv4 } from 'uuid'

export class AuditService {
  async create(tenantId: string, data: CreateAuditLogDto): Promise<AuditLog> {
    return prisma.auditLog.create({
      data: {
        id: uuidv4(),
        tenantId,
        userId: data.userId,
        username: data.username,
        type: data.type,
        action: data.action,
        resource: data.resource,
        resourceId: data.resourceId,
        method: data.method,
        ip: data.ip,
        userAgent: data.userAgent,
        location: data.location,
        status: data.status,
        errorMessage: data.errorMessage,
        changes: data.changes ?? Prisma.JsonNull
      }
    })
  }

  async findById(tenantId: string, id: string): Promise<AuditLog | null> {
    return prisma.auditLog.findFirst({
      where: {
        id,
        tenantId
      }
    })
  }

  async findAll(tenantId: string, query: AuditLogQueryDto): Promise<AuditLogListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 20
    const skip = (page - 1) * pageSize

    const where: Prisma.AuditLogWhereInput = {
      tenantId
    }

    if (query.userId) {
      where.userId = query.userId
    }

    if (query.username) {
      where.username = {
        contains: query.username
      }
    }

    if (query.type) {
      where.type = query.type
    }

    if (query.action) {
      where.action = query.action
    }

    if (query.resource) {
      where.resource = query.resource
    }

    if (query.resourceId) {
      where.resourceId = query.resourceId
    }

    if (query.status) {
      where.status = query.status
    }

    if (query.ip) {
      where.ip = {
        contains: query.ip
      }
    }

    if (query.startDate || query.endDate) {
      where.createdAt = {}
      if (query.startDate) {
        where.createdAt.gte = new Date(query.startDate)
      }
      if (query.endDate) {
        const endDate = new Date(query.endDate)
        endDate.setHours(23, 59, 59, 999)
        where.createdAt.lte = endDate
      }
    }

    const [logs, total] = await Promise.all([
      prisma.auditLog.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { createdAt: 'desc' }
      }),
      prisma.auditLog.count({ where })
    ])

    return {
      logs,
      total,
      page,
      pageSize
    }
  }

  async getStats(tenantId: string): Promise<AuditLogStats> {
    const now = new Date()
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const weekStart = new Date(todayStart.getTime() - 7 * 24 * 60 * 60 * 1000)
    const monthStart = new Date(todayStart.getTime() - 30 * 24 * 60 * 60 * 1000)

    const [totalLogs, successLogs, failedLogs, todayLogs, weekLogs, monthLogs] = await Promise.all([
      prisma.auditLog.count({
        where: { tenantId }
      }),
      prisma.auditLog.count({
        where: { tenantId, status: 'success' }
      }),
      prisma.auditLog.count({
        where: { tenantId, status: 'failed' }
      }),
      prisma.auditLog.count({
        where: {
          tenantId,
          createdAt: { gte: todayStart }
        }
      }),
      prisma.auditLog.count({
        where: {
          tenantId,
          createdAt: { gte: weekStart }
        }
      }),
      prisma.auditLog.count({
        where: {
          tenantId,
          createdAt: { gte: monthStart }
        }
      })
    ])

    const topActions = await prisma.auditLog.groupBy({
      by: ['action'],
      where: { tenantId },
      _count: {
        action: true
      },
      orderBy: {
        _count: {
          action: 'desc'
        }
      },
      take: 10
    })

    const topUsers = await prisma.auditLog.groupBy({
      by: ['userId', 'username'],
      where: {
        tenantId,
        userId: { not: null }
      },
      _count: {
        userId: true
      },
      orderBy: {
        _count: {
          userId: 'desc'
        }
      },
      take: 10
    })

    const topIps = await prisma.auditLog.groupBy({
      by: ['ip'],
      where: { tenantId },
      _count: {
        ip: true
      },
      orderBy: {
        _count: {
          ip: 'desc'
        }
      },
      take: 10
    })

    return {
      totalLogs,
      successLogs,
      failedLogs,
      todayLogs,
      weekLogs,
      monthLogs,
      topActions: topActions.map(item => ({
        action: item.action,
        count: item._count.action
      })),
      topUsers: topUsers.map(item => ({
        userId: item.userId || '',
        username: item.username || '',
        count: item._count.userId
      })),
      topIps: topIps.map(item => ({
        ip: item.ip,
        count: item._count.ip
      }))
    }
  }

  async deleteOldLogs(tenantId: string, daysToKeep: number = 90): Promise<number> {
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - daysToKeep)

    const result = await prisma.auditLog.deleteMany({
      where: {
        tenantId,
        createdAt: {
          lt: cutoffDate
        }
      }
    })

    return result.count
  }

  async exportLogs(
    tenantId: string,
    format: 'csv' | 'json' = 'json',
    query: AuditLogQueryDto
  ): Promise<string> {
    const { logs } = await this.findAll(tenantId, { ...query, pageSize: 10000 })

    if (format === 'json') {
      return JSON.stringify(logs, null, 2)
    }

    const headers = [
      'ID',
      'User ID',
      'Username',
      'Type',
      'Action',
      'Resource',
      'Resource ID',
      'Method',
      'IP',
      'User Agent',
      'Location',
      'Status',
      'Error Message',
      'Created At'
    ]

    const rows = logs.map(log => [
      log.id,
      log.userId || '',
      log.username || '',
      log.type,
      log.action,
      log.resource,
      log.resourceId || '',
      log.method || '',
      log.ip,
      log.userAgent || '',
      log.location || '',
      log.status,
      log.errorMessage || '',
      log.createdAt.toISOString()
    ])

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n')

    return csvContent
  }
}

export default new AuditService()
