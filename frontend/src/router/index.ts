import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

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
        path: 'tenant',
        name: 'Tenant',
        component: () => import('@/views/tenant/index.vue'),
        meta: { title: '租户管理', requiresAuth: true }
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
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计日志', requiresAuth: true }
      },
      {
        path: 'group',
        name: 'Group',
        component: () => import('@/views/group/index.vue'),
        meta: { title: '用户组', requiresAuth: true }
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
        path: 'social-identity-provider',
        name: 'SocialIdentityProvider',
        component: () => import('@/views/socialIdentityProvider/index.vue'),
        meta: { title: '社会化身份源', requiresAuth: true }
      },
      {
        path: 'brand-settings',
        name: 'BrandSettings',
        component: () => import('@/views/brandSettings/index.vue'),
        meta: { title: '品牌设置', requiresAuth: true }
      },
      {
        path: 'sso',
        name: 'SSO',
        component: () => import('@/views/sso/index.vue'),
        meta: { title: '单点登录', requiresAuth: true }
      },
      {
        path: 'security',
        name: 'Security',
        component: () => import('@/views/security/index.vue'),
        meta: { title: '安全设置', requiresAuth: true }
      },
      {
        path: 'personalization',
        name: 'Personalization',
        component: () => import('@/views/personalization/index.vue'),
        meta: { title: '个性化设置', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/oauth2/authorize',
    name: 'OAuth2Authorize',
    component: () => import('@/views/oauth2/authorize.vue'),
    meta: { title: 'OAuth2授权', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} | Easy1Auth` : 'Easy1Auth'
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login') {
    next()
  } else {
    next()
  }
})

export default router
