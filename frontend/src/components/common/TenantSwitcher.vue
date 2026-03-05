<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { tenantApi } from '@/api/tenant'
import type { TenantInfo } from '@/types/auth'

const userStore = useUserStore()
const loading = ref(false)
const showCreateDialog = ref(false)
const newTenantName = ref('')

const currentTenant = computed(() => userStore.currentTenant)
const tenants = computed(() => userStore.tenants)

async function handleSwitchTenant(tenant: TenantInfo) {
  if (tenant.id === currentTenant.value?.id) {
    return
  }

  userStore.setCurrentTenant(tenant)
  ElMessage.success(`已切换到租户: ${tenant.name}`)
}

async function handleCreateTenant() {
  if (!newTenantName.value.trim()) {
    ElMessage.warning('请输入租户名称')
    return
  }

  loading.value = true
  try {
    const response = await tenantApi.createTenant({ name: newTenantName.value })
    userStore.setTenants([...tenants.value, response.tenant])
    userStore.setCurrentTenant(response.tenant)
    ElMessage.success('租户创建成功')
    showCreateDialog.value = false
    newTenantName.value = ''
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建租户失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 租户初始化逻辑已移至 App.vue
})
</script>

<template>
  <div class="tenant-switcher">
    <el-dropdown trigger="click" @command="handleSwitchTenant">
      <div class="tenant-selector">
        <el-icon><OfficeBuilding /></el-icon>
        <span class="tenant-name">{{ currentTenant?.name || '选择租户' }}</span>
        <el-icon class="arrow"><ArrowDown /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="tenant in tenants"
            :key="tenant.id"
            :command="tenant"
            :class="{ 'is-active': tenant.id === currentTenant?.id }"
          >
            <div class="tenant-item">
              <span>{{ tenant.name }}</span>
              <el-tag v-if="tenant.role === 'owner'" size="small" type="warning">所有者</el-tag>
              <el-icon v-if="tenant.id === currentTenant?.id" class="check-icon"><Check /></el-icon>
            </div>
          </el-dropdown-item>
          <el-dropdown-item divided @click="showCreateDialog = true">
            <div class="create-tenant-item">
              <el-icon><Plus /></el-icon>
              <span>创建新租户</span>
            </div>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-dialog
      v-model="showCreateDialog"
      title="创建新租户"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form @submit.prevent="handleCreateTenant">
        <el-form-item label="租户名称">
          <el-input
            v-model="newTenantName"
            placeholder="请输入租户名称"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleCreateTenant">
          创建
        </el-button>
      </template>
    </el-dialog>
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

.arrow {
  font-size: 12px;
  color: #64748B;
}

.tenant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  justify-content: space-between;
}

.check-icon {
  color: #0369A1;
  font-weight: bold;
}

.create-tenant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0369A1;
}

.is-active {
  background-color: #F0F9FF;
}
</style>
