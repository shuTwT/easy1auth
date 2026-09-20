<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AppWindow, FolderTree, LockKeyhole, RefreshCw, Search, ShieldCheck } from '@lucide/vue'
import { authorizationApi } from '@/api/authorization'
import type { ManagementMenu } from '@/types/authorization'
import { Empty, Spin, Table as ATable, message } from 'antdv-next'
interface MenuTreeNode {
  id: string
  label: string
  code: string
  resource: string
  scope: string
  type: 'directory' | 'menu' | 'action'
  children: MenuTreeNode[]
}

const router = useRouter()
const loading = ref(false)
const search = ref('')
const menus = ref<ManagementMenu[]>([])
const permissions = ref<ManagementMenu[]>([])

const menuTree = computed<MenuTreeNode[]>(() => {
  const nodes = new Map<string, MenuTreeNode>()
  const roots: MenuTreeNode[] = []

  menus.value
    .slice()
    .sort((left, right) => left.sortOrder - right.sortOrder || left.code.localeCompare(right.code))
    .forEach((menu) => nodes.set(menu.code, {
      id: menu.code,
      label: menu.name,
      code: menu.code,
      resource: menu.resource,
      scope: menu.scope,
      type: menu.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
      children: [],
    }))

  menus.value.forEach((menu) => {
    const node = nodes.get(menu.code)!
    const parent = menu.parentCode ? nodes.get(menu.parentCode) : undefined
    if (parent) parent.children.push(node)
    else roots.push(node)
  })

  permissions.value
    .filter((permission) => permission.type.toUpperCase() === 'ACTION' && permission.parentCode && nodes.has(permission.parentCode))
    .sort((left, right) => left.sortOrder - right.sortOrder || left.code.localeCompare(right.code))
    .forEach((permission) => nodes.get(permission.parentCode!)!.children.push({
      id: permission.code,
      label: permission.name,
      code: permission.code,
      resource: permission.resource,
      scope: permission.scope,
      type: 'action',
      children: [],
    }))

  return roots
})

const filteredTree = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return menuTree.value
  const filter = (node: MenuTreeNode): MenuTreeNode | null => {
    const children = node.children.map(filter).filter((item): item is MenuTreeNode => item !== null)
    const matched = [node.label, node.code, node.resource].some((value) => value.toLowerCase().includes(keyword))
    return matched || children.length > 0 ? { ...node, children } : null
  }
  return menuTree.value.map(filter).filter((item): item is MenuTreeNode => item !== null)
})

const directoryCount = computed(() => menus.value.filter((menu) => menu.type.toUpperCase() === 'DIRECTORY').length)
const menuCount = computed(() => menus.value.filter((menu) => menu.type.toUpperCase() === 'MENU').length)
const assignedActionCount = computed(() => permissions.value.filter((permission) => permission.type.toUpperCase() === 'ACTION' && permission.parentCode).length)
const menuColumns = [
  { title: '菜单 / 权限', dataIndex: 'label', key: 'label', width: 360 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '作用域', dataIndex: 'scope', key: 'scope', width: 100 },
  { title: '编码', dataIndex: 'code', key: 'code', width: 260 },
  { title: '资源', dataIndex: 'resource', key: 'resource', width: 180 },
]

function scopeLabel(scope: string) {
  return scope.toUpperCase() === 'PLATFORM' ? '平台' : '租户'
}

async function loadCatalog() {
  loading.value = true
  try {
    const response = await authorizationApi.getMenuCatalog()
    menus.value = response.menus
    permissions.value = response.permissions
  } catch (error) {
    console.error('加载菜单权限目录失败:', error)
    message.error('加载菜单权限目录失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadCatalog)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)] space-y-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div>
        <div class="flex items-center gap-2"><FolderTree class="size-5 text-primary" /><h1 class="text-2xl font-bold">菜单管理</h1></div>
        <p class="mt-1 text-sm text-muted-foreground">查看管理端菜单权限、层级和关联操作；菜单的可见范围由租户套餐授权决定。</p>
      </div>
      <div class="flex gap-2">
        <Button  :disabled="loading" @click="loadCatalog"><RefreshCw class="size-4 mr-2" :class="loading ? 'animate-spin' : ''" />刷新</Button>
        <Button @click="router.push('/tenant-package')"><ShieldCheck />配置套餐权限</Button>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Card><div class="pb-3"><p>目录</p><h3 class="text-2xl">{{ directoryCount }}</h3></div><div class="flex items-center gap-2 text-xs text-muted-foreground"><FolderTree /> 仅承载层级，不对应路由</div></Card>
      <Card><div class="pb-3"><p>菜单</p><h3 class="text-2xl">{{ menuCount }}</h3></div><div class="flex items-center gap-2 text-xs text-muted-foreground"><AppWindow /> 对应一个管理端路由</div></Card>
      <Card><div class="pb-3"><p>权限目录</p><h3 class="text-2xl">{{ menus.length }}</h3></div><div class="flex items-center gap-2 text-xs text-muted-foreground"><ShieldCheck /> 平台和租户菜单节点</div></Card>
      <Card><div class="pb-3"><p>关联操作</p><h3 class="text-2xl">{{ assignedActionCount }}</h3></div><div class="flex items-center gap-2 text-xs text-muted-foreground"><LockKeyhole /> 菜单下的操作权限</div></Card>
    </div>

    <Card>
      <div class="gap-4 md:flex-row md:items-center md:justify-between">
        <div><h3>菜单权限树</h3><p>平台菜单仅在系统租户中出现；租户菜单按套餐的菜单权限动态显示。</p></div>
        <div class="relative w-full md:w-80"><Search class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" /><Input v-model:value="search" class="pl-9" placeholder="搜索菜单名称、编码或资源" /></div>
      </div>
      <div>
        <Spin v-if="loading" class="flex min-h-48 items-center justify-center" />
        <Empty v-else-if="filteredTree.length === 0" class="flex min-h-48 flex-col items-center justify-center" description="暂无匹配菜单" />
        <ATable
          v-else
          :columns="menuColumns"
          :data-source="filteredTree"
          :expandable="{ defaultExpandAllRows: true, indentSize: 20 }"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1000 }"
        >
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.key === 'label'">
              <div class="flex min-w-0 items-center gap-2">
                <FolderTree v-if="item.type === 'directory'" class="size-4 shrink-0 text-primary" />
                <AppWindow v-else-if="item.type === 'menu'" class="size-4 shrink-0 text-primary" />
                <LockKeyhole v-else class="size-4 shrink-0 text-muted-foreground" />
                <span class="truncate font-medium">{{ item.label }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'type'">
              <Tag :color="item.type === 'menu' ? 'blue' : item.type === 'directory' ? 'purple' : 'green'">{{ item.type === 'directory' ? '目录' : item.type === 'menu' ? '菜单' : '按钮' }}</Tag>
            </template>
            <template v-else-if="column.key === 'scope'">
              <Tag v-if="item.type !== 'action'">{{ scopeLabel(item.scope) }}</Tag>
              <span v-else class="text-muted-foreground">-</span>
            </template>
            <template v-else-if="column.key === 'code'">
              <code class="text-xs text-muted-foreground">{{ item.code }}</code>
            </template>
            <template v-else-if="column.key === 'resource'">
              <span class="text-sm text-muted-foreground">{{ item.resource || '-' }}</span>
            </template>
          </template>
        </ATable>
      </div>
    </Card>
  </div>
</template>
