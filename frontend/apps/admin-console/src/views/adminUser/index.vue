<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { message, type TabsProps } from 'antdv-next'
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
import { useUserStore } from '@/stores/user'
import { adminUserApi } from '@/api/adminUser'
import { adminRoleApi } from '@/api/adminRole'
import type {
  AdminUser,
  AdminMembership,
  AdminUserQueryDto,
  AdminUserStats,
  AdminStatus,
  UpdateAdminDto,
} from '@/types/adminUser'
import type {
  AdminRole,
  AdminRoleStats,
  CreateAdminRoleDto,
  UpdateAdminRoleDto,
  PermissionCatalogItem,
} from '@/types/adminRole'
type ApiErrorResponse = {
  readonly msg?: string
}

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) return fallback
  return error.response?.data?.msg ?? fallback
}

const userStore = useUserStore()
const currentAdminId = computed<string>(() => userStore.userInfo?.id || '')

const activeTab = ref<'admins' | 'roles'>('admins')
const adminTabs: TabsProps['items'] = [
  { key: 'admins', label: '管理员', forceRender: true },
  { key: 'roles', label: '管理员角色', forceRender: true },
]

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

const adminQuery = reactive<Required<Pick<AdminUserQueryDto, 'page' | 'pageSize'>> & AdminUserQueryDto>({
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
const adminStatusOptions = [
  { value: 'active', label: '正常' },
  { value: 'disabled', label: '禁用' },
]
const adminRoleOptions = computed(() => adminRolesForFilter.value.map((role) => ({ value: role.id, label: role.name })))

// edit admin dialog
const editDialogVisible = ref(false)
const editTarget = ref<AdminUser | null>(null)
const editForm = reactive<UpdateAdminDto>({ username: '', email: '', phone: '' })
const editSubmitting = ref(false)

// reset password dialog
const resetPwdDialogVisible = ref(false)
const resetPwdTarget = ref<AdminUser | null>(null)
const resetPwdForm = reactive({ newPassword: '', confirmPassword: '' })
const resetPwdFormRules = {
  newPassword: [{ required: true, message: '请输入新密码' }],
  confirmPassword: [{ required: true, message: '请确认密码' }, { validator: (_rule: any, value: string) => value === resetPwdForm.newPassword ? Promise.resolve() : Promise.reject('两次输入的密码不一致') }],
}
const resetPwdSubmitting = ref(false)

// reset MFA confirmation
const resetMfaDialogVisible = ref(false)
const resetMfaTarget = ref<AdminUser | null>(null)
const resetMfaSubmitting = ref(false)

// assign roles dialog
const assignRolesDialogVisible = ref(false)
const assignRolesTarget = ref<AdminUser | null>(null)
const assignRolesSelected = ref<string[]>([])
const assignRolesSubmitting = ref(false)
const assignRolesMembership = computed<AdminMembership | undefined>(() => {
  const target = assignRolesTarget.value
  return target ? currentMembership(target) : undefined
})

// remove from tenant confirmation
const removeDialogVisible = ref(false)
const removeTarget = ref<AdminUser | null>(null)
const removeSubmitting = ref(false)

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

// create/edit role dialog
const roleDialogVisible = ref(false)
const roleDialogIsEdit = ref(false)
const roleDialogSubmitting = ref(false)
const roleForm = reactive<Omit<CreateAdminRoleDto, 'permissions'> & { id?: string }>({
  name: '',
  code: '',
  description: '',
})
const roleFormRules = {
  name: [{ required: true, message: '请输入角色名称' }],
  code: [
    { required: true, message: '请输入角色编码' },
    { pattern: /^[a-z][a-z0-9_]{1,99}$/, message: '请输入2至100位小写字母、数字或下划线，且以字母开头' },
  ],
}
const roleDialogTitle = computed(() => (roleDialogIsEdit.value ? '编辑管理员角色' : '创建管理员角色'))

// configure role permissions dialog
const rolePermissionDialogVisible = ref(false)
const rolePermissionTarget = ref<AdminRole | null>(null)
const rolePermissionCodes = ref<string[]>([])
const rolePermissionSubmitting = ref(false)

// delete role confirmation
const deleteRoleDialogVisible = ref(false)
const deleteRoleTarget = ref<AdminRole | null>(null)
const deleteRoleSubmitting = ref(false)

// ---------------------------------------------------------------------------
// Permission catalog grouping
// ---------------------------------------------------------------------------
const CATEGORY_LABELS: Record<string, string> = {
  'admin-user': '管理员用户管理',
  'admin-role': '管理员角色管理',
}

interface PermissionCategory {
  code: string
  name: string
  items: { code: string; name: string }[]
}

const permissionCategories = computed<PermissionCategory[]>(() => {
  const groups: Record<string, { code: string; name: string }[]> = {}
  const menuNames = new Map(permissionCatalog.value
    .filter(permission => permission.type.toLowerCase() === 'menu')
    .map(permission => [permission.resource, permission.name]))
  for (const permission of permissionCatalog.value) {
    const prefix = permission.resource || permission.code.split(':')[0] || permission.code
    if (!groups[prefix]) groups[prefix] = []
    groups[prefix].push({ code: permission.code, name: permission.name || permission.code })
  }
  return Object.entries(groups).map(([prefix, items]) => ({
    code: prefix,
    name: CATEGORY_LABELS[prefix] || menuNames.get(prefix) || prefix,
    items,
  }))
})

// ---------------------------------------------------------------------------
// Admin tab helpers
// ---------------------------------------------------------------------------
const adminStatsCards = computed(() => [
  { label: '管理员总数', value: adminStats.value.totalAdmins, icon: Users, tone: 'primary' },
  { label: '活跃管理员', value: adminStats.value.activeAdmins, icon: ShieldCheck, tone: 'success' },
  { label: '已禁用', value: adminStats.value.disabledAdmins, icon: ShieldAlert, tone: 'danger' },
  { label: 'MFA 已启用', value: adminStats.value.mfaEnabledAdmins, icon: KeyRound, tone: 'info' },
  { label: '用户池所有者', value: adminStats.value.ownerCount || 0, icon: Shield, tone: 'warning' },
])

const adminColumns = [
  { title: '用户名', key: 'username', width: 190 },
  { title: '邮箱', key: 'email', width: 210 },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '当前用户池角色', key: 'tenantRole', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: 'MFA', key: 'mfa', width: 100 },
  { title: '最后登录', key: 'lastLoginAt', width: 180 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 310, fixed: 'end' as const },
]

function isSelf(row: AdminUser): boolean {
  return !!currentAdminId.value && row.id === currentAdminId.value
}

/** The account owns at least one tenant (anywhere). Guards account-level actions. */
function isAccountOwner(row: AdminUser): boolean {
  return row.userPools.some((m) => m.tenantRole === 'owner')
}

/** Membership of this account within the currently selected tenant, if any. */
function currentMembership(row: AdminUser): AdminMembership | undefined {
  const tid = userStore.currentUserPool?.id
  if (!tid) return undefined
  return row.userPools.find((m) => m.userPoolId === tid)
}

/** Role of the current-tenant membership, or null when the account is not a member. */
function currentUserPoolRole(row: AdminUser): string | null {
  return currentMembership(row)?.tenantRole ?? null
}

/** The current membership's role is the immutable 'owner' role. */
function isCurrentMembershipOwner(row: AdminUser): boolean {
  return currentMembership(row)?.tenantRole === 'owner'
}

function adminStatusVariant(status: AdminStatus): string {
  return status === 'active' ? 'green' : 'red'
}

function adminStatusText(status: AdminStatus): string {
  return status === 'active' ? '正常' : '禁用'
}

function tenantRoleText(role: string | null): string {
  if (!role) return '-'
  const map: Record<string, string> = {
    owner: '用户池所有者',
    admin: '管理员',
  }
  return map[role] || role
}

function tenantRoleVariant(role: string | null): string {
  if (role === 'owner') return 'purple'
  return 'blue'
}

function formatDate(date: string | null): string {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

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

// edit admin
const openEditDialog = (row: AdminUser) => {
  editTarget.value = row
  editForm.username = row.username
  editForm.email = row.email
  editForm.phone = row.phone || ''
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editTarget.value) return
  editSubmitting.value = true
  try {
    await adminUserApi.update(editTarget.value.id, {
      username: editForm.username,
      email: editForm.email,
      phone: editForm.phone || undefined,
    })
    message.success('管理员信息更新成功')
    editDialogVisible.value = false
    loadAdmins()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '更新管理员失败'))
  } finally {
    editSubmitting.value = false
  }
}

// status change
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

// reset password
const openResetPwdDialog = (row: AdminUser) => {
  resetPwdTarget.value = row
  resetPwdForm.newPassword = ''
  resetPwdForm.confirmPassword = ''
  resetPwdDialogVisible.value = true
}

const handleResetPwdSubmit = async () => {
  if (!resetPwdTarget.value) return
  if (!resetPwdForm.newPassword) {
    message.warning('请输入新密码')
    return
  }
  if (resetPwdForm.newPassword !== resetPwdForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  resetPwdSubmitting.value = true
  try {
    await adminUserApi.resetPassword(resetPwdTarget.value.id, { newPassword: resetPwdForm.newPassword })
    message.success('密码重置成功')
    resetPwdDialogVisible.value = false
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '重置密码失败'))
  } finally {
    resetPwdSubmitting.value = false
  }
}

// reset MFA
const openResetMfaDialog = (row: AdminUser) => {
  resetMfaTarget.value = row
  resetMfaDialogVisible.value = true
}

const handleResetMfaConfirm = async () => {
  if (!resetMfaTarget.value) return
  resetMfaSubmitting.value = true
  try {
    await adminUserApi.resetMfa(resetMfaTarget.value.id)
    message.success('MFA 已重置')
    resetMfaDialogVisible.value = false
    loadAdmins()
    loadAdminStats()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '重置 MFA 失败'))
  } finally {
    resetMfaSubmitting.value = false
  }
}

// assign roles
const openAssignRolesDialog = (row: AdminUser) => {
  const membership = currentMembership(row)
  if (!membership) return
  assignRolesTarget.value = row
  assignRolesSelected.value = membership.roles.map((r) => r.id)
  assignRolesDialogVisible.value = true
}

const toggleRoleSelection = (roleId: string) => {
  const idx = assignRolesSelected.value.indexOf(roleId)
  if (idx > -1) {
    assignRolesSelected.value.splice(idx, 1)
  } else {
    assignRolesSelected.value.push(roleId)
  }
}

const handleAssignRolesSubmit = async () => {
  const target = assignRolesTarget.value
  const membership = target ? currentMembership(target) : undefined
  if (!target || !membership) return
  assignRolesSubmitting.value = true
  try {
    await adminUserApi.assignRoles(target.id, {
      userPoolId: membership.userPoolId,
      roleIds: assignRolesSelected.value,
    })
    message.success('角色分配成功')
    assignRolesDialogVisible.value = false
    loadAdmins()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '分配角色失败'))
  } finally {
    assignRolesSubmitting.value = false
  }
}

// remove from tenant
const openRemoveDialog = (row: AdminUser) => {
  removeTarget.value = row
  removeDialogVisible.value = true
}

const handleRemoveConfirm = async () => {
  const target = removeTarget.value
  const membership = target ? currentMembership(target) : undefined
  if (!target || !membership) return
  removeSubmitting.value = true
  try {
    await adminUserApi.removeFromUserPool(target.id, membership.userPoolId)
    message.success('已将管理员移出用户池')
    removeDialogVisible.value = false
    loadAdmins()
    loadAdminStats()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '移出用户池失败'))
  } finally {
    removeSubmitting.value = false
  }
}

// ---------------------------------------------------------------------------
// Admin role tab helpers
// ---------------------------------------------------------------------------
const roleStatsCards = computed(() => [
  { label: '角色总数', value: roleStats.value.totalRoles, icon: Settings2, tone: 'primary' },
  { label: '系统角色', value: roleStats.value.systemRoles, icon: Lock, tone: 'danger' },
  { label: '自定义角色', value: roleStats.value.customRoles, icon: UserCog, tone: 'success' },
])

const roleColumns = [
  { title: '角色名称', key: 'name', width: 220 },
  { title: '角色编码', dataIndex: 'code', key: 'code', width: 200 },
  { title: '描述', key: 'description', minWidth: 220 },
  { title: '权限', key: 'permissions', width: 140 },
  { title: '管理员数', key: 'adminCount', width: 110, align: 'center' as const },
  { title: '类型', key: 'type', width: 110 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 230, fixed: 'end' as const },
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
  roleDialogIsEdit.value = false
  roleForm.id = undefined
  roleForm.name = ''
  roleForm.code = ''
  roleForm.description = ''
  roleDialogVisible.value = true
}

const openEditRoleDialog = (row: AdminRole) => {
  roleDialogIsEdit.value = true
  roleForm.id = row.id
  roleForm.name = row.name
  roleForm.code = row.code
  roleForm.description = row.description || ''
  roleDialogVisible.value = true
}

const toggleRolePermission = (code: string) => {
  const idx = rolePermissionCodes.value.indexOf(code)
  if (idx > -1) {
    rolePermissionCodes.value.splice(idx, 1)
  } else {
    rolePermissionCodes.value.push(code)
  }
}

const openRolePermissionDialog = (row: AdminRole) => {
  rolePermissionTarget.value = row
  rolePermissionCodes.value = [...row.permissions]
  rolePermissionDialogVisible.value = true
}

const handleRolePermissionSubmit = async () => {
  if (!rolePermissionTarget.value) return
  rolePermissionSubmitting.value = true
  try {
    await adminRoleApi.update(rolePermissionTarget.value.id, {
      permissions: rolePermissionCodes.value,
    })
    message.success('角色权限配置成功')
    rolePermissionDialogVisible.value = false
    await loadRoles()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '角色权限配置失败'))
  } finally {
    rolePermissionSubmitting.value = false
  }
}

const handleRoleSubmit = async () => {
  if (!roleForm.name.trim()) {
    message.warning('请填写角色名称')
    return
  }
  if (!/^[a-z][a-z0-9_]{1,99}$/.test(roleForm.code.trim())) {
    message.warning('请填写有效的角色编码')
    return
  }
  roleDialogSubmitting.value = true
  try {
    if (roleDialogIsEdit.value && roleForm.id) {
      const payload: UpdateAdminRoleDto = {
        name: roleForm.name,
        description: roleForm.description || undefined,
      }
      await adminRoleApi.update(roleForm.id, payload)
      message.success('角色更新成功')
    } else {
      const payload: CreateAdminRoleDto = {
        name: roleForm.name,
        code: roleForm.code.trim(),
        description: roleForm.description || undefined,
        permissions: [],
      }
      await adminRoleApi.create(payload)
      message.success('角色创建成功，请在角色列表中配置权限')
    }
    roleDialogVisible.value = false
    loadRoles()
    loadRoleStats()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '操作失败'))
  } finally {
    roleDialogSubmitting.value = false
  }
}

const openDeleteRoleDialog = (row: AdminRole) => {
  deleteRoleTarget.value = row
  deleteRoleDialogVisible.value = true
}

const handleDeleteRoleConfirm = async () => {
  if (!deleteRoleTarget.value) return
  deleteRoleSubmitting.value = true
  try {
    await adminRoleApi.delete(deleteRoleTarget.value.id)
    message.success('角色删除成功')
    deleteRoleDialogVisible.value = false
    loadRoles()
    loadRoleStats()
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '删除角色失败'))
  } finally {
    deleteRoleSubmitting.value = false
  }
}

const roleTypeVariant = (isSystem: boolean): string => (isSystem ? 'purple' : 'blue')

const permissionsSummary = (permissions: string[]): string => {
  if (permissions.includes('*')) return '全部权限'
  if (permissions.length === 0) return '无权限'
  return `${permissions.length} 项权限`
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

    <!-- ===================== Edit admin dialog ===================== -->
    <a-modal v-model:open="editDialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>编辑管理员</h3>
        </div>
        <a-form :model="editForm" layout="vertical" class="py-4" @finish="handleEditSubmit">
          <div class="grid gap-4">
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="editForm.username" placeholder="请输入用户名" />
            </a-form-item>
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="editForm.email" placeholder="请输入邮箱" />
            </a-form-item>
            <a-form-item label="手机号" name="phone">
              <a-input v-model:value="editForm.phone" placeholder="请输入手机号" />
            </a-form-item>
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <a-button html-type="button" @click="editDialogVisible = false">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="editSubmitting" :disabled="editSubmitting">
              {{ editSubmitting ? '提交中...' : '确定' }}
            </a-button>
          </div>
        </a-form>
      </div>
    </a-modal>

    <!-- ===================== Reset password dialog ===================== -->
    <a-modal v-model:open="resetPwdDialogVisible" :footer="null">
      <div class="max-w-md">
        <div>
          <h3>重置管理员密码</h3>
        </div>
        <a-form :model="resetPwdForm" :rules="resetPwdFormRules" layout="vertical" class="py-4" @finish="handleResetPwdSubmit">
          <div class="grid gap-4">
            <a-form-item label="新密码" name="newPassword">
              <a-input v-model:value="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" />
            </a-form-item>
            <a-form-item label="确认密码" name="confirmPassword">
              <a-input v-model:value="resetPwdForm.confirmPassword" type="password" placeholder="请确认新密码" />
            </a-form-item>
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <a-button html-type="button" @click="resetPwdDialogVisible = false">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="resetPwdSubmitting" :disabled="resetPwdSubmitting">
              {{ resetPwdSubmitting ? '提交中...' : '确定' }}
            </a-button>
          </div>
        </a-form>
      </div>
    </a-modal>

    <!-- ===================== Reset MFA confirmation ===================== -->
    <a-modal v-model:open="resetMfaDialogVisible" :footer="null">
      <div>
        <div>
          <h3>重置管理员 MFA</h3>
          <p>
            确定要重置管理员 <span class="font-medium">{{ resetMfaTarget?.username }}</span> 的多因素认证配置吗？重置后该管理员需要重新配置 MFA。
          </p>
        </div>
        <div>
          <a-button :disabled="resetMfaSubmitting">取消</a-button>
          <a-button type="primary" :loading="resetMfaSubmitting" :disabled="resetMfaSubmitting" @click="handleResetMfaConfirm">
            {{ resetMfaSubmitting ? '处理中...' : '确认重置' }}
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- ===================== Assign roles dialog ===================== -->
    <a-modal v-model:open="assignRolesDialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>分配管理员角色</h3>
        </div>
        <div class="py-4">
          <div class="grid gap-2 mb-4">
            <label>当前角色</label>
            <div v-if="assignRolesMembership && assignRolesMembership.roles.length > 0" class="flex flex-wrap gap-2">
              <a-tag
                v-for="role in assignRolesMembership.roles"
                :key="role.id"
                color="blue"
                class="text-xs"
              >
                {{ role.name }}
              </a-tag>
            </div>
            <div v-else class="text-[#52677D] text-sm">暂未分配角色</div>
          </div>
          <a-divider class="my-2" />
          <div class="grid gap-2">
            <label>选择角色</label>
            <div class="border rounded-lg p-3 max-h-72 overflow-y-auto">
              <div v-if="adminRolesForFilter.length === 0" class="text-[#52677D] text-sm text-center py-4">
                暂无可分配角色
              </div>
              <div
                v-for="role in adminRolesForFilter"
                :key="role.id"
                class="flex items-center gap-2 py-1.5"
              >
                <a-checkbox
                  :checked="assignRolesSelected.includes(role.id)"
                  @update:checked="toggleRoleSelection(role.id)"
                />
                <a-tag :color="roleTypeVariant(role.isSystem)" class="text-xs">
                  {{ role.isSystem ? '系统' : '自定义' }}
                </a-tag>
                <span class="font-medium text-sm">{{ role.name }}</span>
                <span class="text-xs text-[#52677D]">{{ permissionsSummary(role.permissions) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div>
          <a-button  @click="assignRolesDialogVisible = false">取消</a-button>
          <a-button type="primary" :loading="assignRolesSubmitting" :disabled="assignRolesSubmitting" @click="handleAssignRolesSubmit">
            {{ assignRolesSubmitting ? '提交中...' : '确定' }}
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- ===================== Remove from tenant confirmation ===================== -->
    <a-modal v-model:open="removeDialogVisible" :footer="null">
      <div>
        <div>
          <h3>移出用户池</h3>
          <p>
            确定要将管理员 <span class="font-medium">{{ removeTarget?.username }}</span> 移出当前用户池吗？此操作只解除该管理员与当前用户池的关联，不会删除管理员账号。
          </p>
        </div>
        <div>
          <a-button :disabled="removeSubmitting">取消</a-button>
          <a-button
            type="primary"
            danger
            :loading="removeSubmitting"
            :disabled="removeSubmitting"
            @click="handleRemoveConfirm"
          >
            {{ removeSubmitting ? '处理中...' : '确认移除' }}
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- ===================== Create / edit role dialog ===================== -->
    <a-modal v-model:open="roleDialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>{{ roleDialogTitle }}</h3>
        </div>
        <a-form :model="roleForm" :rules="roleFormRules" layout="vertical" class="py-4" @finish="handleRoleSubmit">
          <div class="grid gap-4">
            <a-form-item label="角色名称" name="name">
              <a-input v-model:value="roleForm.name" placeholder="请输入角色名称" />
            </a-form-item>
            <a-form-item label="角色编码" name="code" extra="创建后不可修改">
              <a-input
                v-model:value="roleForm.code"
                placeholder="例如：operations_admin"
                :maxlength="100"
                :disabled="roleDialogIsEdit"
              />
            </a-form-item>
            <a-form-item label="描述" name="description">
              <a-textarea v-model:value="roleForm.description" :rows="2" placeholder="请输入角色描述" />
            </a-form-item>
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <a-button html-type="button" @click="roleDialogVisible = false">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="roleDialogSubmitting" :disabled="roleDialogSubmitting">
              {{ roleDialogSubmitting ? '提交中...' : '确定' }}
            </a-button>
          </div>
        </a-form>
      </div>
    </a-modal>

    <!-- ===================== Configure role permissions dialog ===================== -->
    <a-modal
      v-model:open="rolePermissionDialogVisible"
      title="配置管理员角色权限"
      :footer="null"
      width="640"
    >
      <div class="max-w-xl">
        <p class="mb-4 text-sm text-[#52677D]">
          为角色“{{ rolePermissionTarget?.name }}”配置可使用的管理端权限。
        </p>
        <div class="border rounded-lg p-3 max-h-96 overflow-y-auto">
          <div
            v-for="category in permissionCategories"
            :key="category.code"
            class="mb-4 last:mb-0"
          >
            <div class="text-sm font-medium mb-2">{{ category.name }}</div>
            <div class="flex flex-col gap-2 pl-1">
              <label
                v-for="item in category.items"
                :key="item.code"
                class="flex items-center gap-2 cursor-pointer"
              >
                <a-checkbox
                  :checked="rolePermissionCodes.includes(item.code)"
                  @update:checked="toggleRolePermission(item.code)"
                />
                <span class="text-sm">{{ item.name }}</span>
                <code class="text-xs text-[#52677D] font-mono">{{ item.code }}</code>
              </label>
            </div>
          </div>
        </div>
        <div class="flex justify-end gap-2 pt-4">
          <a-button html-type="button" @click="rolePermissionDialogVisible = false">取消</a-button>
          <a-button
            type="primary"
            :loading="rolePermissionSubmitting"
            :disabled="rolePermissionSubmitting"
            @click="handleRolePermissionSubmit"
          >
            {{ rolePermissionSubmitting ? '保存中...' : '保存权限' }}
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- ===================== Delete role confirmation ===================== -->
    <a-modal v-model:open="deleteRoleDialogVisible" :footer="null">
      <div>
        <div>
          <h3>删除管理员角色</h3>
          <p>
            确定要删除角色 <span class="font-medium">{{ deleteRoleTarget?.name }}</span> 吗？已分配该角色的管理员将失去对应权限。此操作不可恢复。
          </p>
        </div>
        <div>
          <a-button :disabled="deleteRoleSubmitting">取消</a-button>
          <a-button
            type="primary"
            danger
            :loading="deleteRoleSubmitting"
            :disabled="deleteRoleSubmitting"
            @click="handleDeleteRoleConfirm"
          >
            {{ deleteRoleSubmitting ? '处理中...' : '确认删除' }}
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>
