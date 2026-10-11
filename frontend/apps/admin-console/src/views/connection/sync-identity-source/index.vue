<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Building2, Plus, RefreshCw, Trash2 } from '@lucide/vue'
import { syncIdentitySourceApi } from '@/api/syncIdentitySource'
import type { SyncIdentitySource, SyncIdentitySourceStats } from '@/types/syncIdentitySource'
import SourceEditDrawer from './components/SourceEditDrawer.vue'
import TaskDrawer from './components/TaskDrawer.vue'
import { getApiErrorMessage, useSyncDict } from './utils/hooks'
import type { SourceEditDrawerExpose, SourceStatus, TaskDrawerExpose } from './utils/types'

const rows = ref<SyncIdentitySource[]>([])
const loading = ref(false)
const search = ref('')
const status = ref<SourceStatus>()
const stats = ref<SyncIdentitySourceStats>({ totalSources: 0, activeSources: 0, inactiveSources: 0 })

const sourceEditDrawerRef = ref<SourceEditDrawerExpose>()
const taskDrawerRef = ref<TaskDrawerExpose>()

const [modal, contextHolder] = Modal.useModal()

const { statusOptions, statusColor, statusText, formatDate } = useSyncDict()

const columns: TableProps<SyncIdentitySource>['columns'] = [
  { title: '身份源', dataIndex: 'name', key: 'name' },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId' },
  { title: '状态', key: 'status' },
  { title: '最近同步', key: 'lastSyncAt' },
  { title: '操作', key: 'actions', width: 260 }
]

const loadSources = async () => {
  loading.value = true
  try {
    const [list, sourceStats] = await Promise.all([
      syncIdentitySourceApi.getList({
        page: 1,
        pageSize: 100,
        search: search.value || undefined,
        status: status.value
      }),
      syncIdentitySourceApi.getStats()
    ])
    rows.value = list.items
    stats.value = sourceStats
  } catch (error: unknown) {
    console.error('加载同步身份源失败:', error)
    message.error('加载同步身份源失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  sourceEditDrawerRef.value?.open()
}

const handleEdit = (row: SyncIdentitySource) => {
  sourceEditDrawerRef.value?.open(row)
}

const handleViewTasks = (row: SyncIdentitySource) => {
  taskDrawerRef.value?.open(row)
}

const handleSync = async (row: SyncIdentitySource) => {
  try {
    await syncIdentitySourceApi.sync(row.id)
    message.success('已提交同步任务')
    loadSources()
  } catch (error: unknown) {
    console.error('提交同步任务失败:', error)
    message.error(getApiErrorMessage(error, '提交同步任务失败'))
  }
}

const handleDelete = async (row: SyncIdentitySource) => {
  const confirmed = await modal.confirm({
    title: '删除同步身份源',
    content: `删除“${row.name}”后，已导入的数据会转为本地管理。确定继续吗？`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed) {
    return
  }
  try {
    await syncIdentitySourceApi.delete(row.id)
    message.success('身份源已删除')
    loadSources()
  } catch (error: unknown) {
    console.error('删除同步身份源失败:', error)
    message.error(getApiErrorMessage(error, '删除同步身份源失败'))
  }
}

onMounted(loadSources)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex gap-3">
        <Building2 class="mt-1 text-[#0369A1] size-6" />
        <div>
          <h1 class="text-2xl font-bold text-[#102A43] mb-2">同步身份源</h1>
          <p class="text-sm text-[#52677D]">从飞书通讯录导入并同步用户和用户组。</p>
        </div>
      </div>
      <a-button type="primary" @click="handleAdd">
        <Plus class="size-4 mr-2" />
        添加飞书同步身份源
      </a-button>
    </div>

    <div class="grid gap-5 sm:grid-cols-3 mb-6">
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats.totalSources }}</div>
          <div class="text-sm text-[#52677D]">全部身份源</div>
        </div>
      </a-card>
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats.activeSources }}</div>
          <div class="text-sm text-[#52677D]">已启用</div>
        </div>
      </a-card>
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats.inactiveSources }}</div>
          <div class="text-sm text-[#52677D]">已停用</div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div class="mb-4 flex flex-wrap gap-3">
        <a-form-item>
          <a-input v-model:value="search" placeholder="搜索身份源名称" @press-enter="loadSources" />
        </a-form-item>
        <a-form-item>
          <a-select
              v-model:value="status"
              allow-clear
              class="w-40"
              placeholder="请选择状态"
              :options="statusOptions"
          />
        </a-form-item>
        <a-form-item>
          <a-button @click="loadSources">
            <RefreshCw class="size-4 mr-2" />
            查询
          </a-button>
        </a-form-item>

      </div>

      <a-table
        :columns="columns"
        :data-source="rows"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 900 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'lastSyncAt'">
            <div>{{ formatDate(record.lastSyncAt) }}</div>
            <small
              v-if="record.lastSyncStatus"
              :class="record.lastSyncStatus === 'failed' ? 'text-[#B91C1C]' : 'text-[#52677D]'"
            >
              {{ statusText(record.lastSyncStatus) }}{{ record.lastError ? `：${record.lastError}` : '' }}
            </small>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex flex-wrap gap-1">
              <a-button type="link" size="small" class="h-auto p-0" @click="handleSync(record)">立即同步</a-button>
              <a-button type="link" size="small" class="h-auto p-0" @click="handleViewTasks(record)">记录</a-button>
              <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" danger class="h-auto p-0" @click="handleDelete(record)">
                <Trash2 class="size-3" />
              </a-button>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <SourceEditDrawer ref="sourceEditDrawerRef" @success="loadSources" />
    <TaskDrawer ref="taskDrawerRef" />
    <contextHolder />
  </div>
</template>
