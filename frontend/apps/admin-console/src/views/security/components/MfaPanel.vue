<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { securityApi } from '@/api/security'
import type { MfaSetupResult, MfaStatus } from '@/api/security'
import { getApiErrorMessage } from '../utils/hooks'

defineProps<{
  mfaStatus: MfaStatus
}>()

const emit = defineEmits<{
  success: []
}>()

const loading = ref(false)
const showMfaSetup = ref(false)
const mfaToken = ref('')
const mfaSetupData = ref<MfaSetupResult>({
  secret: '',
  qrCodeUrl: '',
  backupCodes: []
})

const handleSetupMfa = async () => {
  loading.value = true
  try {
    mfaSetupData.value = await securityApi.setupMfa()
    mfaToken.value = ''
    showMfaSetup.value = true
  } catch (error: unknown) {
    console.error('MFA设置失败:', error)
    message.error(getApiErrorMessage(error, 'MFA设置失败'))
  } finally {
    loading.value = false
  }
}

const handleCancelSetup = () => {
  showMfaSetup.value = false
  mfaToken.value = ''
}

const handleEnableMfa = async () => {
  if (!mfaToken.value) {
    message.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await securityApi.enableMfa(mfaToken.value)
    message.success('MFA已启用')
    showMfaSetup.value = false
    mfaToken.value = ''
    emit('success')
  } catch (error: unknown) {
    console.error('启用MFA失败:', error)
    message.error(getApiErrorMessage(error, '启用MFA失败'))
  } finally {
    loading.value = false
  }
}

const handleDisableMfa = async () => {
  if (!mfaToken.value) {
    message.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await securityApi.disableMfa(mfaToken.value)
    message.success('MFA已禁用')
    mfaToken.value = ''
    emit('success')
  } catch (error: unknown) {
    console.error('禁用MFA失败:', error)
    message.error(getApiErrorMessage(error, '禁用MFA失败'))
  } finally {
    loading.value = false
  }
}

const copyBackupCodes = async () => {
  try {
    await navigator.clipboard.writeText(mfaSetupData.value.backupCodes.join('\n'))
    message.success('备用码已复制到剪贴板')
  } catch (error: unknown) {
    console.error('复制失败:', error)
    message.error('复制失败')
  }
}
</script>

<template>
  <a-card>
    <div class="flex flex-row items-center gap-3">
      <h3>多因素认证 (MFA)</h3>
      <a-tag :color="mfaStatus.enabled ? 'green' : 'orange'">
        {{ mfaStatus.enabled ? '已启用' : '未启用' }}
      </a-tag>
    </div>
    <div>
      <div v-if="!showMfaSetup && !mfaStatus.enabled" class="text-center py-10 px-5">
        <div class="mb-4 text-[#0369A1]">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="mx-auto">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-[#102A43] mb-2">增强账户安全</h3>
        <p class="text-[#52677D] mb-6">启用多因素认证后，登录时需要输入验证码，大大提高账户安全性。</p>
        <a-button :loading="loading" @click="handleSetupMfa">启用 MFA</a-button>
      </div>

      <div v-else-if="showMfaSetup" class="max-w-lg mx-auto">
        <div class="mb-8">
          <h4 class="text-base font-semibold text-[#102A43] mb-2">步骤 1：扫描二维码</h4>
          <p class="text-sm text-[#52677D] mb-3">使用 Google Authenticator 或其他 TOTP 应用扫描以下二维码：</p>
          <div class="flex justify-center p-4 bg-white border rounded-lg mb-3">
            <img :src="mfaSetupData.qrCodeUrl" alt="MFA QR Code" class="w-50 h-50" />
          </div>
          <p class="text-sm">
            或手动输入密钥：<code class="bg-[#F4F8FB] px-2 py-1 rounded text-sm font-mono">{{ mfaSetupData.secret }}</code>
          </p>
        </div>

        <div class="mb-8">
          <h4 class="text-base font-semibold text-[#102A43] mb-2">步骤 2：保存备用码</h4>
          <p class="text-sm text-[#52677D] mb-3">请保存以下备用码，当无法使用验证器时可用来登录：</p>
          <div class="flex flex-wrap gap-2 mb-3">
            <code
              v-for="code in mfaSetupData.backupCodes"
              :key="code"
              class="bg-[#F4F8FB] px-3 py-2 rounded text-sm font-mono"
            >
              {{ code }}
            </code>
          </div>
          <a-button size="small" @click="copyBackupCodes">复制备用码</a-button>
        </div>

        <div>
          <h4 class="text-base font-semibold text-[#102A43] mb-2">步骤 3：验证设置</h4>
          <p class="text-sm text-[#52677D] mb-3">请输入验证器显示的 6 位数字验证码：</p>
          <a-input v-model:value="mfaToken" placeholder="请输入验证码" :maxlength="6" class="w-50 mb-4" />
          <div class="flex gap-3">
            <a-button @click="handleCancelSetup">取消</a-button>
            <a-button :loading="loading" @click="handleEnableMfa">确认启用</a-button>
          </div>
        </div>
      </div>

      <div v-else>
        <div class="text-center py-10 px-5">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2" class="mx-auto mb-4">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
          <h3 class="text-lg font-semibold text-[#102A43] mb-2">MFA 已启用</h3>
          <p class="text-[#52677D]">当前认证方式：{{ mfaStatus.type === 'totp' ? '验证器应用' : '邮箱验证码' }}</p>
        </div>

        <a-divider class="my-6" />

        <div>
          <h4 class="text-sm font-semibold text-[#102A43] mb-2">禁用 MFA</h4>
          <p class="text-sm text-[#52677D] mb-3">禁用后，登录时将不再需要验证码。</p>
          <div class="flex items-center gap-3">
            <a-input v-model:value="mfaToken" placeholder="请输入验证码以禁用 MFA" :maxlength="6" class="w-64" />
            <a-button danger :loading="loading" @click="handleDisableMfa">禁用 MFA</a-button>
          </div>
        </div>
      </div>
    </div>
  </a-card>
</template>
