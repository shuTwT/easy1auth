<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStats, RecentLogin } from '@/api/dashboard'
import { Building2, TrendingUp, User, Monitor, Link, ArrowUp, ArrowDown, MoreHorizontal, RefreshCw, ArrowRight, Library, FileText, Loader2 } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const router = useRouter()
const userStore = useUserStore()

const stats = ref<DashboardStats>({
  tenantCount: 0,
  userCount: 0,
  applicationCount: 0,
  todayLoginCount: 0
})

const recentLogins = ref<RecentLogin[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const getStatusVariant = (status: string) => {
  return status === 'success' ? 'default' : 'destructive'
}

const getStatusText = (status: string) => {
  return status === 'success' ? '成功' : '失败'
}

const formatNumber = (num: number) => {
  if (num === 0) return '--'
  return num.toLocaleString()
}

const formatTime = (timeStr: string) => {
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const statCards = [
  { key: 'tenantCount' as const, title: '租户总数', icon: Building2, color: 'primary' },
  { key: 'userCount' as const, title: '用户总数', icon: User, color: 'success' },
  { key: 'applicationCount' as const, title: '应用总数', icon: Monitor, color: 'warning' },
  { key: 'todayLoginCount' as const, title: '今日登录', icon: Link, color: 'info' }
]

const quickActions = [
  { title: '添加用户', icon: User, route: '/user', color: 'primary' },
  { title: '创建应用', icon: Monitor, route: '/application', color: 'success' },
  { title: '角色管理', icon: Library, route: '/role', color: 'warning' },
  { title: '审计日志', icon: FileText, route: '/audit', color: 'info' }
]

const fetchDashboardData = async () => {
  error.value = null
  loading.value = true
  try {
    const res = await dashboardApi.getStats()
    const data = res.data
    stats.value = data.stats
    recentLogins.value = data.recentLogins
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '获取控制台数据失败'
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  fetchDashboardData()
}

const navigateTo = (route: string) => {
  router.push(route)
}

onMounted(() => {
  fetchDashboardData()
})
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
          <RefreshCw v-if="!loading" class="w-4 h-4 mr-2" />
          <Loader2 v-else class="w-4 h-4 mr-2 animate-spin" />
          刷新数据
        </Button>
      </div>
    </div>

    <div v-if="error" class="error-banner mb-6 p-4 bg-destructive/10 border border-destructive/30 rounded-lg text-destructive text-sm">
      {{ error }}
    </div>

    <div class="stats-grid">
      <Card v-for="stat in statCards" :key="stat.key" class="stat-card">
        <CardContent class="pt-6">
          <div class="stat-content">
            <div class="stat-icon" :class="stat.color">
              <component :is="stat.icon" class="w-6 h-6" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                <span v-if="loading" class="loading-placeholder">--</span>
                <span v-else>{{ formatNumber(stats[stat.key]) }}</span>
              </div>
              <div class="stat-title">{{ stat.title }}</div>
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
              <Button variant="link" size="sm" @click="navigateTo('/audit')">
                查看全部
                <ArrowRight class="w-4 h-4 ml-1" />
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div v-if="loading" class="flex justify-center py-12">
              <Loader2 class="w-6 h-6 animate-spin text-muted-foreground" />
            </div>

            <div v-else-if="recentLogins.length === 0" class="empty-state py-12 text-center text-muted-foreground text-sm">
              暂无登录记录
            </div>

            <Table v-else>
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
                  <TableCell>{{ login.email || '-' }}</TableCell>
                  <TableCell>{{ login.ip }}</TableCell>
                  <TableCell>{{ formatTime(login.time) }}</TableCell>
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
                @click="navigateTo(action.route)"
              >
                <div class="action-icon">
                  <component :is="action.icon" class="w-6 h-6" />
                </div>
                <span class="action-title">{{ action.title }}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "@/styles/tailwind.css";
.dashboard {
  @apply space-y-6 p-6;
}

.page-header {
  @apply flex items-center justify-between;
}

.header-content {
  @apply space-y-1;
}

.page-title {
  @apply text-2xl font-bold tracking-tight;
}

.page-subtitle {
  @apply text-sm text-muted-foreground;
}

.header-actions {
  @apply flex items-center gap-3;
}

.stats-grid {
  @apply grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4;
}

.stat-card {
  @apply transition-shadow hover:shadow-md;
}

.stat-content {
  @apply flex items-center gap-4;
}

.stat-icon {
  @apply flex items-center justify-center w-12 h-12 rounded-lg;
}

.stat-icon.primary {
  @apply bg-primary/10 text-primary;
}

.stat-icon.success {
  @apply bg-green-500/10 text-green-500;
}

.stat-icon.warning {
  @apply bg-orange-500/10 text-orange-500;
}

.stat-icon.info {
  @apply bg-blue-500/10 text-blue-500;
}

.stat-info {
  @apply flex-1;
}

.stat-value {
  @apply text-2xl font-bold;
}

.loading-placeholder {
  @apply text-muted-foreground;
}

.stat-title {
  @apply text-sm text-muted-foreground;
}

.card-header {
  @apply flex items-center justify-between;
}

.user-cell {
  @apply flex items-center gap-2;
}

.chart-placeholder {
  @apply flex flex-col items-center justify-center py-16 text-center;
}

.chart-placeholder p {
  @apply text-muted-foreground mt-2;
}

.chart-hint {
  @apply text-sm text-muted-foreground/60;
}

.quick-actions {
  @apply grid grid-cols-2 gap-3;
}

.quick-action-item {
  @apply flex flex-col items-center justify-center p-4 rounded-lg border cursor-pointer transition-all hover:shadow-md hover:scale-105 gap-2;
}

.quick-action-item.primary {
  @apply border-primary/20 hover:bg-primary/5;
}

.quick-action-item.success {
  @apply border-green-500/20 hover:bg-green-500/5;
}

.quick-action-item.warning {
  @apply border-orange-500/20 hover:bg-orange-500/5;
}

.quick-action-item.info {
  @apply border-blue-500/20 hover:bg-blue-500/5;
}

.action-icon {
  @apply flex items-center justify-center w-10 h-10 rounded-full bg-muted;
}

.action-title {
  @apply text-sm font-medium;
}

.empty-state {
  @apply text-sm text-muted-foreground;
}

.error-banner {
  @apply p-4 rounded-lg text-sm;
}
</style>
