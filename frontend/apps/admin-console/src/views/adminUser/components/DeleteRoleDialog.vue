<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { adminRoleApi } from '@/api/adminRole'
import type { AdminRole } from '@/types/adminRole'
import { getApiErrorMessage } from '../utils/hooks'

const emit = defineEmits<{
  success: []
}>()

const deleteRoleDialogVisible = ref(false)
const deleteRoleTarget = ref<AdminRole | null>(null)
const deleteRoleSubmitting = ref(false)

const open = (row: AdminRole) => {
  deleteRoleTarget.value = row
  deleteRoleDialogVisible.value = true
}

const handleDeleteRoleConfirm = async () => {
  if (!deleteRoleTarget.value) {
    return
  }
  deleteRoleSubmitting.value = true
  try {
    await adminRoleApi.delete(deleteRoleTarget.value.id)
    message.success('角色删除成功')
    deleteRoleDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '删除角色失败'))
  } finally {
    deleteRoleSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="deleteRoleDialogVisible" :footer="null">
    <div>
      <div>
        <h3>删除管理员角色</h3>
        <p>
          确定要删除角色 <span class="font-medium">{{ deleteRoleTarget?.name }}</span> 吗？已分配该角色的管理员将失去对应权限。此操作不可恢复。
        </p>
      </div>
      <div>
        <a-button :disabled="deleteRoleSubmitting">取消</a-button>
        <a-button
          type="primary"
          danger
          :loading="deleteRoleSubmitting"
          :disabled="deleteRoleSubmitting"
          @click="handleDeleteRoleConfirm"
        >
          {{ deleteRoleSubmitting ? '处理中...' : '确认删除' }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>
