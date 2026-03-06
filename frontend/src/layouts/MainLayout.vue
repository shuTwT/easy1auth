<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { brandSettingsApi } from '@/api/brandSettings'
import TenantSwitcher from '@/components/common/TenantSwitcher.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const brandSettings = ref<any>(null)

const menuItems = [
  { index: '/dashboard', title: '控制台', icon: 'Odometer' },
  { index: '/tenant', title: '租户管理', icon: 'OfficeBuilding' },
  {
    index: '/user-management',
    title: '用户管理',
    icon: 'User',
    children: [
      { index: '/user', title: '用户列表', icon: 'User' },
      { index: '/group', title: '用户组', icon: 'UserFilled' }
    ]
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
  { index: '/settings', title: '系统设置', icon: 'Setting' }
]

const handleSelect = (index: string) => {
  router.push(index)
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const loadBrandSettings = async () => {
  try {
    const response = await brandSettingsApi.get()
    brandSettings.value = response.brandSettings
    applyBrandStyles()
  } catch (error) {
    console.error('加载品牌设置失败:', error)
  }
}

const applyBrandStyles = () => {
  if (!brandSettings.value?.adminPanel) return
  
  const root = document.documentElement
  const adminPanel = brandSettings.value.adminPanel
  
  if (adminPanel.primaryColor) {
    root.style.setProperty('--primary-color', adminPanel.primaryColor)
    root.style.setProperty('--el-color-primary', adminPanel.primaryColor)
  }
  
  if (adminPanel.sidebarColor) {
    root.style.setProperty('--sidebar-bg', adminPanel.sidebarColor)
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

const logoText = computed(() => {
  return isCollapse.value ? 'EA' : (brandSettings.value?.adminPanel?.logo ? '' : 'Easy1Auth')
})

const currentMenuTitle = computed(() => {
  for (const item of menuItems) {
    if (item.index === route.path) {
      return item.title
    }
    if (item.children) {
      const child = item.children.find(c => c.index === route.path)
      if (child) {
        return `${item.title} / ${child.title}`
      }
    }
  }
  return '首页'
})

onMounted(() => {
  loadBrandSettings()
})
</script>

<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo-container">
        <div class="logo">
          <img 
            v-if="brandSettings?.adminPanel?.logo" 
            :src="brandSettings.adminPanel.logo" 
            class="logo-image"
            :class="{ 'logo-collapsed': isCollapse }"
          />
          <template v-else>
            <div class="logo-icon">
              <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
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
            <transition name="fade">
              <span v-if="!isCollapse" class="logo-text">Easy1Auth</span>
            </transition>
          </template>
        </div>
      </div>
      
      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="route.path"
          :collapse="isCollapse"
          :collapse-transition="false"
          class="sidebar-menu"
          @select="handleSelect"
        >
          <template v-for="item in menuItems" :key="item.index">
            <el-sub-menu v-if="item.children" :index="item.index" class="menu-item">
              <template #title>
                <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
                <span class="menu-title">{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.index"
                :index="child.index"
                class="sub-menu-item"
              >
                <el-icon class="menu-icon"><component :is="child.icon" /></el-icon>
                <template #title>
                  <span class="menu-title">{{ child.title }}</span>
                </template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.index" class="menu-item">
              <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
              <template #title>
                <span class="menu-title">{{ item.title }}</span>
              </template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
      
      <div class="sidebar-footer">
        <div class="collapse-btn" @click="isCollapse = !isCollapse">
          <el-icon :size="18">
            <component :is="isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
        </div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <div class="breadcrumb">
            <el-icon :size="16"><HomeFilled /></el-icon>
            <span class="breadcrumb-text">{{ currentMenuTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <TenantSwitcher class="tenant-switcher" />
          
          <el-tooltip content="消息通知" placement="bottom">
            <el-badge :value="3" :max="99" class="notification-badge">
              <el-icon :size="20" class="header-icon"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          
          <el-dropdown class="user-dropdown" trigger="click">
            <div class="user-info">
              <el-avatar :size="36" class="user-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-detail">
                <span class="user-name">{{ userStore.userInfo?.username || '管理员' }}</span>
                <span class="user-role">超级管理员</span>
              </div>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item>
                  <el-icon><Setting /></el-icon>
                  账号设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="slide-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.aside {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 100;
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-image {
  height: 32px;
  width: auto;
  transition: all var(--transition-normal);
}

.logo-image.logo-collapsed {
  height: 28px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: white;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.menu-scrollbar {
  flex: 1;
  overflow: hidden;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
  padding: 8px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

:deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-radius: var(--border-radius);
  color: var(--sidebar-text);
  background: transparent;
  transition: all var(--transition-fast);
}

:deep(.el-menu-item:hover) {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-active-text);
}

:deep(.el-menu-item.is-active) {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-active-text);
}

:deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

:deep(.el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-radius: var(--border-radius);
  color: var(--sidebar-text);
  background: transparent;
  transition: all var(--transition-fast);
}

:deep(.el-sub-menu__title:hover) {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-active-text);
}

:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: var(--sidebar-active-text);
}

:deep(.el-sub-menu .el-icon) {
  font-size: 18px;
  margin-right: 12px;
}

:deep(.el-sub-menu .el-sub-menu__icon-arrow) {
  font-size: 12px;
  color: var(--sidebar-text);
}

:deep(.el-sub-menu .el-menu--inline) {
  background: transparent !important;
}

:deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

.sub-menu-item {
  padding-left: 56px !important;
  background: rgba(0, 0, 0, 0.1) !important;
}

:deep(.sub-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  border-radius: var(--border-radius);
  color: var(--sidebar-text);
  background: transparent;
  transition: all var(--transition-fast);
}

:deep(.sub-menu-item:hover) {
  background: var(--sidebar-hover-bg) !important;
  color: var(--sidebar-active-text);
}

:deep(.sub-menu-item.is-active) {
  background: var(--sidebar-hover-bg) !important;
  color: var(--sidebar-active-text);
}

.sidebar-menu.el-menu--collapse :deep(.el-menu-item) {
  justify-content: center;
}

.sidebar-menu.el-menu--collapse :deep(.el-menu-item .el-icon) {
  margin-right: 0;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border-radius: var(--border-radius);
  color: var(--sidebar-text);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  background: var(--sidebar-hover-bg);
  color: var(--sidebar-active-text);
}

.main-container {
  display: flex;
  flex-direction: column;
  background-color: var(--main-bg);
}

.header {
  height: 64px;
  background: var(--header-bg);
  box-shadow: var(--header-shadow);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  z-index: 99;
}

.header-left {
  display: flex;
  align-items: center;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.breadcrumb-text {
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.tenant-switcher {
  margin-right: 8px;
}

.notification-badge {
  cursor: pointer;
}

.header-icon {
  color: var(--text-secondary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.header-icon:hover {
  color: var(--primary-color);
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-radius: var(--border-radius);
  transition: background-color var(--transition-fast);
}

.user-info:hover {
  background-color: #F1F5F9;
}

.user-avatar {
  background: var(--primary-gradient);
  color: white;
}

.user-detail {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.user-role {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.2;
}

.dropdown-arrow {
  color: var(--text-muted);
  font-size: 12px;
  transition: transform var(--transition-fast);
}

.user-dropdown :deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.main {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active {
  transition: all var(--transition-normal);
}

.slide-fade-leave-active {
  transition: all var(--transition-fast);
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
