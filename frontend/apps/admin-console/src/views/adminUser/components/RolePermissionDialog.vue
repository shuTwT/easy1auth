<script setup lang="ts">
import { ref, toRef } from 'vue'
import { message } from 'antdv-next'
import { adminRoleApi } from '@/api/adminRole'
import type { AdminRole, PermissionCatalogItem } from '@/types/adminRole'
import { getApiErrorMessage, usePermissionCategories } from '../utils/hooks'

const props = defineProps<{
  permissionCatalog: PermissionCatalogItem[]
}>()

const emit = defineEmits<{
  success: []
}>()

const { permissionCategories } = usePermissionCategories(toRef(props, 'permissionCatalog'))

const rolePermissionDialogVisible = ref(false)
const rolePermissionTarget = ref<AdminRole | null>(null)
const rolePermissionCodes = ref<string[]>([])
const rolePermissionSubmitting = ref(false)

const open = (row: AdminRole) => {
  rolePermissionTarget.value = row
  rolePermissionCodes.value = [...row.permissions]
  rolePermissionDialogVisible.value = true
}

const toggleRolePermission = (code: string) => {
  const idx = rolePermissionCodes.value.indexOf(code)
  if (idx > -1) {
    rolePermissionCodes.value.splice(idx, 1)
  } else {
    rolePermissionCodes.value.push(code)
  }
}

const handleRolePermissionSubmit = async () => {
  if (!rolePermissionTarget.value) {
    return
  }
  rolePermissionSubmitting.value = true
  try {
    await adminRoleApi.update(rolePermissionTarget.value.id, {
      permissions: rolePermissionCodes.value
    })
    message.success('角色权限配置成功')
    rolePermissionDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '角色权限配置失败'))
  } finally {
    rolePermissionSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal
    v-model:open="rolePermissionDialogVisible"
    title="配置管理员角色权限"
    :footer="null"
    width="640"
  >
    <div class="max-w-xl">
      <p class="mb-4 text-sm text-[#52677D]">
        为角色“{{ rolePermissionTarget?.name }}”配置可使用的管理端权限。
      </p>
      <div class="border rounded-lg p-3 max-h-96 overflow-y-auto">
        <div
          v-for="category in permissionCategories"
          :key="category.code"
          class="mb-4 last:mb-0"
        >
          <div class="text-sm font-medium mb-2">{{ category.name }}</div>
          <div class="flex flex-col gap-2 pl-1">
            <label
              v-for="item in category.items"
              :key="item.code"
              class="flex items-center gap-2 cursor-pointer"
            >
              <a-checkbox
                :checked="rolePermissionCodes.includes(item.code)"
                @update:checked="toggleRolePermission(item.code)"
              />
              <span class="text-sm">{{ item.name }}</span>
              <code class="text-xs text-[#52677D] font-mono">{{ item.code }}</code>
            </label>
          </div>
        </div>
      </div>
      <div class="flex justify-end gap-2 pt-4">
        <a-button html-type="button" @click="rolePermissionDialogVisible = false">取消</a-button>
        <a-button
          type="primary"
          :loading="rolePermissionSubmitting"
          :disabled="rolePermissionSubmitting"
          @click="handleRolePermissionSubmit"
        >
          {{ rolePermissionSubmitting ? '保存中...' : '保存权限' }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>
