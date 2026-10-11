<script setup lang="ts">
import { reactive, ref, toRef } from 'vue'
import { message } from 'antdv-next'
import { securityApi } from '@/api/security'
import type { PasswordExpiryStatus, PasswordPolicy } from '@/api/security'
import { getApiErrorMessage, usePasswordChecker } from '../utils/hooks'
import { createPasswordRules } from '../utils/rules'
import type { PasswordForm } from '../utils/types'

const props = defineProps<{
  policy: PasswordPolicy | null
  expiryStatus: PasswordExpiryStatus
}>()

const emit = defineEmits<{
  success: []
}>()

const loading = ref(false)

const passwordForm = reactive<PasswordForm>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRules = createPasswordRules(passwordForm)

const { passwordStrength, requirements } = usePasswordChecker(toRef(passwordForm, 'newPassword'), toRef(props, 'policy'))

const handleSubmit = async () => {
  loading.value = true
  try {
    await securityApi.changePassword(passwordForm)
    message.success('密码修改成功')
    Object.assign(passwordForm, {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    emit('success')
  } catch (error: unknown) {
    console.error('密码修改失败:', error)
    message.error(getApiErrorMessage(error, '密码修改失败'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <a-card>
    <div class="flex flex-row items-center gap-3">
      <h3>修改密码</h3>
      <a-tag v-if="expiryStatus.expired" color="red">密码已过期</a-tag>
      <a-tag v-else-if="expiryStatus.daysUntilExpiry <= 7">
        密码将在 {{ expiryStatus.daysUntilExpiry }} 天后过期
      </a-tag>
    </div>
    <div>
      <div v-if="policy" class="bg-[#F4F8FB] rounded-lg p-4 mb-6">
        <h4 class="text-sm font-semibold text-[#102A43] mb-3">密码要求</h4>
        <ul class="space-y-1">
          <li
            v-for="item in requirements"
            :key="item.key"
            class="text-sm text-[#52677D]"
            :class="{ 'text-emerald-500': item.met }"
          >
            {{ item.met ? '●' : '○' }} {{ item.label }}
          </li>
        </ul>
      </div>

      <a-form
        :model="passwordForm"
        :rules="passwordFormRules"
        layout="vertical"
        class="max-w-md space-y-4"
        @finish="handleSubmit"
      >
        <a-form-item label="当前密码" name="currentPassword">
          <a-input
            v-model:value="passwordForm.currentPassword"
            type="password"
            autocomplete="current-password"
            placeholder="请输入当前密码"
          />
        </a-form-item>

        <a-form-item label="新密码" name="newPassword">
          <a-input
            v-model:value="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            placeholder="请输入新密码"
          />
          <div v-if="passwordForm.newPassword" class="flex items-center gap-2 text-sm">
            <span>密码强度：</span>
            <a-progress :percent="passwordStrength.level * 33.33" class="w-24 h-2" />
            <span :style="{ color: passwordStrength.color }">{{ passwordStrength.text }}</span>
          </div>
        </a-form-item>

        <a-form-item label="确认密码" name="confirmPassword">
          <a-input
            v-model:value="passwordForm.confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入新密码"
          />
        </a-form-item>

        <a-button type="primary" html-type="submit" :loading="loading">修改密码</a-button>
      </a-form>
    </div>
  </a-card>
</template>
