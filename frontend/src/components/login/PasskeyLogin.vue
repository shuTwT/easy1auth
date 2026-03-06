<script setup lang="ts">
// import { Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuth } from '@/composables/useAuth'

const { loading, passkeyLogin } = useAuth()

async function handlePasskeyLogin() {
  if (!window.PublicKeyCredential) {
    ElMessage.error('您的浏览器不支持 Passkey，请使用现代浏览器')
    return
  }
  
  await passkeyLogin()
}
</script>

<template>
  <div class="passkey-login">
    <div class="passkey-info">
      <div class="passkey-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="m21 2-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0 3 3L22 7l-3-3m-3.5 3.5L19 4"/>
        </svg>
      </div>
      <h3>Passkey 登录</h3>
      <p>使用生物识别或安全密钥快速登录</p>
    </div>

    <button
      type="button"
      class="submit-button"
      :disabled="loading"
      @click="handlePasskeyLogin"
    >
      <span v-if="loading" class="loading-spinner"></span>
      <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m21 2-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0 3 3L22 7l-3-3m-3.5 3.5L19 4"/>
      </svg>
      <span>{{ loading ? '登录中...' : '使用 Passkey 登录' }}</span>
    </button>

    <div class="passkey-tips">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          Passkey 是一种更安全、更便捷的登录方式
        </template>
        <p>支持指纹、面部识别或安全密钥</p>
      </el-alert>
    </div>
  </div>
</template>

<style scoped>
.passkey-login {
  text-align: center;
}

.passkey-info {
  margin-bottom: 32px;
}

.passkey-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
  border-radius: 50%;
  color: white;
}

.passkey-info h3 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #0C4A6E;
}

.passkey-info p {
  margin: 0;
  font-size: 14px;
  color: #64748B;
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

.passkey-tips {
  margin-top: 24px;
  text-align: left;
}

.passkey-tips :deep(.el-alert__title) {
  font-size: 14px;
  font-weight: 500;
}

.passkey-tips p {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748B;
}

@media (prefers-reduced-motion: reduce) {
  .submit-button {
    transition: none;
  }
  
  .loading-spinner {
    animation: none;
  }
}
</style>
