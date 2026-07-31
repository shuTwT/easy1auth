<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Table as ATable } from 'antdv-next'
import {
  AppWindow,
  Check,
  Gauge,
  Package,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  Users,
  X,
} from '@lucide/vue'
import { tenantPackageApi } from '@/api/tenantPackage'
import type {
  ManagementPermission,
  TenantPackage,
  TenantPackageMutation,
  TenantPackageStatus,
} from '@/types/tenantPackage'
type TreeNodeData = Record<string, unknown>

interface PermissionTreeNode extends TreeNodeData {
  id: string
  label: string
  code?: string
  type: 'group' | 'directory' | 'menu' | 'action'
  children: PermissionTreeNode[]
  ancestorCodes: string[]
}

const packages = ref<TenantPackage[]>([])
const permissionCatalog = ref<ManagementPermission[]>([])
const loading = ref(false)
const submitting = ref(false)
const permissionSubmitting = ref(false)
const permissionReturnsToPackageForm = ref(false)
const search = ref('')
const packageDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const editingPackage = ref<TenantPackage | null>(null)
const deletingPackage = ref<TenantPackage | null>(null)
const permissionTarget = ref<TenantPackage | null>(null)

const packageForm = reactive<TenantPackageMutation>({
  code: '',
  name: '',
  maxUsers: 100,
  maxApps: 10,
  permissionCodes: [],
})
const selectedPermissions = ref<string[]>([])

const tenantPermissions = computed(() => permissionCatalog.value
  .filter((permission) => permission.active && permission.scope.toUpperCase() === 'TENANT')
  .sort((a, b) => a.sortOrder - b.sortOrder || a.code.localeCompare(b.code)))

const permissionTree = computed<PermissionTreeNode[]>(() => {
  const menuNodes = new Map<string, PermissionTreeNode>()
  const rootMenus: PermissionTreeNode[] = []
  const otherActions: PermissionTreeNode[] = []

  tenantPermissions.value
    .filter((permission) => ['DIRECTORY', 'MENU'].includes(permission.type.toUpperCase()))
    .forEach((permission) => menuNodes.set(permission.code, {
      id: permission.code,
      label: permission.name,
      code: permission.code,
      type: permission.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
      children: [],
      ancestorCodes: [],
    }))

  tenantPermissions.value
    .filter((permission) => ['DIRECTORY', 'MENU'].includes(permission.type.toUpperCase()))
    .forEach((permission) => {
      const node = menuNodes.get(permission.code)!
      const parent = permission.parentCode ? menuNodes.get(permission.parentCode) : undefined
      if (parent) {
        node.ancestorCodes = [...parent.ancestorCodes, parent.code!]
        parent.children.push(node)
      } else {
        rootMenus.push(node)
      }
    })

  tenantPermissions.value
    .filter((permission) => permission.type.toUpperCase() === 'ACTION')
    .forEach((permission) => {
      const parent = permission.parentCode ? menuNodes.get(permission.parentCode) : undefined
      const node: PermissionTreeNode = {
        id: permission.code,
        label: permission.name,
        code: permission.code,
        type: 'action',
        children: [],
        ancestorCodes: parent ? [...parent.ancestorCodes, parent.code!] : [],
      }
      if (parent) parent.children.push(node)
      else otherActions.push(node)
    })

  const groups: PermissionTreeNode[] = []
  if (rootMenus.length > 0) groups.push({ id: 'group:menus', label: '菜单权限', type: 'group', children: rootMenus, ancestorCodes: [] })
  if (otherActions.length > 0) groups.push({ id: 'group:actions', label: '其他操作权限', type: 'group', children: otherActions, ancestorCodes: [] })
  return groups
})

const filteredPermissionTree = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return permissionTree.value
  const filter = (node: PermissionTreeNode): PermissionTreeNode | null => {
    const children = node.children.map(filter).filter((item): item is PermissionTreeNode => item !== null)
    const source = node.code ? tenantPermissions.value.find((permission) => permission.code === node.code) : undefined
    const matched = [node.label, node.code || '', source?.resource || ''].some((value) => value.toLowerCase().includes(keyword))
    return matched || children.length > 0 ? { ...node, children } : null
  }
  return permissionTree.value.map(filter).filter((item): item is PermissionTreeNode => item !== null)
})

const activeCount = computed(() => packages.value.filter((item) => item.status === 'active').length)
const defaultPackage = computed(() => packages.value.find((item) => item.defaultPackage))
const totalPermissionCount = computed(() => new Set(packages.value.flatMap((item) => item.permissionCodes)).size)
const packageDialogTitle = computed(() => editingPackage.value ? '编辑租户套餐' : '创建租户套餐')
const filteredPackages = computed(() => packages.value.filter((item) => !search.value
  || `${item.name} ${item.code}`.toLowerCase().includes(search.value.toLowerCase())))
const packageColumns = [
  { title: '套餐', key: 'package', width: 250 },
  { title: '状态', key: 'status', width: 110 },
  { title: '资源配额', key: 'quota', width: 280 },
  { title: '权限', key: 'permissions', width: 120 },
  { title: '更新时间', key: 'updatedAt', width: 190 },
  { title: '操作', key: 'actions', width: 300, fixed: 'end' as const },
]

function resetPackageForm() {
  Object.assign(packageForm, {
    code: '',
    name: '',
    maxUsers: 100,
    maxApps: 10,
    permissionCodes: [],
  })
}

function permissionTypeLabel(type: string) {
  return ({ DIRECTORY: '目录', MENU: '菜单', ACTION: '按钮' } as Record<string, string>)[type.toUpperCase()] || type
}

function permissionTypeVariant(type: string): 'default' | 'secondary' | 'outline' {
  return type.toUpperCase() === 'MENU' ? 'default' : 'secondary'
}

function statusVariant(status: TenantPackageStatus): 'default' | 'secondary' | 'outline' {
  return status === 'active' ? 'default' : 'outline'
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString() : '-'
}

function formatLimit(value: number) {
  return value >= 2147483647 ? '不限' : value.toLocaleString()
}

async function loadData() {
  loading.value = true
  try {
    const [packageRows, permissions] = await Promise.all([
      tenantPackageApi.list(),
      tenantPackageApi.permissionCatalog(),
    ])
    packages.value = packageRows
    permissionCatalog.value = permissions
  } catch (error) {
    console.error('加载租户套餐失败:', error)
    message.error('加载租户套餐失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingPackage.value = null
  resetPackageForm()
  packageDialogVisible.value = true
}

function openEditDialog(item: TenantPackage) {
  editingPackage.value = item
  Object.assign(packageForm, {
    code: item.code,
    name: item.name,
    maxUsers: item.maxUsers,
    maxApps: item.maxApps,
    permissionCodes: [...item.permissionCodes],
  })
  packageDialogVisible.value = true
}

async function submitPackage() {
  if (!packageForm.code.trim() || !packageForm.name.trim()) {
    message.error('请填写套餐编码和套餐名称')
    return
  }
  if (packageForm.permissionCodes.length === 0) {
    message.error('请至少选择一项租户权限')
    return
  }

  submitting.value = true
  try {
    if (editingPackage.value) {
      await tenantPackageApi.update(editingPackage.value.id, packageForm)
      message.success('套餐更新成功')
    } else {
      await tenantPackageApi.create(packageForm)
      message.success('套餐创建成功')
    }
    packageDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('保存租户套餐失败:', error)
    message.error('保存租户套餐失败')
  } finally {
    submitting.value = false
  }
}

function openPermissionDialog(item: TenantPackage) {
  permissionTarget.value = item
  permissionReturnsToPackageForm.value = false
  selectedPermissions.value = [...item.permissionCodes]
  search.value = ''
  permissionDialogVisible.value = true
}

function descendantCodes(node: PermissionTreeNode): string[] {
  return [node.code, ...node.children.flatMap(descendantCodes)].filter((code): code is string => !!code)
}

function toggleTreePermission(node: PermissionTreeNode) {
  if (!node.code) return
  if (selectedPermissions.value.includes(node.code)) {
    const removals = ['directory', 'menu'].includes(node.type) ? descendantCodes(node) : [node.code]
    selectedPermissions.value = selectedPermissions.value.filter((code) => !removals.includes(code))
    return
  }
  selectedPermissions.value = [...new Set([...selectedPermissions.value, ...node.ancestorCodes, node.code])]
}

async function submitPermissions() {
  if (selectedPermissions.value.length === 0) {
    message.error('请至少选择一项租户权限')
    return
  }
  if (permissionReturnsToPackageForm.value) {
    packageForm.permissionCodes = [...selectedPermissions.value]
    permissionDialogVisible.value = false
    packageDialogVisible.value = true
    return
  }
  if (!permissionTarget.value) return
  permissionSubmitting.value = true
  try {
    await tenantPackageApi.replacePermissions(permissionTarget.value.id, selectedPermissions.value)
    message.success('套餐权限更新成功')
    permissionDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('更新套餐权限失败:', error)
    message.error('更新套餐权限失败')
  } finally {
    permissionSubmitting.value = false
  }
}

function closePermissionDialog() {
  permissionDialogVisible.value = false
  if (permissionReturnsToPackageForm.value) {
    permissionReturnsToPackageForm.value = false
    packageDialogVisible.value = true
  }
}

async function toggleStatus(item: TenantPackage) {
  const nextStatus: TenantPackageStatus = item.status === 'active' ? 'inactive' : 'active'
  try {
    await tenantPackageApi.updateStatus(item.id, nextStatus)
    message.success(nextStatus === 'active' ? '套餐已启用' : '套餐已停用')
    await loadData()
  } catch (error) {
    console.error('更新套餐状态失败:', error)
    message.error('更新套餐状态失败')
  }
}

function openDeleteDialog(item: TenantPackage) {
  deletingPackage.value = item
  deleteDialogVisible.value = true
}

async function confirmDelete() {
  if (!deletingPackage.value) return
  try {
    await tenantPackageApi.remove(deletingPackage.value.id)
    message.success('套餐已删除')
    deleteDialogVisible.value = false
    await loadData()
  } catch (error) {
    console.error('删除租户套餐失败:', error)
    message.error('删除租户套餐失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="flex flex-col gap-6 p-5 lg:p-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div>
        <div class="flex items-center gap-2">
          <Package class="text-primary" />
          <h1 class="text-2xl font-semibold tracking-tight">租户套餐</h1>
        </div>
        <p class="mt-1 text-sm text-muted-foreground">配置租户的资源配额和可用管理权限。</p>
      </div>
      <div class="flex gap-2">
        <Button  :disabled="loading" @click="loadData">
          <RefreshCw data-icon="inline-start" :class="loading ? 'animate-spin' : ''" />
          刷新
        </Button>
        <Button @click="openCreateDialog">
          <Plus data-icon="inline-start" />
          新建套餐
        </Button>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Card>
        <div class="pb-3"><p>套餐总数</p><h3 class="text-2xl">{{ packages.length }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-muted-foreground"><Package /> 已配置的套餐</div>
      </Card>
      <Card>
        <div class="pb-3"><p>启用中</p><h3 class="text-2xl">{{ activeCount }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-muted-foreground"><Check /> 可分配给新租户</div>
      </Card>
      <Card>
        <div class="pb-3"><p>默认套餐</p><h3 class="truncate text-2xl">{{ defaultPackage?.name || '-' }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-muted-foreground"><Gauge /> 新租户的默认方案</div>
      </Card>
      <Card>
        <div class="pb-3"><p>已使用权限</p><h3 class="text-2xl">{{ totalPermissionCount }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-muted-foreground"><ShieldCheck /> 去重后的权限项</div>
      </Card>
    </div>

    <Alert>
      <ShieldCheck />
      <h3>权限作用域提示</h3>
      <p>这里只能配置租户作用域权限；平台级权限不会出现在权限目录中。</p>
    </Alert>

    <Card>
      <div class="gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h3>套餐列表</h3>
          <p>套餐变更会影响已绑定该套餐的普通租户。</p>
        </div>
        <div class="relative w-full md:w-72">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
          <Input v-model:value="search" class="pl-9" placeholder="搜索套餐名称或编码" />
        </div>
      </div>
      <div>
        <div v-if="loading" class="flex min-h-40 items-center justify-center text-sm text-muted-foreground">加载中...</div>
        <div v-else-if="filteredPackages.length === 0" class="flex min-h-40 items-center justify-center text-sm text-muted-foreground">暂无匹配套餐</div>
        <ATable
          v-else
          :columns="packageColumns"
          :data-source="filteredPackages"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1250 }"
        >
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.key === 'package'">
                <div class="flex items-center gap-3">
                  <div class="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary"><Package /></div>
                  <div>
                    <div class="flex items-center gap-2 font-medium">{{ item.name }}<Tag v-if="item.defaultPackage" color="blue">默认</Tag></div>
                    <code class="text-xs text-muted-foreground">{{ item.code }}</code>
                  </div>
                </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="statusVariant(item.status)">{{ item.status === 'active' ? '启用中' : '已停用' }}</Tag>
            </template>
            <template v-else-if="column.key === 'quota'">
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
                  <span class="inline-flex items-center gap-1"><Users /> {{ formatLimit(item.maxUsers) }} 用户</span>
                  <span class="inline-flex items-center gap-1"><AppWindow /> {{ formatLimit(item.maxApps) }} 应用</span>
                </div>
            </template>
            <template v-else-if="column.key === 'permissions'">
              <button class="cursor-pointer text-left text-sm text-primary underline-offset-4 transition-colors hover:underline" @click="openPermissionDialog(item)">{{ item.permissionCodes.length }} 项权限</button>
            </template>
            <template v-else-if="column.key === 'updatedAt'">
              <span class="text-sm text-muted-foreground">{{ formatDate(item.updatedAt) }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
                <div class="flex flex-wrap justify-end gap-1">
                  <Button type="text" size="small" @click="openPermissionDialog(item)"><ShieldCheck data-icon="inline-start" />权限</Button>
                  <Button type="text" size="small" @click="openEditDialog(item)"><Pencil data-icon="inline-start" />编辑</Button>
                  <Button type="text" size="small" :disabled="item.defaultPackage" @click="toggleStatus(item)">{{ item.status === 'active' ? '停用' : '启用' }}</Button>
                  <Button type="text" size="small" class="text-destructive hover:text-destructive" :disabled="item.defaultPackage" @click="openDeleteDialog(item)"><Trash2 data-icon="inline-start" />删除</Button>
                </div>
            </template>
          </template>
        </ATable>
      </div>
    </Card>

    <Modal v-model:open="packageDialogVisible" :footer="null">
      <div class="max-w-xl">
        <div>
          <h3>{{ packageDialogTitle }}</h3>
          <p>设置套餐标识、资源上限和初始权限。</p>
        </div>
        <form class="grid gap-4" @submit.prevent="submitPackage">
          <div class="grid gap-4 sm:grid-cols-2">
            <div class="grid gap-2"><label for="package-code">套餐编码</label><Input id="package-code" v-model="packageForm.code" :disabled="!!editingPackage" placeholder="例如 professional" /></div>
            <div class="grid gap-2"><label for="package-name">套餐名称</label><Input id="package-name" v-model="packageForm.name" placeholder="例如 专业版" /></div>
          </div>
          <div class="grid gap-4 sm:grid-cols-2">
            <div class="grid gap-2"><label for="package-users">用户上限</label><InputNumber id="package-users" v-model:value="packageForm.maxUsers" :min="1" :max="2147483647" /></div>
            <div class="grid gap-2"><label for="package-apps">应用上限</label><InputNumber id="package-apps" v-model:value="packageForm.maxApps" :min="1" :max="2147483647" /></div>
          </div>
          <div class="grid gap-2">
            <label>套餐权限</label>
            <Button type="button"  class="justify-between" @click="permissionReturnsToPackageForm = true; permissionTarget = null; selectedPermissions = [...packageForm.permissionCodes]; search = ''; packageDialogVisible = false; permissionDialogVisible = true">
              <span>{{ packageForm.permissionCodes.length }} 项权限已选择</span><ShieldCheck data-icon="inline-end" />
            </Button>
          </div>
          <div>
            <Button type="button"  @click="packageDialogVisible = false">取消</Button>
            <Button type="submit" :disabled="submitting">{{ submitting ? '保存中...' : '保存套餐' }}</Button>
          </div>
        </form>
      </div>
    </Modal>

    <Modal v-model:open="permissionDialogVisible" :footer="null">
      <div class="max-w-3xl">
        <div>
          <h3>配置套餐权限{{ permissionTarget ? ` · ${permissionTarget.name}` : '' }}</h3>
        <p>按菜单层级选择权限；选择操作时会自动保留其父菜单，取消菜单会同时取消子菜单和关联操作。</p>
        </div>
        <div class="flex items-center gap-2"><Search class="text-muted-foreground" /><Input v-model:value="search" placeholder="搜索权限名称、编码或资源" /><Button type="text" size="icon" @click="search = ''"><X /></Button></div>
        <div class="h-[min(60vh,520px)] rounded-md border p-4">
          <div v-if="filteredPermissionTree.length === 0" class="py-12 text-center text-sm text-muted-foreground">暂无匹配权限</div>
          <Tree v-else :data="filteredPermissionTree" node-key="id">
            <template #default="{ data }">
              <div class="flex min-w-0 items-center gap-2 py-1">
                <Checkbox v-if="data.code" :checked="selectedPermissions.includes(data.code)" @update:checked="toggleTreePermission(data)" />
                <span :class="data.type === 'group' ? 'font-medium' : 'text-sm'">{{ data.label }}</span>
                <Tag v-if="data.type !== 'group'" :color="permissionTypeVariant(data.type)">{{ permissionTypeLabel(data.type) }}</Tag>
                <code v-if="data.code" class="ml-auto hidden truncate text-xs text-muted-foreground md:block">{{ data.code }}</code>
              </div>
            </template>
          </Tree>
        </div>
        <div>
          <span class="mr-auto text-sm text-muted-foreground">已选择 {{ selectedPermissions.length }} 项</span>
          <Button  @click="closePermissionDialog">取消</Button>
          <Button :disabled="permissionSubmitting" @click="submitPermissions">{{ permissionSubmitting ? '保存中...' : '保存权限' }}</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="deleteDialogVisible" :footer="null">
      <div>
        <div><h3>删除租户套餐</h3><p>确定删除“{{ deletingPackage?.name }}”吗？已分配给租户的套餐不能删除。</p></div>
        <div><Button>取消</Button><Button class="bg-destructive text-destructive-foreground hover:bg-destructive/90" @click="confirmDelete">确认删除</Button></div>
      </div>
    </Modal>
  </div>
</template>
