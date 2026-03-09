<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applicationApi } from '@/api/application'

const route = useRoute()

const loading = ref(true)
const authorizing = ref(false)
const application = ref<any>(null)
const scopes = ref<string[]>([])
const error = ref('')

const client_id = route.query.client_id as string
const redirect_uri = route.query.redirect_uri as string
const response_type = route.query.response_type as string
const scope = route.query.scope as string
const state = route.query.state as string
const code_challenge = route.query.code_challenge as string
const code_challenge_method = route.query.code_challenge_method as string

const loadApplicationInfo = async () => {
  try {
    if (!client_id || !redirect_uri || !response_type) {
      error.value = '缺少必要的授权参数'
      loading.value = false
      return
    }

    const res = await applicationApi.getList({
      page: 1,
      pageSize: 100
    })

    const apps = res.data.applications || []
    const app = apps.find((a: any) => a.clientId === client_id)
    
    if (!app) {
      error.value = '应用不存在或未授权'
      loading.value = false
      return
    }

    application.value = app
    
    if (scope) {
      scopes.value = scope.split(' ')
    }

    loading.value = false
  } catch (err: any) {
    console.error('加载应用信息失败:', err)
    error.value = err.response?.data?.message || '加载应用信息失败'
    loading.value = false
  }
}

const handleAuthorize = async () => {
  try {
    authorizing.value = true

    const params = new URLSearchParams({
      client_id,
      redirect_uri,
      response_type,
      ...(scope && { scope }),
      ...(state && { state }),
      ...(code_challenge && { code_challenge }),
      ...(code_challenge_method && { code_challenge_method })
    })

    window.location.href = `/oauth2/authorize?${params.toString()}`
  } catch (err: any) {
    console.error('授权失败:', err)
    ElMessage.error(err.response?.data?.error || '授权失败')
    authorizing.value = false
  }
}

const handleDeny = () => {
  const redirectUrl = new URL(redirect_uri)
  redirectUrl.searchParams.set('error', 'access_denied')
  if (state) {
    redirectUrl.searchParams.set('state', state)
  }
  window.location.href = redirectUrl.toString()
}

onMounted(() => {
  loadApplicationInfo()
})
</script>

<template>
  <div class="oauth-container">
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="oauth-card">
      <div class="oauth-header">
        <div class="logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="url(#gradient)"/>
            <path d="M24 12L32 18V30L24 36L16 30V18L24 12Z" stroke="white" stroke-width="2" fill="none"/>
            <circle cx="24" cy="24" r="4" fill="white"/>
            <defs>
              <linearGradient id="gradient" x1="0" y1="0" x2="48" y2="48">
                <stop offset="0%" stop-color="#0369A1"/>
                <stop offset="100%" stop-color="#0EA5E9"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <h1 class="title">Easy1Auth</h1>
        <p class="subtitle">授权请求</p>
      </div>

      <div v-loading="loading" class="oauth-content">
        <template v-if="error">
          <div class="error-state">
            <div class="error-icon">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            </div>
            <h2 class="error-title">授权失败</h2>
            <p class="error-message">{{ error }}</p>
            <div class="error-actions">
              <button class="btn btn-primary" @click="handleDeny">
                返回应用
              </button>
            </div>
          </div>
        </template>

        <template v-else-if="application">
          <div class="application-info">
            <div class="app-avatar">
              <img v-if="application.logo" :src="application.logo" :alt="application.name" />
              <span v-else class="app-avatar-text">{{ application.name.charAt(0).toUpperCase() }}</span>
            </div>
            <h2 class="app-name">{{ application.name }}</h2>
            <p v-if="application.description" class="app-description">
              {{ application.description }}
            </p>
          </div>

          <div class="divider"></div>

          <div class="permissions">
            <h3 class="permissions-title">该应用请求以下权限</h3>
            <ul class="scope-list">
              <li v-if="scopes.includes('openid')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                <span>获取您的基本信息</span>
              </li>
              <li v-if="scopes.includes('profile')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
                <span>访问您的个人资料</span>
              </li>
              <li v-if="scopes.includes('email')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
                <span>访问您的邮箱地址</span>
              </li>
              <li v-if="scopes.includes('phone')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
                </svg>
                <span>访问您的手机号码</span>
              </li>
              <li v-if="!scopes.length || scopes.every(s => !['openid', 'profile', 'email', 'phone'].includes(s))">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
                <span>基本访问权限</span>
              </li>
            </ul>
          </div>

          <div class="divider"></div>

          <div class="authorize-actions">
            <button 
              class="btn btn-secondary" 
              @click="handleDeny" 
              :disabled="authorizing"
            >
              拒绝
            </button>
            <button 
              class="btn btn-primary" 
              @click="handleAuthorize" 
              :disabled="authorizing"
            >
              {{ authorizing ? '授权中...' : '授权' }}
            </button>
          </div>

          <div class="authorize-notice">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
            <span>授权后，该应用将可以访问上述信息</span>
          </div>
        </template>
      </div>

      <div class="oauth-footer">
        <p>© 2024 Easy1Auth. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@300;400;500;600;700&family=Poppins:wght@400;500;600;700&display=swap');

.oauth-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 50%, #22C55E 100%);
  font-family: 'Open Sans', sans-serif;
  overflow: hidden;
}

.background-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
  animation: float 20s infinite ease-in-out;
}

.shape-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  right: -50px;
  animation: float 15s infinite ease-in-out reverse;
}

.shape-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
  animation: float 18s infinite ease-in-out;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

.oauth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
  margin: 24px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.oauth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  display: inline-block;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-family: 'Poppins', sans-serif;
  font-size: 32px;
  font-weight: 700;
  color: #0C4A6E;
  letter-spacing: -0.5px;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #64748B;
  font-weight: 400;
}

.oauth-content {
  min-height: 300px;
}

.error-state {
  text-align: center;
  padding: 24px 0;
}

.error-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  margin-bottom: 24px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 50%;
  color: #EF4444;
}

.error-title {
  margin: 0 0 12px;
  font-family: 'Poppins', sans-serif;
  font-size: 24px;
  font-weight: 600;
  color: #0C4A6E;
}

.error-message {
  margin: 0 0 24px;
  font-size: 14px;
  color: #64748B;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.application-info {
  text-align: center;
  padding: 16px 0;
}

.app-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
  border-radius: 16px;
  overflow: hidden;
}

.app-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-avatar-text {
  font-family: 'Poppins', sans-serif;
  font-size: 28px;
  font-weight: 600;
  color: white;
}

.app-name {
  margin: 0 0 8px;
  font-family: 'Poppins', sans-serif;
  font-size: 22px;
  font-weight: 600;
  color: #0C4A6E;
}

.app-description {
  margin: 0;
  font-size: 14px;
  color: #64748B;
  line-height: 1.5;
}

.divider {
  height: 1px;
  background: rgba(148, 163, 184, 0.2);
  margin: 24px 0;
}

.permissions {
  padding: 0;
}

.permissions-title {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #0C4A6E;
}

.scope-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.scope-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: rgba(241, 245, 249, 0.5);
  border-radius: 8px;
  font-size: 14px;
  color: #334155;
}

.scope-list li svg {
  flex-shrink: 0;
  color: #0369A1;
}

.authorize-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-family: 'Open Sans', sans-serif;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(3, 105, 161, 0.4);
}

.btn-secondary {
  background: rgba(241, 245, 249, 0.8);
  color: #475569;
}

.btn-secondary:hover:not(:disabled) {
  background: rgba(226, 232, 240, 0.9);
}

.authorize-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(251, 191, 36, 0.1);
  border-radius: 8px;
  font-size: 13px;
  color: #B45309;
}

.authorize-notice svg {
  flex-shrink: 0;
}

.oauth-footer {
  margin-top: 32px;
  text-align: center;
  font-size: 12px;
  color: #94A3B8;
}

@media (max-width: 768px) {
  .oauth-card {
    margin: 16px;
    padding: 32px 24px;
  }

  .title {
    font-size: 28px;
  }

  .authorize-actions {
    flex-direction: column-reverse;
  }

  .error-actions {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .shape {
    animation: none;
  }
}
</style>
