<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'antdv-next'
import { Dropdown, Form, FormItem, Input, Modal } from 'antdv-next'
import { useUserStore } from '@/stores/user'
import { tenantApi } from '@/api/tenant'
import type { TenantInfo } from '@/types/auth'
import { Building2, ChevronDown } from '@lucide/vue'

const userStore = useUserStore()
const loading = ref(false)
const showCreateDialog = ref(false)
const newTenantName = ref('')

const currentTenant = computed(() => userStore.currentTenant)
const tenants = computed(() => userStore.tenants)
const tenantMenu = computed(() => ({
  items: [
    ...tenants.value.map(tenant => ({ key: tenant.id, label: tenant.name })),
    { type: 'divider' as const },
    { key: 'create', label: '创建新租户' },
  ],
  onClick: ({ key }: { key: string }) => {
    if (key === 'create') {
      showCreateDialog.value = true
      return
    }
    const tenant = tenants.value.find(item => item.id === key)
    if (tenant) handleSwitchTenant(tenant)
  },
}))

async function handleSwitchTenant(tenant: TenantInfo) {
  if (tenant.id === currentTenant.value?.id) {
    return
  }

  userStore.setCurrentTenant(tenant)
  message.success(`已切换到租户: ${tenant.name}`)
}

async function handleCreateTenant() {
  if (!newTenantName.value.trim()) {
    message.warning('请输入租户名称')
    return
  }

  loading.value = true
  try {
    const response = await tenantApi.createTenant({ name: newTenantName.value })
    userStore.setTenants([...tenants.value, response])
    userStore.setCurrentTenant(response)
    message.success('租户创建成功')
    showCreateDialog.value = false
    newTenantName.value = ''
  } catch (error: any) {
    message.error(error.response?.data?.msg || '创建租户失败')
  } finally {
    loading.value = false
  }
}

</script>

<template>
  <div class="tenant-switcher">
    <Dropdown :menu="tenantMenu" :trigger="['click']">
        <button class="tenant-selector">
          <Building2 class="size-4" />
          <span class="tenant-name">{{ currentTenant?.name || '选择租户' }}</span>
          <ChevronDown class="size-3 text-muted-foreground" />
        </button>
    </Dropdown>

    <Modal v-model:open="showCreateDialog" title="创建新租户" ok-text="创建" cancel-text="取消" :confirm-loading="loading" @ok="handleCreateTenant" :footer="null">
      <Form layout="vertical" @finish="handleCreateTenant"><FormItem label="租户名称"><Input v-model:value="newTenantName" placeholder="请输入租户名称" /></FormItem></Form>
    </Modal>
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
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  font-size: inherit;
  color: inherit;
}

.tenant-selector:hover {
  background: white;
  border-color: #0369A1;
}

.tenant-name {
  font-size: 14px;
  font-weight: 500;
  color: #0C4A6E;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: space-between;
}

.create-tenant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0369A1;
}
</style>
