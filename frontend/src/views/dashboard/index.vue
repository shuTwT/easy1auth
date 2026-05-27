<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { Building2, TrendingUp, User, Monitor, Link, ArrowUp, ArrowDown, MoreHorizontal, RefreshCw, ArrowRight, Library, FileText } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Progress } from '@/components/ui/progress'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const userStore = useUserStore()

const stats = ref([
  { 
    title: '租户总数', 
    value: 128, 
    icon: Building2, 
    trend: '+12%',
    trendUp: true,
    color: 'primary'
  },
  { 
    title: '用户总数', 
    value: 12580, 
    icon: User, 
    trend: '+8.5%',
    trendUp: true,
    color: 'success'
  },
  { 
    title: '应用总数', 
    value: 356, 
    icon: Monitor, 
    trend: '+3.2%',
    trendUp: true,
    color: 'warning'
  },
  { 
    title: '今日登录', 
    value: 8920, 
    icon: Link, 
    trend: '-2.1%',
    trendUp: false,
    color: 'info'
  }
])

const recentLogins = ref([
  { username: 'admin', email: 'admin@example.com', ip: '192.168.1.1', time: '2024-01-15 14:30:00', status: 'success' },
  { username: 'user1', email: 'user1@example.com', ip: '192.168.1.2', time: '2024-01-15 14:25:00', status: 'success' },
  { username: 'user2', email: 'user2@example.com', ip: '192.168.1.3', time: '2024-01-15 14:20:00', status: 'failed' },
  { username: 'user3', email: 'user3@example.com', ip: '192.168.1.4', time: '2024-01-15 14:15:00', status: 'success' },
  { username: 'user4', email: 'user4@example.com', ip: '192.168.1.5', time: '2024-01-15 14:10:00', status: 'success' }
])

const quickActions = ref([
  { title: '添加用户', icon: User, route: '/user', color: 'primary' },
  { title: '创建应用', icon: Monitor, route: '/application', color: 'success' },
  { title: '角色管理', icon: Library, route: '/role', color: 'warning' },
  { title: '审计日志', icon: FileText, route: '/audit', color: 'info' }
])

const loading = ref(false)

const handleRefresh = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

const getStatusVariant = (status: string) => {
  return status === 'success' ? 'default' : 'destructive'
}

const getStatusText = (status: string) => {
  return status === 'success' ? '成功' : '失败'
}

const formatNumber = (num: number) => {
  return num.toLocaleString()
}
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">控制台</h1>
        <p class="page-subtitle">欢迎回来，{{ userStore.userInfo?.username || '管理员' }}</p>
      </div>
      <div class="header-actions">
        <Button @click="handleRefresh" :disabled="loading">
          <RefreshCw class="w-4 h-4 mr-2" :class="{ 'animate-spin': loading }" />
          刷新数据
        </Button>
      </div>
    </div>

    <div class="stats-grid">
      <Card v-for="stat in stats" :key="stat.title" class="stat-card">
        <CardContent class="pt-6">
          <div class="stat-content">
            <div class="stat-icon" :class="stat.color">
              <component :is="stat.icon" class="w-6 h-6" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(stat.value) }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
            <div class="stat-trend" :class="{ 'trend-up': stat.trendUp, 'trend-down': !stat.trendUp }">
              <component :is="stat.trendUp ? ArrowUp : ArrowDown" class="w-3.5 h-3.5" />
              <span>{{ stat.trend }}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <Card>
          <CardHeader>
            <div class="card-header">
              <CardTitle>最近登录</CardTitle>
              <Button variant="link" size="sm">
                查看全部
                <ArrowRight class="w-4 h-4 ml-1" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>用户名</TableHead>
                  <TableHead>邮箱</TableHead>
                  <TableHead>IP地址</TableHead>
                  <TableHead>登录时间</TableHead>
                  <TableHead>状态</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="login in recentLogins" :key="login.username + login.time">
                  <TableCell>
                    <div class="user-cell">
                      <Avatar class="h-7 w-7">
                        <AvatarFallback class="text-xs">
                          {{ login.username.charAt(0).toUpperCase() }}
                        </AvatarFallback>
                      </Avatar>
                      <span>{{ login.username }}</span>
                    </div>
                  </TableCell>
                  <TableCell>{{ login.email }}</TableCell>
                  <TableCell>{{ login.ip }}</TableCell>
                  <TableCell>{{ login.time }}</TableCell>
                  <TableCell>
                    <Badge :variant="getStatusVariant(login.status)">
                      {{ getStatusText(login.status) }}
                    </Badge>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div class="card-header">
              <CardTitle>登录趋势</CardTitle>
              <DropdownMenu>
                <DropdownMenuTrigger as-child>
                  <Button variant="ghost" size="sm">
                    <MoreHorizontal class="w-4 h-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem>最近7天</DropdownMenuItem>
                  <DropdownMenuItem>最近30天</DropdownMenuItem>
                  <DropdownMenuItem>最近90天</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </CardHeader>
          <CardContent>
            <div class="chart-placeholder">
              <TrendingUp class="w-12 h-12 text-slate-300" />
              <p>登录趋势图表</p>
              <p class="chart-hint">集成图表库后显示数据可视化</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div class="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>快捷操作</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="quick-actions">
              <div 
                v-for="action in quickActions" 
                :key="action.title" 
                class="quick-action-item"
                :class="action.color"
              >
                <div class="action-icon">
                  <component :is="action.icon" class="w-6 h-6" />
                </div>
                <span class="action-title">{{ action.title }}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>系统公告</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="announcement-list">
              <div class="announcement-item">
                <div class="announcement-dot"></div>
                <div class="announcement-content">
                  <div class="announcement-title">系统升级通知</div>
                  <div class="announcement-desc">系统将于本周六凌晨进行升级维护</div>
                  <div class="announcement-time">2024-01-15</div>
                </div>
              </div>
              <div class="announcement-item">
                <div class="announcement-dot"></div>
                <div class="announcement-content">
                  <div class="announcement-title">新功能上线</div>
                  <div class="announcement-desc">支持微信、QQ等社会化登录</div>
                  <div class="announcement-time">2024-01-10</div>
                </div>
              </div>
              <div class="announcement-item">
                <div class="announcement-dot"></div>
                <div class="announcement-content">
                  <div class="announcement-title">安全提醒</div>
                  <div class="announcement-desc">请定期更新密码，确保账号安全</div>
                  <div class="announcement-time">2024-01-05</div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>系统信息</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="system-info">
              <div class="info-item">
                <span class="info-label">系统版本</span>
                <span class="info-value">v1.0.0</span>
              </div>
              <div class="info-item">
                <span class="info-label">运行时间</span>
                <span class="info-value">30 天</span>
              </div>
              <div class="info-item">
                <div class="flex justify-between mb-2">
                  <span class="info-label">CPU使用率</span>
                  <span class="info-value">45%</span>
                </div>
                <Progress :model-value="45" class="h-1.5" />
              </div>
              <div class="info-item">
                <div class="flex justify-between mb-2">
                  <span class="info-label">内存使用率</span>
                  <span class="info-value">68%</span>
                </div>
                <Progress :model-value="68" class="h-1.5" />
              </div>
              <div class="info-item">
                <div class="flex justify-between mb-2">
                  <span class="info-label">磁盘使用率</span>
                  <span class="info-value">32%</span>
                </div>
                <Progress :model-value="32" class="h-1.5" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-hover-shadow);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: var(--primary-gradient);
}

.stat-icon.success {
  background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%);
}

.stat-icon.info {
  background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-title {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 20px;
}

.stat-trend.trend-up {
  color: var(--success-color);
  background: rgba(16, 185, 129, 0.1);
}

.stat-trend.trend-down {
  color: var(--danger-color);
  background: rgba(239, 68, 68, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
}

.chart-placeholder p {
  margin: 12px 0 4px;
  font-size: 14px;
}

.chart-hint {
  font-size: 12px;
  color: var(--text-light);
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border-radius: var(--border-radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.quick-action-item.primary {
  background: rgba(3, 105, 161, 0.08);
  color: var(--primary-color);
}

.quick-action-item.primary:hover {
  background: rgba(3, 105, 161, 0.15);
}

.quick-action-item.success {
  background: rgba(16, 185, 129, 0.08);
  color: var(--success-color);
}

.quick-action-item.success:hover {
  background: rgba(16, 185, 129, 0.15);
}

.quick-action-item.warning {
  background: rgba(245, 158, 11, 0.08);
  color: var(--warning-color);
}

.quick-action-item.warning:hover {
  background: rgba(245, 158, 11, 0.15);
}

.quick-action-item.info {
  background: rgba(14, 165, 233, 0.08);
  color: var(--info-color);
}

.quick-action-item.info:hover {
  background: rgba(14, 165, 233, 0.15);
}

.action-icon {
  margin-bottom: 8px;
}

.action-title {
  font-size: 13px;
  font-weight: 500;
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.announcement-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.announcement-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.announcement-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color);
  margin-top: 6px;
  flex-shrink: 0;
}

.announcement-content {
  flex: 1;
}

.announcement-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.announcement-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.announcement-time {
  font-size: 12px;
  color: var(--text-muted);
}

.system-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
