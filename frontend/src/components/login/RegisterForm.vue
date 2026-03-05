<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useAuth } from '@/composables/useAuth'

const emit = defineEmits<{
  switchToLogin: []
}>()

const { loading, sendingCode, countdown, isCountingDown, register, sendCode } = useAuth()

const form = reactive({
  email: '',
  password: '',
  code: '',
  username: ''
})

const showPassword = shallowRef(false)

async function handleSendCode() {
  if (!form.email) {
    return
  }
  
  await sendCode({
    email: form.email,
    type: 'register'
  })
}

async function handleSubmit() {
  await register({
    email: form.email,
    password: form.password,
    code: form.code,
    username: form.username || undefined
  })
}
</script>

<template>
  <el-form :model="form" @submit.prevent="handleSubmit">
    <el-form-item>
      <label class="form-label">邮箱地址</label>
      <el-input
        v-model="form.email"
        placeholder="请输入邮箱地址"
        size="large"
        clearable
        class="custom-input"
      >
        <template #prefix>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
            <polyline points="22,6 12,13 2,6"/>
          </svg>
        </template>
      </el-input>
    </el-form-item>

    <el-form-item>
      <label class="form-label">验证码</label>
      <div class="code-input-wrapper">
        <el-input
          v-model="form.code"
          placeholder="请输入6位验证码"
          size="large"
          maxlength="6"
          class="custom-input"
        >
          <template #prefix>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
          </template>
        </el-input>
        <button
          type="button"
          class="code-button"
          :disabled="isCountingDown || !form.email"
          @click="handleSendCode"
        >
          {{ isCountingDown ? `${countdown}s后重试` : '获取验证码' }}
        </button>
      </div>
    </el-form-item>

    <el-form-item>
      <label class="form-label">用户名（可选）</label>
      <el-input
        v-model="form.username"
        placeholder="请输入用户名"
        size="large"
        clearable
        class="custom-input"
      >
        <template #prefix>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
        </template>
      </el-input>
    </el-form-item>

    <el-form-item>
      <label class="form-label">密码</label>
      <el-input
        v-model="form.password"
        :type="showPassword ? 'text' : 'password'"
        placeholder="请输入密码（至少8位，包含大小写字母和数字）"
        size="large"
        class="custom-input"
      >
        <template #prefix>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
        </template>
        <template #suffix>
          <button
            type="button"
            class="password-toggle"
            @click="showPassword = !showPassword"
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
        </template>
      </el-input>
    </el-form-item>

    <button
      type="submit"
      class="submit-button"
      :disabled="loading"
      @click="handleSubmit"
    >
      <span v-if="loading" class="loading-spinner"></span>
      <span>{{ loading ? '注册中...' : '立即注册' }}</span>
    </button>

    <div class="form-footer">
      <span>已有账号？</span>
      <button type="button" class="link-button" @click="emit('switchToLogin')">
        立即登录
      </button>
    </div>
  </el-form>
</template>

<style scoped>
.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}

.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(59, 130, 246, 0.3);
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.9);
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.custom-input :deep(.el-input__inner) {
  color: #1f2937;
  font-size: 14px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: #9ca3af;
}

.code-input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-input-wrapper .custom-input {
  flex: 1;
}

.code-button {
  padding: 0 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
  height: 40px;
}

.code-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.code-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.password-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  color: #9ca3af;
  transition: color 0.3s ease;
}

.password-toggle:hover {
  color: #3b82f6;
}

.submit-button {
  width: 100%;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
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
  margin-top: 24px;
  font-size: 14px;
  color: #6b7280;
}

.link-button {
  background: none;
  border: none;
  color: #3b82f6;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  margin-left: 4px;
  transition: color 0.3s ease;
}

.link-button:hover {
  color: #2563eb;
  text-decoration: underline;
}
</style>
