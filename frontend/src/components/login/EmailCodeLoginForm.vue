<script setup lang="ts">
import { reactive } from 'vue'
import { useAuth } from '@/composables/useAuth'
const emit = defineEmits<{
  switchToPassword: []
  switchToRegister: []
}>()

const { loading, sendingCode, countdown, isCountingDown, login, sendCode } = useAuth()

const form = reactive({
  email: '',
  code: ''
})

const errors = reactive({
  email: '',
  code: '',
})

function validate(): boolean {
  let valid = true
  if (!form.email) {
    errors.email = '请输入邮箱地址'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '请输入正确的邮箱地址'
    valid = false
  } else {
    errors.email = ''
  }
  if (!form.code) {
    errors.code = '请输入验证码'
    valid = false
  } else if (form.code.length !== 6) {
    errors.code = '验证码长度为6位'
    valid = false
  } else {
    errors.code = ''
  }
  return valid
}

async function handleSendCode() {
  if (!form.email) {
    errors.email = '请输入邮箱地址'
    return
  }
  errors.email = ''
  await sendCode({
    email: form.email,
    type: 'login'
  })
}

async function handleSubmit() {
  if (!validate()) return
  await login({
    email: form.email,
    code: form.code,
    loginType: 'email'
  })
}
</script>

<template>
  <form @submit.prevent="handleSubmit">
    <div class="grid gap-2">
      <label class="form-label">邮箱地址</label>
      <div class="relative">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
          width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        >
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
          <polyline points="22,6 12,13 2,6"/>
        </svg>
        <Input v-model:value="form.email"
          placeholder="请输入邮箱地址"
          class="login-input h-11 pl-10"
          :class="{ 'border-destructive': errors.email }"
          @input="errors.email = ''"
        />
      </div>
      <p v-if="errors.email" class="text-destructive text-xs px-1">{{ errors.email }}</p>
    </div>

    <div class="grid gap-2 mt-4">
      <label class="form-label">验证码</label>
      <div class="code-input-wrapper">
        <div class="relative flex-1">
          <svg
            class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none"
            width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          >
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
          <Input v-model:value="form.code"
            placeholder="请输入6位验证码"
            maxlength="6"
            class="login-input h-11 pl-10"
            :class="{ 'border-destructive': errors.code }"
            @input="errors.code = ''"
            @keyup.enter="handleSubmit"
          />
        </div>
        <button
          type="button"
          class="code-button"
          :disabled="isCountingDown || !form.email"
          :class="{ 'is-loading': sendingCode }"
          @click="handleSendCode"
        >
          <span v-if="sendingCode" class="loading-spinner"></span>
          <span>{{ isCountingDown ? `${countdown}s后重试` : '获取验证码' }}</span>
        </button>
      </div>
      <p v-if="errors.code" class="text-destructive text-xs px-1">{{ errors.code }}</p>
    </div>

    <button
      type="submit"
      class="submit-button"
      :disabled="loading"
    >
      <span v-if="loading" class="loading-spinner"></span>
      <span>{{ loading ? '登录中...' : '登录' }}</span>
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
  padding-left: 40px !important;
  transition: all 0.2s ease;
}

.login-input:focus {
  background: white;
  border-color: #0369A1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

.code-input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-button {
  padding: 0 20px;
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
}

.code-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

.code-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
  margin-top: 24px;
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
  .code-button,
  .submit-button,
  .link-button {
    transition: none;
  }

  .loading-spinner {
    animation: none;
  }
}
</style>
