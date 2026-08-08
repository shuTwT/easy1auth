<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Button, Drawer, Form, FormItem, Input, Modal, Select, SelectOption, Table, Tag, message } from 'antdv-next'
import { Building2, Copy, Plus, RefreshCw, Trash2 } from '@lucide/vue'
import { enterpriseIdentitySourceApi } from '@/api/enterpriseIdentitySource'
import type { EnterpriseIdentitySource, EnterpriseIdentityTask } from '@/types/enterpriseIdentitySource'

const rows = ref<EnterpriseIdentitySource[]>([])
const tasks = ref<EnterpriseIdentityTask[]>([])
const loading = ref(false)
const submitting = ref(false)
const drawerOpen = ref(false)
const taskDrawerOpen = ref(false)
const editing = ref<EnterpriseIdentitySource | null>(null)
const search = ref('')
const status = ref<string>()
const selectedSource = ref<EnterpriseIdentitySource | null>(null)
const stats = ref({ totalSources: 0, activeSources: 0, inactiveSources: 0 })
const form = reactive({ name: '', appId: '', appSecret: '', verificationToken: '', encryptKey: '', status: 'active' as 'active' | 'disabled' })

const [modal, contextHolder] = Modal.useModal()
const publicBaseUrl = computed(() => import.meta.env.VITE_ENTERPRISE_IDENTITY_PUBLIC_BASE_URL || window.location.origin)
const callbackUrl = computed(() => editing.value ? `${publicBaseUrl.value}/api/enterprise-identity-sources/${editing.value.id}/feishu/events` : '创建后生成专属回调地址')

const columns = [
  { title: '身份源', dataIndex: 'name', key: 'name' },
  { title: '应用 ID', dataIndex: 'appId', key: 'appId' },
  { title: '状态', key: 'status' },
  { title: '最近同步', key: 'lastSyncAt' },
  { title: '操作', key: 'actions', width: 260 },
]

async function load() {
  loading.value = true
  try {
    const [list, sourceStats] = await Promise.all([
      enterpriseIdentitySourceApi.getList({ page: 1, pageSize: 100, search: search.value || undefined, status: status.value }),
      enterpriseIdentitySourceApi.getStats(),
    ])
    rows.value = list.items || list.data || []
    stats.value = sourceStats
  } catch (error) { console.error(error); message.error('加载企业身份源失败') } finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', appId: '', appSecret: '', verificationToken: '', encryptKey: '', status: 'active' })
  drawerOpen.value = true
}
function openEdit(row: EnterpriseIdentitySource) {
  editing.value = row
  Object.assign(form, { name: row.name, appId: row.appId, appSecret: '', verificationToken: '', encryptKey: '', status: row.status })
  drawerOpen.value = true
}
async function save() {
  if (!form.name || !form.appId || (!editing.value && (!form.appSecret || !form.verificationToken || !form.encryptKey))) {
    message.warning('请填写完整的飞书应用配置')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (editing.value) await enterpriseIdentitySourceApi.update(editing.value.id, payload)
    else await enterpriseIdentitySourceApi.create(payload)
    message.success(editing.value ? '身份源已更新' : '身份源已创建')
    drawerOpen.value = false
    await load()
  } finally { submitting.value = false }
}
async function sync(row: EnterpriseIdentitySource) {
  await enterpriseIdentitySourceApi.sync(row.id)
  message.success('已提交同步任务')
  await load()
}
async function remove(row: EnterpriseIdentitySource) {
  const confirmed = await modal.confirm({
    title: '删除企业身份源',
    content: `删除”${row.name}”后，已导入的数据会转为本地管理。确定继续吗？`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return
  await enterpriseIdentitySourceApi.delete(row.id)
  message.success('身份源已删除')
  await load()
}
async function viewTasks(row: EnterpriseIdentitySource) {
  selectedSource.value = row
  taskDrawerOpen.value = true
  tasks.value = await enterpriseIdentitySourceApi.tasks(row.id)
}
async function copyCallback() { await navigator.clipboard.writeText(callbackUrl.value); message.success('回调地址已复制') }
function statusColor(value: string) { return value === 'active' || value === 'succeeded' ? 'success' : value === 'partial' || value === 'processing' || value === 'pending' ? 'warning' : 'error' }
function statusText(value: string | null) { return ({ active: '启用', disabled: '停用', succeeded: '成功', partial: '部分完成', failed: '失败', pending: '排队中', processing: '同步中' } as Record<string, string>)[value || ''] || '-' }
function date(value: string | null) { return value ? new Date(value).toLocaleString() : '-' }
onMounted(load)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)] space-y-6">
    <section class="rounded-xl border bg-card p-6 shadow-sm">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="flex gap-3"><Building2 class="mt-1 text-primary size-6" /><div><h1 class="text-2xl font-bold text-foreground">企业身份源</h1><p class="mt-1 text-sm text-muted-foreground">通过飞书通讯录同步组织机构和 pool_user；不用于管理后台登录。</p></div></div>
        <Button type="primary" @click="openCreate"><Plus class="size-4 mr-2" /> 添加飞书身份源</Button>
      </div>
      <div class="mt-5 grid gap-3 sm:grid-cols-3"><div v-for="item in [{ label: '全部身份源', value: stats.totalSources }, { label: '已启用', value: stats.activeSources }, { label: '已停用', value: stats.inactiveSources }]" :key="item.label" class="rounded-lg bg-muted/50 px-4 py-3"><p class="text-sm text-muted-foreground">{{ item.label }}</p><p class="mt-1 text-2xl font-bold text-foreground">{{ item.value }}</p></div></div>
    </section>
    <section class="rounded-xl border bg-card p-5 shadow-sm">
      <div class="mb-4 flex flex-wrap gap-3"><div class="w-full sm:w-60"><Input v-model:value="search" placeholder="搜索身份源名称" @press-enter="load" /></div><div class="w-full sm:w-32"><Select v-model:value="status" allow-clear class="w-full" placeholder="状态"><SelectOption value="active">启用</SelectOption><SelectOption value="disabled">停用</SelectOption></Select></div><Button @click="load"><RefreshCw class="size-4 mr-2" /> 查询</Button></div>
      <Table :columns="columns" :data-source="rows" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 900 }">
        <template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusText(record.status) }}</Tag></template><template v-else-if="column.key === 'lastSyncAt'"><div>{{ date(record.lastSyncAt) }}</div><small v-if="record.lastSyncStatus" :class="record.lastSyncStatus === 'failed' ? 'text-destructive' : 'text-muted-foreground'">{{ statusText(record.lastSyncStatus) }}{{ record.lastError ? `：${record.lastError}` : '' }}</small></template><template v-else-if="column.key === 'actions'"><div class="flex flex-wrap gap-1"><Button type="link" size="small" class="h-auto p-0" @click="sync(record)">立即同步</Button><Button type="link" size="small" class="h-auto p-0" @click="viewTasks(record)">记录</Button><Button type="link" size="small" class="h-auto p-0" @click="openEdit(record)">编辑</Button><Button type="link" size="small" class="h-auto p-0 text-destructive" @click="remove(record)"><Trash2 class="size-3" /></Button></div></template></template>
      </Table>
    </section>
    <Drawer v-model:open="drawerOpen" :title="editing ? '编辑飞书身份源' : '添加飞书身份源'" size="large" destroy-on-hidden>
      <Form layout="vertical" :model="form"><FormItem label="身份源名称" required><Input v-model:value="form.name" placeholder="例如：总部飞书通讯录" /></FormItem><FormItem label="飞书 App ID" required><Input v-model:value="form.appId" placeholder="cli_xxx" /></FormItem><FormItem :label="editing ? 'App Secret（留空则不修改）' : 'App Secret'" required><Input v-model:value="form.appSecret" type="password" /></FormItem><FormItem :label="editing ? 'Verification Token（留空则不修改）' : 'Verification Token'" required><Input v-model:value="form.verificationToken" type="password" /></FormItem><FormItem :label="editing ? 'Encrypt Key（留空则不修改）' : 'Encrypt Key'" required><Input v-model:value="form.encryptKey" type="password" /></FormItem><FormItem label="状态"><Select v-model:value="form.status"><SelectOption value="active">启用</SelectOption><SelectOption value="disabled">停用</SelectOption></Select></FormItem></Form>
      <div class="rounded-lg border bg-muted/40 p-4 text-sm"><p class="font-medium">飞书事件配置</p><p class="mt-2 break-all text-muted-foreground">{{ callbackUrl }}</p><Button class="mt-2" size="small" :disabled="!editing" @click="copyCallback"><Copy class="size-3" /> 复制回调地址</Button><p class="mt-3 text-muted-foreground">在飞书自建应用中配置该地址，并订阅用户、部门的创建、更新、删除事件；应用需具备通讯录、邮箱、手机和部门信息读取权限。</p></div>
      <template #footer><div class="flex justify-end gap-2"><Button @click="drawerOpen = false">取消</Button><Button type="primary" :loading="submitting" @click="save">保存</Button></div></template>
    </Drawer>
    <Drawer v-model:open="taskDrawerOpen" :title="`${selectedSource?.name || ''} 的同步记录`" size="large"><Table :data-source="tasks" row-key="id" :pagination="false" :columns="[{title:'类型',dataIndex:'type'},{title:'状态',key:'status'},{title:'结果',key:'summary'},{title:'提交时间',key:'createdAt'}]"><template #bodyCell="{ column, record }"><template v-if="column.key === 'status'"><Tag :color="statusColor(record.status)">{{ statusText(record.status) }}</Tag></template><template v-else-if="column.key === 'summary'"><span>{{ Object.entries(record.summary || {}).map(([key, value]) => `${key}: ${value}`).join('，') || record.lastError || '-' }}</span></template><template v-else-if="column.key === 'createdAt'">{{ date(record.createdAt) }}</template></template></Table></Drawer>
    <contextHolder />
  </div>
</template>
