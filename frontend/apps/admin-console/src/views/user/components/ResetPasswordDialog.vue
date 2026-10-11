<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'antdv-next'
import { userApi } from '@/api/user'
import { getApiErrorMessage } from '../utils/hooks'
import { createResetPasswordRules } from '../utils/rules'
import type { ResetPasswordForm } from '../utils/types'

const dialogVisible = ref(false)
const submitting = ref(false)
const currentUserId = ref('')

const resetPasswordForm = reactive<ResetPasswordForm>({
  newPassword: '',
  confirmPassword: ''
})

const resetPasswordFormRules = createResetPasswordRules(resetPasswordForm)

const open = (userId: string) => {
  currentUserId.value = userId
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await userApi.resetPassword(currentUserId.value, resetPasswordForm.newPassword)
    message.success('密码重置成功')
    dialogVisible.value = false
  } catch (error: unknown) {
    console.error('重置密码失败:', error)
    message.error(getApiErrorMessage(error, '重置密码失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" title="重置密码" :footer="null">
    <a-form :model="resetPasswordForm" :rules="resetPasswordFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid gap-4">
        <a-form-item label="新密码" name="newPassword">
          <a-input v-model:value="resetPasswordForm.newPassword" type="password" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input v-model:value="resetPasswordForm.confirmPassword" type="password" placeholder="请确认新密码" />
        </a-form-item>
      </div>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">确定</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
