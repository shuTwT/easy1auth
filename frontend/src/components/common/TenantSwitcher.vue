<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { toast } from 'vue-sonner'
import { useUserStore } from '@/stores/user'
import { tenantApi } from '@/api/tenant'
import type { TenantInfo } from '@/types/auth'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Building2, ChevronDown, Check, Plus } from '@lucide/vue'

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
  toast.success(`已切换到租户: ${tenant.name}`)
}

async function handleCreateTenant() {
  if (!newTenantName.value.trim()) {
    toast.warning('请输入租户名称')
    return
  }

  loading.value = true
  try {
    const response = await tenantApi.createTenant({ name: newTenantName.value })
    userStore.setTenants([...tenants.value, response])
    userStore.setCurrentTenant(response)
    toast.success('租户创建成功')
    showCreateDialog.value = false
    newTenantName.value = ''
  } catch (error: any) {
    toast.error(error.response?.data?.msg || '创建租户失败')
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
    <DropdownMenu>
      <DropdownMenuTrigger as-child>
        <button class="tenant-selector">
          <Building2 class="size-4" />
          <span class="tenant-name">{{ currentTenant?.name || '选择租户' }}</span>
          <ChevronDown class="size-3 text-muted-foreground" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" class="w-56">
        <DropdownMenuItem
          v-for="tenant in tenants"
          :key="tenant.id"
          @click="handleSwitchTenant(tenant)"
        >
          <div class="tenant-item">
            <span>{{ tenant.name }}</span>
            <div class="flex items-center gap-1">
              <Badge v-if="tenant.role === 'owner'" variant="outline" class="text-xs">所有者</Badge>
              <Check v-if="tenant.id === currentTenant?.id" class="size-3.5 text-primary" />
            </div>
          </div>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem @click="showCreateDialog = true">
          <div class="create-tenant-item">
            <Plus class="size-4" />
            <span>创建新租户</span>
          </div>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>

    <Dialog v-model:open="showCreateDialog">
      <DialogContent class="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle>创建新租户</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="handleCreateTenant">
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">租户名称</label>
              <Input
                v-model="newTenantName"
                placeholder="请输入租户名称"
              />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="showCreateDialog = false">取消</Button>
          <Button :disabled="loading" @click="handleCreateTenant">{{ loading ? '创建中...' : '创建' }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
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
