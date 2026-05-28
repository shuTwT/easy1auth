import prisma from '../lib/prisma'

export interface DashboardStats {
  tenantCount: number
  userCount: number
  applicationCount: number
  todayLoginCount: number
}

export interface RecentLogin {
  username: string
  email: string
  ip: string
  time: string
  status: string
}

export interface DashboardData {
  stats: DashboardStats
  recentLogins: RecentLogin[]
}

export class DashboardService {
  async getDashboardData(tenantId: string): Promise<DashboardData> {
    const now = new Date()
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())

    const [tenantCount, userCount, applicationCount, todayLoginCount, recentLoginRecords] =
      await Promise.all([
        prisma.tenant.count(),
        prisma.user.count({ where: { tenantId } }),
        prisma.application.count({ where: { tenantId } }),
        prisma.auditLog.count({
          where: {
            tenantId,
            action: 'login',
            status: 'success',
            createdAt: { gte: todayStart }
          }
        }),
        prisma.auditLog.findMany({
          where: {
            tenantId,
            action: 'login'
          },
          select: {
            username: true,
            status: true,
            ip: true,
            createdAt: true,
            userId: true
          },
          orderBy: { createdAt: 'desc' },
          take: 10
        })
      ])

    const userIds = recentLoginRecords
      .map((log) => log.userId)
      .filter((id): id is string => id !== null)

    const userEmailMap = new Map<string, string>()
    if (userIds.length > 0) {
      const users = await prisma.user.findMany({
        where: {
          id: { in: userIds },
          tenantId
        },
        select: {
          id: true,
          email: true
        }
      })
      for (const user of users) {
        userEmailMap.set(user.id, user.email)
      }
    }

    const recentLogins: RecentLogin[] = recentLoginRecords.map((log) => ({
      username: log.username || '未知',
      email: (log.userId && userEmailMap.get(log.userId)) || '',
      ip: log.ip || '未知',
      time: log.createdAt.toISOString(),
      status: log.status
    }))

    return {
      stats: {
        tenantCount,
        userCount,
        applicationCount,
        todayLoginCount
      },
      recentLogins
    }
  }
}

export default new DashboardService()
