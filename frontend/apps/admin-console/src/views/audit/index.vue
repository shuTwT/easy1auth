<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Table, message } from 'antdv-next'
import { FileText, CheckCircle, XCircle, Clock, Search, RefreshCw, Download, Trash2 } from '@lucide/vue'
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
    logs.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载审计日志失败:', error)
    message.error('加载审计日志失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await auditApi.getStats()
    stats.value = res
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
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败')
  }
}

const handleCleanup = async () => {
  const confirmed = window.confirm('确定要清理90天前的审计日志吗？此操作不可恢复！')
  if (!confirmed) return

  try {
    await auditApi.cleanup(90)
    message.success('清理成功')
    loadLogs()
    loadStats()
  } catch (error) {
    console.error('清理失败:', error)
    message.error('清理失败')
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

const getTypeVariant = (type: string) => {
  const typeVariantMap: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    auth: 'default',
    user: 'secondary',
    application: 'outline',
    tenant: 'destructive',
    role: 'secondary',
    group: 'outline',
    system: 'outline'
  }
  return typeVariantMap[type] || 'secondary'
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

const getStatusVariant = (status: string) => {
  return status === 'success' ? 'default' : 'destructive'
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

const totalPages = () => Math.ceil(total.value / queryForm.pageSize!)

onMounted(() => {
  loadLogs()
  loadStats()
})
</script>

<template>
  <div class="audit-log-management p-5">
    <div class="grid grid-cols-4 gap-5 mb-5">
      <Card>
        <div class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-blue-500">
              <FileText class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.totalLogs || 0 }}</div>
              <div class="text-sm text-slate-400">总日志数</div>
            </div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-green-500">
              <CheckCircle class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.successLogs || 0 }}</div>
              <div class="text-sm text-slate-400">成功日志</div>
            </div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-red-500">
              <XCircle class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.failedLogs || 0 }}</div>
              <div class="text-sm text-slate-400">失败日志</div>
            </div>
          </div>
        </div>
      </Card>
      <Card>
        <div class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-amber-500">
              <Clock class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.todayLogs || 0 }}</div>
              <div class="text-sm text-slate-400">今日日志</div>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <Card>
      <div class="flex flex-row items-center justify-between">
        <h3>审计日志</h3>
        <div class="flex gap-2">
          <Button size="small" @click="handleExport('json')">
            <Download class="w-4 h-4 mr-2" />
            导出JSON
          </Button>
          <Button size="small"  @click="handleExport('csv')">
            <Download class="w-4 h-4 mr-2" />
            导出CSV
          </Button>
          <Button size="small" color="error" @click="handleCleanup">
            <Trash2 class="w-4 h-4 mr-2" />
            清理日志
          </Button>
        </div>
      </div>
      <div>
        <div class="flex flex-wrap items-end gap-4 mb-5">
          <div class="grid gap-1.5">
            <label>用户名</label>
            <Input v-model:value="queryForm.username" placeholder="请输入用户名" class="w-[150px]" />
          </div>
          <div class="grid gap-1.5">
            <label>日志类型</label>
            <Select v-model:value="queryForm.type" class="w-[150px]">
              <div>

              </div>

                <SelectOptGroup>
                  <SelectOption value="auth">认证</SelectOption>
                  <SelectOption value="user">用户</SelectOption>
                  <SelectOption value="application">应用</SelectOption>
                  <SelectOption value="tenant">租户</SelectOption>
                  <SelectOption value="role">角色</SelectOption>
                  <SelectOption value="group">用户组</SelectOption>
                  <SelectOption value="system">系统</SelectOption>
                </SelectOptGroup>

            </Select>
          </div>
          <div class="grid gap-1.5">
            <label>操作</label>
            <Input v-model:value="queryForm.action" placeholder="请输入操作" class="w-[150px]" />
          </div>
          <div class="grid gap-1.5">
            <label>状态</label>
            <Select v-model:value="queryForm.status" class="w-[120px]">
              <div>

              </div>

                <SelectOptGroup>
                  <SelectOption value="success">成功</SelectOption>
                  <SelectOption value="failed">失败</SelectOption>
                </SelectOptGroup>

            </Select>
          </div>
          <div class="grid gap-1.5">
            <label>IP地址</label>
            <Input v-model:value="queryForm.ip" placeholder="请输入IP地址" class="w-[150px]" />
          </div>
          <div class="grid gap-1.5">
            <label>时间范围</label>
            <div class="flex items-center gap-2">
              <Input v-model:value="queryForm.startDate" type="date" class="w-[140px]" />
              <span class="text-muted-foreground">至</span>
              <Input v-model:value="queryForm.endDate" type="date" class="w-[140px]" />
            </div>
          </div>
          <div class="flex gap-2">
            <Button size="small" @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button size="small"  @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
        </div>

        <Table :columns="[
          { title: '时间', key: 'createdAt', width: 180 }, { title: '用户', key: 'username', width: 120 },
          { title: '类型', key: 'type', width: 100 }, { title: '操作', key: 'action', width: 120 },
          { title: '资源', dataIndex: 'resource', width: 120 }, { title: 'IP地址', dataIndex: 'ip', width: 140 },
          { title: '状态', key: 'status', width: 80 }, { title: '错误信息', key: 'errorMessage', width: 200 },
          { title: '操作', key: 'actions', width: 100 }
        ]" :data-source="logs" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1160 }">
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.key === 'createdAt'">{{ formatDate(item.createdAt) }}</template>
            <template v-else-if="column.key === 'username'">{{ item.username || '-' }}</template>
            <template v-else-if="column.key === 'type'">
                <Tag :color="getTypeVariant(item.type)" size="sm">
                  {{ getTypeText(item.type) }}
                </Tag>
            </template>
            <template v-else-if="column.key === 'action'">{{ getActionText(item.action) }}</template>
            <template v-else-if="column.key === 'status'">
                <Tag :color="getStatusVariant(item.status)" size="sm">
                  {{ getStatusText(item.status) }}
                </Tag>
            </template>
            <template v-else-if="column.key === 'errorMessage'"><span class="text-muted-foreground">{{ item.errorMessage || '-' }}</span></template>
            <template v-else-if="column.key === 'actions'">
                <Button type="link" size="small" @click="handleViewDetail(item)">详情</Button>
            </template>
          </template>
        </Table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <Button  size="small" :disabled="queryForm.page! <= 1" @click="handlePageChange(queryForm.page! - 1)">上一页</Button>
            <span class="text-sm px-2">{{ queryForm.page! }} / {{ totalPages() }}</span>
            <Button  size="small" :disabled="queryForm.page! >= totalPages()" @click="handlePageChange(queryForm.page! + 1)">下一页</Button>
          </div>
        </div>
      </div>
    </Card>

    <Modal v-model:open="detailDialogVisible" :footer="null">
      <div class="sm:max-w-[700px]">
        <div>
          <h3>审计日志详情</h3>
        </div>
        <div v-if="currentLog" class="grid grid-cols-2 gap-4 py-4">
          <div class="space-y-1">
            <label class="text-muted-foreground">日志ID</label>
            <div class="text-sm font-mono">{{ currentLog.id }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">时间</label>
            <div class="text-sm">{{ formatDate(currentLog.createdAt) }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">用户ID</label>
            <div class="text-sm">{{ currentLog.userId || '-' }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">用户名</label>
            <div class="text-sm">{{ currentLog.username || '-' }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">类型</label>
            <div class="text-sm">
              <Tag :color="getTypeVariant(currentLog.type)" size="sm">
                {{ getTypeText(currentLog.type) }}
              </Tag>
            </div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">操作</label>
            <div class="text-sm">{{ getActionText(currentLog.action) }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">资源</label>
            <div class="text-sm">{{ currentLog.resource }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">资源ID</label>
            <div class="text-sm">{{ currentLog.resourceId || '-' }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">请求方法</label>
            <div class="text-sm">{{ currentLog.method || '-' }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">IP地址</label>
            <div class="text-sm">{{ currentLog.ip }}</div>
          </div>
          <div class="col-span-2 space-y-1">
            <label class="text-muted-foreground">User Agent</label>
            <div class="text-sm text-muted-foreground">{{ currentLog.userAgent || '-' }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">位置</label>
            <div class="text-sm">{{ currentLog.location || '-' }}</div>
          </div>
          <div class="space-y-1">
            <label class="text-muted-foreground">状态</label>
            <div class="text-sm">
              <Tag :color="getStatusVariant(currentLog.status)" size="sm">
                {{ getStatusText(currentLog.status) }}
              </Tag>
            </div>
          </div>
          <div v-if="currentLog.errorMessage" class="col-span-2 space-y-1">
            <label class="text-muted-foreground">错误信息</label>
            <div class="text-sm text-destructive">{{ currentLog.errorMessage }}</div>
          </div>
          <div v-if="currentLog.changes" class="col-span-2 space-y-1">
            <label class="text-muted-foreground">变更内容</label>
            <pre class="text-sm bg-muted p-3 rounded-md overflow-auto max-h-75">{{ JSON.stringify(currentLog.changes, null, 2) }}</pre>
          </div>
        </div>
        <div>
          <Button  @click="detailDialogVisible = false">关闭</Button>
        </div>
      </div>
    </Modal>
  </div>
</template>
