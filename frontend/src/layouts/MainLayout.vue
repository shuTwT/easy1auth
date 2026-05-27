<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { brandSettingsApi } from '@/api/brandSettings'
import TenantSwitcher from '@/components/common/TenantSwitcher.vue'

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
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
  Gauge,
  Building2,
  User,
  Users,
  Briefcase,
  Library,
  Link,
  Paintbrush,
  Monitor,
  FileText,
  Lock,
  Shield,
  WandSparkles,
  Settings,
  House,
  Bell,
  LogOut,
  ChevronRight,
  PanelLeftClose,

} from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const brandSettings = ref<any>(null)

// Icon map for menu items (string → lucide component)
const iconMap: Record<string, any> = {
  Odometer: Gauge,
  OfficeBuilding: Building2,
  User,
  UserFilled: Users,
  Briefcase,
  Collection: Library,
  Connection: Link,
  Brush: Paintbrush,
  Monitor,
  Document: FileText,
  Lock,
  Shield,
  MagicStick: WandSparkles,
  Setting: Settings,
  HomeFilled: House,
  Bell,
  SwitchButton: LogOut,
}

const menuItems = [
  { index: '/dashboard', title: '控制台', icon: 'Odometer' },
  { index: '/tenant', title: '租户管理', icon: 'OfficeBuilding' },
  {
    index: '/user-management',
    title: '用户管理',
    icon: 'User',
    children: [
      { index: '/user', title: '用户列表', icon: 'User' },
      { index: '/group', title: '用户组', icon: 'UserFilled' },
    ],
  },
  { index: '/position', title: '岗位管理', icon: 'Briefcase' },
  { index: '/role', title: '角色管理', icon: 'Collection' },
  { index: '/social-identity-provider', title: '社会化身份源', icon: 'Connection' },
  { index: '/brand-settings', title: '品牌设置', icon: 'Brush' },
  { index: '/application', title: '应用管理', icon: 'Monitor' },
  { index: '/audit', title: '审计日志', icon: 'Document' },
  { index: '/sso', title: '单点登录', icon: 'Connection' },
  { index: '/permission', title: '权限管理', icon: 'Lock' },
  { index: '/security', title: '安全设置', icon: 'Shield' },
  { index: '/personalization', title: '个性化设置', icon: 'MagicStick' },
  { index: '/settings', title: '系统设置', icon: 'Setting' },
]

const handleSelect = (index: string) => {
  router.push(index)
}

const isActive = (path: string) => route.path === path

const isChildActive = (children: { index: string }[]) =>
  children.some((child) => isActive(child.index))

const currentMenuTitle = computed(() => {
  for (const item of menuItems) {
    if (item.index === route.path) {
      return item.title
    }
    if (item.children) {
      const child = item.children.find((c) => c.index === route.path)
      if (child) {
        return `${item.title} / ${child.title}`
      }
    }
  }
  return '首页'
})

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
</script>

<template>
  <SidebarProvider>
    <Sidebar collapsible="icon" variant="sidebar">
      <SidebarHeader class="p-0">
        <div class="flex h-16 items-center justify-center border-b border-sidebar-border px-4">
          <div class="flex items-center gap-3">
            <div v-if="brandSettings?.adminPanel?.logo" class="flex items-center justify-center">
              <img
                :src="brandSettings.adminPanel.logo"
                class="h-8 w-auto"
                :class="{ 'hidden group-data-[collapsible=icon]:block': true }"
              />
            </div>
            <div v-else class="flex items-center gap-3 group-data-[collapsible=icon]:justify-center">
              <div class="size-8 shrink-0">
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
              <span class="text-lg font-bold text-white group-data-[collapsible=icon]:hidden">Easy1Auth</span>
            </div>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarMenu>
          <template v-for="item in menuItems" :key="item.index">
            <!-- Items with children (submenu) -->
            <Collapsible v-if="item.children" :default-open="isChildActive(item.children)">
              <SidebarMenuItem>
                <CollapsibleTrigger as-child>
                  <SidebarMenuButton
                    :is-active="isChildActive(item.children)"
                    class="group/collapsible"
                  >
                    <component :is="iconMap[item.icon]" class="size-4" />
                    <span>{{ item.title }}</span>
                    <ChevronRight class="ml-auto size-3 transition-transform group-data-[state=open]/collapsible:rotate-90" />
                  </SidebarMenuButton>
                </CollapsibleTrigger>
              </SidebarMenuItem>
              <CollapsibleContent>
                <SidebarMenuSub>
                  <SidebarMenuItem v-for="child in item.children" :key="child.index">
                    <SidebarMenuSubButton
                      :is-active="isActive(child.index)"
                      @click="handleSelect(child.index)"
                    >
                      <component :is="iconMap[child.icon]" class="size-4" />
                      <span>{{ child.title }}</span>
                    </SidebarMenuSubButton>
                  </SidebarMenuItem>
                </SidebarMenuSub>
              </CollapsibleContent>
            </Collapsible>

            <!-- Simple items -->
            <SidebarMenuItem v-else>
              <SidebarMenuButton
                :is-active="isActive(item.index)"
                @click="handleSelect(item.index)"
              >
                <component :is="iconMap[item.icon]" class="size-4" />
                <span>{{ item.title }}</span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </template>
        </SidebarMenu>
      </SidebarContent>

      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarTrigger>
              <PanelLeftClose class="size-4" />
            </SidebarTrigger>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>

    <SidebarInset>
      <header
        class="flex h-16 shrink-0 items-center justify-between border-b bg-white px-6 shadow-sm"
        :style="{ background: 'var(--header-bg, #ffffff)' }"
      >
        <div class="flex items-center gap-2">
          <House class="size-4 text-muted-foreground" />
          <span class="text-sm font-medium text-muted-foreground">{{ currentMenuTitle }}</span>
        </div>

        <div class="flex items-center gap-4">
          <TenantSwitcher class="mr-2" />

          <Button variant="ghost" size="icon-sm" class="relative">
            <Bell class="size-5" />
            <Badge class="absolute -top-1 -right-1 flex size-4 items-center justify-center rounded-full p-0 text-[10px]">
              3
            </Badge>
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="ghost" class="flex items-center gap-2 px-2">
                <Avatar class="size-9">
                  <AvatarFallback class="bg-gradient-to-br from-sky-700 to-sky-400 text-white text-xs font-semibold">
                    {{ (userStore.userInfo?.username || '管理员').charAt(0).toUpperCase() }}
                  </AvatarFallback>
                </Avatar>
                <div class="hidden text-left md:block">
                  <p class="text-sm font-semibold leading-tight">{{ userStore.userInfo?.username || '管理员' }}</p>
                  <p class="text-xs text-muted-foreground leading-tight">超级管理员</p>
                </div>
              </Button>
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
              <DropdownMenuItem @click="handleLogout">
                <LogOut class="size-4" />
                <span>退出登录</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <main class="flex-1 overflow-auto">
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
