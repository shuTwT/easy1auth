<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Form, FormItem, message } from 'antdv-next'
import { securityApi, type PasswordPolicy, type MfaStatus } from '@/api/security'
const activeTab = ref('password')

const passwordPolicy = ref<PasswordPolicy | null>(null)
const expiryStatus = ref({ expired: false, daysUntilExpiry: 0 })
const mfaStatus = ref<MfaStatus>({ enabled: false, type: null })

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码' }],
  newPassword: [{ required: true, message: '请输入新密码' }],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    {
      validator: (_rule: unknown, value: string) => {
        if (value !== passwordForm.newPassword) {
          return Promise.reject('两次输入的新密码不一致')
        }
        return Promise.resolve()
      },
    },
  ],
}

const mfaSetupData = ref({
  secret: '',
  qrCodeUrl: '',
  backupCodes: [] as string[]
})

const mfaToken = ref('')
const showMfaSetup = ref(false)
const loading = ref(false)

const passwordStrength = computed(() => {
  const pwd = passwordForm.newPassword
  if (!pwd) return { level: 0, text: '', color: '#E5E7EB' }

  let score = 0
  if (pwd.length >= 8) score++
  if (pwd.length >= 12) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/[a-z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(pwd)) score++

  if (score <= 2) return { level: 1, text: '弱', color: '#EF4444' }
  if (score <= 4) return { level: 2, text: '中', color: '#F59E0B' }
  return { level: 3, text: '强', color: '#10B981' }
})

const loadSecurityData = async () => {
  try {
    loading.value = true
    const [policyRes, mfaRes] = await Promise.all([
      securityApi.getPasswordPolicy(),
      securityApi.getMfaStatus()
    ])

    passwordPolicy.value = policyRes.policy
    expiryStatus.value = policyRes.expiryStatus
    mfaStatus.value = mfaRes
  } catch (error) {
    console.error('加载安全设置失败:', error)
    message.error('加载安全设置失败')
  } finally {
    loading.value = false
  }
}

const handleChangePassword = async () => {
  try {
    loading.value = true
    await securityApi.changePassword(passwordForm)
    message.success('密码修改成功')
    Object.assign(passwordForm, {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    })
    loadSecurityData()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '密码修改失败')
  } finally {
    loading.value = false
  }
}

const handleSetupMfa = async () => {
  try {
    loading.value = true
    const res = await securityApi.setupMfa()
    mfaSetupData.value = res
    showMfaSetup.value = true
  } catch (error: any) {
    message.error(error.response?.data?.msg || 'MFA设置失败')
  } finally {
    loading.value = false
  }
}

const handleEnableMfa = async () => {
  if (!mfaToken.value) {
    message.warning('请输入验证码')
    return
  }

  try {
    loading.value = true
    await securityApi.enableMfa(mfaToken.value)
    message.success('MFA已启用')
    showMfaSetup.value = false
    mfaToken.value = ''
    loadSecurityData()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '启用MFA失败')
  } finally {
    loading.value = false
  }
}

const handleDisableMfa = async () => {
  if (!mfaToken.value) {
    message.warning('请输入验证码')
    return
  }

  try {
    loading.value = true
    await securityApi.disableMfa(mfaToken.value)
    message.success('MFA已禁用')
    mfaToken.value = ''
    loadSecurityData()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '禁用MFA失败')
  } finally {
    loading.value = false
  }
}

const copyBackupCodes = () => {
  navigator.clipboard.writeText(mfaSetupData.value.backupCodes.join('\n'))
  message.success('备用码已复制到剪贴板')
}

onMounted(() => {
  loadSecurityData()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground mb-2">安全设置</h1>
      <p class="text-sm text-muted-foreground">管理您的账户安全选项</p>
    </div>

    <div class="bg-white rounded-xl p-4">
      <div class="mb-4">
        <Button :type="activeTab === 'password' ? 'primary' : 'default'" @click="activeTab = 'password'">密码管理</Button>
        <Button :type="activeTab === 'mfa' ? 'primary' : 'default'" @click="activeTab = 'mfa'">多因素认证</Button>
      </div>

      <div v-show="activeTab === 'password'">
        <Card class="">
          <div class="flex flex-row items-center gap-3">
            <h3>修改密码</h3>
            <Tag v-if="expiryStatus.expired" color="red">密码已过期</Tag>
            <Tag v-else-if="expiryStatus.daysUntilExpiry <= 7" >
              密码将在 {{ expiryStatus.daysUntilExpiry }} 天后过期
            </Tag>
          </div>
          <div>
            <div v-if="passwordPolicy" class="bg-muted rounded-lg p-4 mb-6">
              <h4 class="text-sm font-semibold text-foreground mb-3">密码要求</h4>
              <ul class="space-y-1">
                <li
                  class="text-sm text-muted-foreground"
                  :class="{ 'text-emerald-500': passwordForm.newPassword.length >= passwordPolicy.minLength }"
                >
                  {{ passwordForm.newPassword.length >= passwordPolicy.minLength ? '●' : '○' }} 至少 {{ passwordPolicy.minLength }} 个字符
                </li>
                <li
                  v-if="passwordPolicy.requireUppercase"
                  class="text-sm text-muted-foreground"
                  :class="{ 'text-emerald-500': /[A-Z]/.test(passwordForm.newPassword) }"
                >
                  {{ /[A-Z]/.test(passwordForm.newPassword) ? '●' : '○' }} 包含大写字母
                </li>
                <li
                  v-if="passwordPolicy.requireLowercase"
                  class="text-sm text-muted-foreground"
                  :class="{ 'text-emerald-500': /[a-z]/.test(passwordForm.newPassword) }"
                >
                  {{ /[a-z]/.test(passwordForm.newPassword) ? '●' : '○' }} 包含小写字母
                </li>
                <li
                  v-if="passwordPolicy.requireNumbers"
                  class="text-sm text-muted-foreground"
                  :class="{ 'text-emerald-500': /\d/.test(passwordForm.newPassword) }"
                >
                  {{ /\d/.test(passwordForm.newPassword) ? '●' : '○' }} 包含数字
                </li>
                <li
                  v-if="passwordPolicy.requireSpecialChars"
                  class="text-sm text-muted-foreground"
                  :class="{ 'text-emerald-500': /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(passwordForm.newPassword) }"
                >
                  {{ /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(passwordForm.newPassword) ? '●' : '○' }} 包含特殊字符
                </li>
              </ul>
            </div>

            <Form :model="passwordForm" :rules="passwordFormRules" layout="vertical" class="max-w-md space-y-4" @finish="handleChangePassword">
              <FormItem label="当前密码" name="currentPassword">
                <Input v-model:value="passwordForm.currentPassword"
                  type="password"
                  autocomplete="current-password"
                  placeholder="请输入当前密码"
                />
              </FormItem>

              <FormItem label="新密码" name="newPassword">
                <Input v-model:value="passwordForm.newPassword"
                  type="password"
                  autocomplete="new-password"
                  placeholder="请输入新密码"
                />
                <div v-if="passwordForm.newPassword" class="flex items-center gap-2 text-sm">
                  <span>密码强度：</span>
                  <Progress :percent="passwordStrength.level * 33.33" class="w-24 h-2" />
                  <span :style="{ color: passwordStrength.color }">{{ passwordStrength.text }}</span>
                </div>
              </FormItem>

              <FormItem label="确认密码" name="confirmPassword">
                <Input v-model:value="passwordForm.confirmPassword"
                  type="password"
                  autocomplete="new-password"
                  placeholder="请再次输入新密码"
                />
              </FormItem>

              <Button type="primary" html-type="submit" :loading="loading">
                修改密码
              </Button>
            </Form>
          </div>
        </Card>
      </div>

      <div v-show="activeTab === 'mfa'">
        <Card class="">
          <div class="flex flex-row items-center gap-3">
            <h3>多因素认证 (MFA)</h3>
            <Tag :color="mfaStatus.enabled ? 'green' : 'orange'">
              {{ mfaStatus.enabled ? '已启用' : '未启用' }}
            </Tag>
          </div>
          <div>
            <div v-if="!showMfaSetup && !mfaStatus.enabled" class="text-center py-10 px-5">
              <div class="mb-4 text-primary">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="mx-auto">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-foreground mb-2">增强账户安全</h3>
              <p class="text-muted-foreground mb-6">启用多因素认证后，登录时需要输入验证码，大大提高账户安全性。</p>
              <Button @click="handleSetupMfa">
                启用 MFA
              </Button>
            </div>

            <div v-else-if="showMfaSetup" class="max-w-lg mx-auto">
              <div class="mb-8">
                <h4 class="text-base font-semibold text-foreground mb-2">步骤 1：扫描二维码</h4>
                <p class="text-sm text-muted-foreground mb-3">使用 Google Authenticator 或其他 TOTP 应用扫描以下二维码：</p>
                <div class="flex justify-center p-4 bg-white border rounded-lg mb-3">
                  <img :src="mfaSetupData.qrCodeUrl" alt="MFA QR Code" class="w-50 h-50" />
                </div>
                <p class="text-sm">
                  或手动输入密钥：<code class="bg-muted px-2 py-1 rounded text-sm font-mono">{{ mfaSetupData.secret }}</code>
                </p>
              </div>

              <div class="mb-8">
                <h4 class="text-base font-semibold text-foreground mb-2">步骤 2：保存备用码</h4>
                <p class="text-sm text-muted-foreground mb-3">请保存以下备用码，当无法使用验证器时可用来登录：</p>
                <div class="flex flex-wrap gap-2 mb-3">
                  <code
                    v-for="(code, index) in mfaSetupData.backupCodes"
                    :key="index"
                    class="bg-muted px-3 py-2 rounded text-sm font-mono"
                  >
                    {{ code }}
                  </code>
                </div>
                <Button size="small"  @click="copyBackupCodes">复制备用码</Button>
              </div>

              <div>
                <h4 class="text-base font-semibold text-foreground mb-2">步骤 3：验证设置</h4>
                <p class="text-sm text-muted-foreground mb-3">请输入验证器显示的 6 位数字验证码：</p>
                <Input v-model:value="mfaToken"
                  placeholder="请输入验证码"
                  maxlength="6"
                  class="w-50 mb-4"
                />
                <div class="flex gap-3">
                  <Button  @click="showMfaSetup = false">取消</Button>
                  <Button :disabled="loading" @click="handleEnableMfa">
                    确认启用
                  </Button>
                </div>
              </div>
            </div>

            <div v-else>
              <div class="text-center py-10 px-5">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2" class="mx-auto mb-4">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                <h3 class="text-lg font-semibold text-foreground mb-2">MFA 已启用</h3>
                <p class="text-muted-foreground">当前认证方式：{{ mfaStatus.type === 'totp' ? '验证器应用' : '邮箱验证码' }}</p>
              </div>

              <Divider class="my-6" />

              <div>
                <h4 class="text-sm font-semibold text-foreground mb-2">禁用 MFA</h4>
                <p class="text-sm text-muted-foreground mb-3">禁用后，登录时将不再需要验证码。</p>
                <div class="flex items-center gap-3">
                  <Input v-model:value="mfaToken"
                    placeholder="请输入验证码以禁用 MFA"
                    maxlength="6"
                    class="w-64"
                  />
                  <Button danger :disabled="loading" @click="handleDisableMfa">
                    禁用 MFA
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>
