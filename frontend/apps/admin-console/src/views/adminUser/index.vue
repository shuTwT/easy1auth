<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'antdv-next'
import type { TableProps, TabsProps } from 'antdv-next'
import {
  Search,
  RefreshCw,
  ShieldCheck,
  ShieldAlert,
  UserCog,
  KeyRound,
  Plus,
  Users,
  Lock,
  Settings2,
  Shield,
} from '@lucide/vue'
import { adminUserApi } from '@/api/adminUser'
import { adminRoleApi } from '@/api/adminRole'
import type { AdminUser, AdminUserStats, AdminStatus } from '@/types/adminUser'
import type { AdminRole, AdminRoleStats, PermissionCatalogItem } from '@/types/adminRole'
import AdminEditDialog from './components/AdminEditDialog.vue'
import ResetPasswordDialog from './components/ResetPasswordDialog.vue'
import ResetMfaDialog from './components/ResetMfaDialog.vue'
import AssignRolesDialog from './components/AssignRolesDialog.vue'
import RemoveFromPoolDialog from './components/RemoveFromPoolDialog.vue'
import RoleEditDialog from './components/RoleEditDialog.vue'
import RolePermissionDialog from './components/RolePermissionDialog.vue'
import DeleteRoleDialog from './components/DeleteRoleDialog.vue'
import { getApiErrorMessage, useAdminDict, useAdminMembership } from './utils/hooks'
import type {
  AdminEditDialogExpose,
  AdminQueryForm,
  AdminTabKey,
  AssignRolesDialogExpose,
  DeleteRoleDialogExpose,
  RemoveFromPoolDialogExpose,
  ResetMfaDialogExpose,
  ResetPasswordDialogExpose,
  RoleEditDialogExpose,
  RolePermissionDialogExpose,
} from './utils/types'

const {
  adminStatusOptions,
  adminStatusVariant,
  adminStatusText,
  tenantRoleText,
  tenantRoleVariant,
  roleTypeVariant,
  permissionsSummary,
  formatDate,
} = useAdminDict()

const {
  isSelf,
  isAccountOwner,
  currentMembership,
  currentUserPoolRole,
  isCurrentMembershipOwner,
} = useAdminMembership()

const activeTab = ref<AdminTabKey>('admins')
const adminTabs: TabsProps['items'] = [
  { key: 'admins', label: '管理员', forceRender: true },
  { key: 'roles', label: '管理员角色', forceRender: true },
]

// dialog refs
const adminEditDialogRef = ref<AdminEditDialogExpose>()
const resetPasswordDialogRef = ref<ResetPasswordDialogExpose>()
const resetMfaDialogRef = ref<ResetMfaDialogExpose>()
const assignRolesDialogRef = ref<AssignRolesDialogExpose>()
const removeFromPoolDialogRef = ref<RemoveFromPoolDialogExpose>()
const roleEditDialogRef = ref<RoleEditDialogExpose>()
const rolePermissionDialogRef = ref<RolePermissionDialogExpose>()
const deleteRoleDialogRef = ref<DeleteRoleDialogExpose>()

// ---------------------------------------------------------------------------
// Admin tab state
// ---------------------------------------------------------------------------
const adminLoading = ref(false)
const admins = ref<AdminUser[]>([])
const adminTotal = ref(0)
const adminStats = ref<AdminUserStats>({
  totalAdmins: 0,
  activeAdmins: 0,
  disabledAdmins: 0,
  mfaEnabledAdmins: 0,
  ownerCount: 0,
})

const adminQuery = reactive<AdminQueryForm>({
  page: 1,
  pageSize: 10,
  username: '',
  email: '',
  phone: '',
  status: undefined,
  roleId: undefined,
})

// roles available for the search filter + assignment dialog
const adminRolesForFilter = ref<AdminRole[]>([])
const adminRoleOptions = computed(() => {
  return adminRolesForFilter.value.map((role) => {
    return { value: role.id, label: role.name }
  })
})

// ---------------------------------------------------------------------------
// Admin role tab state
// ---------------------------------------------------------------------------
const roleLoading = ref(false)
const roles = ref<AdminRole[]>([])
const roleTotal = ref(0)
const roleStats = ref<AdminRoleStats>({
  totalRoles: 0,
  systemRoles: 0,
  customRoles: 0,
  totalAdmins: 0,
})
const rolePage = ref(1)
const rolePageSize = ref(10)
const roleSearch = ref('')

const permissionCatalog = ref<PermissionCatalogItem[]>([])

// ---------------------------------------------------------------------------
// Admin tab helpers
// ---------------------------------------------------------------------------
const adminStatsCards = computed(() => {
  return [
    { label: '管理员总数', value: adminStats.value.totalAdmins, icon: Users, tone: 'primary' },
    { label: '活跃管理员', value: adminStats.value.activeAdmins, icon: ShieldCheck, tone: 'success' },
    { label: '已禁用', value: adminStats.value.disabledAdmins, icon: ShieldAlert, tone: 'danger' },
    { label: 'MFA 已启用', value: adminStats.value.mfaEnabledAdmins, icon: KeyRound, tone: 'info' },
    { label: '用户池所有者', value: adminStats.value.ownerCount || 0, icon: Shield, tone: 'warning' },
  ]
})

const adminColumns: TableProps<AdminUser>['columns'] = [
  { title: '用户名', key: 'username', width: 190 },
  { title: '邮箱', key: 'email', width: 210 },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '当前用户池角色', key: 'tenantRole', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: 'MFA', key: 'mfa', width: 100 },
  { title: '最后登录', key: 'lastLoginAt', width: 180 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 310, fixed: 'end' },
]

const loadAdminStats = async () => {
  try {
    const res = await adminUserApi.getStats()
    adminStats.value = res
  } catch (error) {
    console.error('加载管理员统计失败:', error)
    message.error('加载管理员统计失败')
  }
}

const loadAdmins = async () => {
  adminLoading.value = true
  try {
    const res = await adminUserApi.getList({
      page: adminQuery.page,
      pageSize: adminQuery.pageSize,
      username: adminQuery.username || undefined,
      email: adminQuery.email || undefined,
      phone: adminQuery.phone || undefined,
      status: adminQuery.status,
      roleId: adminQuery.roleId || undefined,
    })
    admins.value = res.items
    adminTotal.value = res.total
  } catch (error) {
    console.error('加载管理员列表失败:', error)
    message.error('加载管理员列表失败')
  } finally {
    adminLoading.value = false
  }
}

const loadAdminRolesForFilter = async () => {
  try {
    const res = await adminRoleApi.getList({ pageSize: 100 })
    adminRolesForFilter.value = res.items
  } catch (error) {
    console.error('加载管理员角色失败:', error)
    message.error('加载管理员角色失败')
  }
}

const reloadAdminsAndStats = () => {
  loadAdmins()
  loadAdminStats()
}

const handleAdminSearch = () => {
  adminQuery.page = 1
  loadAdmins()
}

const handleAdminReset = () => {
  adminQuery.username = ''
  adminQuery.email = ''
  adminQuery.phone = ''
  adminQuery.status = undefined
  adminQuery.roleId = undefined
  adminQuery.page = 1
  loadAdmins()
}

const handleAdminPageChange = (page: number, _pageSize?: number) => {
  adminQuery.page = page
  loadAdmins()
}

const handleAdminPageSizeChange = (_current: number, size: number) => {
  adminQuery.pageSize = size
  adminQuery.page = 1
  loadAdmins()
}

const handleStatusToggle = async (row: AdminUser) => {
  const next: AdminStatus = row.status === 'active' ? 'disabled' : 'active'
  try {
    await adminUserApi.updateStatus(row.id, { status: next })
    message.success(next === 'active' ? '已启用管理员' : '已禁用管理员')
    loadAdmins()
    loadAdminStats()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '状态更新失败'))
  }
}

const openEditDialog = (row: AdminUser) => {
  adminEditDialogRef.value?.open(row)
}

const openResetPwdDialog = (row: AdminUser) => {
  resetPasswordDialogRef.value?.open(row)
}

const openResetMfaDialog = (row: AdminUser) => {
  resetMfaDialogRef.value?.open(row)
}

const openAssignRolesDialog = (row: AdminUser) => {
  assignRolesDialogRef.value?.open(row)
}

const openRemoveDialog = (row: AdminUser) => {
  removeFromPoolDialogRef.value?.open(row)
}

// ---------------------------------------------------------------------------
// Admin role tab helpers
// ---------------------------------------------------------------------------
const roleStatsCards = computed(() => {
  return [
    { label: '角色总数', value: roleStats.value.totalRoles, icon: Settings2, tone: 'primary' },
    { label: '系统角色', value: roleStats.value.systemRoles, icon: Lock, tone: 'danger' },
    { label: '自定义角色', value: roleStats.value.customRoles, icon: UserCog, tone: 'success' },
  ]
})

const roleColumns: TableProps<AdminRole>['columns'] = [
  { title: '角色名称', key: 'name', width: 220 },
  { title: '角色编码', dataIndex: 'code', key: 'code', width: 200 },
  { title: '描述', key: 'description', minWidth: 220 },
  { title: '权限', key: 'permissions', width: 140 },
  { title: '管理员数', key: 'adminCount', width: 110, align: 'center' },
  { title: '类型', key: 'type', width: 110 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 230, fixed: 'end' },
]

const loadRoleStats = async () => {
  try {
    const res = await adminRoleApi.getStats()
    roleStats.value = res
  } catch (error) {
    console.error('加载管理员角色统计失败:', error)
    message.error('加载管理员角色统计失败')
  }
}

const loadRoles = async () => {
  roleLoading.value = true
  try {
    const res = await adminRoleApi.getList({
      page: rolePage.value,
      pageSize: rolePageSize.value,
      name: roleSearch.value || undefined,
    })
    roles.value = res.items
    roleTotal.value = res.total
  } catch (error) {
    console.error('加载管理员角色列表失败:', error)
    message.error('加载管理员角色列表失败')
  } finally {
    roleLoading.value = false
  }
}

const loadPermissionCatalog = async () => {
  try {
    const res = await adminRoleApi.getPermissionCatalog()
    permissionCatalog.value = res.permissions
  } catch (error) {
    console.error('加载权限目录失败:', error)
    message.error('加载权限目录失败')
  }
}

const reloadRolesAndStats = () => {
  loadRoles()
  loadRoleStats()
}

const handleRoleSearch = () => {
  rolePage.value = 1
  loadRoles()
}

const handleRoleReset = () => {
  roleSearch.value = ''
  rolePage.value = 1
  loadRoles()
}

const handleRolePageChange = (page: number, _pageSize?: number) => {
  rolePage.value = page
  loadRoles()
}

const openCreateRoleDialog = () => {
  roleEditDialogRef.value?.open('create')
}

const openEditRoleDialog = (row: AdminRole) => {
  roleEditDialogRef.value?.open('edit', row)
}

const openRolePermissionDialog = (row: AdminRole) => {
  rolePermissionDialogRef.value?.open(row)
}

const openDeleteRoleDialog = (row: AdminRole) => {
  deleteRoleDialogRef.value?.open(row)
}

// ---------------------------------------------------------------------------
// Init
// ---------------------------------------------------------------------------
onMounted(() => {
  loadAdminStats()
  loadAdmins()
  loadAdminRolesForFilter()
  loadRoleStats()
  loadRoles()
  loadPermissionCatalog()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#102A43] mb-2">管理员管理</h1>
      <p class="text-sm text-[#52677D]">管理员账号为全局账号；管理员角色归属于当前选中的用户池</p>
    </div>

    <a-tabs v-model:active-key="activeTab" :items="adminTabs">
      <template #contentRender="{ item }">
        <template v-if="item.key === 'admins'">
          <!-- stats -->
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 mb-6">
            <a-card v-for="(stat, index) in adminStatsCards" :key="index" class="transition-all hover:shadow-md">
              <div class="pt-4">
                <div class="flex items-center gap-3">
                  <div
                    class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
                    :class="{
                      'bg-gradient-to-br from-[#0369A1] to-[#0369A1]/60': stat.tone === 'primary',
                      'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.tone === 'success',
                      'bg-gradient-to-br from-red-500 to-red-400': stat.tone === 'danger',
                      'bg-gradient-to-br from-sky-500 to-sky-400': stat.tone === 'info',
                      'bg-gradient-to-br from-amber-500 to-amber-400': stat.tone === 'warning',
                    }"
                  >
                    <component :is="stat.icon" class="size-5" />
                  </div>
                  <div>
                    <div class="text-2xl font-bold text-[#102A43] leading-tight">{{ stat.value }}</div>
                    <div class="text-xs text-[#52677D] mt-0.5">{{ stat.label }}</div>
                  </div>
                </div>
              </div>
            </a-card>
          </div>

          <!-- search -->
          <a-card class="mb-4!">
            <div class="pt-6">
              <div class="flex flex-wrap gap-4 items-end">
                <div class="grid gap-2">
                  <label>用户名</label>
                  <a-input v-model:value="adminQuery.username" placeholder="请输入用户名" class="w-40" />
                </div>
                <div class="grid gap-2">
                  <label>邮箱</label>
                  <a-input v-model:value="adminQuery.email" placeholder="请输入邮箱" class="w-40" />
                </div>
                <div class="grid gap-2">
                  <label>手机号</label>
                  <a-input v-model:value="adminQuery.phone" placeholder="请输入手机号" class="w-40" />
                </div>
                <div class="grid gap-2">
                  <label>状态</label>
                  <a-select v-model:value="adminQuery.status" class="w-32" allow-clear :options="adminStatusOptions" />
                </div>
                <div class="grid gap-2">
                  <label>管理员角色</label>
                  <a-select v-model:value="adminQuery.roleId" class="w-44" allow-clear :options="adminRoleOptions" />
                </div>
                <div class="flex gap-2">
                  <a-button @click="handleAdminSearch">
                    <Search class="size-4 mr-1" />
                    搜索
                  </a-button>
                  <a-button  @click="handleAdminReset">
                    <RefreshCw class="size-4 mr-1" />
                    重置
                  </a-button>
                </div>
              </div>
            </div>
          </a-card>

          <!-- table -->
          <a-card>
            <div class="pt-6">
              <a-table
                :columns="adminColumns"
                :data-source="admins"
                :loading="adminLoading"
                :pagination="false"
                row-key="id"
                :scroll="{ x: 1540 }"
              >
                <template #bodyCell="{ column, record: row }">
                  <template v-if="column.key === 'username'">
                      <div class="flex items-center gap-2">
                        <a-avatar class="size-8 bg-gradient-to-br from-[#0369A1] to-[#0369A1]/60">
                          <span class="bg-transparent text-white text-xs font-semibold">
                            {{ row.username.charAt(0).toUpperCase() }}
                          </span>
                        </a-avatar>
                        <div class="flex flex-col">
                          <span class="font-medium">{{ row.username }}</span>
                          <span v-if="isSelf(row)" class="text-xs text-[#0369A1]">（我）</span>
                        </div>
                      </div>
                  </template>
                  <template v-else-if="column.key === 'email'">{{ row.email }}</template>
                  <template v-else-if="column.key === 'phone'">{{ row.phone || '-' }}</template>
                  <template v-else-if="column.key === 'tenantRole'">
                      <a-tag v-if="currentUserPoolRole(row)" :color="tenantRoleVariant(currentUserPoolRole(row))" class="text-xs">
                        {{ tenantRoleText(currentUserPoolRole(row)) }}
                      </a-tag>
                      <span v-else class="text-[#52677D]">-</span>
                  </template>
                  <template v-else-if="column.key === 'status'">
                      <a-tag :color="adminStatusVariant(row.status)" class="text-xs">
                        {{ adminStatusText(row.status) }}
                      </a-tag>
                  </template>
                  <template v-else-if="column.key === 'mfa'">
                      <a-tag :color="row.mfaEnabled ? 'green' : 'default'" class="text-xs">
                        {{ row.mfaEnabled ? '已启用' : '未启用' }}
                      </a-tag>
                  </template>
                  <span v-else-if="column.key === 'lastLoginAt'" class="text-[#52677D]">{{ formatDate(row.lastLoginAt) }}</span>
                  <span v-else-if="column.key === 'createdAt'" class="text-[#52677D]">{{ formatDate(row.createdAt) }}</span>
                  <template v-else-if="column.key === 'actions'">
                      <div class="flex gap-1 flex-wrap">
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="isSelf(row) || isAccountOwner(row)"
                          @click="openEditDialog(row)"
                        >
                          编辑
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="isSelf(row) || isAccountOwner(row)"
                          @click="handleStatusToggle(row)"
                        >
                          {{ row.status === 'active' ? '禁用' : '启用' }}
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="isSelf(row) || !currentMembership(row) || isCurrentMembershipOwner(row)"
                          @click="openAssignRolesDialog(row)"
                        >
                          分配角色
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="isSelf(row) || isAccountOwner(row)"
                          @click="openResetPwdDialog(row)"
                        >
                          重置密码
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="isSelf(row) || isAccountOwner(row) || !row.mfaEnabled"
                          @click="openResetMfaDialog(row)"
                        >
                          重置 MFA
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0 text-[#B91C1C] disabled:text-[#52677D]"
                          :disabled="isSelf(row) || !currentMembership(row) || isCurrentMembershipOwner(row)"
                          @click="openRemoveDialog(row)"
                        >
                          移出用户池
                        </a-button>
                      </div>
                  </template>
                </template>
              </a-table>

              <div class="flex items-center justify-between mt-4 pt-4 border-t">
                <span class="text-sm text-[#52677D]">共 {{ adminTotal }} 条</span>
                <a-pagination
                  :current="adminQuery.page"
                  :page-size="adminQuery.pageSize"
                  :total="adminTotal"
                  :show-size-changer="true"
                  :page-size-options="['10', '20', '50']"
                  size="small"
                  @change="handleAdminPageChange"
                  @show-size-change="handleAdminPageSizeChange"
                />
              </div>
            </div>
          </a-card>
        </template>
        <template v-else-if="item.key === 'roles'">
          <!-- stats -->
          <div class="grid grid-cols-3 gap-4 mb-6">
            <a-card v-for="(stat, index) in roleStatsCards" :key="index" class="transition-all hover:shadow-md">
              <div class="pt-4">
                <div class="flex items-center gap-3">
                  <div
                    class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
                    :class="{
                      'bg-gradient-to-br from-[#0369A1] to-[#0369A1]/60': stat.tone === 'primary',
                      'bg-gradient-to-br from-red-500 to-red-400': stat.tone === 'danger',
                      'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.tone === 'success',
                    }"
                  >
                    <component :is="stat.icon" class="size-5" />
                  </div>
                  <div>
                    <div class="text-2xl font-bold text-[#102A43] leading-tight">{{ stat.value }}</div>
                    <div class="text-xs text-[#52677D] mt-0.5">{{ stat.label }}</div>
                  </div>
                </div>
              </div>
            </a-card>
          </div>

          <!-- role list -->
          <a-card>
            <div>
              <div class="flex justify-between items-center mb-4">
                <div class="flex items-center gap-2">
                  <div class="relative">
                    <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-[#52677D]" />
                    <a-input v-model:value="roleSearch"
                      placeholder="搜索角色名称"
                      class="w-64 pl-8"
                      @keyup.enter="handleRoleSearch"
                    />
                  </div>
                  <a-button @click="handleRoleSearch">
                    <Search class="size-4 mr-1" />
                    搜索
                  </a-button>
                  <a-button  @click="handleRoleReset">
                    <RefreshCw class="size-4 mr-1" />
                    重置
                  </a-button>
                </div>
                <a-button @click="openCreateRoleDialog">
                  <Plus class="size-4 mr-1" />
                  创建自定义角色
                </a-button>
              </div>

              <a-table
                :columns="roleColumns"
                :data-source="roles"
                :loading="roleLoading"
                :pagination="false"
                row-key="id"
                :scroll="{ x: 1310 }"
              >
                <template #bodyCell="{ column, record: row }">
                  <template v-if="column.key === 'name'">
                      <div class="flex items-center gap-2">
                        <a-tag :color="roleTypeVariant(row.isSystem)" class="text-xs">
                          {{ row.isSystem ? '系统' : '自定义' }}
                        </a-tag>
                        <span class="font-medium">{{ row.name }}</span>
                      </div>
                  </template>
                  <code v-else-if="column.key === 'code'" class="font-mono text-sm text-[#52677D]">{{ row.code }}</code>
                  <span v-else-if="column.key === 'description'" class="text-[#52677D]">{{ row.description || '-' }}</span>
                  <template v-else-if="column.key === 'permissions'">
                      <a-tag  class="text-xs">{{ permissionsSummary(row.permissions) }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'adminCount'">{{ row.adminCount || 0 }}</template>
                  <template v-else-if="column.key === 'type'">
                      <a-tag :color="row.isSystem ? 'purple' : 'blue'" class="text-xs">
                        {{ row.isSystem ? '系统' : '自定义' }}
                      </a-tag>
                  </template>
                  <span v-else-if="column.key === 'createdAt'" class="text-[#52677D]">{{ formatDate(row.createdAt) }}</span>
                  <template v-else-if="column.key === 'actions'">
                      <div class="flex gap-2">
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="row.isSystem"
                          @click="openRolePermissionDialog(row)"
                        >
                          配置权限
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0"
                          :disabled="row.isSystem"
                          @click="openEditRoleDialog(row)"
                        >
                          编辑
                        </a-button>
                        <a-button
                          type="link"
                          size="small"
                          class="h-auto p-0 text-[#B91C1C]"
                          :disabled="row.isSystem"
                          @click="openDeleteRoleDialog(row)"
                        >
                          删除
                        </a-button>
                      </div>
                  </template>
                </template>
              </a-table>

              <div class="flex items-center justify-between mt-4 pt-4 border-t">
                <span class="text-sm text-[#52677D]">共 {{ roleTotal }} 条</span>
                <a-pagination
                  :current="rolePage"
                  :page-size="rolePageSize"
                  :total="roleTotal"
                  :show-size-changer="false"
                  size="small"
                  @change="handleRolePageChange"
                />
              </div>
            </div>
          </a-card>
        </template>
      </template>
    </a-tabs>

    <AdminEditDialog ref="adminEditDialogRef" @success="loadAdmins" />
    <ResetPasswordDialog ref="resetPasswordDialogRef" />
    <ResetMfaDialog ref="resetMfaDialogRef" @success="reloadAdminsAndStats" />
    <AssignRolesDialog ref="assignRolesDialogRef" :roles="adminRolesForFilter" @success="loadAdmins" />
    <RemoveFromPoolDialog ref="removeFromPoolDialogRef" @success="reloadAdminsAndStats" />
    <RoleEditDialog ref="roleEditDialogRef" @success="reloadRolesAndStats" />
    <RolePermissionDialog ref="rolePermissionDialogRef" :permission-catalog="permissionCatalog" @success="loadRoles" />
    <DeleteRoleDialog ref="deleteRoleDialogRef" @success="reloadRolesAndStats" />
  </div>
</template>
