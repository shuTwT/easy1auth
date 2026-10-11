<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'antdv-next'
import type { TabsProps } from 'antdv-next'
import { securityApi } from '@/api/security'
import type { MfaStatus, PasswordExpiryStatus, PasswordPolicy } from '@/api/security'
import PasswordPanel from './components/PasswordPanel.vue'
import MfaPanel from './components/MfaPanel.vue'

const activeTab = ref('password')
const securityTabs: TabsProps['items'] = [
  { key: 'password', label: '密码管理', forceRender: true },
  { key: 'mfa', label: '多因素认证', forceRender: true }
]

const passwordPolicy = ref<PasswordPolicy | null>(null)
const expiryStatus = ref<PasswordExpiryStatus>({ expired: false, daysUntilExpiry: 0 })
const mfaStatus = ref<MfaStatus>({ enabled: false, type: null })

const loadSecurityData = async () => {
  try {
    const [policyRes, mfaRes] = await Promise.all([
      securityApi.getPasswordPolicy(),
      securityApi.getMfaStatus()
    ])
    passwordPolicy.value = policyRes.policy
    expiryStatus.value = policyRes.expiryStatus
    mfaStatus.value = mfaRes
  } catch (error: unknown) {
    console.error('加载安全设置失败:', error)
    message.error('加载安全设置失败')
  }
}

onMounted(loadSecurityData)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#102A43] mb-2">安全设置</h1>
      <p class="text-sm text-[#52677D]">管理您的账户安全选项</p>
    </div>

    <div class="bg-white rounded-xl p-4">
      <a-tabs v-model:active-key="activeTab" :items="securityTabs">
        <template #contentRender="{ item }">
          <PasswordPanel
            v-if="item.key === 'password'"
            :policy="passwordPolicy"
            :expiry-status="expiryStatus"
            @success="loadSecurityData"
          />
          <MfaPanel v-else-if="item.key === 'mfa'" :mfa-status="mfaStatus" @success="loadSecurityData" />
        </template>
      </a-tabs>
    </div>
  </div>
</template>
