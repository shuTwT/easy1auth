<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { FileText, CheckCircle, XCircle, Clock, Search, RefreshCw, Download, Trash2 } from '@lucide/vue'
import { auditApi } from '@/api/audit'
import type { AuditLog, AuditLogQueryDto, AuditLogStats } from '@/types/audit'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Label } from '@/components/ui/label'

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
    toast.error('加载审计日志失败')
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
    toast.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    toast.error('导出失败')
  }
}

const handleCleanup = async () => {
  const confirmed = window.confirm('确定要清理90天前的审计日志吗？此操作不可恢复！')
  if (!confirmed) return
  
  try {
    await auditApi.cleanup(90)
    toast.success('清理成功')
    loadLogs()
    loadStats()
  } catch (error) {
    console.error('清理失败:', error)
    toast.error('清理失败')
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
        <CardContent class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-blue-500">
              <FileText class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.totalLogs || 0 }}</div>
              <div class="text-sm text-slate-400">总日志数</div>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-green-500">
              <CheckCircle class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.successLogs || 0 }}</div>
              <div class="text-sm text-slate-400">成功日志</div>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-red-500">
              <XCircle class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.failedLogs || 0 }}</div>
              <div class="text-sm text-slate-400">失败日志</div>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center">
            <div class="w-15 h-15 rounded-lg flex items-center justify-center mr-4 bg-amber-500">
              <Clock class="w-7 h-7 text-white" />
            </div>
            <div>
              <div class="text-2xl font-bold text-slate-800">{{ stats?.todayLogs || 0 }}</div>
              <div class="text-sm text-slate-400">今日日志</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader class="flex flex-row items-center justify-between">
        <CardTitle>审计日志</CardTitle>
        <div class="flex gap-2">
          <Button size="sm" @click="handleExport('json')">
            <Download class="w-4 h-4 mr-2" />
            导出JSON
          </Button>
          <Button size="sm" variant="outline" @click="handleExport('csv')">
            <Download class="w-4 h-4 mr-2" />
            导出CSV
          </Button>
          <Button size="sm" variant="destructive" @click="handleCleanup">
            <Trash2 class="w-4 h-4 mr-2" />
            清理日志
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div class="flex flex-wrap items-end gap-4 mb-5">
          <div class="grid gap-1.5">
            <Label>用户名</Label>
            <Input v-model="queryForm.username" placeholder="请输入用户名" class="w-[150px]" />
          </div>
          <div class="grid gap-1.5">
            <Label>日志类型</Label>
            <Select v-model="queryForm.type" class="w-[150px]">
              <SelectTrigger>
                <SelectValue placeholder="请选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="auth">认证</SelectItem>
                  <SelectItem value="user">用户</SelectItem>
                  <SelectItem value="application">应用</SelectItem>
                  <SelectItem value="tenant">租户</SelectItem>
                  <SelectItem value="role">角色</SelectItem>
                  <SelectItem value="group">用户组</SelectItem>
                  <SelectItem value="system">系统</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <div class="grid gap-1.5">
            <Label>操作</Label>
            <Input v-model="queryForm.action" placeholder="请输入操作" class="w-[150px]" />
          </div>
          <div class="grid gap-1.5">
            <Label>状态</Label>
            <Select v-model="queryForm.status" class="w-[120px]">
              <SelectTrigger>
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="success">成功</SelectItem>
                  <SelectItem value="failed">失败</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <div class="grid gap-1.5">
            <Label>IP地址</Label>
            <Input v-model="queryForm.ip" placeholder="请输入IP地址" class="w-[150px]" />
          </div>
          <div class="grid gap-1.5">
            <Label>时间范围</Label>
            <div class="flex items-center gap-2">
              <Input v-model="queryForm.startDate" type="date" class="w-[140px]" />
              <span class="text-muted-foreground">至</span>
              <Input v-model="queryForm.endDate" type="date" class="w-[140px]" />
            </div>
          </div>
          <div class="flex gap-2">
            <Button size="sm" @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button size="sm" variant="outline" @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[180px]">时间</TableHead>
              <TableHead class="w-[120px]">用户</TableHead>
              <TableHead class="w-[100px]">类型</TableHead>
              <TableHead class="w-[120px]">操作</TableHead>
              <TableHead class="w-[120px]">资源</TableHead>
              <TableHead class="w-[140px]">IP地址</TableHead>
              <TableHead class="w-[80px]">状态</TableHead>
              <TableHead class="min-w-[200px]">错误信息</TableHead>
              <TableHead class="w-[100px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="9" class="text-center text-muted-foreground">加载中...</TableCell>
            </TableRow>
            <TableRow v-else-if="logs.length === 0">
              <TableCell colspan="9" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
            </TableRow>
            <TableRow v-for="item in logs" :key="item.id">
              <TableCell>{{ formatDate(item.createdAt) }}</TableCell>
              <TableCell>{{ item.username || '-' }}</TableCell>
              <TableCell>
                <Badge :variant="getTypeVariant(item.type)" size="sm">
                  {{ getTypeText(item.type) }}
                </Badge>
              </TableCell>
              <TableCell>{{ getActionText(item.action) }}</TableCell>
              <TableCell>{{ item.resource }}</TableCell>
              <TableCell>{{ item.ip }}</TableCell>
              <TableCell>
                <Badge :variant="getStatusVariant(item.status)" size="sm">
                  {{ getStatusText(item.status) }}
                </Badge>
              </TableCell>
              <TableCell class="text-muted-foreground">{{ item.errorMessage || '-' }}</TableCell>
              <TableCell>
                <Button variant="link" size="sm" @click="handleViewDetail(item)">详情</Button>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <Button variant="outline" size="sm" :disabled="queryForm.page! <= 1" @click="handlePageChange(queryForm.page! - 1)">上一页</Button>
            <span class="text-sm px-2">{{ queryForm.page! }} / {{ totalPages() }}</span>
            <Button variant="outline" size="sm" :disabled="queryForm.page! >= totalPages()" @click="handlePageChange(queryForm.page! + 1)">下一页</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="detailDialogVisible">
      <DialogContent class="sm:max-w-[700px]">
        <DialogHeader>
          <DialogTitle>审计日志详情</DialogTitle>
        </DialogHeader>
        <div v-if="currentLog" class="grid grid-cols-2 gap-4 py-4">
          <div class="space-y-1">
            <Label class="text-muted-foreground">日志ID</Label>
            <div class="text-sm font-mono">{{ currentLog.id }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">时间</Label>
            <div class="text-sm">{{ formatDate(currentLog.createdAt) }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">用户ID</Label>
            <div class="text-sm">{{ currentLog.userId || '-' }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">用户名</Label>
            <div class="text-sm">{{ currentLog.username || '-' }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">类型</Label>
            <div class="text-sm">
              <Badge :variant="getTypeVariant(currentLog.type)" size="sm">
                {{ getTypeText(currentLog.type) }}
              </Badge>
            </div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">操作</Label>
            <div class="text-sm">{{ getActionText(currentLog.action) }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">资源</Label>
            <div class="text-sm">{{ currentLog.resource }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">资源ID</Label>
            <div class="text-sm">{{ currentLog.resourceId || '-' }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">请求方法</Label>
            <div class="text-sm">{{ currentLog.method || '-' }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">IP地址</Label>
            <div class="text-sm">{{ currentLog.ip }}</div>
          </div>
          <div class="col-span-2 space-y-1">
            <Label class="text-muted-foreground">User Agent</Label>
            <div class="text-sm text-muted-foreground">{{ currentLog.userAgent || '-' }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">位置</Label>
            <div class="text-sm">{{ currentLog.location || '-' }}</div>
          </div>
          <div class="space-y-1">
            <Label class="text-muted-foreground">状态</Label>
            <div class="text-sm">
              <Badge :variant="getStatusVariant(currentLog.status)" size="sm">
                {{ getStatusText(currentLog.status) }}
              </Badge>
            </div>
          </div>
          <div v-if="currentLog.errorMessage" class="col-span-2 space-y-1">
            <Label class="text-muted-foreground">错误信息</Label>
            <div class="text-sm text-destructive">{{ currentLog.errorMessage }}</div>
          </div>
          <div v-if="currentLog.changes" class="col-span-2 space-y-1">
            <Label class="text-muted-foreground">变更内容</Label>
            <pre class="text-sm bg-muted p-3 rounded-md overflow-auto max-h-75">{{ JSON.stringify(currentLog.changes, null, 2) }}</pre>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="detailDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
