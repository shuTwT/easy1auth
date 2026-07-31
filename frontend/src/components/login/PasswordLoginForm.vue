<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { Input } from '@/components/antd-compat'
import { Checkbox } from '@/components/antd-compat'
import { Label } from '@/components/antd-compat'

const emit = defineEmits<{
  switchToEmail: []
  switchToRegister: []
}>()

const { loading, login, mfaChallenge, verifyMfa } = useAuth()

const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const showPassword = shallowRef(false)
const mfaCode = shallowRef('')

const errors = reactive({
  username: '',
  password: '',
})

function validate(): boolean {
  let valid = true
  if (!form.username) {
    errors.username = '请输入用户名或邮箱'
    valid = false
  } else {
    errors.username = ''
  }
  if (!form.password) {
    errors.password = '请输入密码'
    valid = false
  } else if (form.password.length < 6) {
    errors.password = '密码长度不能少于6位'
    valid = false
  } else {
    errors.password = ''
  }
  return valid
}

async function handleSubmit() {
  if (mfaChallenge.value) {
    if (!/^\d{6}$/.test(mfaCode.value)) { errors.password = '请输入6位动态验证码'; return }
    await verifyMfa(mfaCode.value)
    return
  }
  if (!validate()) return
  await login({
    username: form.username,
    password: form.password,
    loginType: 'password'
  })
}

function togglePassword() {
  showPassword.value = !showPassword.value
}
</script>

<template>
  <form @submit.prevent="handleSubmit">
    <div v-if="mfaChallenge" class="grid gap-2">
      <label class="form-label">动态验证码</label>
      <Input v-model="mfaCode" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="请输入6位验证码" class="login-input h-11" />
      <p class="text-xs text-muted-foreground">密码已验证，完成 MFA 后才会签发登录令牌。</p>
    </div>
    <template v-else>
    <div class="grid gap-2">
      <label class="form-label">用户名或邮箱</label>
      <div class="relative">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
          width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        >
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
        <Input
          v-model="form.username"
          placeholder="请输入用户名或邮箱"
          class="login-input h-11 pl-10"
          :class="{ 'border-destructive': errors.username }"
          @input="errors.username = ''"
        />
      </div>
      <p v-if="errors.username" class="text-destructive text-xs px-1">{{ errors.username }}</p>
    </div>

    <div class="grid gap-2 mt-4">
      <label class="form-label">密码</label>
      <div class="relative">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
          width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        >
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <Input
          v-model="form.password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="请输入密码"
          class="login-input h-11 pl-10 pr-10"
          :class="{ 'border-destructive': errors.password }"
          @input="errors.password = ''"
          @keyup.enter="handleSubmit"
        />
        <button
          type="button"
          class="password-toggle"
          @click="togglePassword"
          aria-label="切换密码可见性"
        >
          <svg v-if="!showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
            <line x1="1" y1="1" x2="23" y2="23"/>
          </svg>
        </button>
      </div>
      <p v-if="errors.password" class="text-destructive text-xs px-1">{{ errors.password }}</p>
    </div>

    <div class="form-options">
      <div class="flex items-center gap-2">
        <Checkbox id="remember-me" v-model:checked="form.rememberMe" />
        <Label for="remember-me" class="remember-label">记住我</Label>
      </div>
      <button type="button" class="forgot-password">
        忘记密码？
      </button>
    </div>
    </template>

    <button
      type="submit"
      class="submit-button"
      :disabled="loading"
    >
      <span v-if="loading" class="loading-spinner"></span>
      <span>{{ loading ? '验证中...' : (mfaChallenge ? '验证并登录' : '登录') }}</span>
    </button>

    <div class="form-footer">
      <span>没有账号？</span>
      <button type="button" class="link-button" @click="emit('switchToRegister')">
        去注册
      </button>
    </div>
  </form>
</template>

<style scoped>
.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #0C4A6E;
}

.login-input {
  background: rgba(255, 255, 255, 0.6) !important;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.login-input:focus {
  background: white;
  border-color: #0369A1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

.password-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #64748B;
  transition: color 0.2s ease;
}

.password-toggle:hover {
  color: #0369A1;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 24px 0;
}

.remember-label {
  font-size: 14px;
  color: #64748B;
  cursor: pointer;
}

.forgot-password {
  background: none;
  border: none;
  font-size: 14px;
  color: #0369A1;
  cursor: pointer;
  transition: color 0.2s ease;
}

.forgot-password:hover {
  color: #0EA5E9;
}

.submit-button {
  width: 100%;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(3, 105, 161, 0.3);
}

.submit-button:active:not(:disabled) {
  transform: translateY(0);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.link-button {
  background: none;
  border: none;
  font-size: 14px;
  color: #0369A1;
  cursor: pointer;
  transition: color 0.2s ease;
}

.link-button:hover {
  color: #0EA5E9;
}

.divider {
  color: #CBD5E1;
}

@media (prefers-reduced-motion: reduce) {
  .submit-button,
  .password-toggle,
  .forgot-password,
  .link-button {
    transition: none;
  }
  
  .loading-spinner {
    animation: none;
  }
}
</style>
