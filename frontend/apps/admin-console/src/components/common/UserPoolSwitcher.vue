<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'antdv-next'

import { useUserStore } from '@/stores/user'
import { userPoolApi } from '@/api/userPool'
import type { UserPoolInfo } from '@/types/auth'
import type { UserPoolPackageOption } from '@/api/userPool'
import { Building2, ChevronDown } from '@lucide/vue'

const userStore = useUserStore()
const loading = ref(false)
const showCreateDialog = ref(false)
const newUserPoolName = ref('')
const newUserPoolPackageId = ref<number | undefined>()
const packages = ref<UserPoolPackageOption[]>([])

const currentUserPool = computed(() => userStore.currentUserPool)
const userPools = computed(() => userStore.userPools)
const userPoolMenu = computed(() => ({
  items: [
    ...userPools.value.map(tenant => ({
      key: tenant.id,
      label: `${tenant.name}${tenant.userPoolPackage?.name ? ` · ${tenant.userPoolPackage.name}` : ''}`,
    })),
    { type: 'divider' as const },
    { key: 'create', label: '创建新用户池' },
  ],
  onClick: ({ key }: { key: string }) => {
    if (key === 'create') {
      showCreateDialog.value = true
      loadPackages()
      return
    }
    const tenant = userPools.value.find(item => item.id === key)
    if (tenant) handleSwitchUserPool(tenant)
  },
}))

async function handleSwitchUserPool(tenant: UserPoolInfo) {
  if (tenant.id === currentUserPool.value?.id) {
    return
  }

  userStore.setCurrentUserPool(tenant)
  message.success(`已切换到用户池: ${tenant.name}`)
}

async function handleCreateUserPool() {
  if (!newUserPoolName.value.trim()) {
    message.warning('请输入用户池名称')
    return
  }
  if (!newUserPoolPackageId.value) {
    message.warning('请选择用户池套餐')
    return
  }

  loading.value = true
  try {
    const response = await userPoolApi.createUserPool({ name: newUserPoolName.value, packageId: newUserPoolPackageId.value })
    userStore.setUserPools([...userPools.value, response])
    userStore.setCurrentUserPool(response)
    message.success('用户池创建成功')
    showCreateDialog.value = false
    newUserPoolName.value = ''
  } catch (error: any) {
    message.error(error.response?.data?.msg || '创建用户池失败')
  } finally {
    loading.value = false
  }
}

async function loadPackages() {
  try {
    packages.value = await userPoolApi.getAvailablePackages()
    newUserPoolPackageId.value = packages.value.find(item => item.id > 0)?.id
  } catch (error) {
    console.error('加载可用用户池套餐失败:', error)
    message.error('加载可用用户池套餐失败')
  }
}

</script>

<template>
  <div class="tenant-switcher">
    <a-dropdown :menu="userPoolMenu" :trigger="['click']">
        <button class="tenant-selector">
          <Building2 class="size-4" />
          <span class="tenant-name">{{ currentUserPool?.name || '选择用户池' }}</span>
          <ChevronDown class="size-3 text-muted-foreground" />
        </button>
    </a-dropdown>

    <a-modal v-model:open="showCreateDialog" title="创建新用户池" :footer="null">
      <a-form layout="vertical" @finish="handleCreateUserPool">
        <a-form-item label="用户池名称"><a-input v-model:value="newUserPoolName" placeholder="请输入用户池名称" /></a-form-item>
        <a-form-item label="用户池套餐"><a-select v-model:value="newUserPoolPackageId" class="w-full" :options="packages.map(item => ({ value: item.id, label: `${item.name} · ${item.maxUsers.toLocaleString()} 用户 / ${item.maxApps.toLocaleString()} 应用` }))" /></a-form-item>
        <div class="flex justify-end"><a-button html-type="submit" :loading="loading">创建</a-button></div>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.tenant-switcher {
  display: inline-block;
}

.tenant-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  font-size: inherit;
  color: inherit;
}

.tenant-selector:hover {
  background: white;
  border-color: var(--primary-color);
}

.tenant-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
