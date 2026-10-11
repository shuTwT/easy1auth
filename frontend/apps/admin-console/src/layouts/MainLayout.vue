<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'antdv-next'
import { useUserStore } from '@/stores/user'
import { authorizationApi } from '@/api/authorization'
import { userPoolApi, type UserPoolPackageOption } from '@/api/userPool'
import type { ManagementMenu } from '@/types/authorization'
import {
  Bell,
  Building2,
  ChevronDown,
  Database,
  FolderTree,
  Globe2,
  LayoutDashboard,
  Link2,
  Lock,
  LockKeyhole,
  KeyRound,
  LogOut,
  Menu as MenuIcon,
  Monitor,
  Mail,
  Package,
  Palette,
  PanelLeftClose,
  ScrollText,
  ShieldCheck,
  UserCog,
  UserRound,
  Users,
  BriefcaseBusiness,
} from '@lucide/vue'

interface SidebarMenuItem {
  code: string
  title: string
  index: string
  type: 'directory' | 'menu'
  icon: Component
  sortOrder: number
  children: SidebarMenuItem[]
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const authorizedMenus = ref<ManagementMenu[]>([])
const menuLoading = ref(false)
const collapsed = ref(false)
const mobileOpen = ref(false)
const mobile = ref(false)
const openKeys = ref<string[]>([])
const createUserPoolDialogOpen = ref(false)
const createUserPoolSubmitting = ref(false)
const createUserPoolName = ref('')
const createUserPoolPackageId = ref<number | undefined>()
const createUserPoolPackages = ref<UserPoolPackageOption[]>([])

const menuIcons: Record<string, Component> = {
  dashboard: LayoutDashboard,
  'user-management': Users,
  user: Users,
  'user-group': Users,
  position: BriefcaseBusiness,
  role: ShieldCheck,
  'permission-management': LockKeyhole,
  'permission-management/permission-space': FolderTree,
  'permission-management/permission-data-resource': Database,
  'admin-user': UserCog,
  application: Monitor,
  'identity-source-management': KeyRound,
  'identity-source-management/social-identity-source': Link2,
  'identity-source-management/enterprise-identity-source': Building2,
  'identity-source-management/sync-identity-source': Building2,
  'user-pool': Users,
  'user-pool-package': Package,
  'pool-tenants': Building2,
  'menu-management': MenuIcon,
  'brand-management': Palette,
  'brand-login-style': Palette,
  'message-service': Mail,
  'custom-domain': Globe2,
  security: Lock,
  audit: ScrollText,
}

const menuResourceRoutes: Record<string, string> = {
  group: 'user-group',
  tenant: 'user-pool',
  'tenant-package': 'user-pool-package',
  'pool-tenant': 'pool-tenants',
  'permission-space': 'permission-management/permission-space',
  'permission-data-resource': 'permission-management/permission-data-resource',
  'social-identity-source': 'identity-source-management/social-identity-source',
  'enterprise-identity-source': 'identity-source-management/enterprise-identity-source',
  'sync-identity-source': 'identity-source-management/sync-identity-source',
}

const menuTree = computed(() => {
  const nodes = new Map<string, SidebarMenuItem>()
  const roots: SidebarMenuItem[] = []
  authorizedMenus.value
    .filter(menu => menu.active && ['DIRECTORY', 'MENU'].includes(menu.type.toUpperCase()))
    .sort((left, right) => left.sortOrder - right.sortOrder || left.code.localeCompare(right.code))
    .forEach(menu => nodes.set(menu.code, {
      code: menu.code,
      title: menu.name,
      index: menu.type.toUpperCase() === 'DIRECTORY' ? '' : `/${menuResourceRoutes[menu.resource] || menu.resource}`,
      type: menu.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
      icon: menuIcons[menuResourceRoutes[menu.resource] || menu.resource] || MenuIcon,
      sortOrder: menu.sortOrder,
      children: [],
    }))
  authorizedMenus.value.filter(menu => nodes.has(menu.code)).forEach(menu => {
    const item = nodes.get(menu.code)!
    const parent = menu.parentCode ? nodes.get(menu.parentCode) : undefined
    if (parent) parent.children.push(item)
    else roots.push(item)
  })
  const sortItems = (items: SidebarMenuItem[]) => {
    items.sort((left, right) => left.sortOrder - right.sortOrder || left.title.localeCompare(right.title))
    items.forEach(item => sortItems(item.children))
    return items
  }
  return sortItems(roots)
})

const menuItems = computed<any[]>(() => {
  const toItem = (item: SidebarMenuItem): any => ({
    key: item.index || item.code,
    label: item.title,
    icon: h(item.icon, { size: 16 }),
    title: item.title,
    children: item.children.length ? item.children.map(toItem) : undefined,
  })
  return menuTree.value.map(toItem)
})

const selectedKeys = computed(() => [typeof route.meta.activeMenu === 'string' ? route.meta.activeMenu : route.path])
const userPoolItems = computed(() => [
  ...userStore.userPools.map(tenant => ({
    key: tenant.id,
    label: `${tenant.name}${tenant.userPoolPackage?.name ? ` · ${tenant.userPoolPackage.name}` : ''}`,
  })),
  { type: 'divider' as const },
  { key: 'create-tenant', label: '新建用户池' },
])
const profileItems: any[] = [
  { key: 'profile', label: '个人中心', icon: h(UserRound, { size: 16 }) },
  { type: 'divider' },
  { key: 'logout', label: '退出登录', danger: true, icon: h(LogOut, { size: 16 }) },
]
const currentTitle = computed(() => route.meta.title as string || '首页')

function updateMobile() {
  mobile.value = window.innerWidth < 992
  if (!mobile.value) mobileOpen.value = false
}

function onMenuClick({ key }: { key: string }) {
  if (key.startsWith('/')) {
    router.push(key)
    mobileOpen.value = false
  }
}

function onUserPoolClick({ key }: { key: string }) {
  if (key === 'create-tenant') {
    openCreateUserPoolDialog()
    return
  }
  const tenant = userStore.userPools.find(item => item.id === key)
  if (tenant) userStore.setCurrentUserPool(tenant)
}

async function openCreateUserPoolDialog() {
  createUserPoolName.value = ''
  createUserPoolPackageId.value = undefined
  createUserPoolDialogOpen.value = true
  try {
    createUserPoolPackages.value = await userPoolApi.getAvailablePackages()
    createUserPoolPackageId.value = createUserPoolPackages.value.find(item => item.id > 0)?.id
  } catch (error) {
    console.error('加载可用用户池套餐失败:', error)
    message.error('加载可用用户池套餐失败')
  }
}

async function submitCreateUserPool() {
  if (!createUserPoolName.value.trim()) {
    message.warning('请输入用户池名称')
    return
  }
  if (!createUserPoolPackageId.value) {
    message.warning('请选择用户池套餐')
    return
  }
  createUserPoolSubmitting.value = true
  try {
    const tenant = await userPoolApi.createUserPool({
      name: createUserPoolName.value.trim(),
      packageId: createUserPoolPackageId.value,
    })
    userStore.setUserPools([...userStore.userPools, tenant])
    userStore.setCurrentUserPool(tenant)
    createUserPoolDialogOpen.value = false
    message.success('用户池创建成功')
  } catch (error) {
    console.error('创建用户池失败:', error)
    message.error('创建用户池失败')
  } finally {
    createUserPoolSubmitting.value = false
  }
}

function onProfileClick({ key }: { key: string }) {
  if (key === 'profile') {
    router.push('/profile')
    mobileOpen.value = false
  } else if (key === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

async function loadAuthorizedMenus() {
  if (!userStore.currentUserPool?.id) {
    authorizedMenus.value = []
    return
  }
  menuLoading.value = true
  try {
    authorizedMenus.value = (await authorizationApi.getContext()).menus
  } catch (error) {
    console.error('加载当前用户池菜单失败:', error)
    message.error('加载当前用户池菜单失败')
    authorizedMenus.value = []
  } finally {
    menuLoading.value = false
  }
}

onMounted(() => {
  updateMobile()
  window.addEventListener('resize', updateMobile)
})

onBeforeUnmount(() => window.removeEventListener('resize', updateMobile))
watch(() => userStore.currentUserPool?.id, loadAuthorizedMenus, { immediate: true })
</script>

<template>
  <a-layout class="app-shell bg-slate-100">
    <a-layout-sider
      v-if="!mobile"
      v-model:collapsed="collapsed"
      :width="240"
      :collapsed-width="80"
      collapsible
      :trigger="null"
      theme="dark"
      :styles="{ body: { display: 'flex', flexDirection: 'column', minHeight: 0, height: '100%' } }"
      class="!bg-white"
    >
      <div class="flex h-14 shrink-0 items-center gap-2 px-4" :class="collapsed ? 'justify-center' : ''">
        <div class="flex size-7 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-sky-700 to-sky-400 ">✓</div>
        <span v-if="!collapsed" class="text-base font-semibold tracking-wide">Easy1Auth</span>
      </div>
      <div v-if="!collapsed" class="shrink-0 px-3 pb-3">
        <a-dropdown :menu="{ items: userPoolItems, onClick: onUserPoolClick }" :trigger="['click']">
          <button class="flex w-full items-center gap-2 rounded-md bg-white/5 px-3 py-2 text-sm  hover:bg-white/10 ">
            <Building2 :size="16" /><span class="flex-1 truncate text-left">{{ userStore.currentUserPool?.name || '选择用户池' }}</span><ChevronDown :size="14" />
          </button>
        </a-dropdown>
      </div>
      <div class="sidebar-menu-scroll min-h-0 flex-1 overflow-y-auto">
        <div v-if="menuLoading" class="p-6 text-center text-slate-400"><a-spin size="small" /> <span class="ml-2 text-xs">正在加载菜单…</span></div>
        <a-menu
          v-else
          v-model:open-keys="openKeys"
          :selected-keys="selectedKeys"
          :items="menuItems"
          mode="inline"
          :inline-collapsed="collapsed"
          class="!border-e-0"
          @click="onMenuClick"
        />
      </div>
      <div class="shrink-0 border-t border-white/10 p-3">
        <a-dropdown :menu="{ items: profileItems, onClick: onProfileClick }" :trigger="['click']" placement="top">
          <button class="flex w-full items-center gap-3 rounded-md p-2 text-left">
            <a-avatar class="shrink-0 bg-sky-600">{{ (userStore.userInfo?.username || '管理员').charAt(0).toUpperCase() }}</a-avatar>
            <span v-if="!collapsed" class="min-w-0 flex-1 truncate text-sm">{{ userStore.userInfo?.username || '管理员' }}</span>
          </button>
        </a-dropdown>
        <a-button type="text" class="!mt-2 !w-full" @click="collapsed = !collapsed"><PanelLeftClose :size="16" /></a-button>
      </div>
    </a-layout-sider>

      <a-drawer v-model:open="mobileOpen" title="Easy1Auth" placement="left" :size="280" :styles="{ body: { padding: 0 } }">
      <div class="p-3"><a-dropdown :menu="{ items: userPoolItems, onClick: onUserPoolClick }" :trigger="['click']"><a-button block><Building2 :size="16" class="mr-2" />{{ userStore.currentUserPool?.name || '选择用户池' }}</a-button></a-dropdown></div>
      <a-menu :selected-keys="selectedKeys" :items="menuItems" mode="inline" @click="onMenuClick" />
    </a-drawer>

    <a-layout class="main-layout">
      <a-layout-header class="!bg-white !flex !h-14 !items-center !justify-between !border-b !border-slate-200 !px-4 !shadow-sm">
        <div class="flex items-center gap-2 text-sm font-medium text-slate-700">
          <a-button v-if="mobile" type="text" @click="mobileOpen = true"><MenuIcon :size="18" /></a-button>
          <span>{{ currentTitle }}</span>
        </div>
        <div class="flex items-center gap-1">
          <a-badge :count="3" size="small"><a-button type="text" aria-label="通知"><Bell :size="18" /></a-button></a-badge>
          <a-dropdown v-if="mobile" :menu="{ items: profileItems, onClick: onProfileClick }" :trigger="['click']" placement="bottomRight">
            <button class="ml-1 flex cursor-pointer items-center rounded-full p-0.5 transition-colors hover:bg-slate-100" aria-label="打开账户菜单">
              <a-avatar class="bg-sky-600">{{ (userStore.userInfo?.username || '管理员').charAt(0).toUpperCase() }}</a-avatar>
            </button>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="page-content overflow-auto bg-slate-50/50">
        <router-view v-slot="{ Component }"><transition name="slide-fade" mode="out-in"><component :is="Component" /></transition></router-view>
      </a-layout-content>
    </a-layout>

    <a-modal v-model:open="createUserPoolDialogOpen" title="新建用户池" :footer="null">
      <form class="grid gap-4" @submit.prevent="submitCreateUserPool">
        <div class="grid gap-2">
          <label for="create-tenant-name">用户池名称</label>
          <a-input id="create-tenant-name" v-model:value="createUserPoolName" placeholder="请输入用户池名称" />
        </div>
        <div class="grid gap-2">
          <label for="create-tenant-package">用户池套餐</label>
          <a-select
            id="create-tenant-package"
            v-model:value="createUserPoolPackageId"
            class="w-full"
            placeholder="请选择用户池套餐"
            :options="createUserPoolPackages.map(item => ({ value: item.id, label: `${item.name} · ${item.maxUsers.toLocaleString()} 用户 / ${item.maxApps.toLocaleString()} 应用` }))"
          />
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <a-button html-type="button" @click="createUserPoolDialogOpen = false">取消</a-button>
          <a-button html-type="submit" :disabled="createUserPoolSubmitting">{{ createUserPoolSubmitting ? '创建中...' : '创建用户池' }}</a-button>
        </div>
      </form>
    </a-modal>
  </a-layout>
</template>

<style scoped>
.slide-fade-enter-active { transition: all .25s ease; }
.slide-fade-leave-active { transition: all .15s ease; }
.slide-fade-enter-from { transform: translateX(10px); opacity: 0; }
.slide-fade-leave-to { transform: translateX(-10px); opacity: 0; }

.app-shell {
  min-height: 100vh;
  min-height: 100dvh;
  height: 100vh;
  height: 100dvh;
}

.main-layout,
.page-content {
  min-height: 0;
}

.main-layout {
  min-width: 0;
}

.sidebar-menu-scroll {
  scrollbar-color: rgba(148, 163, 184, 0.42) transparent;
  scrollbar-width: thin;
}

.sidebar-menu-scroll::-webkit-scrollbar {
  width: 6px;
}

.sidebar-menu-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu-scroll::-webkit-scrollbar-thumb {
  border: 1px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
  background-color: rgba(148, 163, 184, 0.42);
  transition: background-color 150ms ease-out;
}

.sidebar-menu-scroll:hover::-webkit-scrollbar-thumb {
  background-color: rgba(186, 230, 253, 0.68);
}
</style>
