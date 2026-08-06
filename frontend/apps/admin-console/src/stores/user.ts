import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { TenantInfo } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<any>(null)
  const tenants = ref<TenantInfo[]>([])
  
  const savedTenant = localStorage.getItem('currentTenant')
  const currentTenant = ref<TenantInfo | null>(savedTenant ? JSON.parse(savedTenant) : null)

  const setSession = (newAccessToken: string, newRefreshToken: string) => {
    accessToken.value = newAccessToken
    refreshToken.value = newRefreshToken
    localStorage.setItem('accessToken', newAccessToken)
    localStorage.setItem('refreshToken', newRefreshToken)
    localStorage.removeItem('token')
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
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    tenants.value = []
    currentTenant.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('token')
    localStorage.removeItem('currentTenant')
    localStorage.removeItem('currentTenantId')
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    tenants,
    currentTenant,
    setSession,
    setUserInfo,
    setTenants,
    setCurrentTenant,
    logout
  }
})
