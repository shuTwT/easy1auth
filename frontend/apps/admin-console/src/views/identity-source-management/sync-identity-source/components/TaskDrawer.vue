<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { syncIdentitySourceApi } from '@/api/syncIdentitySource'
import type { SyncIdentitySource, SyncIdentityTask } from '@/types/syncIdentitySource'
import { getApiErrorMessage, useSyncDict } from '../utils/hooks'

const { statusColor, statusText, formatDate } = useSyncDict()

const drawerOpen = ref(false)
const loading = ref(false)
const tasks = ref<SyncIdentityTask[]>([])
const selectedSource = ref<SyncIdentitySource | null>(null)

const columns: TableProps<SyncIdentityTask>['columns'] = [
  { title: '类型', dataIndex: 'type' },
  { title: '状态', key: 'status' },
  { title: '结果', key: 'summary' },
  { title: '提交时间', key: 'createdAt' }
]

const getSummary = (task: SyncIdentityTask) => {
  const text = Object.entries(task.summary || {})
    .map(([key, value]) => `${key}: ${value}`)
    .join('，')
  return text || task.lastError || '-'
}

const open = async (row: SyncIdentitySource) => {
  selectedSource.value = row
  tasks.value = []
  drawerOpen.value = true
  loading.value = true
  try {
    tasks.value = await syncIdentitySourceApi.tasks(row.id)
  } catch (error: unknown) {
    console.error('加载同步记录失败:', error)
    message.error(getApiErrorMessage(error, '加载同步记录失败'))
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-drawer v-model:open="drawerOpen" :title="`${selectedSource?.name || ''} 的同步记录`" size="large">
    <a-table :columns="columns" :data-source="tasks" :loading="loading" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'summary'">
          <span>{{ getSummary(record) }}</span>
        </template>
        <template v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt) }}</template>
      </template>
    </a-table>
  </a-drawer>
</template>
