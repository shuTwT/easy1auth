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

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' }
  ]
}

async function handleSendCode() {
  if (!form.email) {
    return
  }
  
  await sendCode({
    email: form.email,
    type: 'login'
  })
}

async function handleSubmit() {
  await login({
    email: form.email,
    code: form.code,
    loginType: 'email'
  })
}
</script>

<template>
  <el-form :model="form" :rules="rules" @submit.prevent="handleSubmit">
    <el-form-item prop="email">
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

    <el-form-item prop="code">
      <label class="form-label">验证码</label>
      <div class="code-input-wrapper">
        <el-input
          v-model="form.code"
          placeholder="请输入6位验证码"
          size="large"
          maxlength="6"
          class="custom-input"
          @keyup.enter="handleSubmit"
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
          :class="{ 'is-loading': sendingCode }"
          @click="handleSendCode"
        >
          <span v-if="sendingCode" class="loading-spinner"></span>
          <span>{{ isCountingDown ? `${countdown}s后重试` : '获取验证码' }}</span>
        </button>
      </div>
    </el-form-item>

    <el-form-item>
      <button
        type="submit"
        class="submit-button"
        :disabled="loading"
        @click="handleSubmit"
      >
        <span v-if="loading" class="loading-spinner"></span>
        <span>{{ loading ? '登录中...' : '登录' }}</span>
      </button>
    </el-form-item>

    <div class="form-footer">
      <button
        type="button"
        class="link-button"
        @click="emit('switchToPassword')"
      >
        账号密码登录
      </button>
      <span class="divider">|</span>
      <button
        type="button"
        class="link-button"
        @click="emit('switchToRegister')"
      >
        立即注册
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
  color: #0C4A6E;
}

.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  box-shadow: none;
  transition: all 0.2s ease;
}

.custom-input :deep(.el-input__wrapper:hover) {
  border-color: #0369A1;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  background: white;
  border-color: #0369A1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
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
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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
