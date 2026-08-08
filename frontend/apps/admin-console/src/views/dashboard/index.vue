<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStats, RecentLogin } from '@/api/dashboard'
import { Dropdown, Empty, Spin, Table as AntTable, Tag, message } from 'antdv-next'
import { Building2, TrendingUp, User, Monitor, Link, MoreHorizontal, RefreshCw, ArrowRight, Library, FileText } from '@lucide/vue'
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

const getStatusText = (status: string) => {
  return status === 'success' ? '成功' : '失败'
}

const recentLoginColumns = [
  { title: '用户名', key: 'username' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: 'IP地址', dataIndex: 'ip', key: 'ip' },
  { title: '登录时间', key: 'time' },
  { title: '状态', key: 'status' },
]

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

const trendRangeItems = [
  { key: '7d', label: '最近7天' },
  { key: '30d', label: '最近30天' },
  { key: '90d', label: '最近90天' },
]

const announcements = [
  { title: '系统升级通知', desc: '系统将于本周六凌晨进行升级维护', time: '2024-01-15' },
  { title: '新功能上线', desc: '支持微信、QQ等社会化登录', time: '2024-01-10' },
  { title: '安全提醒', desc: '请定期更新密码，确保账号安全', time: '2024-01-05' }
]

const systemInfo = {
  version: 'v1.0.0',
  uptime: '30 天',
  cpuUsage: 45,
  memoryUsage: 68,
  diskUsage: 32
}

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const res = await dashboardApi.getStats()
    const data = res
    stats.value = data.stats
    recentLogins.value = data.recentLogins
  } catch {
    message.error('获取控制台数据失败')
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
        <Button @click="handleRefresh" :disabled="loading" :loading="loading">
          <RefreshCw v-if="!loading" class="w-4 h-4 mr-2" />
          刷新数据
        </Button>
      </div>
    </div>

    <div class="stats-grid">
      <Card v-for="stat in statCards" :key="stat.key" class="stat-card">
        <div class="pt-6">
          <div class="stat-content">
            <div class="stat-icon" :class="stat.color">
              <component :is="stat.icon" class="size-6" />
            </div>
            <div class="stat-info">
              <div class="stat-value">
                <span v-if="loading" class="loading-placeholder">--</span>
                <span v-else>{{ formatNumber(stats[stat.key]) }}</span>
              </div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
          </div>
          </div>
        </Card>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-6">
        <Card>
          <div>
            <div class="card-header">
              <h3>最近登录</h3>
              <Button type="link" size="small" @click="navigateTo('/audit')">
                查看全部
                <ArrowRight class="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
          <div>
            <div v-if="loading" class="flex justify-center py-12">
              <Spin />
            </div>

            <Empty v-else-if="recentLogins.length === 0" class="py-12" description="暂无登录记录" />

            <AntTable v-else :columns="recentLoginColumns" :data-source="recentLogins" :pagination="false" :row-key="login => `${login.username}-${login.time}`" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'username'">
                  <div class="user-cell"><Avatar class="size-7"><span class="text-xs">{{ record.username.charAt(0).toUpperCase() }}</span></Avatar><span>{{ record.username }}</span></div>
                </template>
                <template v-else-if="column.key === 'email'">{{ record.email || '-' }}</template>
                <template v-else-if="column.key === 'time'">{{ formatTime(record.time) }}</template>
                <Tag v-else-if="column.key === 'status'" :color="record.status === 'success' ? 'success' : 'error'">{{ getStatusText(record.status) }}</Tag>
              </template>
            </AntTable>
          </div>
        </Card>

        <Card>
          <div>
            <div class="card-header">
              <h3>登录趋势</h3>
              <Dropdown :menu="{ items: trendRangeItems }" :trigger="['click']" placement="bottomRight">
                <Button type="text" size="small" aria-label="选择登录趋势时间范围"><MoreHorizontal class="size-4" /></Button>
              </Dropdown>
            </div>
          </div>
          <div>
            <div class="chart-placeholder">
              <TrendingUp class="w-12 h-12 text-muted-foreground" />
              <p>登录趋势图表</p>
              <p class="chart-hint">集成图表库后显示数据可视化</p>
            </div>
          </div>
        </Card>
      </div>

      <div class="space-y-6">
        <Card>
          <div>
            <h3>快捷操作</h3>
          </div>
          <div>
            <div class="quick-actions">
              <div
                v-for="action in quickActions"
                :key="action.title"
                class="quick-action-item"
                :class="action.color"
                @click="navigateTo(action.route)"
              >
                <div class="action-icon">
                  <component :is="action.icon" class="size-6" />
                </div>
                <span class="action-title">{{ action.title }}</span>
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div>
            <h3>系统公告</h3>
          </div>
          <div>
            <div class="announcement-list">
              <div v-for="item in announcements" :key="item.title" class="announcement-item">
                <div class="announcement-dot"></div>
                <div class="announcement-content">
                  <div class="announcement-title">{{ item.title }}</div>
                  <div class="announcement-desc">{{ item.desc }}</div>
                  <div class="announcement-time">{{ item.time }}</div>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <Card>
          <div>
            <h3>系统信息</h3>
          </div>
          <div>
            <div class="system-info">
              <div class="info-item">
                <span class="info-label">系统版本</span>
                <span class="info-value">{{ systemInfo.version }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">运行时间</span>
                <span class="info-value">{{ systemInfo.uptime }}</span>
              </div>
              <div class="info-item">
                <div class="flex justify-between mb-2">
                  <span class="info-label">CPU使用率</span>
                  <span class="info-value">{{ systemInfo.cpuUsage }}%</span>
                </div>
                <Progress :percent="systemInfo.cpuUsage" class="h-1.5" />
              </div>
              <div class="info-item">
                <div class="flex justify-between mb-2">
                  <span class="info-label">内存使用率</span>
                  <span class="info-value">{{ systemInfo.memoryUsage }}%</span>
                </div>
                <Progress :percent="systemInfo.memoryUsage" class="h-1.5" />
              </div>
              <div class="info-item">
                <div class="flex justify-between mb-2">
                  <span class="info-label">磁盘使用率</span>
                  <span class="info-value">{{ systemInfo.diskUsage }}%</span>
                </div>
                <Progress :percent="systemInfo.diskUsage" class="h-1.5" />
              </div>
            </div>
          </div>
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
  @apply bg-emerald-500/10 text-emerald-500;
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
  @apply border-emerald-500/20 hover:bg-emerald-500/5;
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

.announcement-list {
  @apply flex flex-col gap-4;
}

.announcement-item {
  @apply flex gap-3 pb-4 border-b last:pb-0 last:border-b-0;
}

.announcement-dot {
  @apply w-2 h-2 rounded-full bg-primary mt-1.5 shrink-0;
}

.announcement-content {
  @apply flex-1;
}

.announcement-title {
  @apply text-sm font-medium mb-1;
}

.announcement-desc {
  @apply text-xs text-muted-foreground mb-1;
}

.announcement-time {
  @apply text-xs text-muted-foreground/60;
}

.system-info {
  @apply flex flex-col gap-4;
}

.info-item {
  @apply flex flex-col gap-2;
}

.info-label {
  @apply text-sm text-muted-foreground;
}

.info-value {
  @apply text-sm font-medium;
}
</style>
