<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Empty, Form, FormItem, Pagination as AntPagination, Spin, Table as AntTable, message } from 'antdv-next'
import { Building2, Pencil, RefreshCw, Search, Trash2, UserRoundCog, Users } from '@lucide/vue'
import { adminUserApi } from '@/api/adminUser'
import { tenantApi } from '@/api/tenant'
import { tenantPackageApi } from '@/api/tenantPackage'
import type { AdminUser } from '@/types/adminUser'
import type { Tenant, TenantQueryDto } from '@/types/tenant'
import type { TenantPackage } from '@/types/tenantPackage'
const loading = ref(false)
const submitting = ref(false)
const transferSubmitting = ref(false)
const tenants = ref<Tenant[]>([])
const packages = ref<TenantPackage[]>([])
const administrators = ref<AdminUser[]>([])
const total = ref(0)
const tenantDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const editingTenant = ref<Tenant | null>(null)
const transferTarget = ref<Tenant | null>(null)
const deleteTarget = ref<Tenant | null>(null)
const transferAdministratorId = ref<string>('')

const queryForm = reactive<TenantQueryDto>({ page: 1, pageSize: 20, name: '', status: undefined })
const tenantForm = reactive({ name: '', packageId: undefined as number | undefined })
const tenantFormRules = {
  name: [{ required: true, message: '请输入租户名称' }],
  packageId: [{ required: true, message: '请选择租户套餐' }],
}
const dialogTitle = computed(() => '编辑租户')
const packageOptions = computed(() => packages.value.map(item => ({
  value: item.id,
  label: `${item.name} · ${formatLimit(item.maxUsers)} 用户 / ${formatLimit(item.maxApps)} 应用`,
})))
const administratorOptions = computed(() => administrators.value.map(item => ({
  value: item.id,
  label: `${item.username} · ${item.email}`,
})))

const tenantColumns = [
  { title: '租户', key: 'tenant' },
  { title: '套餐', key: 'package' },
  { title: '状态', key: 'status' },
  { title: '配额', key: 'quota' },
  { title: '租户管理员', key: 'administrator' },
  { title: '操作', key: 'actions', align: 'right' as const },
]

function formatLimit(value?: number) {
  if (value === undefined) return '-'
  return value >= 2147483647 ? '不限' : value.toLocaleString()
}

function statusLabel(status: Tenant['status']) {
  return status === 'active' ? '正常' : status === 'suspended' ? '停用' : '已删除'
}

function roleLabel(role: string) {
  return role === 'super_admin' ? '超级管理员' : role === 'tenant_admin' ? '租户管理员' : '普通成员'
}

async function loadData() {
  loading.value = true
  try {
    const [tenantPage, packageRows, administratorPage] = await Promise.all([
      tenantApi.getList(queryForm),
      tenantPackageApi.list(),
      adminUserApi.getList({ page: 1, pageSize: 100, status: 'active' }),
    ])
    tenants.value = tenantPage.items
    total.value = tenantPage.total
    packages.value = packageRows.filter((item) => item.status === 'active')
    administrators.value = administratorPage.items
  } catch (error) {
    console.error('加载租户数据失败:', error)
    message.error('加载租户数据失败')
  } finally {
    loading.value = false
  }
}

function openEditDialog(tenant: Tenant) {
  if (!tenant.tenantPackage) return
  editingTenant.value = tenant
  Object.assign(tenantForm, { name: tenant.name, packageId: tenant.tenantPackage.id })
  tenantDialogVisible.value = true
}

async function submitTenant() {
  submitting.value = true
  try {
    if (!editingTenant.value) return
    await tenantApi.update(editingTenant.value.id, { name: tenantForm.name.trim(), packageId: tenantForm.packageId })
    message.success('租户更新成功')
    tenantDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('保存租户失败:', error)
    message.error('保存租户失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(tenant: Tenant) {
  try {
    const status = tenant.status === 'active' ? 'suspended' : 'active'
    await tenantApi.updateStatus(tenant.id, status)
    message.success(status === 'active' ? '租户已启用' : '租户已停用')
    await loadData()
  } catch (error) {
    console.error('更新租户状态失败:', error)
    message.error('更新租户状态失败')
  }
}

function openTransferDialog(tenant: Tenant) {
  transferTarget.value = tenant
  transferAdministratorId.value = ''
  transferDialogVisible.value = true
}

async function submitTransfer() {
  if (!transferTarget.value || !transferAdministratorId.value) {
    message.error('请选择目标管理员')
    return
  }
  transferSubmitting.value = true
  try {
    await tenantApi.transferAdministrator(transferTarget.value.id, transferAdministratorId.value)
    message.success('租户管理员转移成功')
    transferDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('转移租户管理员失败:', error)
    message.error('转移租户管理员失败')
  } finally {
    transferSubmitting.value = false
  }
}

function openDeleteDialog(tenant: Tenant) {
  deleteTarget.value = tenant
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await tenantApi.delete(deleteTarget.value.id)
    message.success('租户已删除')
    deleteDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('删除租户失败:', error)
    message.error('删除租户失败')
  }
}

async function handleSearch() {
  queryForm.page = 1
  await loadData()
}

async function handleReset() {
  Object.assign(queryForm, { page: 1, name: '', status: undefined })
  await loadData()
}

async function handlePageChange(page: number, _pageSize?: number) {
  queryForm.page = page
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)] space-y-6">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">租户管理</h1>
        <p class="text-sm text-muted-foreground">查看平台内全部租户，并管理普通租户的套餐、状态和租户管理员。</p>
      </div>
      <div class="flex gap-3"><Button :disabled="loading" @click="loadData"><RefreshCw class="size-4 mr-2" :class="loading ? 'animate-spin' : ''" />刷新</Button></div>
    </div>

    <Card>
      <div class="gap-4 md:flex-row md:items-end md:justify-between">
        <div><h3>租户列表</h3><p>共 {{ total }} 个租户</p></div>
        <div class="flex flex-col gap-2 sm:flex-row">
          <div class="relative"><Search class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" /><Input v-model:value="queryForm.name" class="w-full pl-9 sm:w-56" placeholder="搜索租户名称" /></div>
          <Select v-model:value="queryForm.status" class="w-full sm:w-32" allow-clear :options="[{ value: 'active', label: '正常' }, { value: 'suspended', label: '停用' }, { value: 'deleted', label: '已删除' }]" />
          <Button  @click="handleSearch">查询</Button>
          <Button  @click="handleReset">重置</Button>
        </div>
      </div>
      <div>
        <Spin v-if="loading" class="flex min-h-40 items-center justify-center" />
        <Empty v-else-if="tenants.length === 0" class="flex min-h-40 flex-col items-center justify-center" description="暂无租户" />
        <AntTable v-else :columns="tenantColumns" :data-source="tenants" :pagination="false" row-key="id" :scroll="{ x: 1120 }" size="middle">
          <template #bodyCell="{ column, record: tenant }">
            <template v-if="column.key === 'tenant'"><div class="flex items-center gap-3"><div class="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary"><Building2 /></div><div><div class="font-medium">{{ tenant.name }}</div><code class="text-xs text-muted-foreground">{{ tenant.id }}</code></div></div></template>
            <Tag v-else-if="column.key === 'package'" color="blue">{{ tenant.tenantPackage?.name || '未配置套餐' }}</Tag>
            <Tag v-else-if="column.key === 'status'" :color="tenant.status === 'active' ? 'green' : 'red'">{{ statusLabel(tenant.status) }}</Tag>
            <div v-else-if="column.key === 'quota'" class="flex gap-4 text-sm text-muted-foreground"><span class="inline-flex items-center gap-1"><Users />{{ formatLimit(tenant.tenantPackage?.maxUsers) }} 用户</span><span>{{ formatLimit(tenant.tenantPackage?.maxApps) }} 应用</span></div>
            <Tag v-else-if="column.key === 'administrator'" >{{ roleLabel(tenant.role) }}</Tag>
            <div v-else-if="column.key === 'actions'" class="flex justify-end gap-1"><template v-if="!tenant.system"><Button type="link" size="small" class="h-auto p-0" :disabled="tenant.status === 'deleted' || !tenant.tenantPackage" @click="openEditDialog(tenant)"><Pencil class="size-3 mr-1" />编辑</Button><Button type="link" size="small" class="h-auto p-0" :disabled="tenant.status === 'deleted'" @click="toggleStatus(tenant)">{{ tenant.status === 'active' ? '停用' : '启用' }}</Button><Button type="link" size="small" class="h-auto p-0" :disabled="tenant.status !== 'active'" @click="openTransferDialog(tenant)"><UserRoundCog class="size-3 mr-1" />转移管理员</Button><Button type="link" size="small" class="h-auto p-0 text-destructive" :disabled="tenant.status === 'deleted'" @click="openDeleteDialog(tenant)"><Trash2 class="size-3 mr-1" />删除</Button></template><span v-else class="text-sm text-muted-foreground">系统内置</span></div>
          </template>
        </AntTable>
        <div class="flex items-center justify-between mt-4 pt-4 border-t">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <AntPagination :current="queryForm.page" :page-size="queryForm.pageSize || 20" :total="total" :show-size-changer="false" size="small" @change="handlePageChange" />
        </div>
      </div>
    </Card>

    <Modal v-model:open="tenantDialogVisible" :footer="null">
      <div class="w-full max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
          <p>{{ editingTenant ? '修改租户名称或更换为启用中的套餐。' : '新租户会绑定套餐，并将当前管理员设为租户管理员。' }}</p>
        </div>
        <Form :model="tenantForm" :rules="tenantFormRules" layout="vertical" class="py-4" @finish="submitTenant">
          <div class="grid gap-4">
            <FormItem label="租户名称" name="name">
              <Input v-model:value="tenantForm.name" placeholder="请输入租户名称" />
            </FormItem>
            <FormItem label="租户套餐" name="packageId">
              <Select v-model:value="tenantForm.packageId" class="w-full" placeholder="请选择租户套餐" :options="packageOptions" />
            </FormItem>
          </div>
          <div class="flex justify-end gap-2">
            <Button @click="tenantDialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit" :loading="submitting">保存</Button>
          </div>
        </Form>
      </div>
    </Modal>

    <Modal v-model:open="transferDialogVisible" :footer="null">
      <div class="w-full max-w-lg">
        <div><h3>转移租户管理员</h3><p>目标管理员将成为“{{ transferTarget?.name }}”的租户管理员，原管理员将被移出该租户。</p></div>
        <div class="mt-6 grid gap-2"><label for="transfer-administrator">目标管理员</label><Select id="transfer-administrator" v-model:value="transferAdministratorId" class="w-full" placeholder="请选择目标管理员" :options="administratorOptions" /></div>
        <div class="mt-6 flex justify-end gap-2"><Button @click="transferDialogVisible = false">取消</Button><Button :disabled="transferSubmitting" @click="submitTransfer">{{ transferSubmitting ? '转移中...' : '确认转移' }}</Button></div>
      </div>
    </Modal>

    <Modal v-model:open="deleteDialogVisible" :footer="null">
      <div><div><h3>删除租户</h3><p>确定删除“{{ deleteTarget?.name }}”吗？删除后将停用该租户及其所有管理员成员关系，但保留业务与审计数据。</p></div><div><Button @click="deleteDialogVisible = false">取消</Button><Button danger @click="confirmDelete">确认删除</Button></div></div>
    </Modal>
  </div>
</template>
