<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { toast } from 'vue-sonner'
import { securityApi, type PasswordPolicy, type MfaStatus } from '@/api/security'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Progress } from '@/components/ui/progress'
import { Separator } from '@/components/ui/separator'
import { Label } from '@/components/ui/label'

const activeTab = ref('password')

const passwordPolicy = ref<PasswordPolicy | null>(null)
const expiryStatus = ref({ expired: false, daysUntilExpiry: 0 })
const mfaStatus = ref<MfaStatus>({ enabled: false, type: null })

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const mfaSetupData = ref({
  secret: '',
  qrCodeUrl: '',
  backupCodes: [] as string[]
})

const mfaToken = ref('')
const showMfaSetup = ref(false)
const loading = ref(false)

const passwordStrength = computed(() => {
  const pwd = passwordForm.value.newPassword
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
    mfaStatus.value = mfaRes.mfa
  } catch (error) {
    console.error('加载安全设置失败:', error)
  } finally {
    loading.value = false
  }
}

const handleChangePassword = async () => {
  if (!passwordForm.value.currentPassword || !passwordForm.value.newPassword || !passwordForm.value.confirmPassword) {
    toast.warning('请填写所有密码字段')
    return
  }

  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.error('两次输入的新密码不一致')
    return
  }

  try {
    loading.value = true
    await securityApi.changePassword(passwordForm.value)
    toast.success('密码修改成功')
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    loadSecurityData()
  } catch (error: any) {
    toast.error(error.response?.data?.message || '密码修改失败')
  } finally {
    loading.value = false
  }
}

const handleSetupMfa = async () => {
  try {
    loading.value = true
    const res = await securityApi.setupMfa()
    mfaSetupData.value = res.data
    showMfaSetup.value = true
  } catch (error: any) {
    toast.error(error.response?.data?.message || 'MFA设置失败')
  } finally {
    loading.value = false
  }
}

const handleEnableMfa = async () => {
  if (!mfaToken.value) {
    toast.warning('请输入验证码')
    return
  }

  try {
    loading.value = true
    await securityApi.enableMfa(mfaToken.value)
    toast.success('MFA已启用')
    showMfaSetup.value = false
    mfaToken.value = ''
    loadSecurityData()
  } catch (error: any) {
    toast.error(error.response?.data?.message || '启用MFA失败')
  } finally {
    loading.value = false
  }
}

const handleDisableMfa = async () => {
  if (!mfaToken.value) {
    toast.warning('请输入验证码')
    return
  }

  try {
    loading.value = true
    await securityApi.disableMfa(mfaToken.value)
    toast.success('MFA已禁用')
    mfaToken.value = ''
    loadSecurityData()
  } catch (error: any) {
    toast.error(error.response?.data?.message || '禁用MFA失败')
  } finally {
    loading.value = false
  }
}

const copyBackupCodes = () => {
  navigator.clipboard.writeText(mfaSetupData.value.backupCodes.join('\n'))
  toast.success('备用码已复制到剪贴板')
}

onMounted(() => {
  loadSecurityData()
})
</script>

<template>
  <div class="security-settings p-6">
    <div class="mb-6">
      <h1 class="text-2xl font-semibold text-slate-900 mb-2">安全设置</h1>
      <p class="text-sm text-slate-500">管理您的账户安全选项</p>
    </div>

    <Tabs v-model="activeTab" class="bg-white rounded-xl p-4">
      <TabsList class="mb-4">
        <TabsTrigger value="password">密码管理</TabsTrigger>
        <TabsTrigger value="mfa">多因素认证</TabsTrigger>
      </TabsList>
      
      <TabsContent value="password">
        <Card class="border-0 shadow-none">
          <CardHeader class="flex flex-row items-center gap-3">
            <CardTitle>修改密码</CardTitle>
            <Badge v-if="expiryStatus.expired" variant="destructive">密码已过期</Badge>
            <Badge v-else-if="expiryStatus.daysUntilExpiry <= 7" variant="outline">
              密码将在 {{ expiryStatus.daysUntilExpiry }} 天后过期
            </Badge>
          </CardHeader>
          <CardContent>
            <div v-if="passwordPolicy" class="bg-slate-50 rounded-lg p-4 mb-6">
              <h4 class="text-sm font-semibold text-slate-700 mb-3">密码要求</h4>
              <ul class="space-y-1">
                <li 
                  class="text-sm text-slate-400"
                  :class="{ 'text-emerald-500': passwordForm.newPassword.length >= passwordPolicy.minLength }"
                >
                  {{ passwordForm.newPassword.length >= passwordPolicy.minLength ? '●' : '○' }} 至少 {{ passwordPolicy.minLength }} 个字符
                </li>
                <li 
                  v-if="passwordPolicy.requireUppercase"
                  class="text-sm text-slate-400"
                  :class="{ 'text-emerald-500': /[A-Z]/.test(passwordForm.newPassword) }"
                >
                  {{ /[A-Z]/.test(passwordForm.newPassword) ? '●' : '○' }} 包含大写字母
                </li>
                <li 
                  v-if="passwordPolicy.requireLowercase"
                  class="text-sm text-slate-400"
                  :class="{ 'text-emerald-500': /[a-z]/.test(passwordForm.newPassword) }"
                >
                  {{ /[a-z]/.test(passwordForm.newPassword) ? '●' : '○' }} 包含小写字母
                </li>
                <li 
                  v-if="passwordPolicy.requireNumbers"
                  class="text-sm text-slate-400"
                  :class="{ 'text-emerald-500': /\d/.test(passwordForm.newPassword) }"
                >
                  {{ /\d/.test(passwordForm.newPassword) ? '●' : '○' }} 包含数字
                </li>
                <li 
                  v-if="passwordPolicy.requireSpecialChars"
                  class="text-sm text-slate-400"
                  :class="{ 'text-emerald-500': /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(passwordForm.newPassword) }"
                >
                  {{ /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(passwordForm.newPassword) ? '●' : '○' }} 包含特殊字符
                </li>
              </ul>
            </div>

            <form class="max-w-md space-y-4">
              <div class="grid gap-2">
                <Label>当前密码</Label>
                <Input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  placeholder="请输入当前密码"
                />
              </div>

              <div class="grid gap-2">
                <Label>新密码</Label>
                <Input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码"
                />
                <div v-if="passwordForm.newPassword" class="flex items-center gap-2 text-sm">
                  <span>密码强度：</span>
                  <Progress :model-value="passwordStrength.level * 33.33" class="w-24 h-2" />
                  <span :style="{ color: passwordStrength.color }">{{ passwordStrength.text }}</span>
                </div>
              </div>

              <div class="grid gap-2">
                <Label>确认密码</Label>
                <Input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                />
              </div>

              <Button type="button" :disabled="loading" @click="handleChangePassword">
                修改密码
              </Button>
            </form>
          </CardContent>
        </Card>
      </TabsContent>
      
      <TabsContent value="mfa">
        <Card class="border-0 shadow-none">
          <CardHeader class="flex flex-row items-center gap-3">
            <CardTitle>多因素认证 (MFA)</CardTitle>
            <Badge :variant="mfaStatus.enabled ? 'default' : 'secondary'">
              {{ mfaStatus.enabled ? '已启用' : '未启用' }}
            </Badge>
          </CardHeader>
          <CardContent>
            <div v-if="!showMfaSetup && !mfaStatus.enabled" class="text-center py-10 px-5">
              <div class="mb-4 text-sky-600">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="mx-auto">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
              </div>
              <h3 class="text-lg font-semibold text-slate-900 mb-2">增强账户安全</h3>
              <p class="text-slate-500 mb-6">启用多因素认证后，登录时需要输入验证码，大大提高账户安全性。</p>
              <Button @click="handleSetupMfa">
                启用 MFA
              </Button>
            </div>

            <div v-else-if="showMfaSetup" class="max-w-lg mx-auto">
              <div class="mb-8">
                <h4 class="text-base font-semibold text-slate-900 mb-2">步骤 1：扫描二维码</h4>
                <p class="text-sm text-slate-500 mb-3">使用 Google Authenticator 或其他 TOTP 应用扫描以下二维码：</p>
                <div class="flex justify-center p-4 bg-white border rounded-lg mb-3">
                  <img :src="mfaSetupData.qrCodeUrl" alt="MFA QR Code" class="w-50 h-50" />
                </div>
                <p class="text-sm">
                  或手动输入密钥：<code class="bg-slate-100 px-2 py-1 rounded text-sm font-mono">{{ mfaSetupData.secret }}</code>
                </p>
              </div>

              <div class="mb-8">
                <h4 class="text-base font-semibold text-slate-900 mb-2">步骤 2：保存备用码</h4>
                <p class="text-sm text-slate-500 mb-3">请保存以下备用码，当无法使用验证器时可用来登录：</p>
                <div class="flex flex-wrap gap-2 mb-3">
                  <code 
                    v-for="(code, index) in mfaSetupData.backupCodes" 
                    :key="index"
                    class="bg-slate-100 px-3 py-2 rounded text-sm font-mono"
                  >
                    {{ code }}
                  </code>
                </div>
                <Button size="sm" variant="outline" @click="copyBackupCodes">复制备用码</Button>
              </div>

              <div>
                <h4 class="text-base font-semibold text-slate-900 mb-2">步骤 3：验证设置</h4>
                <p class="text-sm text-slate-500 mb-3">请输入验证器显示的 6 位数字验证码：</p>
                <Input
                  v-model="mfaToken"
                  placeholder="请输入验证码"
                  maxlength="6"
                  class="w-50 mb-4"
                />
                <div class="flex gap-3">
                  <Button variant="outline" @click="showMfaSetup = false">取消</Button>
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
                <h3 class="text-lg font-semibold text-slate-900 mb-2">MFA 已启用</h3>
                <p class="text-slate-500">当前认证方式：{{ mfaStatus.type === 'totp' ? '验证器应用' : '邮箱验证码' }}</p>
              </div>

              <Separator class="my-6" />

              <div>
                <h4 class="text-sm font-semibold text-slate-900 mb-2">禁用 MFA</h4>
                <p class="text-sm text-slate-500 mb-3">禁用后，登录时将不再需要验证码。</p>
                <div class="flex items-center gap-3">
                  <Input
                    v-model="mfaToken"
                    placeholder="请输入验证码以禁用 MFA"
                    maxlength="6"
                    class="w-64"
                  />
                  <Button variant="destructive" :disabled="loading" @click="handleDisableMfa">
                    禁用 MFA
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </TabsContent>
    </Tabs>
  </div>
</template>