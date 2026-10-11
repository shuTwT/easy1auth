<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Search, RefreshCw, Download, Trash2 } from '@lucide/vue'
import { auditApi } from '@/api/audit'
import type { AuditLog, AuditLogQueryDto, AuditLogStats } from '@/types/audit'
import DetailDialog from './components/DetailDialog.vue'
import { useAuditDict } from './utils/hooks'
import type { DetailDialogExpose } from './utils/types'

const loading = ref(false)
const logs = ref<AuditLog[]>([])
const total = ref(0)
const stats = ref<AuditLogStats | null>(null)
const detailDialogRef = ref<DetailDialogExpose>()

const [modal, contextHolder] = Modal.useModal()

const {
  auditTypeOptions,
  auditStatusOptions,
  getTypeText,
  getTypeVariant,
  getActionText,
  getStatusText,
  getStatusVariant,
  formatDate
} = useAuditDict()

const queryForm = reactive<AuditLogQueryDto>({
  page: 1,
  pageSize: 20,
  username: '',
  type: undefined,
  action: '',
  status: undefined,
  createAt: undefined,
  ip: ''
})

const columns: TableProps<AuditLog>['columns'] = [
  { title: '时间', key: 'createdAt', width: 180 },
  { title: '用户', key: 'username', width: 120 },
  { title: '类型', key: 'type', width: 100 },
  { title: '操作', key: 'action', width: 120 },
  { title: '资源', dataIndex: 'resource', width: 120 },
  { title: 'IP地址', dataIndex: 'ip', width: 140 },
  { title: '状态', key: 'status', width: 80 },
  { title: '错误信息', key: 'errorMessage', width: 200 },
  { title: '操作', key: 'actions', width: 100 }
]

const loadLogs = async () => {
  loading.value = true
  try {
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
    stats.value = await auditApi.getStats()
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载审计统计数据失败')
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
  queryForm.createAt = undefined
  queryForm.page = 1
  loadLogs()
}

const handlePageChange = (page: number, _pageSize?: number) => {
  queryForm.page = page
  loadLogs()
}

const handleViewDetail = (row: AuditLog) => {
  detailDialogRef.value?.open(row)
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
  const confirmed = await modal.confirm({
    title: '清理审计日志',
    content: '确定要清理 90 天前的审计日志吗？此操作不可恢复！',
    okText: '清理',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) {
    return
  }

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

onMounted(() => {
  loadLogs()
  loadStats()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <a-card>
      <div class="flex flex-wrap items-end gap-4">
        <a-form-item name="username" label="用户名">
          <a-input v-model:value="queryForm.username" placeholder="请输入用户名" class="w-[150px]" />
        </a-form-item>
        <a-form-item name="type" label="日志类型">
          <a-select v-model:value="queryForm.type" placeholder="请选择类型" class="w-[150px]" allow-clear :options="auditTypeOptions" />
        </a-form-item>
        <a-form-item name="action" label="操作">
          <a-input v-model:value="queryForm.action" placeholder="请输入操作" class="w-[150px]" />
        </a-form-item>
        <a-form-item name="status" label="状态">
          <a-select v-model:value="queryForm.status" placeholder="请选择状态" class="w-[120px]" allow-clear :options="auditStatusOptions" />
        </a-form-item>
        <a-form-item name="createAt" label="创建时间">
          <a-range-picker v-model:value="queryForm.createAt" />
        </a-form-item>
        <a-form-item>
          <div class="flex gap-2">
            <a-button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </a-button>
            <a-button @click="handleReset">
              <RefreshCw class="size-4 mr-2" />
              重置
            </a-button>
            <a-button @click="handleExport('json')">
              <Download class="size-4 mr-2" />
              导出JSON
            </a-button>
            <a-button @click="handleExport('csv')">
              <Download class="size-4 mr-2" />
              导出CSV
            </a-button>
            <a-button danger @click="handleCleanup">
              <Trash2 class="size-4 mr-2" />
              清理日志
            </a-button>
          </div>
        </a-form-item>
      </div>
    </a-card>

    <a-card class="mt-3!">
      <a-table :columns="columns" :data-source="logs" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1160 }">
        <template #bodyCell="{ column, record: item }">
          <template v-if="column.key === 'createdAt'">{{ formatDate(item.createdAt) }}</template>
          <template v-else-if="column.key === 'username'">{{ item.username || '-' }}</template>
          <template v-else-if="column.key === 'type'">
            <a-tag :color="getTypeVariant(item.type)">
              {{ getTypeText(item.type) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">{{ getActionText(item.action) }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusVariant(item.status)">
              {{ getStatusText(item.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'errorMessage'"><span class="text-[#52677D]">{{ item.errorMessage || '-' }}</span></template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" class="h-auto p-0" @click="handleViewDetail(item)">详情</a-button>
          </template>
        </template>
      </a-table>

      <div class="flex items-center justify-between mt-4 pt-4">
        <span class="text-sm text-[#52677D]">共 {{ total }} 条</span>
        <a-pagination
          :current="queryForm.page"
          :page-size="queryForm.pageSize"
          :total="total"
          :show-size-changer="false"
          size="small"
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <DetailDialog ref="detailDialogRef" />
    <contextHolder />
  </div>
</template>
