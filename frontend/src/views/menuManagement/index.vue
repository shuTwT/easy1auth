<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AppWindow, FolderTree, LockKeyhole, RefreshCw, Search, ShieldCheck } from '@lucide/vue'
import { authorizationApi } from '@/api/authorization'
import type { ManagementMenu } from '@/types/authorization'
import type { TreeNodeData } from '@/components/ui/tree'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Tree } from '@/components/ui/tree'

interface MenuTreeNode extends TreeNodeData {
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
  } finally {
    loading.value = false
  }
}

onMounted(loadCatalog)
</script>

<template>
  <div class="flex flex-col gap-6 p-5 lg:p-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div>
        <div class="flex items-center gap-2"><FolderTree class="text-primary" /><h1 class="text-2xl font-semibold tracking-tight">菜单管理</h1></div>
        <p class="mt-1 text-sm text-muted-foreground">查看管理端菜单权限、层级和关联操作；菜单的可见范围由租户套餐授权决定。</p>
      </div>
      <div class="flex gap-2">
        <Button variant="outline" :disabled="loading" @click="loadCatalog"><RefreshCw data-icon="inline-start" :class="loading ? 'animate-spin' : ''" />刷新</Button>
        <Button @click="router.push('/tenant-package')"><ShieldCheck data-icon="inline-start" />配置套餐权限</Button>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Card><CardHeader class="pb-3"><CardDescription>目录</CardDescription><CardTitle class="text-2xl">{{ directoryCount }}</CardTitle></CardHeader><CardContent class="flex items-center gap-2 text-xs text-muted-foreground"><FolderTree /> 仅承载层级，不对应路由</CardContent></Card>
      <Card><CardHeader class="pb-3"><CardDescription>菜单</CardDescription><CardTitle class="text-2xl">{{ menuCount }}</CardTitle></CardHeader><CardContent class="flex items-center gap-2 text-xs text-muted-foreground"><AppWindow /> 对应一个管理端路由</CardContent></Card>
      <Card><CardHeader class="pb-3"><CardDescription>权限目录</CardDescription><CardTitle class="text-2xl">{{ menus.length }}</CardTitle></CardHeader><CardContent class="flex items-center gap-2 text-xs text-muted-foreground"><ShieldCheck /> 平台和租户菜单节点</CardContent></Card>
      <Card><CardHeader class="pb-3"><CardDescription>关联操作</CardDescription><CardTitle class="text-2xl">{{ assignedActionCount }}</CardTitle></CardHeader><CardContent class="flex items-center gap-2 text-xs text-muted-foreground"><LockKeyhole /> 菜单下的操作权限</CardContent></Card>
    </div>

    <Card>
      <CardHeader class="gap-4 md:flex-row md:items-center md:justify-between">
        <div><CardTitle>菜单权限树</CardTitle><CardDescription>平台菜单仅在系统租户中出现；租户菜单按套餐的菜单权限动态显示。</CardDescription></div>
        <div class="relative w-full md:w-80"><Search class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" /><Input v-model="search" class="pl-9" placeholder="搜索菜单名称、编码或资源" /></div>
      </CardHeader>
      <CardContent>
        <div v-if="loading" class="flex min-h-48 items-center justify-center text-sm text-muted-foreground">加载中...</div>
        <div v-else-if="filteredTree.length === 0" class="flex min-h-48 items-center justify-center text-sm text-muted-foreground">暂无匹配菜单</div>
        <Tree v-else :data="filteredTree" node-key="id" class="rounded-md border p-3">
          <template #default="{ data }">
            <div class="flex min-w-0 items-center gap-2 py-1">
              <FolderTree v-if="data.type === 'directory'" class="shrink-0 text-primary" />
              <AppWindow v-else-if="data.type === 'menu'" class="shrink-0 text-primary" />
              <LockKeyhole v-else class="shrink-0 text-muted-foreground" />
              <span class="truncate text-sm font-medium">{{ data.label }}</span>
              <Badge :variant="data.type === 'menu' ? 'default' : data.type === 'directory' ? 'secondary' : 'outline'">{{ data.type === 'directory' ? '目录' : data.type === 'menu' ? '菜单' : '按钮' }}</Badge>
              <Badge v-if="data.type !== 'action'" variant="outline">{{ scopeLabel(data.scope) }}</Badge>
              <code class="ml-auto hidden truncate text-xs text-muted-foreground md:block">{{ data.code }}</code>
            </div>
          </template>
        </Tree>
      </CardContent>
    </Card>
  </div>
</template>
