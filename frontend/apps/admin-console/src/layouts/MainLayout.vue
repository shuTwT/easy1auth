<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Avatar,
  Badge,
  Button,
  Drawer,
  Dropdown,
  Layout,
  LayoutContent,
  LayoutHeader,
  LayoutSider,
  Menu,
  Modal,
  Input,
  Select,
  Spin,
  message,
} from 'antdv-next'
import { useUserStore } from '@/stores/user'
import { authorizationApi } from '@/api/authorization'
import { brandSettingsApi } from '@/api/brandSettings'
import { tenantApi, type TenantPackageOption } from '@/api/tenant'
import { setAdminTheme } from '@/config/antd'
import type { ManagementMenu } from '@/types/authorization'
import {
  Bell,
  Building2,
  ChevronDown,
  LayoutDashboard,
  Link2,
  Lock,
  LockKeyhole,
  KeyRound,
  LogOut,
  Menu as MenuIcon,
  Monitor,
  Package,
  Palette,
  PanelLeftClose,
  ScrollText,
  ShieldCheck,
  UserCog,
  UserRound,
  Users,
  BriefcaseBusiness,
  WandSparkles,
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
const createTenantDialogOpen = ref(false)
const createTenantSubmitting = ref(false)
const createTenantName = ref('')
const createTenantPackageId = ref<number | undefined>()
const createTenantPackages = ref<TenantPackageOption[]>([])

const menuIcons: Record<string, Component> = {
  dashboard: LayoutDashboard,
  'user-management': Users,
  user: Users,
  group: Users,
  position: BriefcaseBusiness,
  role: ShieldCheck,
  permission: LockKeyhole,
  'admin-user': UserCog,
  application: Monitor,
  'social-identity-provider': Link2,
  'enterprise-identity-source': Building2,
  'identity-source-management': KeyRound,
  tenant: Building2,
  'tenant-package': Package,
  'menu-management': MenuIcon,
  'brand-settings': Palette,
  security: Lock,
  personalization: WandSparkles,
  audit: ScrollText,
}

const menuGroups = computed(() => {
  const nodes = new Map<string, SidebarMenuItem>()
  const roots: Array<{ scope: string; item: SidebarMenuItem }> = []
  authorizedMenus.value
    .filter(menu => menu.active && ['DIRECTORY', 'MENU'].includes(menu.type.toUpperCase()))
    .sort((left, right) => left.sortOrder - right.sortOrder || left.code.localeCompare(right.code))
    .forEach(menu => nodes.set(menu.code, {
      code: menu.code,
      title: menu.name,
      index: menu.type.toUpperCase() === 'DIRECTORY' ? '' : `/${menu.resource}`,
      type: menu.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
      icon: menuIcons[menu.resource] || MenuIcon,
      sortOrder: menu.sortOrder,
      children: [],
    }))
  authorizedMenus.value.filter(menu => nodes.has(menu.code)).forEach(menu => {
    const item = nodes.get(menu.code)!
    const parent = menu.parentCode ? nodes.get(menu.parentCode) : undefined
    if (parent) parent.children.push(item)
    else roots.push({ scope: menu.scope, item })
  })
  const sortItems = (items: SidebarMenuItem[]) => {
    items.sort((left, right) => left.sortOrder - right.sortOrder || left.title.localeCompare(right.title))
    items.forEach(item => sortItems(item.children))
    return items
  }
  const groups = new Map<string, SidebarMenuItem[]>()
  roots.forEach(({ scope, item }) => {
    const label = scope.toUpperCase() === 'PLATFORM' ? '平台管理' : '租户管理'
    groups.set(label, [...(groups.get(label) || []), item])
  })
  return [...groups.entries()].map(([label, items]) => ({ label, items: sortItems(items) }))
})

const menuItems = computed<any[]>(() => {
  const toItem = (item: SidebarMenuItem): any => ({
    key: item.index || item.code,
    label: item.title,
    icon: h(item.icon, { size: 16 }),
    title: item.title,
    children: item.children.length ? item.children.map(toItem) : undefined,
  })
  return menuGroups.value.map(group => ({ type: 'group', label: group.label, children: group.items.map(toItem) }))
})

const selectedKeys = computed(() => [route.path])
const tenantItems = computed(() => [
  ...userStore.tenants.map(tenant => ({
    key: tenant.id,
    label: `${tenant.name}${tenant.tenantPackage?.name ? ` · ${tenant.tenantPackage.name}` : ''}`,
  })),
  { type: 'divider' as const },
  { key: 'create-tenant', label: '新建租户' },
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

function onTenantClick({ key }: { key: string }) {
  if (key === 'create-tenant') {
    openCreateTenantDialog()
    return
  }
  const tenant = userStore.tenants.find(item => item.id === key)
  if (tenant) userStore.setCurrentTenant(tenant)
}

async function openCreateTenantDialog() {
  createTenantName.value = ''
  createTenantPackageId.value = undefined
  createTenantDialogOpen.value = true
  try {
    createTenantPackages.value = await tenantApi.getAvailablePackages()
    createTenantPackageId.value = createTenantPackages.value.find(item => item.id > 0)?.id
  } catch (error) {
    console.error('加载可用租户套餐失败:', error)
  }
}

async function submitCreateTenant() {
  if (!createTenantName.value.trim()) {
    message.warning('请输入租户名称')
    return
  }
  if (!createTenantPackageId.value) {
    message.warning('请选择租户套餐')
    return
  }
  createTenantSubmitting.value = true
  try {
    const tenant = await tenantApi.createTenant({
      name: createTenantName.value.trim(),
      packageId: createTenantPackageId.value,
    })
    userStore.setTenants([...userStore.tenants, tenant])
    userStore.setCurrentTenant(tenant)
    createTenantDialogOpen.value = false
    message.success('租户创建成功')
  } catch (error) {
    console.error('创建租户失败:', error)
  } finally {
    createTenantSubmitting.value = false
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
  if (!userStore.currentTenant?.id) {
    authorizedMenus.value = []
    return
  }
  menuLoading.value = true
  try {
    authorizedMenus.value = (await authorizationApi.getContext()).menus
  } catch (error) {
    console.error('加载当前租户菜单失败:', error)
    authorizedMenus.value = []
  } finally {
    menuLoading.value = false
  }
}

onMounted(async () => {
  updateMobile()
  window.addEventListener('resize', updateMobile)
  try {
    const response = await brandSettingsApi.get()
    const panel = (response as any).brandSettings?.adminPanel
    setAdminTheme(panel)
    if (panel?.customCSS) {
      const style = document.getElementById('custom-admin-styles') || document.head.appendChild(Object.assign(document.createElement('style'), { id: 'custom-admin-styles' }))
      style.textContent = panel.customCSS
    }
  } catch (error) {
    console.error('加载品牌设置失败:', error)
  }
})

onBeforeUnmount(() => window.removeEventListener('resize', updateMobile))
watch(() => userStore.currentTenant?.id, loadAuthorizedMenus, { immediate: true })
</script>

<template>
  <Layout class="app-shell bg-slate-100">
    <LayoutSider
      v-if="!mobile"
      v-model:collapsed="collapsed"
      :width="240"
      :collapsed-width="80"
      collapsible
      :trigger="null"
      theme="dark"
      :styles="{ body: { display: 'flex', flexDirection: 'column', minHeight: 0, height: '100%' } }"
      class="!bg-slate-950/95"
    >
      <div class="flex h-14 shrink-0 items-center gap-2 px-4 text-white" :class="collapsed ? 'justify-center' : ''">
        <div class="flex size-7 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-sky-700 to-sky-400 text-white">✓</div>
        <span v-if="!collapsed" class="text-base font-semibold tracking-wide">Easy1Auth</span>
      </div>
      <div v-if="!collapsed" class="shrink-0 px-3 pb-3">
        <Dropdown :menu="{ items: tenantItems, onClick: onTenantClick }" :trigger="['click']">
          <button class="flex w-full items-center gap-2 rounded-md bg-white/5 px-3 py-2 text-sm text-slate-300 hover:bg-white/10 hover:text-white">
            <Building2 :size="16" /><span class="flex-1 truncate text-left">{{ userStore.currentTenant?.name || '选择租户' }}</span><ChevronDown :size="14" />
          </button>
        </Dropdown>
      </div>
      <div class="min-h-0 flex-1 overflow-y-auto">
        <div v-if="menuLoading" class="p-6 text-center text-slate-400"><Spin size="small" /> <span class="ml-2 text-xs">正在加载菜单…</span></div>
        <Menu
          v-else
          v-model:open-keys="openKeys"
          :selected-keys="selectedKeys"
          :items="menuItems"
          mode="inline"
          theme="dark"
          :inline-collapsed="collapsed"
          class="!border-e-0 !bg-transparent"
          @click="onMenuClick"
        />
      </div>
      <div class="shrink-0 border-t border-white/10 p-3">
        <Dropdown :menu="{ items: profileItems, onClick: onProfileClick }" :trigger="['click']" placement="top">
          <button class="flex w-full items-center gap-3 rounded-md p-2 text-left hover:bg-white/5">
            <Avatar class="shrink-0 bg-sky-600">{{ (userStore.userInfo?.username || '管理员').charAt(0).toUpperCase() }}</Avatar>
            <span v-if="!collapsed" class="min-w-0 flex-1 truncate text-sm text-slate-200">{{ userStore.userInfo?.username || '管理员' }}</span>
          </button>
        </Dropdown>
        <Button type="text" class="!mt-2 !w-full !text-slate-400 hover:!text-white" @click="collapsed = !collapsed"><PanelLeftClose :size="16" /></Button>
      </div>
    </LayoutSider>

      <Drawer v-model:open="mobileOpen" title="Easy1Auth" placement="left" :size="280" :styles="{ body: { padding: 0 } }">
      <div class="p-3"><Dropdown :menu="{ items: tenantItems, onClick: onTenantClick }" :trigger="['click']"><Button block><Building2 :size="16" class="mr-2" />{{ userStore.currentTenant?.name || '选择租户' }}</Button></Dropdown></div>
      <Menu :selected-keys="selectedKeys" :items="menuItems" mode="inline" @click="onMenuClick" />
    </Drawer>

    <Layout class="main-layout">
      <LayoutHeader class="!flex !h-14 !items-center !justify-between !border-b !border-slate-200 !px-4 !shadow-sm">
        <div class="flex items-center gap-2 text-sm font-medium text-slate-700">
          <Button v-if="mobile" type="text" @click="mobileOpen = true"><MenuIcon :size="18" /></Button>
          <span>{{ currentTitle }}</span>
        </div>
        <div class="flex items-center gap-1">
          <Badge :count="3" size="small"><Button type="text" aria-label="通知"><Bell :size="18" /></Button></Badge>
          <Dropdown v-if="mobile" :menu="{ items: profileItems, onClick: onProfileClick }" :trigger="['click']" placement="bottomRight">
            <button class="ml-1 flex cursor-pointer items-center rounded-full p-0.5 transition-colors hover:bg-slate-100" aria-label="打开账户菜单">
              <Avatar class="bg-sky-600">{{ (userStore.userInfo?.username || '管理员').charAt(0).toUpperCase() }}</Avatar>
            </button>
          </Dropdown>
        </div>
      </LayoutHeader>
      <LayoutContent class="page-content overflow-auto bg-slate-50/50">
        <router-view v-slot="{ Component }"><transition name="slide-fade" mode="out-in"><component :is="Component" /></transition></router-view>
      </LayoutContent>
    </Layout>

    <Modal v-model:open="createTenantDialogOpen" title="新建租户" :footer="null">
      <form class="grid gap-4" @submit.prevent="submitCreateTenant">
        <div class="grid gap-2">
          <label for="create-tenant-name">租户名称</label>
          <Input id="create-tenant-name" v-model:value="createTenantName" placeholder="请输入租户名称" />
        </div>
        <div class="grid gap-2">
          <label for="create-tenant-package">租户套餐</label>
          <Select
            id="create-tenant-package"
            v-model:value="createTenantPackageId"
            class="w-full"
            placeholder="请选择租户套餐"
            :options="createTenantPackages.map(item => ({ value: item.id, label: `${item.name} · ${item.maxUsers.toLocaleString()} 用户 / ${item.maxApps.toLocaleString()} 应用` }))"
          />
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <Button html-type="button" @click="createTenantDialogOpen = false">取消</Button>
          <Button html-type="submit" :disabled="createTenantSubmitting">{{ createTenantSubmitting ? '创建中...' : '创建租户' }}</Button>
        </div>
      </form>
    </Modal>
  </Layout>
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
</style>
