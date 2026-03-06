<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditApi } from '@/api/audit'
import type { AuditLog, AuditLogQueryDto, AuditLogStats } from '@/types/audit'

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const stats = ref<AuditLogStats | null>(null)
const detailDialogVisible = ref(false)
const currentLog = ref<AuditLog | null>(null)

const queryForm = reactive<AuditLogQueryDto>({
  page: 1,
  pageSize: 20,
  username: '',
  type: undefined,
  action: '',
  status: undefined,
  startDate: '',
  endDate: '',
  ip: ''
})

const dateRange = ref<[string, string] | null>(null)

const loadLogs = async () => {
  loading.value = true
  try {
    if (dateRange.value) {
      queryForm.startDate = dateRange.value[0]
      queryForm.endDate = dateRange.value[1]
    }
    
    const res = await auditApi.getList(queryForm)
    logs.value = res.data.logs
    total.value = res.data.total
  } catch (error) {
    console.error('加载审计日志失败:', error)
    ElMessage.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await auditApi.getStats()
    stats.value = res.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadLogs()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.type = undefined
  queryForm.action = ''
  queryForm.status = undefined
  queryForm.ip = ''
  dateRange.value = null
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryForm.page = 1
  loadLogs()
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadLogs()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadLogs()
}

const handleViewDetail = (row: AuditLog) => {
  currentLog.value = row
  detailDialogVisible.value = true
}

const handleExport = async (format: 'csv' | 'json') => {
  try {
    const blob = await auditApi.export(format, queryForm)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.${format}`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleCleanup = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清理90天前的审计日志吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await auditApi.cleanup(90)
    ElMessage.success('清理成功')
    loadLogs()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清理失败:', error)
      ElMessage.error('清理失败')
    }
  }
}

const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    auth: '认证',
    user: '用户',
    application: '应用',
    tenant: '租户',
    role: '角色',
    group: '用户组',
    system: '系统'
  }
  return typeMap[type] || type
}

const getTypeTagType = (type: string) => {
  const typeColorMap: Record<string, string> = {
    auth: 'primary',
    user: 'success',
    application: 'warning',
    tenant: 'danger',
    role: 'info',
    group: '',
    system: ''
  }
  return typeColorMap[type] || ''
}

const getActionText = (action: string) => {
  const actionMap: Record<string, string> = {
    login: '登录',
    logout: '登出',
    login_failed: '登录失败',
    create: '创建',
    update: '更新',
    delete: '删除',
    view: '查看',
    enable: '启用',
    disable: '禁用',
    lock: '锁定',
    unlock: '解锁',
    assign: '分配',
    revoke: '撤销',
    password_change: '修改密码',
    password_reset: '重置密码',
    token_generate: '生成令牌',
    token_revoke: '撤销令牌',
    config_change: '配置变更'
  }
  return actionMap[action] || action
}

const getStatusText = (status: string) => {
  return status === 'success' ? '成功' : '失败'
}

const getStatusTagType = (status: string) => {
  return status === 'success' ? 'success' : 'danger'
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadLogs()
  loadStats()
})
</script>

<template>
  <div class="audit-log-management">
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats?.totalLogs || 0 }}</div>
              <div class="stat-label">总日志数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats?.successLogs || 0 }}</div>
              <div class="stat-label">成功日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats?.failedLogs || 0 }}</div>
              <div class="stat-label">失败日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats?.todayLogs || 0 }}</div>
              <div class="stat-label">今日日志</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>审计日志</span>
          <div>
            <el-button type="primary" @click="handleExport('json')">
              <el-icon><Download /></el-icon>
              导出JSON
            </el-button>
            <el-button type="success" @click="handleExport('csv')">
              <el-icon><Download /></el-icon>
              导出CSV
            </el-button>
            <el-button type="danger" @click="handleCleanup">
              <el-icon><Delete /></el-icon>
              清理日志
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="日志类型">
          <el-select v-model="queryForm.type" placeholder="请选择类型" clearable style="width: 150px">
            <el-option label="认证" value="auth" />
            <el-option label="用户" value="user" />
            <el-option label="应用" value="application" />
            <el-option label="租户" value="tenant" />
            <el-option label="角色" value="role" />
            <el-option label="用户组" value="group" />
            <el-option label="系统" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="queryForm.action" placeholder="请输入操作" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="queryForm.ip" placeholder="请输入IP地址" clearable />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" v-loading="loading" style="width: 100%">
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="120">
          <template #default="{ row }">
            {{ row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="120">
          <template #default="{ row }">
            {{ getActionText(row.action) }}
          </template>
        </el-table-column>
        <el-table-column prop="resource" label="资源" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200">
          <template #default="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="审计日志详情" width="700px">
      <el-descriptions v-if="currentLog" :column="2" border>
        <el-descriptions-item label="日志ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatDate(currentLog.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentLog.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentLog.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="getTypeTagType(currentLog.type)" size="small">
            {{ getTypeText(currentLog.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作">{{ getActionText(currentLog.action) }}</el-descriptions-item>
        <el-descriptions-item label="资源">{{ currentLog.resource }}</el-descriptions-item>
        <el-descriptions-item label="资源ID">{{ currentLog.resourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="User Agent" :span="2">{{ currentLog.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ currentLog.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentLog.status)" size="small">
            {{ getStatusText(currentLog.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentLog.errorMessage">
          {{ currentLog.errorMessage }}
        </el-descriptions-item>
        <el-descriptions-item label="变更内容" :span="2" v-if="currentLog.changes">
          <pre style="max-height: 300px; overflow: auto;">{{ JSON.stringify(currentLog.changes, null, 2) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.audit-log-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: white;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style>
