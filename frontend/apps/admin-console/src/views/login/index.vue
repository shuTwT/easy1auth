<script setup lang="ts">
import { shallowRef } from 'vue'
import { Tabs } from 'antdv-next'
import PasswordLoginForm from '@/components/login/PasswordLoginForm.vue'
import EmailCodeLoginForm from '@/components/login/EmailCodeLoginForm.vue'
import PasskeyLogin from '@/components/login/PasskeyLogin.vue'
import RegisterForm from '@/components/login/RegisterForm.vue'

type LoginMode = 'password' | 'email' | 'passkey' | 'register'

const currentMode = shallowRef<LoginMode>('password')
const loginTabs = [
  { key: 'password', label: '账号密码' },
  { key: 'email', label: '邮箱验证码' },
  { key: 'passkey', label: 'Passkey' }
]

function switchToPassword() {
  currentMode.value = 'password'
}

function switchToEmail() {
  currentMode.value = 'email'
}

function switchToRegister() {
  currentMode.value = 'register'
}

</script>

<template>
  <div class="login-container">
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="login-card">
      <div class="login-header flex items-center justify-center">
        <div class="logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="url(#gradient-default)"/>
            <path d="M24 12L32 18V30L24 36L16 30V18L24 12Z" stroke="white" stroke-width="2" fill="none"/>
            <circle cx="24" cy="24" r="4" fill="white"/>
            <defs>
              <linearGradient id="gradient-default" x1="0" y1="0" x2="48" y2="48">
                <stop offset="0%" stop-color="#0369A1"/>
                <stop offset="100%" stop-color="#0EA5E9"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div class="flex flex-col">
          <h1 class="title">Easy1Auth</h1>
          <p class="subtitle">企业级统一身份管理平台</p>
        </div>
      </div>

      <Tabs
        v-if="currentMode !== 'register'"
        v-model:active-key="currentMode"
        :items="loginTabs"
        :animated="false"
        class="login-tabs"
      />

      <div class="login-content">
        <transition name="fade" mode="out-in">
          <PasswordLoginForm
            v-if="currentMode === 'password'"
            @switch-to-email="switchToEmail"
            @switch-to-register="switchToRegister"
          />
          <EmailCodeLoginForm
            v-else-if="currentMode === 'email'"
            @switch-to-password="switchToPassword"
            @switch-to-register="switchToRegister"
          />
          <PasskeyLogin v-else-if="currentMode === 'passkey'" />
          <RegisterForm
            v-else
            @switch-to-login="switchToPassword"
          />
        </transition>
      </div>

      <div class="login-footer">
        <p>© 2024 Easy1Auth. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Open+Sans:wght@300;400;500;600;700&family=Poppins:wght@400;500;600;700&display=swap');

.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #0369A1 0%, #0EA5E9 50%, #22C55E 100%);
  background-size: cover;
  background-position: center;
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

.login-card {
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

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: inline-block;
  margin-right: 16px;
}

.logo-image {
  max-width: 120px;
  max-height: 48px;
  object-fit: contain;
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

.login-tabs {
  margin-bottom: 16px;
}

.login-tabs :deep(.ant-tabs-nav) {
  margin: 0;
  padding: 4px;
  background: rgba(241, 245, 249, 0.5);
  border-radius: 8px;
}

.login-tabs :deep(.ant-tabs-nav::before),
.login-tabs :deep(.ant-tabs-ink-bar),
.login-tabs :deep(.ant-tabs-body-holder) {
  display: none;
}

.login-tabs :deep(.ant-tabs-nav-wrap),
.login-tabs :deep(.ant-tabs-nav-list) {
  width: 100%;
}

.login-tabs :deep(.ant-tabs-tab) {
  display: flex;
  flex: 1;
  justify-content: center;
  margin: 0;
  padding: 10px 16px;
  border-radius: 6px;
  color: #64748B;
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.login-tabs :deep(.ant-tabs-tab:hover) {
  color: #0369A1;
}

.login-tabs :deep(.ant-tabs-tab-active) {
  background: white;
  color: #0369A1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.login-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: inherit;
}

.login-content {
  min-height: 320px;
}

.login-footer {
  margin-top: 32px;
  text-align: center;
  font-size: 12px;
  color: #94A3B8;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .login-card {
    margin: 16px;
    padding: 32px 24px;
  }

  .title {
    font-size: 28px;
  }

  .login-tabs :deep(.ant-tabs-nav-list) {
    flex-direction: column;
  }

}

@media (prefers-reduced-motion: reduce) {
  .shape {
    animation: none;
  }
  
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
