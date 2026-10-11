<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { adminUserApi } from '@/api/adminUser'
import type { AdminUser } from '@/types/adminUser'
import { getApiErrorMessage, useAdminMembership } from '../utils/hooks'

const emit = defineEmits<{
  success: []
}>()

const { currentMembership } = useAdminMembership()

const removeDialogVisible = ref(false)
const removeTarget = ref<AdminUser | null>(null)
const removeSubmitting = ref(false)

const open = (row: AdminUser) => {
  removeTarget.value = row
  removeDialogVisible.value = true
}

const handleRemoveConfirm = async () => {
  const target = removeTarget.value
  const membership = target ? currentMembership(target) : undefined
  if (!target || !membership) {
    return
  }
  removeSubmitting.value = true
  try {
    await adminUserApi.removeFromUserPool(target.id, membership.userPoolId)
    message.success('已将管理员移出用户池')
    removeDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '移出用户池失败'))
  } finally {
    removeSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="removeDialogVisible" :footer="null">
    <div>
      <div>
        <h3>移出用户池</h3>
        <p>
          确定要将管理员 <span class="font-medium">{{ removeTarget?.username }}</span> 移出当前用户池吗？此操作只解除该管理员与当前用户池的关联，不会删除管理员账号。
        </p>
      </div>
      <div>
        <a-button :disabled="removeSubmitting">取消</a-button>
        <a-button
          type="primary"
          danger
          :loading="removeSubmitting"
          :disabled="removeSubmitting"
          @click="handleRemoveConfirm"
        >
          {{ removeSubmitting ? '处理中...' : '确认移除' }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>
