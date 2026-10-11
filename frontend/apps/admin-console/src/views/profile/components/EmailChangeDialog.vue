<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'antdv-next'
import { Mail } from '@lucide/vue'
import { profileApi } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import { useCountdown } from '../utils/hooks'
import { emailCodePattern, emailPattern, emailRuleMessages } from '../utils/rules'
import type { EmailChangeForm, EmailChangeStep } from '../utils/types'

defineProps<{
  currentEmail: string
}>()

const router = useRouter()
const userStore = useUserStore()
const { countdown, start: startCountdown } = useCountdown(60)

const dialogVisible = ref(false)
const step = ref<EmailChangeStep>('email')
const sending = ref(false)
const verifying = ref(false)
const challengeToken = ref('')
const emailForm = reactive<EmailChangeForm>({ email: '', code: '' })

const open = () => {
  step.value = 'email'
  emailForm.email = ''
  emailForm.code = ''
  challengeToken.value = ''
  dialogVisible.value = true
}

const sendCode = async () => {
  const email = emailForm.email.trim().toLowerCase()
  if (!emailPattern.test(email)) {
    message.warning(emailRuleMessages.emailInvalid)
    return
  }
  sending.value = true
  try {
    const result = await profileApi.sendEmailChangeCode(email)
    challengeToken.value = result.challengeToken
    step.value = 'code'
    emailForm.code = ''
    startCountdown()
    message.success('验证码已发送到新邮箱')
  } catch (error: unknown) {
    console.error('发送邮箱换绑验证码失败:', error)
    message.error('发送邮箱换绑验证码失败')
  } finally {
    sending.value = false
  }
}

const verify = async () => {
  if (!emailCodePattern.test(emailForm.code)) {
    message.warning(emailRuleMessages.codeInvalid)
    return
  }
  verifying.value = true
  try {
    await profileApi.verifyEmailChange(challengeToken.value, emailForm.code)
    message.success('邮箱换绑成功，请使用新邮箱重新登录')
    dialogVisible.value = false
    userStore.logout()
    await router.replace('/login')
  } catch (error: unknown) {
    console.error('验证邮箱换绑验证码失败:', error)
    message.error('验证邮箱换绑验证码失败')
  } finally {
    verifying.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" title="换绑邮箱" :footer="null" :mask-closable="!sending && !verifying">
    <div v-if="step === 'email'" class="space-y-5 pt-2">
      <div class="rounded-lg border border-sky-100 bg-sky-50 p-4 text-sm text-sky-900">
        当前邮箱：<span class="font-medium">{{ currentEmail }}</span>
      </div>
      <div class="grid gap-2">
        <label for="new-admin-email" class="text-sm font-medium text-[#102A43]">新邮箱地址</label>
        <a-input id="new-admin-email" v-model:value="emailForm.email" autocomplete="email" :maxlength="320" placeholder="name@company.com">
          <template #prefix><Mail class="size-4 text-[#52677D]" /></template>
        </a-input>
      </div>
      <a-button type="primary" block :loading="sending" @click="sendCode">发送验证码</a-button>
    </div>

    <div v-else class="space-y-5 pt-2">
      <div>
        <p class="text-sm text-[#52677D]">验证码已发送至</p>
        <p class="mt-1 break-all font-medium text-[#102A43]">{{ emailForm.email }}</p>
      </div>
      <div class="grid gap-2">
        <label for="admin-email-code" class="text-sm font-medium text-[#102A43]">邮箱验证码</label>
        <a-input id="admin-email-code" v-model:value="emailForm.code" inputmode="numeric" autocomplete="one-time-code" :maxlength="6" placeholder="请输入6位验证码" @press-enter="verify" />
      </div>
      <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <a-button :disabled="countdown > 0" :loading="sending" @click="sendCode">
          {{ countdown > 0 ? `${countdown}s 后可重新发送` : '重新发送' }}
        </a-button>
        <a-button type="primary" :loading="verifying" @click="verify">验证并换绑</a-button>
      </div>
    </div>
  </a-modal>
</template>
