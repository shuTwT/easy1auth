import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TenantInfo } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const tenants = ref<TenantInfo[]>([])
  
  const savedTenant = localStorage.getItem('currentTenant')
  const currentTenant = ref<TenantInfo | null>(savedTenant ? JSON.parse(savedTenant) : null)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
  }

  const setTenants = (tenantList: TenantInfo[]) => {
    tenants.value = tenantList
  }

  const setCurrentTenant = (tenant: TenantInfo | null) => {
    currentTenant.value = tenant
    if (tenant) {
      localStorage.setItem('currentTenant', JSON.stringify(tenant))
      localStorage.setItem('currentTenantId', tenant.id)
    } else {
      localStorage.removeItem('currentTenant')
      localStorage.removeItem('currentTenantId')
    }
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    tenants.value = []
    currentTenant.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('currentTenant')
    localStorage.removeItem('currentTenantId')
  }

  return {
    token,
    userInfo,
    tenants,
    currentTenant,
    setToken,
    setUserInfo,
    setTenants,
    setCurrentTenant,
    logout
  }
})
