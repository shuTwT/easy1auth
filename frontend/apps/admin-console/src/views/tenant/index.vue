<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message, type TableProps } from 'antdv-next'
import { Plus, RefreshCw, Search } from '@lucide/vue'
import { tenantApi } from '@/api/tenant'
import { useUserStore } from '@/stores/user'
import type { Tenant, TenantInput, TenantQuery } from '@/types/tenant'

const userStore = useUserStore()
const rows = ref<Tenant[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const editorOpen = ref(false)
const editing = ref<Tenant | null>(null)
const deleteTarget = ref<Tenant | null>(null)
const query = reactive<TenantQuery>({ page: 1, pageSize: 20, name: '', status: undefined })
const form = reactive<TenantInput>({ name: '', code: '', description: '', status: 'active' })
const statusOptions = [{ label: '启用', value: 'active' }, { label: '停用', value: 'inactive' }]
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

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

async function load() {
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
    if (version !== loadVersion || poolId !== userStore.currentUserPool?.id) return
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    if (version === loadVersion) message.error(errorMessage(error, '加载租户失败'))
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.name = ''
  query.status = undefined
  search()
}

function changePage(pager: { current?: number; pageSize?: number }) {
  query.page = pager.current || 1
  query.pageSize = pager.pageSize || 20
  load()
}

function openEditor(row?: Tenant) {
  editing.value = row || null
  Object.assign(form, {
    name: row?.name || '',
    code: row?.code || '',
    description: row?.description || '',
    status: row?.status || 'active',
  })
  editorOpen.value = true
}

async function save() {
  if (saving.value) return
  const input: TenantInput = {
    name: form.name.trim(),
    code: form.code.trim().toLowerCase(),
    description: form.description?.trim(),
    status: form.status,
  }
  if (!input.name) {
    message.warning('请输入租户名称')
    return
  }
  if (!/^[a-z0-9_-]{1,100}$/.test(input.code)) {
    message.warning('租户编码须为1至100位小写字母、数字、下划线或连字符')
    return
  }
  saving.value = true
  const poolId = userStore.currentUserPool?.id
  try {
    if (editing.value) await tenantApi.update(editing.value.id, input)
    else await tenantApi.create(input)
    if (poolId !== userStore.currentUserPool?.id) return
    editorOpen.value = false
    message.success(editing.value ? '租户已更新' : '租户已创建')
    await load()
  } catch (error) {
    message.error(errorMessage(error, '保存租户失败'))
  } finally {
    saving.value = false
  }
}

async function remove() {
  const target = deleteTarget.value
  if (!target || deleting.value) return
  deleting.value = true
  try {
    await tenantApi.delete(target.id)
    if (target.userPoolId !== userStore.currentUserPool?.id) return
    deleteTarget.value = null
    message.success('租户已删除')
    if (rows.value.length === 1 && query.page > 1) query.page--
    await load()
  } catch (error) {
    message.error(errorMessage(error, '删除租户失败'))
  } finally {
    deleting.value = false
  }
}

watch(() => userStore.currentUserPool?.id, () => {
  editorOpen.value = false
  deleteTarget.value = null
  query.page = 1
  load()
}, { immediate: true })
</script>

<template>
  <div class="space-y-6">
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-[var(--color-text)]">租户管理</h1>
        <p class="mt-2 text-sm text-[var(--color-text-muted)]">
          管理用户池「{{ userStore.currentUserPool?.name || '未选择' }}」下的租户资料。租户用途待定，当前仅维护基础资料。
        </p>
      </div>
      <a-button type="primary" :disabled="!userStore.currentUserPool" @click="openEditor()">
        <template #icon><Plus :size="16" /></template>新建租户
      </a-button>
    </div>

    <a-card :bordered="false">
      <div class="mb-6 flex flex-wrap gap-3">
        <a-input v-model:value="query.name" class="!w-56" placeholder="搜索租户名称" aria-label="租户名称" allow-clear @press-enter="search" />
        <a-select v-model:value="query.status" class="!w-36" placeholder="全部状态" aria-label="租户状态" :options="statusOptions" allow-clear />
        <a-button @click="search"><template #icon><Search :size="16" /></template>查询</a-button>
        <a-button @click="reset"><template #icon><RefreshCw :size="16" /></template>重置</a-button>
      </div>
      <a-table row-key="id" :columns="columns" :data-source="rows" :loading="loading" :pagination="pagination" :scroll="{ x: 900 }" @change="changePage">
        <template #bodyCell="{ column, record }">
          <a-tag v-if="column.key === 'status'" :color="record.status === 'active' ? 'green' : 'default'">{{ record.status === 'active' ? '启用' : '停用' }}</a-tag>
          <span v-else-if="column.key === 'createdAt'">{{ new Date(record.createdAt).toLocaleString('zh-CN', { hour12: false }) }}</span>
          <span v-else-if="column.key === 'description'">{{ record.description || '—' }}</span>
          <a-space v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" @click="openEditor(record)">编辑</a-button>
            <a-button type="link" size="small" danger @click="deleteTarget = record">删除</a-button>
          </a-space>
        </template>
        <template #emptyText><a-empty description="当前用户池暂无租户" /></template>
      </a-table>
    </a-card>

    <a-modal v-model:open="editorOpen" :title="editing ? '编辑租户' : '新建租户'" ok-text="保存" cancel-text="取消" :confirm-loading="saving" :mask="{ closable: false }" @ok="save">
      <a-form layout="vertical" class="mt-4" @submit.prevent="save">
        <a-form-item label="租户名称" required><a-input v-model:value="form.name" :maxlength="200" placeholder="请输入租户名称" /></a-form-item>
        <a-form-item label="租户编码" required><a-input v-model:value="form.code" :maxlength="100" placeholder="小写字母、数字、下划线或连字符" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" :maxlength="2000" :rows="3" placeholder="可选，说明租户用途" /></a-form-item>
        <a-form-item label="状态"><a-select v-model:value="form.status" :options="statusOptions" /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal :open="!!deleteTarget" title="删除租户" ok-text="删除" cancel-text="取消" :ok-button-props="{ danger: true }" :confirm-loading="deleting" @ok="remove" @cancel="deleteTarget = null">
      <p>确定删除租户「{{ deleteTarget?.name }}」的资料吗？</p>
    </a-modal>
  </div>
</template>
