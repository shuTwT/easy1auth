<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Building2, Pencil, RefreshCw, Search, Trash2, UserRoundCog, Users } from '@lucide/vue'
import { adminUserApi } from '@/api/adminUser'
import { userPoolApi } from '@/api/userPool'
import { userPoolPackageApi } from '@/api/userPoolPackage'
import type { AdminUser } from '@/types/adminUser'
import type { UserPool, UserPoolQueryDto } from '@/types/userPool'
import type { UserPoolPackage } from '@/types/userPoolPackage'
import UserPoolEditDialog from './components/UserPoolEditDialog.vue'
import TransferAdministratorDialog from './components/TransferAdministratorDialog.vue'
import UserPoolDeleteDialog from './components/UserPoolDeleteDialog.vue'
import { useUserPoolDict } from './utils/hooks'
import type {
  TransferAdministratorDialogExpose,
  UserPoolDeleteDialogExpose,
  UserPoolEditDialogExpose,
} from './utils/types'

const loading = ref(false)
const userPools = ref<UserPool[]>([])
const packages = ref<UserPoolPackage[]>([])
const administrators = ref<AdminUser[]>([])
const total = ref(0)

const userPoolEditDialogRef = ref<UserPoolEditDialogExpose>()
const transferAdministratorDialogRef = ref<TransferAdministratorDialogExpose>()
const userPoolDeleteDialogRef = ref<UserPoolDeleteDialogExpose>()

const { userPoolStatusOptions, formatLimit, statusLabel, roleLabel } = useUserPoolDict()

const queryForm = reactive<UserPoolQueryDto>({ page: 1, pageSize: 20, name: '', status: undefined })

const userPoolColumns: TableProps<UserPool>['columns'] = [
  { title: '用户池', key: 'tenant' },
  { title: '套餐', key: 'package' },
  { title: '状态', key: 'status' },
  { title: '配额', key: 'quota' },
  { title: '用户池管理员', key: 'administrator' },
  { title: '操作', key: 'actions', align: 'right' },
]

const loadData = async () => {
  loading.value = true
  try {
    const [userPoolPage, packageRows, administratorPage] = await Promise.all([
      userPoolApi.getList(queryForm),
      userPoolPackageApi.list(),
      adminUserApi.getList({ page: 1, pageSize: 100, status: 'active' }),
    ])
    userPools.value = userPoolPage.items
    total.value = userPoolPage.total
    packages.value = packageRows.filter((item) => {
      return item.status === 'active'
    })
    administrators.value = administratorPage.items
  } catch (error) {
    console.error('加载用户池数据失败:', error)
    message.error('加载用户池数据失败')
  } finally {
    loading.value = false
  }
}

const openEditDialog = (tenant: UserPool) => {
  userPoolEditDialogRef.value?.open(tenant)
}

const toggleStatus = async (tenant: UserPool) => {
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

const openTransferDialog = (tenant: UserPool) => {
  transferAdministratorDialogRef.value?.open(tenant)
}

const openDeleteDialog = (tenant: UserPool) => {
  userPoolDeleteDialogRef.value?.open(tenant)
}

const handleSearch = async () => {
  queryForm.page = 1
  await loadData()
}

const handleReset = async () => {
  Object.assign(queryForm, { page: 1, name: '', status: undefined })
  await loadData()
}

const handlePageChange = async (page: number, _pageSize?: number) => {
  queryForm.page = page
  await loadData()
}

onMounted(() => {
  loadData()
})
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
          <a-select v-model:value="queryForm.status" class="w-full sm:w-32" allow-clear :options="userPoolStatusOptions" />
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

    <UserPoolEditDialog ref="userPoolEditDialogRef" :packages="packages" @success="loadData" />

    <TransferAdministratorDialog ref="transferAdministratorDialogRef" :administrators="administrators" @success="loadData" />

    <UserPoolDeleteDialog ref="userPoolDeleteDialogRef" @success="loadData" />
  </div>
</template>
