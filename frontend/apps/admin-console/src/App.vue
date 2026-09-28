<script setup lang="ts">
import { onMounted } from 'vue'
import { message } from 'antdv-next'
import { useUserStore } from '@/stores/user'
import { tenantApi } from '@/api/tenant'
import { antdLocale, antdTheme } from '@/config/antd'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.accessToken) {
    try {
      if (userStore.tenants.length === 0) {
        const response = await tenantApi.getCurrentSimpleList()
        userStore.setTenants(response)
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
      message.error('初始化租户信息失败')
    }
  }
})
</script>

<template>
  <a-config-provider :locale="antdLocale" :theme="antdTheme" component-size="middle">
    <a-app>
      <router-view />
    </a-app>
  </a-config-provider>
</template>
