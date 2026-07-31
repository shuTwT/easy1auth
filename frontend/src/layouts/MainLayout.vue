<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import type { Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { brandSettingsApi } from '@/api/brandSettings'
import { authorizationApi } from '@/api/authorization'
import type { ManagementMenu } from '@/types/authorization'

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  LayoutDashboard,
  Building2,
  Users,
  BriefcaseBusiness,
  ShieldCheck,
  UserCog,
  LockKeyhole,
  Monitor,
  Link2,
  Palette,
  Lock,
  Package,
  WandSparkles,
  ScrollText,
  House,
  Bell,
  LogOut,
  ChevronRight,
  PanelLeftClose,
  User,
  Settings,
  ChevronDown,
  Menu,
} from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const brandSettings = ref<any>(null)
const authorizedMenus = ref<ManagementMenu[]>([])
const menuLoading = ref(false)

interface SidebarMenuItem {
  code: string
  title: string
  index: string
  type: 'directory' | 'menu'
  icon: Component
  sortOrder: number
  children: SidebarMenuItem[]
}

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
  tenant: Building2,
  'tenant-package': Package,
  'menu-management': Menu,
  'brand-settings': Palette,
  security: Lock,
  personalization: WandSparkles,
  audit: ScrollText,
}

const menuGroups = computed(() => {
  const nodes = new Map<string, SidebarMenuItem>()
  const roots: Array<{ scope: string; item: SidebarMenuItem }> = []

  authorizedMenus.value
    .filter((menu) => menu.active && ['DIRECTORY', 'MENU'].includes(menu.type.toUpperCase()))
    .sort((left, right) => left.sortOrder - right.sortOrder || left.code.localeCompare(right.code))
    .forEach((menu) => nodes.set(menu.code, {
      code: menu.code,
      title: menu.name,
      index: menu.type.toUpperCase() === 'DIRECTORY' ? '' : `/${menu.resource}`,
      type: menu.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
      icon: menuIcons[menu.resource] || Menu,
      sortOrder: menu.sortOrder,
      children: [],
    }))

  authorizedMenus.value
    .filter((menu) => nodes.has(menu.code))
    .forEach((menu) => {
      const item = nodes.get(menu.code)!
      const parent = menu.parentCode ? nodes.get(menu.parentCode) : undefined
      if (parent) {
        parent.children.push(item)
      } else {
        roots.push({ scope: menu.scope, item })
      }
    })

  const sortItems = (items: SidebarMenuItem[]) => {
    items.sort((left, right) => left.sortOrder - right.sortOrder || left.title.localeCompare(right.title))
    items.forEach((item) => sortItems(item.children))
    return items
  }

  const groups = new Map<string, SidebarMenuItem[]>()
  roots.forEach(({ scope, item }) => {
    const label = scope.toUpperCase() === 'PLATFORM' ? '平台管理' : '租户管理'
    groups.set(label, [...(groups.get(label) || []), item])
  })
  return [...groups.entries()].map(([label, items]) => ({ label, items: sortItems(items) }))
})

const flattenedMenus = computed(() => {
  const flatten = (items: SidebarMenuItem[]): SidebarMenuItem[] => items.flatMap((item) => [item, ...flatten(item.children)])
  return menuGroups.value.flatMap((group) => flatten(group.items))
})

const handleSelect = (index: string) => {
  router.push(index)
}

const isActive = (path: string) => {
  if (path === '/application') {
    return route.path === '/application' || /^\/application\/[^/]+$/.test(route.path)
  }

  return route.path === path
}

const isChildActive = (children: { index: string }[]) =>
  children.some((child) => isActive(child.index))

const currentMenuTitle = computed(() => {
  if (/^\/application\/[^/]+$/.test(route.path)) {
    return (route.meta.title as string | undefined) ?? '应用详情'
  }

  return flattenedMenus.value.find((item) => item.index === route.path)?.title
    ?? (route.meta.title as string | undefined)
    ?? '首页'
})

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

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  const root = document.documentElement

  try {
    const response = await brandSettingsApi.get()
    brandSettings.value = (response as any).brandSettings
    const adminPanel = brandSettings.value?.adminPanel
    if (adminPanel) {
      if (adminPanel.primaryColor) {
        root.style.setProperty('--primary-color', adminPanel.primaryColor)
        root.style.setProperty('--sidebar-primary', adminPanel.primaryColor)
      }
      if (adminPanel.sidebarColor) {
        root.style.setProperty('--sidebar-bg', adminPanel.sidebarColor)
        root.style.setProperty('--sidebar', adminPanel.sidebarColor)
      }
      if (adminPanel.headerColor) {
        root.style.setProperty('--header-bg', adminPanel.headerColor)
      }
      if (adminPanel.customCSS) {
        let styleElement = document.getElementById('custom-admin-styles')
        if (!styleElement) {
          styleElement = document.createElement('style')
          styleElement.id = 'custom-admin-styles'
          document.head.appendChild(styleElement)
        }
        styleElement.textContent = adminPanel.customCSS
      }
    }
  } catch (error) {
    console.error('加载品牌设置失败:', error)
  }
})

watch(() => userStore.currentTenant?.id, loadAuthorizedMenus, { immediate: true })
</script>

<template>
  <SidebarProvider>
    <Sidebar collapsible="icon" variant="sidebar" class="bg-slate-950/95 backdrop-blur-sm">
      <!-- Sidebar Header: Logo + Tenant Switcher -->
      <SidebarHeader class="p-0">
        <div class="flex flex-col border-b border-white/10">
          <!-- Logo Area -->
          <div class="flex h-14 items-center px-4 group-data-[collapsible=icon]:justify-center">
            <div v-if="brandSettings?.adminPanel?.logo" class="flex items-center justify-center">
              <img
                :src="brandSettings.adminPanel.logo"
                class="h-7 w-auto"
              />
            </div>
            <div v-else class="flex items-center gap-2.5 group-data-[collapsible=icon]:justify-center">
              <div class="size-7 shrink-0">
                <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="size-full">
                  <rect width="32" height="32" rx="8" fill="url(#gradient)"/>
                  <path d="M8 16L14 22L24 10" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                  <defs>
                    <linearGradient id="gradient" x1="0" y1="0" x2="32" y2="32">
                      <stop stop-color="#0369A1"/>
                      <stop offset="1" stop-color="#0EA5E9"/>
                    </linearGradient>
                  </defs>
                </svg>
              </div>
              <span class="text-base font-semibold tracking-wide text-white group-data-[collapsible=icon]:hidden">Easy1Auth</span>
            </div>
          </div>
          <!-- Tenant Switcher in Sidebar Header -->
          <div class="px-3 pb-3 group-data-[collapsible=icon]:hidden">
            <DropdownMenu>
              <DropdownMenuTrigger as-child>
                <button class="flex w-full items-center gap-2 rounded-md bg-white/5 px-3 py-2 text-sm text-slate-300 transition-colors hover:bg-white/10 hover:text-white">
                  <Building2 class="size-4 shrink-0" />
                  <span class="truncate flex-1 text-left">{{ userStore.currentTenant?.name || '选择租户' }}</span>
                  <ChevronDown class="size-3.5 shrink-0 opacity-60" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" class="w-52">
                <DropdownMenuItem
                  v-for="tenant in userStore.tenants"
                  :key="tenant.id"
                  @click="userStore.setCurrentTenant(tenant)"
                >
                  <div class="flex w-full items-center justify-between gap-2">
                    <span class="truncate">{{ tenant.name }}</span>
                    <Badge v-if="tenant.role === 'owner'" variant="outline" class="text-xs">所有者</Badge>
                  </div>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </SidebarHeader>

      <!-- Sidebar Content: current tenant's permission-driven menus -->
      <SidebarContent class="gap-0">
        <SidebarGroup v-if="menuLoading" class="py-2">
          <SidebarGroupContent class="px-4 py-3 text-xs text-slate-400 group-data-[collapsible=icon]:hidden">正在加载菜单…</SidebarGroupContent>
        </SidebarGroup>
        <SidebarGroup v-else-if="menuGroups.length === 0" class="py-2">
          <SidebarGroupContent class="px-4 py-3 text-xs text-slate-400 group-data-[collapsible=icon]:hidden">当前租户暂无可见菜单</SidebarGroupContent>
        </SidebarGroup>
        <template v-for="group in menuGroups" :key="group.label">
          <SidebarGroup class="py-2">
            <SidebarGroupLabel class="px-3 text-[11px] font-medium uppercase tracking-wider text-slate-500 group-data-[collapsible=icon]:hidden">
              {{ group.label }}
            </SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu class="gap-0.5">
                <template v-for="item in group.items" :key="item.index">
                  <!-- Items with children (submenu) -->
                  <Collapsible v-if="item.children.length > 0" :default-open="isChildActive(item.children)">
                    <SidebarMenuItem>
                      <CollapsibleTrigger as-child>
                        <SidebarMenuButton
                          :is-active="isChildActive(item.children)"
                          :tooltip="item.title"
                          class="group/collapsible relative text-slate-300 transition-colors duration-200 hover:bg-white/5 hover:text-white data-[active=true]:bg-white/5 data-[active=true]:text-sky-400"
                        >
                          <component :is="item.icon" class="size-4" />
                          <span>{{ item.title }}</span>
                          <ChevronRight class="ml-auto size-3.5 transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
                          <!-- Active indicator -->
                          <div
                            v-if="isChildActive(item.children)"
                            class="absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-r bg-sky-500"
                          />
                        </SidebarMenuButton>
                      </CollapsibleTrigger>
                    </SidebarMenuItem>
                    <CollapsibleContent>
                      <SidebarMenuSub class="ml-4 border-l border-white/10 pl-2">
                        <SidebarMenuItem v-for="child in item.children" :key="child.index">
                          <SidebarMenuSubButton
                            :is-active="isActive(child.index)"
                            @click="handleSelect(child.index)"
                            class="relative text-slate-400 transition-colors duration-200 hover:bg-white/5 hover:text-white data-[active=true]:bg-white/5 data-[active=true]:text-sky-400"
                          >
                            <component :is="child.icon" class="size-4" />
                            <span>{{ child.title }}</span>
                            <!-- Active indicator for sub-items -->
                            <div
                              v-if="isActive(child.index)"
                              class="absolute left-0 top-1/2 h-4 w-0.5 -translate-y-1/2 rounded-r bg-sky-500"
                            />
                          </SidebarMenuSubButton>
                        </SidebarMenuItem>
                      </SidebarMenuSub>
                    </CollapsibleContent>
                  </Collapsible>

                  <!-- Simple items -->
                  <SidebarMenuItem v-else>
                    <SidebarMenuButton
                      :is-active="isActive(item.index)"
                      :tooltip="item.title"
                      @click="item.type === 'menu' && handleSelect(item.index)"
                      class="relative text-slate-300 transition-colors duration-200 hover:bg-white/5 hover:text-white data-[active=true]:bg-white/5 data-[active=true]:text-sky-400"
                    >
                      <component :is="item.icon" class="size-4" />
                      <span>{{ item.title }}</span>
                      <!-- Active indicator -->
                      <div
                        v-if="isActive(item.index)"
                        class="absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-r bg-sky-500"
                      />
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                </template>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </template>
      </SidebarContent>

      <!-- Sidebar Footer: User Profile + Collapse Toggle -->
      <SidebarFooter class="border-t border-white/10 p-3">
        <!-- User Profile Section -->
        <DropdownMenu>
          <DropdownMenuTrigger as-child>
            <button class="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left transition-colors duration-200 hover:bg-white/5 group-data-[collapsible=icon]:justify-center">
              <Avatar class="size-8 shrink-0">
                <AvatarFallback class="bg-gradient-to-br from-sky-600 to-sky-400 text-xs font-semibold text-white">
                  {{ (userStore.userInfo?.username || '管理员').charAt(0).toUpperCase() }}
                </AvatarFallback>
              </Avatar>
              <div class="min-w-0 flex-1 group-data-[collapsible=icon]:hidden">
                <p class="truncate text-sm font-medium text-slate-200">{{ userStore.userInfo?.username || '管理员' }}</p>
                <p class="truncate text-xs text-slate-500">超级管理员</p>
              </div>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" class="w-48">
            <DropdownMenuItem>
              <User class="size-4" />
              <span>个人中心</span>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Settings class="size-4" />
              <span>账号设置</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem @click="handleLogout" class="text-red-600 focus:text-red-600">
              <LogOut class="size-4" />
              <span>退出登录</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <!-- Collapse Toggle -->
        <SidebarMenu class="mt-2">
          <SidebarMenuItem>
            <SidebarTrigger class="text-slate-400 transition-colors duration-200 hover:text-white">
              <PanelLeftClose class="size-4" />
            </SidebarTrigger>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>

      <SidebarRail class="bg-white/5" />
    </Sidebar>

    <SidebarInset>
      <!-- Header: Breadcrumb + Notifications + Collapse Trigger -->
      <header
        class="flex h-14 shrink-0 items-center justify-between border-b bg-white/80 px-4 backdrop-blur-sm"
        :style="{ background: 'var(--header-bg, rgba(255, 255, 255, 0.8))' }"
      >
        <!-- Left: Breadcrumb -->
        <div class="flex items-center gap-2">
          <House class="size-4 text-slate-400" />
          <span class="text-sm font-medium text-slate-600">{{ currentMenuTitle }}</span>
        </div>

        <!-- Right: Notification Bell + Sidebar Trigger -->
        <div class="flex items-center gap-2">
          <Button variant="ghost" size="icon-sm" class="relative">
            <Bell class="size-4 text-slate-600" />
            <Badge class="absolute -top-0.5 -right-0.5 flex size-4 items-center justify-center rounded-full p-0 text-[9px]">
              3
            </Badge>
          </Button>
          <SidebarTrigger class="lg:hidden">
            <PanelLeftClose class="size-4" />
          </SidebarTrigger>
        </div>
      </header>

      <main class="flex-1 overflow-auto bg-slate-50/50">
        <router-view v-slot="{ Component }">
          <transition name="slide-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </SidebarInset>
  </SidebarProvider>
</template>

<style scoped>
.slide-fade-enter-active {
  transition: all 0.25s ease;
}
.slide-fade-leave-active {
  transition: all 0.15s ease;
}
.slide-fade-enter-from {
  transform: translateX(10px);
  opacity: 0;
}
.slide-fade-leave-to {
  transform: translateX(-10px);
  opacity: 0;
}
</style>
