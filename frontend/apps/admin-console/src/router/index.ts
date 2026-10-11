import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { message } from 'antdv-next'
import { systemInitializationApi } from '@/api/systemInitialization'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '控制台', requiresAuth: true }
      },
      {
        path: 'user-pool',
        name: 'UserPool',
        component: () => import('@/views/userPool/index.vue'),
        meta: { title: '用户池管理', requiresAuth: true }
      },
      {
        path: 'user-pool-package',
        name: 'UserPoolPackage',
        component: () => import('@/views/userPoolPackage/index.vue'),
        meta: { title: '用户池套餐', requiresAuth: true }
      },
      { path: 'tenant', redirect: '/user-pool' },
      { path: 'tenant-package', redirect: '/user-pool-package' },
      {
        path: 'pool-tenants',
        name: 'Tenant',
        component: () => import('@/views/tenant/index.vue'),
        meta: { title: '租户管理', requiresAuth: true }
      },
      {
        path: 'menu-management',
        name: 'MenuManagement',
        component: () => import('@/views/menuManagement/index.vue'),
        meta: { title: '菜单管理', requiresAuth: true }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户列表', requiresAuth: true }
      },
      {
        path: 'application',
        name: 'Application',
        component: () => import('@/views/application/index.vue'),
        meta: { title: '应用管理', requiresAuth: true }
      },
      {
        path: 'application/:id',
        name: 'ApplicationDetail',
        component: () => import('@/views/application/detail.vue'),
        meta: { title: '应用详情', requiresAuth: true }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计日志', requiresAuth: true }
      },
      {
        path: 'group',
        redirect: '/user-group'
      },
      {
        path: 'user-group',
        name: 'UserGroup',
        component: () => import('@/views/user-group/index.vue'),
        meta: { title: '用户组', requiresAuth: true }
      },
      {
        path: 'user-group/:id',
        name: 'UserGroupDetail',
        component: () => import('@/views/user-group/detail.vue'),
        meta: { title: '用户组详情', requiresAuth: true, activeMenu: '/user-group' }
      },
      {
        path: 'position',
        name: 'Position',
        component: () => import('@/views/position/index.vue'),
        meta: { title: '岗位管理', requiresAuth: true }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色管理', requiresAuth: true }
      },
      {
        path: 'identity-source-management/social-identity-source',
        name: 'SocialIdentitySource',
        component: () => import('@/views/identity-source-management/social-identity-source/index.vue'),
        meta: { title: '社会化身份源', requiresAuth: true }
      },
      {
        path: 'identity-source-management/enterprise-identity-source',
        name: 'EnterpriseIdentitySource',
        component: () => import('@/views/identity-source-management/enterprise-identity-source/index.vue'),
        meta: { title: '企业身份源', requiresAuth: true }
      },
      {
        path: 'identity-source-management/sync-identity-source',
        name: 'SyncIdentitySource',
        component: () => import('@/views/identity-source-management/sync-identity-source/index.vue'),
        meta: { title: '同步身份源', requiresAuth: true }
      },
      // Keep the retired flat identity source URLs working.
      { path: 'social-identity-source', redirect: '/identity-source-management/social-identity-source' },
      { path: 'enterprise-identity-source', redirect: '/identity-source-management/enterprise-identity-source' },
      { path: 'sync-identity-source', redirect: '/identity-source-management/sync-identity-source' },
      {
        path: 'brand-login-style',
        name: 'BrandLoginStyle',
        component: () => import('@/views/personalization/LoginStyleBuilder.vue'),
        meta: { title: '个性化登录页面', brandSection: 'loginStyle', requiresAuth: true }
      },
      {
        path: 'message-service',
        name: 'MessageService',
        component: () => import('@/views/messageService/index.vue'),
        meta: { title: '消息服务', requiresAuth: true }
      },
      {
        path: 'custom-domain',
        name: 'CustomDomain',
        component: () => import('@/views/customDomain/index.vue'),
        meta: { title: '自定义域名', requiresAuth: true }
      },
      {
        path: 'security',
        name: 'Security',
        component: () => import('@/views/security/index.vue'),
        meta: { title: '安全设置', requiresAuth: true }
      },
      {
        path: 'permission-management/permission-space',
        name: 'PermissionSpace',
        component: () => import('@/views/permission-management/permission-space/index.vue'),
        meta: { title: '权限空间管理', requiresAuth: true }
      },
      {
        path: 'permission-management/permission-data-resource',
        name: 'PermissionDataResource',
        component: () => import('@/views/permission-management/permission-data-resource/index.vue'),
        meta: { title: '数据资源管理', requiresAuth: true }
      },
      // Keep the retired combined permission page URL working.
      { path: 'permission', redirect: '/permission-management/permission-data-resource' },
      {
        path: 'admin-user',
        name: 'AdminUser',
        component: () => import('@/views/adminUser/index.vue'),
        meta: { title: '管理员管理', requiresAuth: true }
      },
      // Keep the retired personalization URL as a compatibility redirect.
      { path: 'personalization', redirect: '/brand-login-style' },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/sso',
    redirect: '/application'
  },
  {
    path: '/initialize',
    name: 'Initialize',
    component: () => import('@/views/initialize/index.vue'),
    meta: { title: '系统初始化', requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const accessToken = localStorage.getItem('accessToken')

  const title = to.meta.title as string | undefined
  document.title = title ? `${title} | Easy1Auth` : 'Easy1Auth'

  try {
    const initialized = await systemInitializationApi.getStatus()
    if (!initialized && to.name !== 'Initialize') {
      return { name: 'Initialize' }
    }
    if (initialized && to.name === 'Initialize') {
      return accessToken ? { name: 'Dashboard' } : { name: 'Login' }
    }
  } catch {
    message.error('无法读取系统初始化状态，请检查服务是否正常')
    if (to.name !== 'Initialize') {
      return { name: 'Initialize' }
    }
  }

  if (to.meta.requiresAuth && !accessToken) {
    return { name: 'Login' }
  }

  return true
})

export default router
