<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { 
  TrendCharts, 
  User, 
  Monitor, 
  Connection,
  ArrowUp,
  ArrowDown,
  MoreFilled,
  Refresh
} from '@element-plus/icons-vue'

const userStore = useUserStore()

const stats = ref([
  { 
    title: '租户总数', 
    value: 128, 
    icon: 'OfficeBuilding', 
    trend: '+12%',
    trendUp: true,
    color: 'primary'
  },
  { 
    title: '用户总数', 
    value: 12580, 
    icon: 'User', 
    trend: '+8.5%',
    trendUp: true,
    color: 'success'
  },
  { 
    title: '应用总数', 
    value: 356, 
    icon: 'Monitor', 
    trend: '+3.2%',
    trendUp: true,
    color: 'warning'
  },
  { 
    title: '今日登录', 
    value: 8920, 
    icon: 'Connection', 
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
  { title: '添加用户', icon: 'User', route: '/user', color: 'primary' },
  { title: '创建应用', icon: 'Monitor', route: '/application', color: 'success' },
  { title: '角色管理', icon: 'Collection', route: '/role', color: 'warning' },
  { title: '审计日志', icon: 'Document', route: '/audit', color: 'info' }
])

const loading = ref(false)

const handleRefresh = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

const getStatusType = (status: string) => {
  return status === 'success' ? 'success' : 'danger'
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
        <el-button @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <el-card v-for="stat in stats" :key="stat.title" class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" :class="stat.color">
            <el-icon :size="24">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatNumber(stat.value) }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
          <div class="stat-trend" :class="{ 'trend-up': stat.trendUp, 'trend-down': !stat.trendUp }">
            <el-icon :size="14">
              <component :is="stat.trendUp ? 'ArrowUp' : 'ArrowDown'" />
            </el-icon>
            <span>{{ stat.trend }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-row :gutter="24" class="content-row">
      <el-col :span="16">
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近登录</span>
              <el-button link type="primary">
                查看全部
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <el-table :data="recentLogins" style="width: 100%" :show-header="true">
            <el-table-column prop="username" label="用户名" width="120">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="28" class="user-avatar">
                    {{ row.username.charAt(0).toUpperCase() }}
                  </el-avatar>
                  <span>{{ row.username }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" width="180" />
            <el-table-column prop="ip" label="IP地址" width="130" />
            <el-table-column prop="time" label="登录时间" width="170" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="content-card" style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <span class="card-title">登录趋势</span>
              <el-dropdown trigger="click">
                <el-button link>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item>最近7天</el-dropdown-item>
                    <el-dropdown-item>最近30天</el-dropdown-item>
                    <el-dropdown-item>最近90天</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <div class="chart-placeholder">
            <el-icon :size="48" color="#CBD5E1"><TrendCharts /></el-icon>
            <p>登录趋势图表</p>
            <p class="chart-hint">集成图表库后显示数据可视化</p>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="content-card quick-actions-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <div 
              v-for="action in quickActions" 
              :key="action.title" 
              class="quick-action-item"
              :class="action.color"
            >
              <div class="action-icon">
                <el-icon :size="24"><component :is="action.icon" /></el-icon>
              </div>
              <span class="action-title">{{ action.title }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="content-card" style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统公告</span>
            </div>
          </template>
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
        </el-card>

        <el-card class="content-card system-info-card" style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统信息</span>
            </div>
          </template>
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
              <span class="info-label">CPU使用率</span>
              <el-progress :percentage="45" :stroke-width="6" />
            </div>
            <div class="info-item">
              <span class="info-label">内存使用率</span>
              <el-progress :percentage="68" :stroke-width="6" status="warning" />
            </div>
            <div class="info-item">
              <span class="info-label">磁盘使用率</span>
              <el-progress :percentage="32" :stroke-width="6" status="success" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
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

.content-row {
  margin-top: 0;
}

.content-card {
  border-radius: var(--border-radius-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: var(--primary-gradient);
  color: white;
  font-size: 12px;
  font-weight: 600;
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
  
  .content-row .el-col {
    span: 24;
    margin-bottom: 24px;
  }
}
</style>
