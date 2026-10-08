<script setup lang="ts">
import { onMounted } from 'vue'
import { message } from 'antdv-next'
import { useUserStore } from '@/stores/user'
import { userPoolApi } from '@/api/userPool'
import { antdLocale, antdTheme } from '@/config/antd'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.accessToken) {
    try {
      if (userStore.userPools.length === 0) {
        const response = await userPoolApi.getCurrentSimpleList()
        userStore.setUserPools(response)
      }

      console.log(userStore.currentUserPool , userStore.userPools)

      if (!userStore.currentUserPool && userStore.userPools.length > 0) {
        const savedUserPoolId = localStorage.getItem('currentUserPoolId')
        let tenant = savedUserPoolId
          ? userStore.userPools.find(t => t.id === savedUserPoolId)
          : null


        if (!tenant) {
          tenant = userStore.userPools[0]
        }

        if (tenant) {
          userStore.setCurrentUserPool(tenant)
        }
      }
    } catch (error) {
      console.error('初始化用户池信息失败:', error)
      message.error('初始化用户池信息失败')
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
