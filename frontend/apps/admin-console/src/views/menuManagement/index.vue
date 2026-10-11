<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AppWindow, FolderTree, LockKeyhole, RefreshCw, ShieldCheck } from '@lucide/vue'
import { authorizationApi } from '@/api/authorization'
import type { ManagementMenu } from '@/types/authorization'
import { message } from 'antdv-next'
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

const menuColumns = [
  { title: '菜单 / 权限', dataIndex: 'label', key: 'label', width: 360 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '作用域', dataIndex: 'scope', key: 'scope', width: 100 },
  { title: '编码', dataIndex: 'code', key: 'code', width: 260 },
  { title: '资源', dataIndex: 'resource', key: 'resource', width: 180 },
]

function scopeLabel(scope: string) {
  return scope.toUpperCase() === 'PLATFORM' ? '平台' : '用户池'
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
        <div class="flex items-center gap-2"><FolderTree class="size-5 text-[#0369A1]" /><h1 class="text-2xl font-bold">菜单管理</h1></div>
        <p class="mt-1 text-sm text-[#52677D]">查看管理端菜单权限、层级和关联操作；菜单的可见范围由用户池套餐授权决定。</p>
      </div>
      <div class="flex gap-2">
        <a-button  :disabled="loading" @click="loadCatalog"><RefreshCw class="size-4 mr-2" :class="loading ? 'animate-spin' : ''" />刷新</a-button>
        <a-button @click="router.push('/tenant-package')"><ShieldCheck />配置套餐权限</a-button>
      </div>
    </div>

    <a-card>
      <div class="gap-4 mb-3! flex">
        <div class="flex">
          <a-input v-model:value="search" class="w-64!" placeholder="搜索菜单名称、编码或资源" />
        </div>
        <a-button type="primary">搜索</a-button>
      </div>
      <div>
        <a-spin v-if="loading" class="flex min-h-48 items-center justify-center" />
        <a-empty v-else-if="filteredTree.length === 0" class="flex min-h-48 flex-col items-center justify-center" description="暂无匹配菜单" />
        <a-table
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
                <FolderTree v-if="item.type === 'directory'" class="size-4 shrink-0 text-[#0369A1]" />
                <AppWindow v-else-if="item.type === 'menu'" class="size-4 shrink-0 text-[#0369A1]" />
                <LockKeyhole v-else class="size-4 shrink-0 text-[#52677D]" />
                <span class="truncate font-medium">{{ item.label }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'type'">
              <a-tag :color="item.type === 'menu' ? 'blue' : item.type === 'directory' ? 'purple' : 'green'">{{ item.type === 'directory' ? '目录' : item.type === 'menu' ? '菜单' : '按钮' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'scope'">
              <a-tag v-if="item.type !== 'action'">{{ scopeLabel(item.scope) }}</a-tag>
              <span v-else class="text-[#52677D]">-</span>
            </template>
            <template v-else-if="column.key === 'code'">
              <code class="text-xs text-[#52677D]">{{ item.code }}</code>
            </template>
            <template v-else-if="column.key === 'resource'">
              <span class="text-sm text-[#52677D]">{{ item.resource || '-' }}</span>
            </template>
          </template>
        </a-table>
      </div>
    </a-card>
  </div>
</template>
