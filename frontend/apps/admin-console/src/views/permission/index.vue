<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Modal, Table, message, Pagination as AntPagination, Form, FormItem } from 'antdv-next'
import { Search, Plus, List, Share2, RefreshCw, Lock, Menu, Settings2, Database } from '@lucide/vue'
import { permissionApi } from '@/api/permission'
import type { Permission, PermissionTree, PermissionStats, CreatePermissionDto, UpdatePermissionDto } from '@/types/permission'
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

const [modal, contextHolder] = Modal.useModal()

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

const permissionFormRules = {
  name: [{ required: true, message: '请输入权限名称' }],
  code: [{ required: true, message: '请输入权限编码' }],
  type: [{ required: true, message: '请选择权限类型' }],
  resource: [{ required: true, message: '请输入资源' }],
  action: [{ required: true, message: '请输入操作' }],
}

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

function getTypeVariant(type: string): string {
  const map: Record<string, string> = {
    menu: 'blue',
    operation: 'green',
    data: 'purple'
  }
  return map[type] || 'default'
}

async function loadStats() {
  try {
    const res = await permissionApi.getStats()
    stats.value = res
  } catch (error) {
    console.error('加载统计信息失败:', error)
    message.error('加载权限统计信息失败')
  }
}

async function loadPermissions() {
  loading.value = true
  try {
    const res = await permissionApi.getList({
      page: page.value,
      pageSize: pageSize.value,
      search: searchQuery.value || undefined,
      type: filterType.value || undefined,
      resource: filterResource.value || undefined,
    })
    permissions.value = res.items
    total.value = res.total
  } catch (error) {
    message.error('加载权限列表失败')
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
    message.error('加载权限树失败')
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
      message.success('更新成功')
    } else {
      await permissionApi.create({
        name: permissionForm.name!.trim(),
        code: permissionForm.code!.trim(),
        description: permissionForm.description,
        type: permissionForm.type,
        resource: permissionForm.resource!.trim(),
        action: permissionForm.action!.trim(),
        parentId: permissionForm.parentId,
      })
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadPermissions()
    loadTree()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Permission) {
  const confirmed = await modal.confirm({
    title: '删除权限',
    content: `确定要删除权限”${row.name}”吗？`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) {
    return
  }
  try {
    await permissionApi.delete(row.id)
    message.success('删除成功')
    loadPermissions()
    loadTree()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除失败')
  }
}

function handlePageChange(p: number, _pageSize?: number) {
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
        <h1 class="text-2xl font-bold text-foreground mb-2">权限管理</h1>
        <p class="text-sm text-muted-foreground">管理用户池权限，定义用户池的操作权限、菜单权限和数据权限</p>
      </div>
      <div>
        <Button @click="handleCreate">
          <Plus class="size-4 mr-2" />
          创建权限
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <Card v-for="(stat, index) in statsData" :key="index" class="transition-all hover:shadow-md">
        <div class="pt-4">
          <div class="flex items-center gap-4">
            <div
              class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
              :class="{
                'bg-gradient-to-br from-primary to-primary/60': stat.class === 'primary',
                'bg-gradient-to-br from-red-500 to-red-400': stat.class === 'danger',
                'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.class === 'success',
                'bg-gradient-to-br from-sky-500 to-sky-400': stat.class === 'info',
              }"
            >
              <component :is="stat.icon" class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-foreground leading-tight">{{ stat.value }}</div>
              <div class="text-xs text-muted-foreground mt-0.5">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <Card>
      <div class="border-b">
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-3">
            <div class="relative">
              <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
              <Input v-model:value="searchQuery"
                placeholder="搜索权限名称、编码、资源"
                class="w-72 pl-8"
                @keyup.enter="handleSearch"
              />
            </div>
            <Select v-model:value="filterType" class="w-36" allow-clear @update:value="handleSearch">
                <SelectOption value="menu">菜单权限</SelectOption>
                <SelectOption value="operation">操作权限</SelectOption>
                <SelectOption value="data">数据权限</SelectOption>

            </Select>
            <Select v-model:value="filterResource" class="w-36" allow-clear @update:value="handleSearch">
                <SelectOption v-for="r in resourceOptions" :key="r" :value="r">
                  {{ r }}
                </SelectOption>

            </Select>
            <Button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </Button>
            <Button  @click="handleReset">
              <RefreshCw class="size-4 mr-2" />
              重置
            </Button>
          </div>
          <div class="flex gap-0">
            <Button
                :type="viewMode === 'list' ? 'primary' : 'default'"
              class="rounded-r-none"
              @click="viewMode = 'list'"
            >
              <List class="size-4 mr-2" />
              列表
            </Button>
            <Button
                :type="viewMode === 'tree' ? 'primary' : 'default'"
              class="rounded-l-none"
              @click="viewMode = 'tree'"
            >
              <Share2 class="size-4 mr-2" />
              树形
            </Button>
          </div>
        </div>
      </div>

      <div class="pt-4">
        <div v-if="viewMode === 'list'">
          <Table :columns="[
            { title: '权限名称', key: 'name', width: 180 }, { title: '权限编码', key: 'code', width: 200 },
            { title: '资源', dataIndex: 'resource', width: 130 }, { title: '操作', dataIndex: 'action', width: 100 },
            { title: '父级权限', key: 'parent', width: 150 }, { title: '描述', key: 'description', width: 200 },
            { title: '操作', key: 'actions', width: 150 }
          ]" :data-source="permissions" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1110 }">
            <template #bodyCell="{ column, record: row }">
              <template v-if="column.key === 'name'">
                  <div class="flex items-center gap-2">
                    <Tag :color="getTypeVariant(row.type)" class="text-xs">
                      {{ getTypeLabel(row.type) }}
                    </Tag>
                    <span class="font-medium text-foreground">{{ row.name }}</span>
                  </div>
              </template>
              <template v-else-if="column.key === 'code'">
                  <code class="bg-muted px-2 py-0.5 rounded text-xs text-muted-foreground font-mono">
                    {{ row.code }}
                  </code>
              </template>
              <template v-else-if="column.key === 'parent'">
                  <span v-if="row.parent">{{ row.parent.name }}</span>
                  <span v-else class="text-muted-foreground">-</span>
              </template>
              <template v-else-if="column.key === 'description'"><span class="text-muted-foreground">{{ row.description || '-' }}</span></template>
              <template v-else-if="column.key === 'actions'">
                  <div class="flex gap-2">
                    <Button type="link" size="small" class="p-0 h-auto" @click="handleEdit(row)">
                      编辑
                    </Button>
                    <Button type="link" size="small" class="p-0 h-auto text-destructive" @click="handleDelete(row)">
                      删除
                    </Button>
                  </div>
              </template>
            </template>
          </Table>
          <div class="flex items-center justify-between mt-4 pt-4 border-t">
            <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
            <AntPagination
              :current="page"
              :page-size="pageSize"
              :total="total"
              :show-size-changer="false"
              size="small"
              @change="handlePageChange"
            />
          </div>
        </div>

        <div v-else>
          <Tree
            :tree-data="permissionTree"
            :field-names="{ key: 'id', title: 'name', children: 'children' }"
            default-expand-all
            :selectable="false"
          >
            <template #titleRender="data">
              <div class="flex items-center gap-2">
                <Tag :color="getTypeVariant(data.type)" class="text-xs">
                  {{ getTypeLabel(data.type) }}
                </Tag>
                <span>{{ data.name }}</span>
                <span class="text-xs text-muted-foreground ml-1">({{ data.code }})</span>
              </div>
            </template>
          </Tree>
        </div>
      </div>
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <Form :model="permissionForm" :rules="permissionFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <FormItem label="权限名称" name="name">
            <Input v-model:value="permissionForm.name" placeholder="如: 查看用户" />
          </FormItem>
          <FormItem label="权限编码" name="code">
            <Input v-model:value="permissionForm.code" placeholder="如: user:read" :disabled="isEdit" />
          </FormItem>
          <FormItem label="权限类型" name="type">
            <Select
              v-model:value="permissionForm.type"
              :options="[
                { value: 'menu', label: '菜单权限' },
                { value: 'operation', label: '操作权限' },
                { value: 'data', label: '数据权限' },
              ]"
            />
          </FormItem>
          <FormItem label="资源" name="resource">
            <Input v-model:value="permissionForm.resource" placeholder="如: user" />
          </FormItem>
          <FormItem label="操作" name="action">
            <Input v-model:value="permissionForm.action" placeholder="如: read" />
          </FormItem>
          <FormItem label="父级权限" name="parentId">
            <TreeSelect
              v-model:value="permissionForm.parentId"
              class="w-full"
              :tree-data="permissionTree"
              :field-names="{ children: 'children', label: 'name', value: 'id' }"
              placeholder="请选择父级权限"
              allow-clear
              tree-default-expand-all
            />
          </FormItem>
          <FormItem label="描述" name="description">
            <InputTextArea v-model:value="permissionForm.description" :rows="2" placeholder="请输入描述" />
          </FormItem>
          <div class="flex justify-end gap-2">
            <Button html-type="button" @click="dialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit" :loading="submitting">确定</Button>
          </div>
        </Form>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>
