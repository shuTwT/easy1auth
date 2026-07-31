<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { toast } from 'vue-sonner'
import { Building2, Check, Pencil, Package, Plus, RefreshCw, Search, Trash2, UserRoundCog, Users } from '@lucide/vue'
import { adminUserApi } from '@/api/adminUser'
import { tenantApi } from '@/api/tenant'
import { tenantPackageApi } from '@/api/tenantPackage'
import { useUserStore } from '@/stores/user'
import type { AdminUser } from '@/types/adminUser'
import type { Tenant, TenantQueryDto } from '@/types/tenant'
import type { TenantPackage } from '@/types/tenantPackage'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationNext, PaginationPrevious } from '@/components/ui/pagination'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

const userStore = useUserStore()
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
const dialogTitle = computed(() => editingTenant.value ? '编辑租户' : '新增租户')
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / (queryForm.pageSize || 20))))

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
    toast.error('加载租户数据失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingTenant.value = null
  Object.assign(tenantForm, { name: '', packageId: packages.value.find((item) => item.defaultPackage)?.id })
  tenantDialogVisible.value = true
}

function openEditDialog(tenant: Tenant) {
  if (!tenant.tenantPackage) return
  editingTenant.value = tenant
  Object.assign(tenantForm, { name: tenant.name, packageId: tenant.tenantPackage.id })
  tenantDialogVisible.value = true
}

async function submitTenant() {
  if (!tenantForm.name.trim()) {
    toast.error('请输入租户名称')
    return
  }
  if (!tenantForm.packageId) {
    toast.error('请选择租户套餐')
    return
  }
  submitting.value = true
  try {
    if (editingTenant.value) {
      await tenantApi.update(editingTenant.value.id, { name: tenantForm.name.trim(), packageId: tenantForm.packageId })
      toast.success('租户更新成功')
    } else {
      const administratorAccountId = userStore.userInfo?.id
      if (!administratorAccountId) {
        toast.error('未获取到当前管理员信息')
        return
      }
      await tenantApi.create({ name: tenantForm.name.trim(), packageId: tenantForm.packageId, administratorAccountId })
      toast.success('租户创建成功')
    }
    tenantDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('保存租户失败:', error)
    toast.error('保存租户失败')
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(tenant: Tenant) {
  try {
    const status = tenant.status === 'active' ? 'suspended' : 'active'
    await tenantApi.updateStatus(tenant.id, status)
    toast.success(status === 'active' ? '租户已启用' : '租户已停用')
    await loadData()
  } catch (error) {
    console.error('更新租户状态失败:', error)
    toast.error('更新租户状态失败')
  }
}

function openTransferDialog(tenant: Tenant) {
  transferTarget.value = tenant
  transferAdministratorId.value = ''
  transferDialogVisible.value = true
}

async function submitTransfer() {
  if (!transferTarget.value || !transferAdministratorId.value) {
    toast.error('请选择目标管理员')
    return
  }
  transferSubmitting.value = true
  try {
    await tenantApi.transferAdministrator(transferTarget.value.id, transferAdministratorId.value)
    toast.success('租户管理员转移成功')
    transferDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('转移租户管理员失败:', error)
    toast.error('转移租户管理员失败')
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
    toast.success('租户已删除')
    deleteDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('删除租户失败:', error)
    toast.error('删除租户失败')
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

async function handlePageChange(page: number) {
  if (page === queryForm.page || page < 1 || page > totalPages.value) return
  queryForm.page = page
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="flex flex-col gap-6 p-5 lg:p-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div>
        <div class="flex items-center gap-2"><Building2 class="text-primary" /><h1 class="text-2xl font-semibold tracking-tight">租户管理</h1></div>
        <p class="mt-1 text-sm text-muted-foreground">查看当前管理员可访问的全部租户，并管理普通租户的套餐、状态和租户管理员。</p>
      </div>
      <div class="flex gap-2"><Button variant="outline" :disabled="loading" @click="loadData"><RefreshCw data-icon="inline-start" :class="loading ? 'animate-spin' : ''" />刷新</Button><Button @click="openCreateDialog"><Plus data-icon="inline-start" />新增租户</Button></div>
    </div>

    <Card>
      <CardHeader class="gap-4 md:flex-row md:items-end md:justify-between">
        <div><CardTitle>租户列表</CardTitle><CardDescription>共 {{ total }} 个可访问租户</CardDescription></div>
        <div class="flex flex-col gap-2 sm:flex-row">
          <div class="relative"><Search class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" /><Input v-model="queryForm.name" class="w-full pl-9 sm:w-56" placeholder="搜索租户名称" /></div>
          <Select v-model="queryForm.status"><SelectTrigger class="w-full sm:w-32"><SelectValue placeholder="全部状态" /></SelectTrigger><SelectContent><SelectGroup><SelectItem value="active">正常</SelectItem><SelectItem value="suspended">停用</SelectItem><SelectItem value="deleted">已删除</SelectItem></SelectGroup></SelectContent></Select>
          <Button variant="outline" @click="handleSearch">查询</Button>
          <Button variant="outline" @click="handleReset">重置</Button>
        </div>
      </CardHeader>
      <CardContent>
        <div v-if="loading" class="flex min-h-40 items-center justify-center text-sm text-muted-foreground">加载中...</div>
        <div v-else-if="tenants.length === 0" class="flex min-h-40 items-center justify-center text-sm text-muted-foreground">暂无租户</div>
        <Table v-else>
          <TableHeader><TableRow><TableHead>租户</TableHead><TableHead>套餐</TableHead><TableHead>状态</TableHead><TableHead>配额</TableHead><TableHead>租户管理员</TableHead><TableHead class="text-right">操作</TableHead></TableRow></TableHeader>
          <TableBody>
            <TableRow v-for="tenant in tenants" :key="tenant.id" class="transition-colors hover:bg-muted/50">
              <TableCell><div class="flex items-center gap-3"><div class="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary"><Building2 /></div><div><div class="font-medium">{{ tenant.name }}</div><code class="text-xs text-muted-foreground">{{ tenant.id }}</code></div></div></TableCell>
              <TableCell><Badge variant="secondary"><Package data-icon="inline-start" />{{ tenant.tenantPackage?.name || '未配置套餐' }}</Badge></TableCell>
              <TableCell><Badge :variant="tenant.status === 'active' ? 'default' : 'outline'"><Check data-icon="inline-start" />{{ statusLabel(tenant.status) }}</Badge></TableCell>
              <TableCell><div class="flex gap-4 text-sm text-muted-foreground"><span class="inline-flex items-center gap-1"><Users />{{ formatLimit(tenant.tenantPackage?.maxUsers) }} 用户</span><span>{{ formatLimit(tenant.tenantPackage?.maxApps) }} 应用</span></div></TableCell>
              <TableCell><Badge variant="outline">{{ roleLabel(tenant.role) }}</Badge></TableCell>
              <TableCell><div v-if="!tenant.system" class="flex justify-end gap-1"><Button variant="ghost" size="sm" :disabled="tenant.status === 'deleted' || !tenant.tenantPackage" @click="openEditDialog(tenant)"><Pencil data-icon="inline-start" />编辑</Button><Button variant="ghost" size="sm" :disabled="tenant.status === 'deleted'" @click="toggleStatus(tenant)">{{ tenant.status === 'active' ? '停用' : '启用' }}</Button><Button variant="ghost" size="sm" :disabled="tenant.status !== 'active'" @click="openTransferDialog(tenant)"><UserRoundCog data-icon="inline-start" />转移管理员</Button><Button variant="ghost" size="sm" class="text-destructive hover:text-destructive" :disabled="tenant.status === 'deleted'" @click="openDeleteDialog(tenant)"><Trash2 data-icon="inline-start" />删除</Button></div><span v-else class="text-sm text-muted-foreground">系统内置</span></TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <div v-if="total > (queryForm.pageSize || 20)" class="mt-4 flex justify-end">
          <Pagination :page="queryForm.page" :items-per-page="queryForm.pageSize || 20" :total="total" @update:page="handlePageChange">
            <PaginationContent v-slot="{ items }">
              <PaginationPrevious>上一页</PaginationPrevious>
              <template v-for="(item, index) in items" :key="index">
                <PaginationItem v-if="item.type === 'page'" :value="item.value" :is-active="item.value === queryForm.page" @click="handlePageChange(item.value)">{{ item.value }}</PaginationItem>
                <PaginationEllipsis v-else :index="index" />
              </template>
              <PaginationNext>下一页</PaginationNext>
            </PaginationContent>
          </Pagination>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="tenantDialogVisible">
      <DialogContent class="max-w-lg"><DialogHeader><DialogTitle>{{ dialogTitle }}</DialogTitle><DialogDescription>{{ editingTenant ? '修改租户名称或更换为启用中的套餐。' : '新租户会绑定套餐，并将当前管理员设为租户管理员。' }}</DialogDescription></DialogHeader><form class="grid gap-4" @submit.prevent="submitTenant"><div class="grid gap-2"><Label for="tenant-name">租户名称</Label><Input id="tenant-name" v-model="tenantForm.name" placeholder="请输入租户名称" /></div><div class="grid gap-2"><Label>租户套餐</Label><Select v-model="tenantForm.packageId"><SelectTrigger><SelectValue placeholder="请选择套餐" /></SelectTrigger><SelectContent><SelectGroup><SelectItem v-for="item in packages" :key="item.id" :value="item.id">{{ item.name }} · {{ formatLimit(item.maxUsers) }} 用户 / {{ formatLimit(item.maxApps) }} 应用</SelectItem></SelectGroup></SelectContent></Select></div><DialogFooter><Button type="button" variant="outline" @click="tenantDialogVisible = false">取消</Button><Button type="submit" :disabled="submitting">{{ submitting ? '保存中...' : '保存' }}</Button></DialogFooter></form></DialogContent>
    </Dialog>

    <Dialog v-model:open="transferDialogVisible">
      <DialogContent class="max-w-lg"><DialogHeader><DialogTitle>转移租户管理员</DialogTitle><DialogDescription>目标管理员将成为“{{ transferTarget?.name }}”的租户管理员，原管理员将被移出该租户。</DialogDescription></DialogHeader><div class="grid gap-2"><Label>目标管理员</Label><Select v-model="transferAdministratorId"><SelectTrigger><SelectValue placeholder="请选择活跃管理员" /></SelectTrigger><SelectContent><SelectGroup><SelectItem v-for="administrator in administrators" :key="administrator.id" :value="administrator.id">{{ administrator.username }} · {{ administrator.email }}</SelectItem></SelectGroup></SelectContent></Select></div><DialogFooter><Button variant="outline" @click="transferDialogVisible = false">取消</Button><Button :disabled="transferSubmitting" @click="submitTransfer">{{ transferSubmitting ? '转移中...' : '确认转移' }}</Button></DialogFooter></DialogContent>
    </Dialog>

    <AlertDialog v-model:open="deleteDialogVisible">
      <AlertDialogContent><AlertDialogHeader><AlertDialogTitle>删除租户</AlertDialogTitle><AlertDialogDescription>确定删除“{{ deleteTarget?.name }}”吗？删除后将停用该租户及其所有管理员成员关系，但保留业务与审计数据。</AlertDialogDescription></AlertDialogHeader><AlertDialogFooter><AlertDialogCancel>取消</AlertDialogCancel><AlertDialogAction class="bg-destructive text-destructive-foreground hover:bg-destructive/90" @click="confirmDelete">确认删除</AlertDialogAction></AlertDialogFooter></AlertDialogContent>
    </AlertDialog>
  </div>
</template>
