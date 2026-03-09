<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { securityApi, type PasswordPolicy, type MfaStatus } from '@/api/security'

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
    ElMessage.warning('请填写所有密码字段')
    return
  }

  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }

  try {
    loading.value = true
    await securityApi.changePassword(passwordForm.value)
    ElMessage.success('密码修改成功')
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    loadSecurityData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '密码修改失败')
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
    ElMessage.error(error.response?.data?.message || 'MFA设置失败')
  } finally {
    loading.value = false
  }
}

const handleEnableMfa = async () => {
  if (!mfaToken.value) {
    ElMessage.warning('请输入验证码')
    return
  }

  try {
    loading.value = true
    await securityApi.enableMfa(mfaToken.value)
    ElMessage.success('MFA已启用')
    showMfaSetup.value = false
    mfaToken.value = ''
    loadSecurityData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '启用MFA失败')
  } finally {
    loading.value = false
  }
}

const handleDisableMfa = async () => {
  if (!mfaToken.value) {
    ElMessage.warning('请输入验证码')
    return
  }

  try {
    loading.value = true
    await securityApi.disableMfa(mfaToken.value)
    ElMessage.success('MFA已禁用')
    mfaToken.value = ''
    loadSecurityData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '禁用MFA失败')
  } finally {
    loading.value = false
  }
}

const copyBackupCodes = () => {
  navigator.clipboard.writeText(mfaSetupData.value.backupCodes.join('\n'))
  ElMessage.success('备用码已复制到剪贴板')
}

onMounted(() => {
  loadSecurityData()
})
</script>

<template>
  <div class="security-settings">
    <div class="page-header">
      <h1>安全设置</h1>
      <p>管理您的账户安全选项</p>
    </div>

    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="密码管理" name="password">
        <el-card v-loading="loading" class="settings-card">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
              <el-tag v-if="expiryStatus.expired" type="danger">密码已过期</el-tag>
              <el-tag v-else-if="expiryStatus.daysUntilExpiry <= 7" type="warning">
                密码将在 {{ expiryStatus.daysUntilExpiry }} 天后过期
              </el-tag>
            </div>
          </template>

          <div v-if="passwordPolicy" class="password-policy">
            <h4>密码要求</h4>
            <ul>
              <li :class="{ active: passwordForm.newPassword.length >= passwordPolicy.minLength }">
                至少 {{ passwordPolicy.minLength }} 个字符
              </li>
              <li v-if="passwordPolicy.requireUppercase" :class="{ active: /[A-Z]/.test(passwordForm.newPassword) }">
                包含大写字母
              </li>
              <li v-if="passwordPolicy.requireLowercase" :class="{ active: /[a-z]/.test(passwordForm.newPassword) }">
                包含小写字母
              </li>
              <li v-if="passwordPolicy.requireNumbers" :class="{ active: /\d/.test(passwordForm.newPassword) }">
                包含数字
              </li>
              <li v-if="passwordPolicy.requireSpecialChars" :class="{ active: /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/.test(passwordForm.newPassword) }">
                包含特殊字符
              </li>
            </ul>
          </div>

          <el-form :model="passwordForm" label-width="100px" class="password-form">
            <el-form-item label="当前密码">
              <el-input
                v-model="passwordForm.currentPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>

            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
              <div v-if="passwordForm.newPassword" class="password-strength">
                <span>密码强度：</span>
                <el-progress
                  :percentage="passwordStrength.level * 33.33"
                  :color="passwordStrength.color"
                  :show-text="false"
                />
                <span :style="{ color: passwordStrength.color }">{{ passwordStrength.text }}</span>
              </div>
            </el-form-item>

            <el-form-item label="确认密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="loading">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="多因素认证" name="mfa">
        <el-card v-loading="loading" class="settings-card">
          <template #header>
            <div class="card-header">
              <span>多因素认证 (MFA)</span>
              <el-tag :type="mfaStatus.enabled ? 'success' : 'info'">
                {{ mfaStatus.enabled ? '已启用' : '未启用' }}
              </el-tag>
            </div>
          </template>

          <div v-if="!showMfaSetup && !mfaStatus.enabled" class="mfa-intro">
            <div class="mfa-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <h3>增强账户安全</h3>
            <p>启用多因素认证后，登录时需要输入验证码，大大提高账户安全性。</p>
            <el-button type="primary" @click="handleSetupMfa">
              启用 MFA
            </el-button>
          </div>

          <div v-else-if="showMfaSetup" class="mfa-setup">
            <div class="setup-step">
              <h4>步骤 1：扫描二维码</h4>
              <p>使用 Google Authenticator 或其他 TOTP 应用扫描以下二维码：</p>
              <div class="qr-code">
                <img :src="mfaSetupData.qrCodeUrl" alt="MFA QR Code" />
              </div>
              <p class="secret-key">
                或手动输入密钥：<code>{{ mfaSetupData.secret }}</code>
              </p>
            </div>

            <div class="setup-step">
              <h4>步骤 2：保存备用码</h4>
              <p>请保存以下备用码，当无法使用验证器时可用来登录：</p>
              <div class="backup-codes">
                <code v-for="(code, index) in mfaSetupData.backupCodes" :key="index">
                  {{ code }}
                </code>
              </div>
              <el-button size="small" @click="copyBackupCodes">复制备用码</el-button>
            </div>

            <div class="setup-step">
              <h4>步骤 3：验证设置</h4>
              <p>请输入验证器显示的 6 位数字验证码：</p>
              <el-input
                v-model="mfaToken"
                placeholder="请输入验证码"
                maxlength="6"
                style="width: 200px"
              />
              <div class="setup-actions">
                <el-button @click="showMfaSetup = false">取消</el-button>
                <el-button type="primary" @click="handleEnableMfa" :loading="loading">
                  确认启用
                </el-button>
              </div>
            </div>
          </div>

          <div v-else class="mfa-enabled">
            <div class="mfa-status">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <h3>MFA 已启用</h3>
              <p>当前认证方式：{{ mfaStatus.type === 'totp' ? '验证器应用' : '邮箱验证码' }}</p>
            </div>

            <el-divider />

            <div class="disable-mfa">
              <h4>禁用 MFA</h4>
              <p>禁用后，登录时将不再需要验证码。</p>
              <el-input
                v-model="mfaToken"
                placeholder="请输入验证码以禁用 MFA"
                maxlength="6"
                style="width: 250px; margin-right: 12px"
              />
              <el-button type="danger" @click="handleDisableMfa" :loading="loading">
                禁用 MFA
              </el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.security-settings {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #0F172A;
}

.page-header p {
  margin: 0;
  color: #64748B;
  font-size: 14px;
}

.settings-tabs {
  background: white;
  border-radius: 12px;
  padding: 16px;
}

.settings-card {
  border: none;
  box-shadow: none;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.password-policy {
  background: #F8FAFC;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
}

.password-policy h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.password-policy ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.password-policy li {
  padding: 6px 0;
  color: #94A3B8;
  font-size: 13px;
}

.password-policy li::before {
  content: '○';
  margin-right: 8px;
}

.password-policy li.active {
  color: #10B981;
}

.password-policy li.active::before {
  content: '●';
}

.password-form {
  max-width: 400px;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 13px;
}

.password-strength .el-progress {
  width: 100px;
}

.mfa-intro,
.mfa-status {
  text-align: center;
  padding: 40px 20px;
}

.mfa-icon {
  margin-bottom: 16px;
  color: #0369A1;
}

.mfa-intro h3,
.mfa-status h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #0F172A;
}

.mfa-intro p,
.mfa-status p {
  margin: 0 0 24px 0;
  color: #64748B;
}

.mfa-setup {
  max-width: 500px;
  margin: 0 auto;
}

.setup-step {
  margin-bottom: 32px;
}

.setup-step h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #0F172A;
}

.setup-step p {
  margin: 0 0 12px 0;
  color: #64748B;
  font-size: 14px;
}

.qr-code {
  display: flex;
  justify-content: center;
  padding: 16px;
  background: white;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  margin-bottom: 12px;
}

.qr-code img {
  width: 200px;
  height: 200px;
}

.secret-key {
  font-size: 13px;
}

.secret-key code {
  background: #F1F5F9;
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
}

.backup-codes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.backup-codes code {
  background: #F1F5F9;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
}

.setup-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.disable-mfa h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #0F172A;
}

.disable-mfa p {
  margin: 0 0 12px 0;
  color: #64748B;
  font-size: 13px;
}
</style>
