<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { tenantApi } from '@/api/tenant'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.token) {
    try {
      if (userStore.tenants.length === 0) {
        const response = await tenantApi.getTenants()
        userStore.setTenants(response.tenants)
      }

      console.log(userStore.currentTenant , userStore.tenants)
      
      if (!userStore.currentTenant && userStore.tenants.length > 0) {
        const savedTenantId = localStorage.getItem('currentTenantId')
        let tenant = savedTenantId 
          ? userStore.tenants.find(t => t.id === savedTenantId)
          : null

        
        if (!tenant) {
          tenant = userStore.tenants[0]
        }
        
        if (tenant) {
          userStore.setCurrentTenant(tenant)
        }
      }
    } catch (error) {
      console.error('初始化租户信息失败:', error)
    }
  }
})
</script>

<template>
  <router-view />
</template>
