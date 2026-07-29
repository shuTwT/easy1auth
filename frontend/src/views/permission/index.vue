<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import { Search, Plus, List, Share2, RefreshCw, Lock, Menu, Settings2, Database } from '@lucide/vue'
import { permissionApi } from '@/api/permission'
import type { Permission, PermissionTree, PermissionStats, CreatePermissionDto, UpdatePermissionDto } from '@/types/permission'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tree, TreeSelect } from '@/components/ui/tree'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'

const loading = ref(false)
const permissions = ref<Permission[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const permissionTree = ref<PermissionTree[]>([])
const searchQuery = ref('')
const filterType = ref('')
const filterResource = ref('')
const viewMode = ref<'list' | 'tree'>('list')

const stats = ref<PermissionStats>({
  totalPermissions: 0,
  menuPermissions: 0,
  operationPermissions: 0,
  dataPermissions: 0,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)

const permissionForm = reactive<CreatePermissionDto & UpdatePermissionDto & { id?: string }>({
  name: '',
  code: '',
  description: '',
  type: 'operation',
  resource: '',
  action: '',
  parentId: undefined,
})

const statsData = computed(() => [
  { label: '总权限数', value: stats.value.totalPermissions, icon: Lock, class: 'primary' },
  { label: '菜单权限', value: stats.value.menuPermissions, icon: Menu, class: 'danger' },
  { label: '操作权限', value: stats.value.operationPermissions, icon: Settings2, class: 'success' },
  { label: '数据权限', value: stats.value.dataPermissions, icon: Database, class: 'info' },
])

const dialogTitle = computed(() => (isEdit.value ? '编辑权限' : '创建权限'))

const resourceOptions = computed(() => {
  const set = new Set<string>()
  permissions.value.forEach((p) => set.add(p.resource))
  return Array.from(set).sort()
})

function getTypeLabel(type: string) {
  const map: Record<string, string> = { menu: '菜单', operation: '操作', data: '数据' }
  return map[type] || type
}

function getTypeVariant(type: string): 'default' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'link' {
  const map: Record<string, 'default' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'link'> = { 
    menu: 'default', 
    operation: 'secondary', 
    data: 'outline' 
  }
  return map[type] || 'secondary'
}

async function loadStats() {
  try {
    const res = await permissionApi.getStats()
    stats.value = res
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

async function loadPermissions() {
  loading.value = true
  try {
    const res = await permissionApi.getList({
      page: page.value,
      pageSize: pageSize.value,
      search: searchQuery.value,
      type: filterType.value,
      resource: filterResource.value,
    })
    permissions.value = res.items
    total.value = res.total
  } catch (error) {
    toast.error('加载权限列表失败')
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  try {
    const res = await permissionApi.getTree()
    permissionTree.value = res
  } catch (error) {
    console.error('加载权限树失败:', error)
  }
}

function handleSearch() {
  page.value = 1
  loadPermissions()
}

function handleReset() {
  searchQuery.value = ''
  filterType.value = ''
  filterResource.value = ''
  page.value = 1
  loadPermissions()
}

function handleCreate() {
  isEdit.value = false
  Object.assign(permissionForm, {
    id: undefined,
    name: '',
    code: '',
    description: '',
    type: 'operation',
    resource: '',
    action: '',
    parentId: undefined,
  })
  dialogVisible.value = true
}

function handleEdit(row: Permission) {
  isEdit.value = true
  Object.assign(permissionForm, {
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description || '',
    type: row.type,
    resource: row.resource,
    action: row.action,
    parentId: row.parentId || undefined,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) {
      await permissionApi.update(permissionForm.id!, {
        name: permissionForm.name,
        description: permissionForm.description,
        type: permissionForm.type,
        resource: permissionForm.resource,
        action: permissionForm.action,
        parentId: permissionForm.parentId,
      })
      toast.success('更新成功')
    } else {
      if (!permissionForm.code || !permissionForm.resource || !permissionForm.action) {
        toast.error('请填写必填字段')
        submitting.value = false
        return
      }
      await permissionApi.create({
        name: permissionForm.name!,
        code: permissionForm.code!,
        description: permissionForm.description,
        type: permissionForm.type,
        resource: permissionForm.resource!,
        action: permissionForm.action!,
        parentId: permissionForm.parentId,
      })
      toast.success('创建成功')
    }
    dialogVisible.value = false
    loadPermissions()
    loadTree()
    loadStats()
  } catch (error: any) {
    toast.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Permission) {
  if (!window.confirm(`确定要删除权限 "${row.name}" 吗？`)) {
    return
  }
  try {
    await permissionApi.delete(row.id)
    toast.success('删除成功')
    loadPermissions()
    loadTree()
    loadStats()
  } catch (error: any) {
    toast.error(error.response?.data?.msg || '删除失败')
  }
}

function handlePageChange(p: number) {
  page.value = p
  loadPermissions()
}

onMounted(() => {
  loadStats()
  loadPermissions()
  loadTree()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-3xl font-bold text-foreground mb-2">权限管理</h1>
        <p class="text-sm text-muted-foreground">管理系统权限，定义操作权限、菜单权限和数据权限</p>
      </div>
      <div>
        <Button @click="handleCreate">
          <Plus class="w-4 h-4 mr-1" />
          创建权限
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-4 gap-5 mb-6">
      <Card v-for="(stat, index) in statsData" :key="index" class="cursor-pointer transition-all hover:-translate-y-1 hover:shadow-lg">
        <CardContent class="pt-4">
          <div class="flex items-center gap-4">
            <div
              class="w-14 h-14 rounded-lg flex items-center justify-center text-white shrink-0"
              :class="{
                'bg-gradient-to-br from-primary to-primary/60': stat.class === 'primary',
                'bg-gradient-to-br from-red-500 to-red-400': stat.class === 'danger',
                'bg-gradient-to-br from-green-500 to-green-400': stat.class === 'success',
                'bg-gradient-to-br from-sky-500 to-sky-400': stat.class === 'info',
              }"
            >
              <component :is="stat.icon" class="w-6 h-6" />
            </div>
            <div>
              <div class="text-3xl font-bold text-foreground leading-tight">{{ stat.value }}</div>
              <div class="text-sm text-muted-foreground mt-1">{{ stat.label }}</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader class="border-b">
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-3">
            <div class="relative">
              <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                v-model="searchQuery"
                placeholder="搜索权限名称、编码、资源"
                class="w-72 pl-8"
                @keyup.enter="handleSearch"
              />
            </div>
            <Select v-model="filterType" @update:model-value="handleSearch">
              <SelectTrigger class="w-36">
                <SelectValue placeholder="权限类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="menu">菜单权限</SelectItem>
                <SelectItem value="operation">操作权限</SelectItem>
                <SelectItem value="data">数据权限</SelectItem>
              </SelectContent>
            </Select>
            <Select v-model="filterResource" @update:model-value="handleSearch">
              <SelectTrigger class="w-36">
                <SelectValue placeholder="资源" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="r in resourceOptions" :key="r" :value="r">
                  {{ r }}
                </SelectItem>
              </SelectContent>
            </Select>
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-1" />
              搜索
            </Button>
            <Button variant="outline" @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-1" />
              重置
            </Button>
          </div>
          <div class="flex gap-0">
            <Button
              :variant="viewMode === 'list' ? 'default' : 'outline'"
              class="rounded-r-none"
              @click="viewMode = 'list'"
            >
              <List class="w-4 h-4 mr-1" />
              列表
            </Button>
            <Button
              :variant="viewMode === 'tree' ? 'default' : 'outline'"
              class="rounded-l-none"
              @click="viewMode = 'tree'"
            >
              <Share2 class="w-4 h-4 mr-1" />
              树形
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent class="pt-4">
        <div v-if="viewMode === 'list'">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead class="min-w-[180px]">权限名称</TableHead>
                <TableHead class="w-[200px]">权限编码</TableHead>
                <TableHead class="w-[130px]">资源</TableHead>
                <TableHead class="w-[100px]">操作</TableHead>
                <TableHead class="w-[150px]">父级权限</TableHead>
                <TableHead class="min-w-[200px]">描述</TableHead>
                <TableHead class="w-[150px]">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="loading">
                <TableCell colspan="7" class="text-center py-8 text-muted-foreground">
                  加载中...
                </TableCell>
              </TableRow>
              <TableRow v-else-if="permissions.length === 0">
                <TableCell colspan="7" class="text-center py-8 text-muted-foreground">
                  暂无数据
                </TableCell>
              </TableRow>
              <TableRow v-for="row in permissions" :key="row.id">
                <TableCell>
                  <div class="flex items-center gap-2">
                    <Badge :variant="getTypeVariant(row.type)" class="text-xs">
                      {{ getTypeLabel(row.type) }}
                    </Badge>
                    <span class="font-medium text-foreground">{{ row.name }}</span>
                  </div>
                </TableCell>
                <TableCell>
                  <code class="bg-muted px-2 py-0.5 rounded text-xs text-muted-foreground font-mono">
                    {{ row.code }}
                  </code>
                </TableCell>
                <TableCell>{{ row.resource }}</TableCell>
                <TableCell>{{ row.action }}</TableCell>
                <TableCell>
                  <span v-if="row.parent">{{ row.parent.name }}</span>
                  <span v-else class="text-muted-foreground">-</span>
                </TableCell>
                <TableCell class="text-muted-foreground">{{ row.description || '-' }}</TableCell>
                <TableCell>
                  <div class="flex gap-2">
                    <Button variant="link" size="sm" class="p-0 h-auto" @click="handleEdit(row)">
                      编辑
                    </Button>
                    <Button variant="link" size="sm" class="p-0 h-auto text-destructive" @click="handleDelete(row)">
                      删除
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <div v-if="total > pageSize" class="flex items-center justify-between mt-4">
            <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
            <div class="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                :disabled="page <= 1"
                @click="handlePageChange(page - 1)"
              >
                上一页
              </Button>
              <span class="text-sm px-2">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
              <Button
                variant="outline"
                size="sm"
                :disabled="page >= Math.ceil(total / pageSize)"
                @click="handlePageChange(page + 1)"
              >
                下一页
              </Button>
            </div>
          </div>
        </div>

        <div v-else>
          <Tree
            :data="permissionTree"
            :props="{ children: 'children', label: 'name' }"
            node-key="id"
            :default-expand-all="true"
          >
            <template #default="{ data }">
              <div class="flex items-center gap-2">
                <Badge :variant="getTypeVariant(data.type)" class="text-xs">
                  {{ getTypeLabel(data.type) }}
                </Badge>
                <span>{{ data.name }}</span>
                <span class="text-xs text-muted-foreground ml-1">({{ data.code }})</span>
              </div>
            </template>
          </Tree>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="handleSubmit">
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">权限名称 <span class="text-destructive">*</span></label>
              <Input v-model="permissionForm.name" placeholder="如: 查看用户" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">权限编码 <span class="text-destructive">*</span></label>
              <Input v-model="permissionForm.code" placeholder="如: user:read" :disabled="isEdit" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">权限类型 <span class="text-destructive">*</span></label>
              <Select v-model="permissionForm.type">
                <SelectTrigger>
                  <SelectValue placeholder="请选择权限类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="menu">菜单权限</SelectItem>
                  <SelectItem value="operation">操作权限</SelectItem>
                  <SelectItem value="data">数据权限</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">资源 <span class="text-destructive">*</span></label>
              <Input v-model="permissionForm.resource" placeholder="如: user" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">操作 <span class="text-destructive">*</span></label>
              <Input v-model="permissionForm.action" placeholder="如: read" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">父级权限</label>
              <TreeSelect
                v-model="permissionForm.parentId"
                :data="permissionTree"
                :props="{ children: 'children', label: 'name', value: 'id' }"
                placeholder="请选择父级权限"
                clearable
              />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">描述</label>
              <Textarea v-model="permissionForm.description" :rows="2" placeholder="请输入描述" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button :disabled="submitting" @click="handleSubmit">
            {{ submitting ? '提交中...' : '确定' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
@media (max-width: 1200px) {
  .grid-cols-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .grid-cols-4 {
    grid-template-columns: 1fr;
  }
}
</style>
