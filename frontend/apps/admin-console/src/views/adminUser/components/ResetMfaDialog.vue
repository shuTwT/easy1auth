<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { adminUserApi } from '@/api/adminUser'
import type { AdminUser } from '@/types/adminUser'
import { getApiErrorMessage } from '../utils/hooks'

const emit = defineEmits<{
  success: []
}>()

const resetMfaDialogVisible = ref(false)
const resetMfaTarget = ref<AdminUser | null>(null)
const resetMfaSubmitting = ref(false)

const open = (row: AdminUser) => {
  resetMfaTarget.value = row
  resetMfaDialogVisible.value = true
}

const handleResetMfaConfirm = async () => {
  if (!resetMfaTarget.value) {
    return
  }
  resetMfaSubmitting.value = true
  try {
    await adminUserApi.resetMfa(resetMfaTarget.value.id)
    message.success('MFA 已重置')
    resetMfaDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '重置 MFA 失败'))
  } finally {
    resetMfaSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="resetMfaDialogVisible" :footer="null">
    <div>
      <div>
        <h3>重置管理员 MFA</h3>
        <p>
          确定要重置管理员 <span class="font-medium">{{ resetMfaTarget?.username }}</span> 的多因素认证配置吗？重置后该管理员需要重新配置 MFA。
        </p>
      </div>
      <div>
        <a-button :disabled="resetMfaSubmitting">取消</a-button>
        <a-button type="primary" :loading="resetMfaSubmitting" :disabled="resetMfaSubmitting" @click="handleResetMfaConfirm">
          {{ resetMfaSubmitting ? '处理中...' : '确认重置' }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>
