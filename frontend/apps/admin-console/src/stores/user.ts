import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserPoolInfo } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string>(localStorage.getItem('accessToken') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<any>(null)
  const userPools = ref<UserPoolInfo[]>([])
  
  // Preserve the selected pool for sessions saved before tenant was renamed to user pool.
  const savedUserPool = localStorage.getItem('currentUserPool') || localStorage.getItem('currentTenant')
  const currentUserPool = ref<UserPoolInfo | null>(savedUserPool ? JSON.parse(savedUserPool) : null)
  if (currentUserPool.value) {
    const legacyPackage = (currentUserPool.value as UserPoolInfo & { tenantPackage?: UserPoolInfo['userPoolPackage'] }).tenantPackage
    currentUserPool.value.userPoolPackage ??= legacyPackage
    localStorage.setItem('currentUserPool', JSON.stringify(currentUserPool.value))
    localStorage.setItem('currentUserPoolId', currentUserPool.value.id)
  }
  localStorage.removeItem('currentTenant')
  localStorage.removeItem('currentTenantId')

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

  const setUserPools = (tenantList: UserPoolInfo[]) => {
    userPools.value = tenantList
  }

  const setCurrentUserPool = (tenant: UserPoolInfo | null) => {
    currentUserPool.value = tenant
    if (tenant) {
      localStorage.setItem('currentUserPool', JSON.stringify(tenant))
      localStorage.setItem('currentUserPoolId', tenant.id)
    } else {
      localStorage.removeItem('currentUserPool')
      localStorage.removeItem('currentUserPoolId')
    }
  }

  const logout = () => {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    userPools.value = []
    currentUserPool.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('token')
    localStorage.removeItem('currentUserPool')
    localStorage.removeItem('currentUserPoolId')
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    userPools,
    currentUserPool,
    setSession,
    setUserInfo,
    setUserPools,
    setCurrentUserPool,
    logout
  }
})
