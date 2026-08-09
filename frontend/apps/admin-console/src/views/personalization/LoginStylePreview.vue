<script setup lang="ts">
import { AuthPortalRenderer } from '@easy1auth/components'
import type { LoginStyleConfig, LoginStyleLegalDocuments } from '@easy1auth/types'
import { QrCode } from '@lucide/vue'

defineProps<{
  config: LoginStyleConfig
  legalDocuments: LoginStyleLegalDocuments
  tab: 'global' | 'standard' | 'qr'
  device: 'desktop' | 'mobile'
}>()
</script>

<template>
  <div class="preview-stage" :class="{ mobile: device === 'mobile' }">
    <div class="preview-device">
      <AuthPortalRenderer :config="config" :mode="tab === 'qr' ? 'qr' : 'login'" :legal="legalDocuments">
        <template #content>
          <div v-if="tab === 'qr'" class="qr-preview">
            <div class="qr-placeholder">
              <img v-if="config.qr.iconUrl" :src="config.qr.iconUrl" alt="扫码图标" />
              <QrCode v-else :size="54" aria-hidden="true" />
            </div>
            <strong>{{ config.qr.title || '扫码登录' }}</strong>
            <p>{{ config.qr.subtitle || '使用手机扫码继续' }}</p>
            <small>扫码登录暂未接入真实认证</small>
          </div>
          <div v-else class="mock-login-form">
            <div class="mock-methods">
              <span :class="{ active: config.standard.methods.includes('password') }">密码登录</span>
              <span :class="{ active: config.standard.methods.includes('email') }">邮箱验证码</span>
            </div>
            <div class="mock-input">{{ config.standard.methods.includes('password') ? '账号或邮箱' : '邮箱地址' }}</div>
            <div class="mock-input">{{ config.standard.methods.includes('password') ? '登录密码' : '验证码' }}</div>
            <div class="mock-button" :style="{ backgroundColor: config.global.primaryColor }">登录</div>
            <div v-if="config.standard.registrationEnabled" class="mock-register">还没有账号？立即注册</div>
          </div>
        </template>
        <template #legal>
          <div v-if="legalDocuments.termsOfService || legalDocuments.privacyPolicy" class="mock-legal">□ 我已阅读并同意服务条款和隐私政策</div>
        </template>
      </AuthPortalRenderer>
    </div>
  </div>
</template>

<style scoped>
.preview-stage { display: grid; place-items: center; min-height: 620px; padding: 2rem; overflow: auto; background: #e8eef4; }
.preview-device { width: min(100%, 900px); min-height: 560px; overflow: hidden; border: 1px solid #cbd5e1; border-radius: 1rem; box-shadow: 0 20px 50px rgba(15, 23, 42, .12); background: #fff; transition: width .25s ease; }
.preview-stage.mobile .preview-device { width: min(375px, 100%); min-height: 680px; border-radius: 1.75rem; }
.preview-device :deep(.portal-shell) { min-height: 560px; }
.preview-stage.mobile .preview-device :deep(.portal-shell) { min-height: 680px; }
.mock-login-form { display: grid; gap: .65rem; }
.mock-methods { display: flex; gap: 1rem; margin-bottom: .5rem; color: #7b8da1; font-size: .75rem; }
.mock-methods .active { color: var(--portal-primary); font-weight: 800; }
.mock-input { padding: .8rem .9rem; border: 1px solid #d7e2ec; border-radius: .7rem; color: #94a3b8; font-size: .8rem; background: #fff; }
.mock-button { padding: .8rem; border-radius: .7rem; color: #fff; text-align: center; font-size: .82rem; font-weight: 800; }
.mock-register, .mock-legal { color: #7b8da1; font-size: .72rem; text-align: center; }
.qr-preview { display: grid; justify-items: center; gap: .6rem; padding: .5rem 0 1rem; text-align: center; }
.qr-preview p, .qr-preview small { margin: 0; color: #7b8da1; }
.qr-placeholder { display: grid; place-items: center; width: 9rem; height: 9rem; border: 1px dashed #94a3b8; border-radius: 1rem; color: var(--portal-primary); background: #f8fafc; font-size: 3rem; }
.qr-placeholder img { width: 65%; height: 65%; object-fit: contain; }
.qr-preview small { color: #b45309; font-size: .68rem; }
@media (max-width: 700px) { .preview-stage { min-height: 540px; padding: 1rem; } }
</style>
