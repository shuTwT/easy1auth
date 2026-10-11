<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'antdv-next'
import { Building2, Pencil, RefreshCw, Search, Trash2, UserRoundCog, Users } from '@lucide/vue'
import { adminUserApi } from '@/api/adminUser'
import { userPoolApi } from '@/api/userPool'
import { userPoolPackageApi } from '@/api/userPoolPackage'
import type { AdminUser } from '@/types/adminUser'
import type { UserPool, UserPoolQueryDto } from '@/types/userPool'
import type { UserPoolPackage } from '@/types/userPoolPackage'
const loading = ref(false)
const submitting = ref(false)
const transferSubmitting = ref(false)
const userPools = ref<UserPool[]>([])
const packages = ref<UserPoolPackage[]>([])
const administrators = ref<AdminUser[]>([])
const total = ref(0)
const userPoolDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const editingUserPool = ref<UserPool | null>(null)
const transferTarget = ref<UserPool | null>(null)
const deleteTarget = ref<UserPool | null>(null)
const transferAdministratorId = ref<string>('')

const queryForm = reactive<UserPoolQueryDto>({ page: 1, pageSize: 20, name: '', status: undefined })
const userPoolForm = reactive({ name: '', packageId: undefined as number | undefined })
const userPoolFormRules = {
  name: [{ required: true, message: '请输入用户池名称' }],
  packageId: [{ required: true, message: '请选择用户池套餐' }],
}
const dialogTitle = computed(() => '编辑用户池')
const packageOptions = computed(() => packages.value.map(item => ({
  value: item.id,
  label: `${item.name} · ${formatLimit(item.maxUsers)} 用户 / ${formatLimit(item.maxApps)} 应用`,
})))
const administratorOptions = computed(() => administrators.value.map(item => ({
  value: item.id,
  label: `${item.username} · ${item.email}`,
})))

const userPoolColumns = [
  { title: '用户池', key: 'tenant' },
  { title: '套餐', key: 'package' },
  { title: '状态', key: 'status' },
  { title: '配额', key: 'quota' },
  { title: '用户池管理员', key: 'administrator' },
  { title: '操作', key: 'actions', align: 'right' as const },
]

function formatLimit(value?: number) {
  if (value === undefined) return '-'
  return value >= 2147483647 ? '不限' : value.toLocaleString()
}

function statusLabel(status: UserPool['status']) {
  return status === 'active' ? '正常' : status === 'suspended' ? '停用' : '已删除'
}

function roleLabel(role: string) {
  return role === 'super_admin' ? '超级管理员' : role === 'tenant_admin' ? '用户池管理员' : '普通成员'
}

async function loadData() {
  loading.value = true
  try {
    const [userPoolPage, packageRows, administratorPage] = await Promise.all([
      userPoolApi.getList(queryForm),
      userPoolPackageApi.list(),
      adminUserApi.getList({ page: 1, pageSize: 100, status: 'active' }),
    ])
    userPools.value = userPoolPage.items
    total.value = userPoolPage.total
    packages.value = packageRows.filter((item) => item.status === 'active')
    administrators.value = administratorPage.items
  } catch (error) {
    console.error('加载用户池数据失败:', error)
    message.error('加载用户池数据失败')
  } finally {
    loading.value = false
  }
}

function openEditDialog(tenant: UserPool) {
  if (!tenant.userPoolPackage) return
  editingUserPool.value = tenant
  Object.assign(userPoolForm, { name: tenant.name, packageId: tenant.userPoolPackage.id })
  userPoolDialogVisible.value = true
}

async function submitUserPool() {
  submitting.value = true
  try {
    if (!editingUserPool.value) return
    await userPoolApi.update(editingUserPool.value.id, { name: userPoolForm.name.trim(), packageId: userPoolForm.packageId })
    message.success('用户池更新成功')
    userPoolDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('保存用户池失败:', error)
    message.error('保存用户池失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(tenant: UserPool) {
  try {
    const status = tenant.status === 'active' ? 'suspended' : 'active'
    await userPoolApi.updateStatus(tenant.id, status)
    message.success(status === 'active' ? '用户池已启用' : '用户池已停用')
    await loadData()
  } catch (error) {
    console.error('更新用户池状态失败:', error)
    message.error('更新用户池状态失败')
  }
}

function openTransferDialog(tenant: UserPool) {
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
    await userPoolApi.transferAdministrator(transferTarget.value.id, transferAdministratorId.value)
    message.success('用户池管理员转移成功')
    transferDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('转移用户池管理员失败:', error)
    message.error('转移用户池管理员失败')
  } finally {
    transferSubmitting.value = false
  }
}

function openDeleteDialog(tenant: UserPool) {
  deleteTarget.value = tenant
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await userPoolApi.delete(deleteTarget.value.id)
    message.success('用户池已删除')
    deleteDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('删除用户池失败:', error)
    message.error('删除用户池失败')
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
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">用户池管理</h1>
        <p class="text-sm text-[#52677D]">查看平台内全部用户池，并管理普通用户池的套餐、状态和用户池管理员。</p>
      </div>
      <div class="flex gap-3"><a-button :disabled="loading" @click="loadData"><RefreshCw class="size-4 mr-2" :class="loading ? 'animate-spin' : ''" />刷新</a-button></div>
    </div>

    <a-card>
      <div class="gap-4 flex mb-3!">
        <div class="flex flex-col gap-2 sm:flex-row">
          <div class="relative"><Search class="absolute left-3 top-1/2 -translate-y-1/2 text-[#52677D]" /><a-input v-model:value="queryForm.name" class="w-full pl-9 sm:w-56" placeholder="搜索用户池名称" /></div>
          <a-select v-model:value="queryForm.status" class="w-full sm:w-32" allow-clear :options="[{ value: 'active', label: '正常' }, { value: 'suspended', label: '停用' }, { value: 'deleted', label: '已删除' }]" />
          <a-button  @click="handleSearch">查询</a-button>
          <a-button  @click="handleReset">重置</a-button>
        </div>
      </div>
      <div>
        <a-spin v-if="loading" class="flex min-h-40 items-center justify-center" />
        <a-empty v-else-if="userPools.length === 0" class="flex min-h-40 flex-col items-center justify-center" description="暂无用户池" />
        <a-table v-else :columns="userPoolColumns" :data-source="userPools" :pagination="false" row-key="id" :scroll="{ x: 1120 }" size="middle">
          <template #bodyCell="{ column, record: tenant }">
            <template v-if="column.key === 'tenant'"><div class="flex items-center gap-3"><div class="flex size-9 items-center justify-center rounded-lg bg-[#0369A1]/10 text-[#0369A1]"><Building2 /></div><div><div class="font-medium">{{ tenant.name }}</div><code class="text-xs text-[#52677D]">{{ tenant.id }}</code></div></div></template>
            <a-tag v-else-if="column.key === 'package'" color="blue">{{ tenant.userPoolPackage?.name || '未配置套餐' }}</a-tag>
            <a-tag v-else-if="column.key === 'status'" :color="tenant.status === 'active' ? 'green' : 'red'">{{ statusLabel(tenant.status) }}</a-tag>
            <div v-else-if="column.key === 'quota'" class="flex gap-4 text-sm text-[#52677D]"><span class="inline-flex items-center gap-1"><Users />{{ formatLimit(tenant.userPoolPackage?.maxUsers) }} 用户</span><span>{{ formatLimit(tenant.userPoolPackage?.maxApps) }} 应用</span></div>
            <a-tag v-else-if="column.key === 'administrator'" >{{ roleLabel(tenant.role) }}</a-tag>
            <div v-else-if="column.key === 'actions'" class="flex justify-end gap-1"><template v-if="!tenant.system"><a-button type="link" size="small" class="h-auto p-0" :disabled="tenant.status === 'deleted' || !tenant.userPoolPackage" @click="openEditDialog(tenant)"><Pencil class="size-3 mr-1" />编辑</a-button><a-button type="link" size="small" class="h-auto p-0" :disabled="tenant.status === 'deleted'" @click="toggleStatus(tenant)">{{ tenant.status === 'active' ? '停用' : '启用' }}</a-button><a-button type="link" size="small" class="h-auto p-0" :disabled="tenant.status !== 'active'" @click="openTransferDialog(tenant)"><UserRoundCog class="size-3 mr-1" />转移管理员</a-button><a-button type="link" size="small" class="h-auto p-0 text-[#B91C1C]" :disabled="tenant.status === 'deleted'" @click="openDeleteDialog(tenant)"><Trash2 class="size-3 mr-1" />删除</a-button></template><span v-else class="text-sm text-[#52677D]">系统内置</span></div>
          </template>
        </a-table>
        <div class="flex items-center justify-between mt-4 pt-4 border-t">
          <span class="text-sm text-[#52677D]">共 {{ total }} 条</span>
          <a-pagination :current="queryForm.page" :page-size="queryForm.pageSize || 20" :total="total" :show-size-changer="false" size="small" @change="handlePageChange" />
        </div>
      </div>
    </a-card>

    <a-modal v-model:open="userPoolDialogVisible" :footer="null">
      <div class="w-full max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
          <p>{{ editingUserPool ? '修改用户池名称或更换为启用中的套餐。' : '新用户池会绑定套餐，并将当前管理员设为用户池管理员。' }}</p>
        </div>
        <a-form :model="userPoolForm" :rules="userPoolFormRules" layout="vertical" class="py-4" @finish="submitUserPool">
          <div class="grid gap-4">
            <a-form-item label="用户池名称" name="name">
              <a-input v-model:value="userPoolForm.name" placeholder="请输入用户池名称" />
            </a-form-item>
            <a-form-item label="用户池套餐" name="packageId">
              <a-select v-model:value="userPoolForm.packageId" class="w-full" placeholder="请选择用户池套餐" :options="packageOptions" />
            </a-form-item>
          </div>
          <div class="flex justify-end gap-2">
            <a-button @click="userPoolDialogVisible = false">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
          </div>
        </a-form>
      </div>
    </a-modal>

    <a-modal v-model:open="transferDialogVisible" :footer="null">
      <div class="w-full max-w-lg">
        <div><h3>转移用户池管理员</h3><p>目标管理员将成为“{{ transferTarget?.name }}”的用户池管理员，原管理员将被移出该用户池。</p></div>
        <div class="mt-6 grid gap-2"><label for="transfer-administrator">目标管理员</label><a-select id="transfer-administrator" v-model:value="transferAdministratorId" class="w-full" placeholder="请选择目标管理员" :options="administratorOptions" /></div>
        <div class="mt-6 flex justify-end gap-2"><a-button @click="transferDialogVisible = false">取消</a-button><a-button :disabled="transferSubmitting" @click="submitTransfer">{{ transferSubmitting ? '转移中...' : '确认转移' }}</a-button></div>
      </div>
    </a-modal>

    <a-modal v-model:open="deleteDialogVisible" :footer="null">
      <div><div><h3>删除用户池</h3><p>确定删除“{{ deleteTarget?.name }}”吗？删除后将停用该用户池及其所有管理员成员关系，但保留业务与审计数据。</p></div><div><a-button @click="deleteDialogVisible = false">取消</a-button><a-button danger @click="confirmDelete">确认删除</a-button></div></div>
    </a-modal>
  </div>
</template>
