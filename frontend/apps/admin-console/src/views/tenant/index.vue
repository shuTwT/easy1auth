<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message, type TableProps } from 'antdv-next'
import { Building2, CircleCheck, CirclePause, Pencil, Plus, RefreshCw, Search, Trash2 } from '@lucide/vue'
import { tenantApi } from '@/api/tenant'
import { useUserStore } from '@/stores/user'
import type { Tenant, TenantQuery } from '@/types/tenant'
import TenantEditDialog from './components/TenantEditDialog.vue'
import TenantDeleteDialog from './components/TenantDeleteDialog.vue'
import { errorMessage, useTenantDict } from './utils/hooks'
import type { TenantDeleteDialogExpose, TenantEditDialogExpose } from './utils/types'

const userStore = useUserStore()
const { statusOptions, formatDate } = useTenantDict()

const rows = ref<Tenant[]>([])
const total = ref(0)
const loading = ref(false)
const tenantEditDialogRef = ref<TenantEditDialogExpose>()
const tenantDeleteDialogRef = ref<TenantDeleteDialogExpose>()
const query = reactive<TenantQuery>({ page: 1, pageSize: 20, name: '', status: undefined })

const columns: TableProps<Tenant>['columns'] = [
  { title: '租户名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '租户编码', dataIndex: 'code', key: 'code', width: 180 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 160, fixed: 'right' },
]

const pagination = computed(() => ({
  current: query.page,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (count: number) => `共 ${count} 个租户`,
}))

let loadVersion = 0

const load = async () => {
  const version = ++loadVersion
  const poolId = userStore.currentUserPool?.id
  rows.value = []
  total.value = 0
  if (!poolId) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const result = await tenantApi.list({ ...query })
    if (version !== loadVersion || poolId !== userStore.currentUserPool?.id) {
      return
    }
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    if (version === loadVersion) {
      message.error(errorMessage(error, '加载租户失败'))
    }
  } finally {
    if (version === loadVersion) {
      loading.value = false
    }
  }
}

const search = () => {
  query.page = 1
  load()
}

const reset = () => {
  query.name = ''
  query.status = undefined
  search()
}

const changePage = (pager: { current?: number; pageSize?: number }) => {
  query.page = pager.current || 1
  query.pageSize = pager.pageSize || 20
  load()
}

const openEditor = (row?: Tenant) => {
  tenantEditDialogRef.value?.open(row)
}

const openDelete = (row: Tenant) => {
  tenantDeleteDialogRef.value?.open(row)
}

const handleDeleteSuccess = () => {
  if (rows.value.length === 1 && (query.page ?? 1) > 1) {
    query.page = (query.page ?? 1) - 1
  }
  load()
}

watch(() => userStore.currentUserPool?.id, () => {
  tenantEditDialogRef.value?.close()
  tenantDeleteDialogRef.value?.close()
  query.page = 1
  load()
}, { immediate: true })
</script>

<template>
  <div class="tenant-page min-h-[calc(100vh-56px)] space-y-6 p-4 sm:p-6">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0">
        <div class="flex items-center gap-3">
          <div class="flex size-10 shrink-0 items-center justify-center rounded-lg bg-[#E0F2FE] text-[#0369A1]">
            <Building2 :size="20" />
          </div>
          <div>
            <h1 class="text-2xl font-semibold leading-tight text-[#102A43]">租户管理</h1>
            <p class="mt-1 text-sm text-[#52677D]">
              当前用户池：{{ userStore.currentUserPool?.name || '未选择' }}
            </p>
          </div>
        </div>
        <p class="mt-3 max-w-2xl text-sm leading-6 text-[#52677D]">
          管理用户池「{{ userStore.currentUserPool?.name || '未选择' }}」下的租户资料。租户用途待定，当前仅维护基础资料。
        </p>
      </div>
      <a-button type="primary" class="shrink-0" :disabled="!userStore.currentUserPool" @click="openEditor()">
        <template #icon><Plus :size="16" /></template>新建租户
      </a-button>
    </div>

    <a-card :bordered="false" class="tenant-card">
      <div class="flex flex-wrap items-center justify-between gap-3 border-[#D7E2EC] pb-4">
        <div>
          <h2 class="text-base font-semibold text-[#102A43]">租户列表</h2>
          <p class="mt-1 text-sm text-[#52677D]">维护当前用户池下的租户基础资料</p>
        </div>
        <span class="text-sm text-[#52677D]">共 {{ total }} 个租户</span>
      </div>

      <a-form
        class="tenant-filters mt-5"
        layout="inline"
        :model="query"
        @finish="search"
      >
        <a-form-item name="name">
          <a-input v-model:value="query.name" placeholder="搜索租户名称" aria-label="租户名称" allow-clear style="width:230px;">
            <template #prefix><Search :size="16" class="text-[#7890A5]" /></template>
          </a-input>
        </a-form-item>
        <a-form-item name="status">
          <a-select v-model:value="query.status" placeholder="全部状态" aria-label="租户状态" :options="statusOptions" allow-clear />
        </a-form-item>
        <a-form-item>
          <div class="flex gap-2">
            <a-button type="primary" html-type="submit"><template #icon><Search :size="16" /></template>查询</a-button>
            <a-button html-type="button" @click="reset"><template #icon><RefreshCw :size="16" /></template>重置</a-button>
          </div>
        </a-form-item>
      </a-form>

      <a-table class="tenant-table mt-5" row-key="id" :columns="columns" :data-source="rows" :loading="loading" :pagination="pagination" :scroll="{ x: 900 }" @change="changePage">
        <template #bodyCell="{ column, record }">
          <div v-if="column.key === 'name'" class="flex min-w-0 items-center gap-3">
            <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-[#E0F2FE] text-[#0369A1]">
              <Building2 :size="17" />
            </div>
            <div class="min-w-0">
              <div class="truncate font-medium text-[#102A43]">{{ record.name }}</div>
              <code class="mt-0.5 block truncate text-xs text-[#7890A5]">{{ record.id }}</code>
            </div>
          </div>
          <div v-else-if="column.key === 'code'" class="font-mono text-sm text-[#52677D]">{{ record.code }}</div>
          <div v-else-if="column.key === 'status'" class="flex items-center gap-1.5">
            <CircleCheck v-if="record.status === 'active'" :size="15" class="text-[#15803D]" />
            <CirclePause v-else :size="15" class="text-[#7890A5]" />
            <a-tag :color="record.status === 'active' ? 'green' : 'default'">{{ record.status === 'active' ? '启用' : '停用' }}</a-tag>
          </div>
          <span v-else-if="column.key === 'createdAt'">{{ formatDate(record.createdAt) }}</span>
          <span v-else-if="column.key === 'description'">{{ record.description || '—' }}</span>
          <a-space v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditor(record)"><Pencil :size="14" />编辑</a-button>
            <a-button type="link" size="small" danger @click="openDelete(record)"><Trash2 :size="14" />删除</a-button>
          </a-space>
        </template>
        <template #emptyText><a-empty description="当前用户池暂无租户" /></template>
      </a-table>
    </a-card>

    <TenantEditDialog ref="tenantEditDialogRef" @success="load" />
    <TenantDeleteDialog ref="tenantDeleteDialogRef" @success="handleDeleteSuccess" />
  </div>
</template>

<style scoped>


</style>
