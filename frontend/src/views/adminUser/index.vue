<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { toast } from 'vue-sonner'
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
} from '@/types/adminRole'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Checkbox } from '@/components/ui/checkbox'
import { Textarea } from '@/components/ui/textarea'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Separator } from '@/components/ui/separator'

type ApiErrorResponse = {
  readonly message?: string
}

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) return fallback
  return error.response?.data?.message ?? fallback
}

const userStore = useUserStore()
const currentAdminId = computed<string>(() => userStore.userInfo?.id || '')

const activeTab = ref<'admins' | 'roles'>('admins')

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

// edit admin dialog
const editDialogVisible = ref(false)
const editTarget = ref<AdminUser | null>(null)
const editForm = reactive<UpdateAdminDto>({ username: '', email: '', phone: '' })
const editSubmitting = ref(false)

// reset password dialog
const resetPwdDialogVisible = ref(false)
const resetPwdTarget = ref<AdminUser | null>(null)
const resetPwdForm = reactive({ newPassword: '', confirmPassword: '' })
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

const permissionCatalog = ref<string[]>([])

// create/edit role dialog
const roleDialogVisible = ref(false)
const roleDialogIsEdit = ref(false)
const roleDialogSubmitting = ref(false)
const roleForm = reactive<CreateAdminRoleDto & { id?: string }>({
  name: '',
  description: '',
  permissions: [],
})
const roleDialogTitle = computed(() => (roleDialogIsEdit.value ? '编辑管理员角色' : '创建管理员角色'))

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

const PERMISSION_LABELS: Record<string, string> = {
  'admin-user:read': '查看管理员',
  'admin-user:update': '编辑管理员',
  'admin-user:status': '启用/禁用管理员',
  'admin-user:reset-password': '重置管理员密码',
  'admin-user:reset-mfa': '重置管理员 MFA',
  'admin-user:assign-role': '分配管理员角色',
  'admin-user:remove-tenant': '移除管理员出租户',
  'admin-role:read': '查看管理员角色',
  'admin-role:create': '创建管理员角色',
  'admin-role:update': '编辑管理员角色',
  'admin-role:delete': '删除管理员角色',
  'admin-role:assign': '分配管理员角色给管理员',
}

interface PermissionCategory {
  code: string
  name: string
  items: { code: string; name: string }[]
}

const permissionCategories = computed<PermissionCategory[]>(() => {
  const groups: Record<string, { code: string; name: string }[]> = {}
  for (const code of permissionCatalog.value) {
    const prefix = code.split(':')[0] ?? code
    if (!groups[prefix]) groups[prefix] = []
    groups[prefix].push({ code, name: PERMISSION_LABELS[code] || code })
  }
  return Object.entries(groups).map(([prefix, items]) => ({
    code: prefix,
    name: CATEGORY_LABELS[prefix] || prefix,
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
  { label: '租户所有者', value: adminStats.value.ownerCount || 0, icon: Shield, tone: 'warning' },
])

const adminTotalPages = computed(() => Math.max(1, Math.ceil(adminTotal.value / adminQuery.pageSize!)))

function isSelf(row: AdminUser): boolean {
  return !!currentAdminId.value && row.id === currentAdminId.value
}

function isOwner(row: AdminUser): boolean {
  return row.tenantRole === 'owner'
}

function adminStatusVariant(status: AdminStatus): 'default' | 'secondary' {
  return status === 'active' ? 'default' : 'secondary'
}

function adminStatusText(status: AdminStatus): string {
  return status === 'active' ? '正常' : '禁用'
}

function tenantRoleText(role: string): string {
  const map: Record<string, string> = {
    owner: '租户所有者',
    admin: '管理员',
  }
  return map[role] || role
}

function tenantRoleVariant(role: string): 'default' | 'secondary' | 'destructive' {
  if (role === 'owner') return 'destructive'
  return 'secondary'
}

function formatDate(date: string | null): string {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadAdminStats = async () => {
  try {
    const res = await adminUserApi.getStats()
    adminStats.value = res.data
  } catch (error) {
    console.error('加载管理员统计失败:', error)
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
    admins.value = res.data.admins
    adminTotal.value = res.data.total
  } catch (error) {
    console.error('加载管理员列表失败:', error)
  } finally {
    adminLoading.value = false
  }
}

const loadAdminRolesForFilter = async () => {
  try {
    const res = await adminRoleApi.getList({ pageSize: 100 })
    adminRolesForFilter.value = res.data.roles
  } catch (error) {
    console.error('加载管理员角色失败:', error)
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

const handleAdminPageChange = (newPage: number) => {
  adminQuery.page = newPage
  loadAdmins()
}

const handleAdminPageSizeChange = (size: number) => {
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
    toast.success('管理员信息更新成功')
    editDialogVisible.value = false
    loadAdmins()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '更新管理员失败'))
  } finally {
    editSubmitting.value = false
  }
}

// status change
const handleStatusToggle = async (row: AdminUser) => {
  const next: AdminStatus = row.status === 'active' ? 'disabled' : 'active'
  try {
    await adminUserApi.updateStatus(row.id, { status: next })
    toast.success(next === 'active' ? '已启用管理员' : '已禁用管理员')
    loadAdmins()
    loadAdminStats()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '状态更新失败'))
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
    toast.warning('请输入新密码')
    return
  }
  if (resetPwdForm.newPassword !== resetPwdForm.confirmPassword) {
    toast.error('两次输入的密码不一致')
    return
  }
  resetPwdSubmitting.value = true
  try {
    await adminUserApi.resetPassword(resetPwdTarget.value.id, { newPassword: resetPwdForm.newPassword })
    toast.success('密码重置成功')
    resetPwdDialogVisible.value = false
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '重置密码失败'))
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
    toast.success('MFA 已重置')
    resetMfaDialogVisible.value = false
    loadAdmins()
    loadAdminStats()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '重置 MFA 失败'))
  } finally {
    resetMfaSubmitting.value = false
  }
}

// assign roles
const openAssignRolesDialog = (row: AdminUser) => {
  assignRolesTarget.value = row
  assignRolesSelected.value = row.roles.map((r) => r.id)
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
  if (!assignRolesTarget.value) return
  assignRolesSubmitting.value = true
  try {
    await adminUserApi.assignRoles(assignRolesTarget.value.id, { roleIds: assignRolesSelected.value })
    toast.success('角色分配成功')
    assignRolesDialogVisible.value = false
    loadAdmins()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '分配角色失败'))
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
  if (!removeTarget.value) return
  removeSubmitting.value = true
  try {
    await adminUserApi.removeFromTenant(removeTarget.value.id)
    toast.success('已将管理员移出租户')
    removeDialogVisible.value = false
    loadAdmins()
    loadAdminStats()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '移出租户失败'))
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

const roleTotalPages = computed(() => Math.max(1, Math.ceil(roleTotal.value / rolePageSize.value)))

const loadRoleStats = async () => {
  try {
    const res = await adminRoleApi.getStats()
    roleStats.value = res.data
  } catch (error) {
    console.error('加载管理员角色统计失败:', error)
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
    roles.value = res.data.roles
    roleTotal.value = res.data.total
  } catch (error) {
    console.error('加载管理员角色列表失败:', error)
  } finally {
    roleLoading.value = false
  }
}

const loadPermissionCatalog = async () => {
  try {
    const res = await adminRoleApi.getPermissionCatalog()
    permissionCatalog.value = res.data.permissions
  } catch (error) {
    console.error('加载权限目录失败:', error)
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

const handleRolePageChange = (newPage: number) => {
  rolePage.value = newPage
  loadRoles()
}

const openCreateRoleDialog = () => {
  roleDialogIsEdit.value = false
  roleForm.id = undefined
  roleForm.name = ''
  roleForm.description = ''
  roleForm.permissions = []
  roleDialogVisible.value = true
}

const openEditRoleDialog = (row: AdminRole) => {
  roleDialogIsEdit.value = true
  roleForm.id = row.id
  roleForm.name = row.name
  roleForm.description = row.description || ''
  roleForm.permissions = [...row.permissions]
  roleDialogVisible.value = true
}

const toggleRolePermission = (code: string) => {
  const idx = roleForm.permissions.indexOf(code)
  if (idx > -1) {
    roleForm.permissions.splice(idx, 1)
  } else {
    roleForm.permissions.push(code)
  }
}

const handleRoleSubmit = async () => {
  if (!roleForm.name.trim()) {
    toast.warning('请填写角色名称')
    return
  }
  roleDialogSubmitting.value = true
  try {
    if (roleDialogIsEdit.value && roleForm.id) {
      const payload: UpdateAdminRoleDto = {
        name: roleForm.name,
        description: roleForm.description || undefined,
        permissions: roleForm.permissions,
      }
      await adminRoleApi.update(roleForm.id, payload)
      toast.success('角色更新成功')
    } else {
      const payload: CreateAdminRoleDto = {
        name: roleForm.name,
        description: roleForm.description || undefined,
        permissions: roleForm.permissions,
      }
      await adminRoleApi.create(payload)
      toast.success('角色创建成功')
    }
    roleDialogVisible.value = false
    loadRoles()
    loadRoleStats()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '操作失败'))
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
    toast.success('角色删除成功')
    deleteRoleDialogVisible.value = false
    loadRoles()
    loadRoleStats()
  } catch (error: unknown) {
    toast.error(getApiErrorMessage(error, '删除角色失败'))
  } finally {
    deleteRoleSubmitting.value = false
  }
}

const roleTypeVariant = (isSystem: boolean): 'destructive' | 'default' => (isSystem ? 'destructive' : 'default')

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
      <h1 class="text-2xl font-bold text-foreground mb-2">管理员管理</h1>
      <p class="text-sm text-muted-foreground">管理当前租户的管理员账号与管理员角色</p>
    </div>

    <Tabs v-model="activeTab">
      <TabsList class="mb-4">
        <TabsTrigger value="admins">管理员</TabsTrigger>
        <TabsTrigger value="roles">管理员角色</TabsTrigger>
      </TabsList>

      <!-- ===================== 管理员 Tab ===================== -->
      <TabsContent value="admins">
        <!-- stats -->
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 mb-6">
          <Card v-for="(stat, index) in adminStatsCards" :key="index" class="transition-all hover:shadow-md">
            <CardContent class="pt-4">
              <div class="flex items-center gap-3">
                <div
                  class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
                  :class="{
                    'bg-gradient-to-br from-primary to-primary/60': stat.tone === 'primary',
                    'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.tone === 'success',
                    'bg-gradient-to-br from-red-500 to-red-400': stat.tone === 'danger',
                    'bg-gradient-to-br from-sky-500 to-sky-400': stat.tone === 'info',
                    'bg-gradient-to-br from-amber-500 to-amber-400': stat.tone === 'warning',
                  }"
                >
                  <component :is="stat.icon" class="size-5" />
                </div>
                <div>
                  <div class="text-2xl font-bold text-foreground leading-tight">{{ stat.value }}</div>
                  <div class="text-xs text-muted-foreground mt-0.5">{{ stat.label }}</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <!-- search -->
        <Card class="mb-4">
          <CardContent class="pt-6">
            <div class="flex flex-wrap gap-4 items-end">
              <div class="grid gap-2">
                <Label>用户名</Label>
                <Input v-model="adminQuery.username" placeholder="请输入用户名" class="w-40" />
              </div>
              <div class="grid gap-2">
                <Label>邮箱</Label>
                <Input v-model="adminQuery.email" placeholder="请输入邮箱" class="w-40" />
              </div>
              <div class="grid gap-2">
                <Label>手机号</Label>
                <Input v-model="adminQuery.phone" placeholder="请输入手机号" class="w-40" />
              </div>
              <div class="grid gap-2">
                <Label>状态</Label>
                <Select v-model="adminQuery.status">
                  <SelectTrigger class="w-32">
                    <SelectValue placeholder="全部状态" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="active">正常</SelectItem>
                    <SelectItem value="disabled">禁用</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div class="grid gap-2">
                <Label>管理员角色</Label>
                <Select v-model="adminQuery.roleId">
                  <SelectTrigger class="w-44">
                    <SelectValue placeholder="全部角色" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="role in adminRolesForFilter" :key="role.id" :value="role.id">
                      {{ role.name }}
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div class="flex gap-2">
                <Button @click="handleAdminSearch">
                  <Search class="size-4 mr-1" />
                  搜索
                </Button>
                <Button variant="outline" @click="handleAdminReset">
                  <RefreshCw class="size-4 mr-1" />
                  重置
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- table -->
        <Card>
          <CardContent class="pt-6">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead class="w-44">用户名</TableHead>
                  <TableHead class="w-48">邮箱</TableHead>
                  <TableHead class="w-32">手机号</TableHead>
                  <TableHead class="w-28">租户角色</TableHead>
                  <TableHead class="w-20">状态</TableHead>
                  <TableHead class="w-20">MFA</TableHead>
                  <TableHead class="w-40">最后登录</TableHead>
                  <TableHead class="w-40">创建时间</TableHead>
                  <TableHead class="w-72">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-if="adminLoading">
                  <TableCell colspan="9" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
                </TableRow>
                <TableRow v-else-if="admins.length === 0">
                  <TableCell colspan="9" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
                </TableRow>
                <TableRow v-for="row in admins" :key="row.id">
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <Avatar class="size-8 bg-gradient-to-br from-primary to-primary/60">
                        <AvatarFallback class="bg-transparent text-white text-xs font-semibold">
                          {{ row.username.charAt(0).toUpperCase() }}
                        </AvatarFallback>
                      </Avatar>
                      <div class="flex flex-col">
                        <span class="font-medium">{{ row.username }}</span>
                        <span v-if="isSelf(row)" class="text-xs text-primary">（我）</span>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>{{ row.email }}</TableCell>
                  <TableCell>{{ row.phone || '-' }}</TableCell>
                  <TableCell>
                    <Badge :variant="tenantRoleVariant(row.tenantRole)" class="text-xs">
                      {{ tenantRoleText(row.tenantRole) }}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge :variant="adminStatusVariant(row.status)" class="text-xs">
                      {{ adminStatusText(row.status) }}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge :variant="row.mfaEnabled ? 'default' : 'outline'" class="text-xs">
                      {{ row.mfaEnabled ? '已启用' : '未启用' }}
                    </Badge>
                  </TableCell>
                  <TableCell class="text-muted-foreground">{{ formatDate(row.lastLoginAt) }}</TableCell>
                  <TableCell class="text-muted-foreground">{{ formatDate(row.createdAt) }}</TableCell>
                  <TableCell>
                    <div class="flex gap-1 flex-wrap">
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0"
                        :disabled="isSelf(row) || isOwner(row)"
                        @click="openEditDialog(row)"
                      >
                        编辑
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0"
                        :disabled="isSelf(row) || isOwner(row)"
                        @click="handleStatusToggle(row)"
                      >
                        {{ row.status === 'active' ? '禁用' : '启用' }}
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0"
                        :disabled="isSelf(row) || isOwner(row)"
                        @click="openAssignRolesDialog(row)"
                      >
                        分配角色
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0"
                        :disabled="isSelf(row) || isOwner(row)"
                        @click="openResetPwdDialog(row)"
                      >
                        重置密码
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0"
                        :disabled="isSelf(row) || isOwner(row) || !row.mfaEnabled"
                        @click="openResetMfaDialog(row)"
                      >
                        重置 MFA
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0 text-destructive"
                        :disabled="isSelf(row) || isOwner(row)"
                        @click="openRemoveDialog(row)"
                      >
                        移出租户
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>

            <div class="flex items-center justify-between mt-4 pt-4 border-t">
              <span class="text-sm text-muted-foreground">共 {{ adminTotal }} 条</span>
              <div class="flex items-center gap-1">
                <Select :model-value="String(adminQuery.pageSize)" @update:model-value="handleAdminPageSizeChange(Number($event))">
                  <SelectTrigger class="w-20">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="10">10</SelectItem>
                    <SelectItem value="20">20</SelectItem>
                    <SelectItem value="50">50</SelectItem>
                  </SelectContent>
                </Select>
                <span class="text-sm px-2">条/页</span>
                <Button variant="outline" size="sm" :disabled="adminQuery.page! <= 1" @click="handleAdminPageChange(adminQuery.page! - 1)">
                  上一页
                </Button>
                <span class="text-sm px-2">{{ adminQuery.page }} / {{ adminTotalPages }}</span>
                <Button variant="outline" size="sm" :disabled="adminQuery.page! >= adminTotalPages" @click="handleAdminPageChange(adminQuery.page! + 1)">
                  下一页
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>

      <!-- ===================== 管理员角色 Tab ===================== -->
      <TabsContent value="roles">
        <!-- stats -->
        <div class="grid grid-cols-3 gap-4 mb-6">
          <Card v-for="(stat, index) in roleStatsCards" :key="index" class="transition-all hover:shadow-md">
            <CardContent class="pt-4">
              <div class="flex items-center gap-3">
                <div
                  class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
                  :class="{
                    'bg-gradient-to-br from-primary to-primary/60': stat.tone === 'primary',
                    'bg-gradient-to-br from-red-500 to-red-400': stat.tone === 'danger',
                    'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.tone === 'success',
                  }"
                >
                  <component :is="stat.icon" class="size-5" />
                </div>
                <div>
                  <div class="text-2xl font-bold text-foreground leading-tight">{{ stat.value }}</div>
                  <div class="text-xs text-muted-foreground mt-0.5">{{ stat.label }}</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <!-- permission catalog display -->
        <Card class="mb-4">
          <CardContent class="pt-6">
            <div class="flex items-center gap-2 mb-4">
              <Shield class="size-4 text-primary" />
              <h3 class="text-sm font-semibold">权限目录</h3>
              <span class="text-xs text-muted-foreground">（共 {{ permissionCatalog.length }} 项权限）</span>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div
                v-for="category in permissionCategories"
                :key="category.code"
                class="border rounded-lg p-3"
              >
                <div class="text-sm font-medium mb-2">{{ category.name }}</div>
                <div class="flex flex-wrap gap-1.5">
                  <Badge
                    v-for="item in category.items"
                    :key="item.code"
                    variant="outline"
                    class="text-xs"
                  >
                    {{ item.name }}
                  </Badge>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- role list -->
        <Card>
          <CardContent class="pt-6">
            <div class="flex justify-between items-center mb-4">
              <div class="flex items-center gap-2">
                <div class="relative">
                  <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                  <Input
                    v-model="roleSearch"
                    placeholder="搜索角色名称"
                    class="w-64 pl-8"
                    @keyup.enter="handleRoleSearch"
                  />
                </div>
                <Button @click="handleRoleSearch">
                  <Search class="size-4 mr-1" />
                  搜索
                </Button>
                <Button variant="outline" @click="handleRoleReset">
                  <RefreshCw class="size-4 mr-1" />
                  重置
                </Button>
              </div>
              <Button @click="openCreateRoleDialog">
                <Plus class="size-4 mr-1" />
                创建自定义角色
              </Button>
            </div>

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead class="w-48">角色名称</TableHead>
                  <TableHead class="min-w-[200px]">描述</TableHead>
                  <TableHead class="w-32">权限</TableHead>
                  <TableHead class="w-24">管理员数</TableHead>
                  <TableHead class="w-24">类型</TableHead>
                  <TableHead class="w-40">创建时间</TableHead>
                  <TableHead class="w-40">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-if="roleLoading">
                  <TableCell colspan="7" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
                </TableRow>
                <TableRow v-else-if="roles.length === 0">
                  <TableCell colspan="7" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
                </TableRow>
                <TableRow v-for="row in roles" :key="row.id">
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <Badge :variant="roleTypeVariant(row.isSystem)" class="text-xs">
                        {{ row.isSystem ? '系统' : '自定义' }}
                      </Badge>
                      <span class="font-medium">{{ row.name }}</span>
                    </div>
                  </TableCell>
                  <TableCell class="text-muted-foreground">{{ row.description || '-' }}</TableCell>
                  <TableCell>
                    <Badge variant="outline" class="text-xs">{{ permissionsSummary(row.permissions) }}</Badge>
                  </TableCell>
                  <TableCell class="text-center">{{ row.adminCount || 0 }}</TableCell>
                  <TableCell>
                    <Badge :variant="row.isSystem ? 'destructive' : 'default'" class="text-xs">
                      {{ row.isSystem ? '系统' : '自定义' }}
                    </Badge>
                  </TableCell>
                  <TableCell class="text-muted-foreground">{{ formatDate(row.createdAt) }}</TableCell>
                  <TableCell>
                    <div class="flex gap-2">
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0"
                        :disabled="row.isSystem"
                        @click="openEditRoleDialog(row)"
                      >
                        编辑
                      </Button>
                      <Button
                        variant="link"
                        size="sm"
                        class="h-auto p-0 text-destructive"
                        :disabled="row.isSystem"
                        @click="openDeleteRoleDialog(row)"
                      >
                        删除
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>

            <div class="flex items-center justify-between mt-4 pt-4 border-t">
              <span class="text-sm text-muted-foreground">共 {{ roleTotal }} 条</span>
              <div class="flex items-center gap-1">
                <Button variant="outline" size="sm" :disabled="rolePage <= 1" @click="handleRolePageChange(rolePage - 1)">
                  上一页
                </Button>
                <span class="text-sm px-2">{{ rolePage }} / {{ roleTotalPages }}</span>
                <Button variant="outline" size="sm" :disabled="rolePage >= roleTotalPages" @click="handleRolePageChange(rolePage + 1)">
                  下一页
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>

    <!-- ===================== Edit admin dialog ===================== -->
    <Dialog v-model:open="editDialogVisible">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>编辑管理员</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="handleEditSubmit">
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <Label>用户名</Label>
              <Input v-model="editForm.username" placeholder="请输入用户名" />
            </div>
            <div class="grid gap-2">
              <Label>邮箱</Label>
              <Input v-model="editForm.email" placeholder="请输入邮箱" />
            </div>
            <div class="grid gap-2">
              <Label>手机号</Label>
              <Input v-model="editForm.phone" placeholder="请输入手机号" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="editDialogVisible = false">取消</Button>
          <Button :disabled="editSubmitting" @click="handleEditSubmit">
            {{ editSubmitting ? '提交中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- ===================== Reset password dialog ===================== -->
    <Dialog v-model:open="resetPwdDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>重置管理员密码</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="handleResetPwdSubmit">
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <Label>新密码</Label>
              <Input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" />
            </div>
            <div class="grid gap-2">
              <Label>确认密码</Label>
              <Input v-model="resetPwdForm.confirmPassword" type="password" placeholder="请确认新密码" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="resetPwdDialogVisible = false">取消</Button>
          <Button :disabled="resetPwdSubmitting" @click="handleResetPwdSubmit">
            {{ resetPwdSubmitting ? '提交中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- ===================== Reset MFA confirmation ===================== -->
    <AlertDialog v-model:open="resetMfaDialogVisible">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>重置管理员 MFA</AlertDialogTitle>
          <AlertDialogDescription>
            确定要重置管理员 <span class="font-medium">{{ resetMfaTarget?.username }}</span> 的多因素认证配置吗？重置后该管理员需要重新配置 MFA。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="resetMfaSubmitting">取消</AlertDialogCancel>
          <AlertDialogAction :disabled="resetMfaSubmitting" @click="handleResetMfaConfirm">
            {{ resetMfaSubmitting ? '处理中...' : '确认重置' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- ===================== Assign roles dialog ===================== -->
    <Dialog v-model:open="assignRolesDialogVisible">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>分配管理员角色</DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <div class="grid gap-2 mb-4">
            <Label>当前角色</Label>
            <div v-if="assignRolesTarget && assignRolesTarget.roles.length > 0" class="flex flex-wrap gap-2">
              <Badge
                v-for="role in assignRolesTarget.roles"
                :key="role.id"
                variant="secondary"
                class="text-xs"
              >
                {{ role.name }}
              </Badge>
            </div>
            <div v-else class="text-muted-foreground text-sm">暂未分配角色</div>
          </div>
          <Separator class="my-2" />
          <div class="grid gap-2">
            <Label>选择角色</Label>
            <div class="border rounded-lg p-3 max-h-72 overflow-y-auto">
              <div v-if="adminRolesForFilter.length === 0" class="text-muted-foreground text-sm text-center py-4">
                暂无可分配角色
              </div>
              <div
                v-for="role in adminRolesForFilter"
                :key="role.id"
                class="flex items-center gap-2 py-1.5"
              >
                <Checkbox
                  :checked="assignRolesSelected.includes(role.id)"
                  @update:checked="toggleRoleSelection(role.id)"
                />
                <Badge :variant="roleTypeVariant(role.isSystem)" class="text-xs">
                  {{ role.isSystem ? '系统' : '自定义' }}
                </Badge>
                <span class="font-medium text-sm">{{ role.name }}</span>
                <span class="text-xs text-muted-foreground">{{ permissionsSummary(role.permissions) }}</span>
              </div>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="assignRolesDialogVisible = false">取消</Button>
          <Button :disabled="assignRolesSubmitting" @click="handleAssignRolesSubmit">
            {{ assignRolesSubmitting ? '提交中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- ===================== Remove from tenant confirmation ===================== -->
    <AlertDialog v-model:open="removeDialogVisible">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>移出租户</AlertDialogTitle>
          <AlertDialogDescription>
            确定要将管理员 <span class="font-medium">{{ removeTarget?.username }}</span> 移出当前租户吗？此操作只解除该管理员与当前租户的关联，不会删除管理员账号。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="removeSubmitting">取消</AlertDialogCancel>
          <AlertDialogAction
            :disabled="removeSubmitting"
            class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            @click="handleRemoveConfirm"
          >
            {{ removeSubmitting ? '处理中...' : '确认移除' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- ===================== Create / edit role dialog ===================== -->
    <Dialog v-model:open="roleDialogVisible">
      <DialogContent class="max-w-xl">
        <DialogHeader>
          <DialogTitle>{{ roleDialogTitle }}</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="handleRoleSubmit">
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <Label>角色名称 <span class="text-destructive">*</span></Label>
              <Input v-model="roleForm.name" placeholder="请输入角色名称" />
            </div>
            <div class="grid gap-2">
              <Label>描述</Label>
              <Textarea v-model="roleForm.description" :rows="2" placeholder="请输入角色描述" />
            </div>
            <div class="grid gap-2">
              <Label>权限配置</Label>
              <div class="border rounded-lg p-3 max-h-80 overflow-y-auto">
                <div
                  v-for="category in permissionCategories"
                  :key="category.code"
                  class="mb-3 last:mb-0"
                >
                  <div class="text-sm font-medium mb-2">{{ category.name }}</div>
                  <div class="flex flex-col gap-2 pl-1">
                    <label
                      v-for="item in category.items"
                      :key="item.code"
                      class="flex items-center gap-2 cursor-pointer"
                    >
                      <Checkbox
                        :checked="roleForm.permissions.includes(item.code)"
                        @update:checked="toggleRolePermission(item.code)"
                      />
                      <span class="text-sm">{{ item.name }}</span>
                      <code class="text-xs text-muted-foreground font-mono">{{ item.code }}</code>
                    </label>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="roleDialogVisible = false">取消</Button>
          <Button :disabled="roleDialogSubmitting" @click="handleRoleSubmit">
            {{ roleDialogSubmitting ? '提交中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- ===================== Delete role confirmation ===================== -->
    <AlertDialog v-model:open="deleteRoleDialogVisible">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>删除管理员角色</AlertDialogTitle>
          <AlertDialogDescription>
            确定要删除角色 <span class="font-medium">{{ deleteRoleTarget?.name }}</span> 吗？已分配该角色的管理员将失去对应权限。此操作不可恢复。
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel :disabled="deleteRoleSubmitting">取消</AlertDialogCancel>
          <AlertDialogAction
            :disabled="deleteRoleSubmitting"
            class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            @click="handleDeleteRoleConfirm"
          >
            {{ deleteRoleSubmitting ? '处理中...' : '确认删除' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
