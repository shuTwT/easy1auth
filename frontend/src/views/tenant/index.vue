<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { Plus, Search, RefreshCw } from '@lucide/vue'
import { tenantApi } from '@/api/tenant'
import type { Tenant, CreateTenantDto, UpdateTenantDto, TenantQueryDto } from '@/types/tenant'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { NumberField, NumberFieldContent, NumberFieldDecrement, NumberFieldIncrement, NumberFieldInput } from '@/components/ui/number-field'
import { Label } from '@/components/ui/label'

const loading = ref(false)
const tenants = ref<Tenant[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const currentTenant = ref<Partial<Tenant>>({})

const queryForm = reactive<TenantQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  status: undefined,
  plan: undefined
})

const tenantForm = reactive<CreateTenantDto & UpdateTenantDto>({
  name: '',
  logo: '',
  domain: '',
  plan: 'basic',
  maxUsers: 100,
  maxApps: 10
})


const loadTenants = async () => {
  loading.value = true
  try {
    const res = await tenantApi.getList(queryForm)
    tenants.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载租户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadTenants()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.status = undefined
  queryForm.plan = undefined
  queryForm.page = 1
  loadTenants()
}

const handleAdd = () => {
  dialogTitle.value = '新增租户'
  Object.assign(tenantForm, {
    name: '',
    logo: '',
    domain: '',
    plan: 'basic',
    maxUsers: 100,
    maxApps: 10
  })
  currentTenant.value = {}
  dialogVisible.value = true
}

const handleEdit = (row: Tenant) => {
  dialogTitle.value = '编辑租户'
  Object.assign(tenantForm, row)
  currentTenant.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: Tenant) => {
  const confirmed = window.confirm('确定要删除该租户吗？删除后无法恢复！')
  if (!confirmed) return
  
  try {
    await tenantApi.delete(row.id)
    toast.success('删除成功')
    loadTenants()
  } catch (error) {
    console.error('删除租户失败:', error)
  }
}

const handleStatusChange = async (row: Tenant, status: string) => {
  try {
    await tenantApi.updateStatus(row.id, status)
    toast.success('状态更新成功')
    loadTenants()
  } catch (error) {
    console.error('更新状态失败:', error)
  }
}

const handleSubmit = async () => {
  try {
    if (currentTenant.value.id) {
      await tenantApi.update(currentTenant.value.id, tenantForm)
      toast.success('更新成功')
    } else {
      await tenantApi.create(tenantForm as CreateTenantDto)
      toast.success('创建成功')
    }
    dialogVisible.value = false
    loadTenants()
  } catch (error) {
    console.error('保存租户失败:', error)
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadTenants()
}

const totalPages = () => Math.ceil(total.value / queryForm.pageSize!)

onMounted(() => {
  loadTenants()
})
</script>

<template>
  <div class="tenant-management p-5">
    <Card>
      <CardHeader class="flex flex-row items-center justify-between">
        <CardTitle>租户管理</CardTitle>
        <Button @click="handleAdd">
          <Plus class="w-4 h-4 mr-2" />
          新增租户
        </Button>
      </CardHeader>
      <CardContent>
        <div class="flex flex-wrap items-end gap-4 mb-5">
          <div class="grid gap-1.5">
            <Label>租户名称</Label>
            <Input v-model="queryForm.name" placeholder="请输入租户名称" class="w-[200px]" />
          </div>
          <div class="grid gap-1.5">
            <Label>状态</Label>
            <Select v-model="queryForm.status" class="w-[120px]">
              <SelectTrigger>
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="active">正常</SelectItem>
                  <SelectItem value="suspended">停用</SelectItem>
                  <SelectItem value="deleted">已删除</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <div class="grid gap-1.5">
            <Label>套餐</Label>
            <Select v-model="queryForm.plan" class="w-[150px]">
              <SelectTrigger>
                <SelectValue placeholder="请选择套餐" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="basic">基础版</SelectItem>
                  <SelectItem value="professional">专业版</SelectItem>
                  <SelectItem value="enterprise">企业版</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button variant="outline" @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[180px]">租户名称</TableHead>
              <TableHead class="w-[200px]">域名</TableHead>
              <TableHead class="w-[120px]">套餐</TableHead>
              <TableHead class="w-[100px]">状态</TableHead>
              <TableHead class="w-[100px]">用户上限</TableHead>
              <TableHead class="w-[100px]">应用上限</TableHead>
              <TableHead class="w-[180px]">创建时间</TableHead>
              <TableHead class="w-[280px]">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="8" class="text-center text-muted-foreground">加载中...</TableCell>
            </TableRow>
            <TableRow v-else-if="tenants.length === 0">
              <TableCell colspan="8" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
            </TableRow>
            <TableRow v-for="item in tenants" :key="item.id">
              <TableCell>{{ item.name }}</TableCell>
              <TableCell>{{ item.domain }}</TableCell>
              <TableCell>
                <Badge v-if="item.plan === 'basic'" variant="secondary">基础版</Badge>
                <Badge v-else-if="item.plan === 'professional'" variant="outline">专业版</Badge>
                <Badge v-else>企业版</Badge>
              </TableCell>
              <TableCell>
                <Badge :variant="item.status === 'active' ? 'default' : item.status === 'suspended' ? 'outline' : 'destructive'">
                  {{ item.status === 'active' ? '正常' : item.status === 'suspended' ? '停用' : '已删除' }}
                </Badge>
              </TableCell>
              <TableCell>{{ item.maxUsers }}</TableCell>
              <TableCell>{{ item.maxApps }}</TableCell>
              <TableCell>{{ new Date(item.createdAt).toLocaleString() }}</TableCell>
              <TableCell>
                <div class="flex gap-2">
                  <Button variant="link" size="sm" @click="handleEdit(item)">编辑</Button>
                  <Button 
                    variant="link" 
                    size="sm"
                    @click="handleStatusChange(item, item.status === 'active' ? 'suspended' : 'active')"
                  >
                    {{ item.status === 'active' ? '停用' : '启用' }}
                  </Button>
                  <Button variant="link" size="sm">统计</Button>
                  <Button variant="link" size="sm" class="text-destructive" @click="handleDelete(item)">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <Button variant="outline" size="sm" :disabled="queryForm.page! <= 1" @click="handlePageChange(queryForm.page! - 1)">上一页</Button>
            <span class="text-sm px-2">{{ queryForm.page! }} / {{ totalPages() }}</span>
            <Button variant="outline" size="sm" :disabled="queryForm.page! >= totalPages()" @click="handlePageChange(queryForm.page! + 1)">下一页</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
          <DialogDescription>配置租户基本信息</DialogDescription>
        </DialogHeader>
        <form class="grid gap-4 py-4">
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">租户名称</Label>
            <Input v-model="tenantForm.name" placeholder="请输入租户名称" class="col-span-3" />
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">Logo</Label>
            <Input v-model="tenantForm.logo" placeholder="请输入Logo URL" class="col-span-3" />
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">域名</Label>
            <Input v-model="tenantForm.domain" placeholder="请输入域名" class="col-span-3" />
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">套餐</Label>
            <Select v-model="tenantForm.plan" class="col-span-3">
              <SelectTrigger>
                <SelectValue placeholder="请选择套餐" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="basic">基础版</SelectItem>
                  <SelectItem value="professional">专业版</SelectItem>
                  <SelectItem value="enterprise">企业版</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">用户上限</Label>
            <NumberField v-model="tenantForm.maxUsers" :min="1" :max="10000" class="col-span-3">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
          <div class="grid grid-cols-4 items-center gap-4">
            <Label class="text-right">应用上限</Label>
            <NumberField v-model="tenantForm.maxApps" :min="1" :max="1000" class="col-span-3">
              <NumberFieldContent>
                <NumberFieldDecrement />
                <NumberFieldInput />
                <NumberFieldIncrement />
              </NumberFieldContent>
            </NumberField>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
