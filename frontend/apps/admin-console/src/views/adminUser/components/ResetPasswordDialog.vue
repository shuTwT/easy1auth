<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'antdv-next'
import { adminUserApi } from '@/api/adminUser'
import type { AdminUser } from '@/types/adminUser'
import { getApiErrorMessage } from '../utils/hooks'
import { createResetPasswordRules } from '../utils/rules'
import type { ResetPasswordForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const resetPwdDialogVisible = ref(false)
const resetPwdTarget = ref<AdminUser | null>(null)
const resetPwdForm = reactive<ResetPasswordForm>({ newPassword: '', confirmPassword: '' })
const resetPwdFormRules = createResetPasswordRules(resetPwdForm)
const resetPwdSubmitting = ref(false)

const open = (row: AdminUser) => {
  resetPwdTarget.value = row
  resetPwdForm.newPassword = ''
  resetPwdForm.confirmPassword = ''
  resetPwdDialogVisible.value = true
}

const handleResetPwdSubmit = async () => {
  if (!resetPwdTarget.value) {
    return
  }
  if (!resetPwdForm.newPassword) {
    message.warning('请输入新密码')
    return
  }
  if (resetPwdForm.newPassword !== resetPwdForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  resetPwdSubmitting.value = true
  try {
    await adminUserApi.resetPassword(resetPwdTarget.value.id, { newPassword: resetPwdForm.newPassword })
    message.success('密码重置成功')
    resetPwdDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '重置密码失败'))
  } finally {
    resetPwdSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="resetPwdDialogVisible" :footer="null">
    <div class="max-w-md">
      <div>
        <h3>重置管理员密码</h3>
      </div>
      <a-form :model="resetPwdForm" :rules="resetPwdFormRules" layout="vertical" class="py-4" @finish="handleResetPwdSubmit">
        <div class="grid gap-4">
          <a-form-item label="新密码" name="newPassword">
            <a-input v-model:value="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" />
          </a-form-item>
          <a-form-item label="确认密码" name="confirmPassword">
            <a-input v-model:value="resetPwdForm.confirmPassword" type="password" placeholder="请确认新密码" />
          </a-form-item>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <a-button html-type="button" @click="resetPwdDialogVisible = false">取消</a-button>
          <a-button type="primary" html-type="submit" :loading="resetPwdSubmitting" :disabled="resetPwdSubmitting">
            {{ resetPwdSubmitting ? '提交中...' : '确定' }}
          </a-button>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>
