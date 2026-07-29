import request from '@/utils/request'

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

export const dashboardApi = {
  getStats(): Promise<DashboardData> {
    return request.get('/dashboard/stats')
  }
}
