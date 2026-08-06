<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { BrandMark } from '@easy1auth/components'
import type { AuthInteractionContext } from '@easy1auth/types'
import { safeCssColor } from '@easy1auth/utils'
import { authPortalApi } from './api'

const route = useRoute()
const context = ref<AuthInteractionContext | null>(null)
const loading = ref(true)
const submitting = ref(false)
const username = ref('')
const password = ref('')
const code = ref('')
const toast = ref('')

const isConsentRoute = computed(() => route.path === '/oauth-consent')
const title = computed(() => context.value?.style?.title ?? 'Easy1Auth')
const style = computed(() => context.value?.style)
const primary = computed(() => safeCssColor(style.value?.primaryColor, '#0369A1'))
const background = computed(() => safeCssColor(style.value?.backgroundColor, '#F5F7FA'))

function setTheme() {
  document.documentElement.style.setProperty('--easy1auth-primary', primary.value)
  document.documentElement.style.setProperty('--easy1auth-background', background.value)
}

async function load() {
  loading.value = true
  toast.value = ''
  try {
    context.value = isConsentRoute.value ? await authPortalApi.getConsent() : await authPortalApi.getInteraction()
    setTheme()
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '页面加载失败，请重新发起请求'
  } finally {
    loading.value = false
  }
}

async function login() {
  if (!username.value.trim() || !password.value) return
  submitting.value = true
  try {
    const result = await authPortalApi.login(username.value.trim(), password.value, context.value?.csrfToken)
    password.value = ''
    if (result.status === 'mfa_required') {
      context.value = { ...(context.value as AuthInteractionContext), status: 'mfa', message: '请输入身份验证器中的动态验证码' }
      toast.value = '请输入动态验证码'
    } else {
      window.location.assign(result.redirectUrl ?? '/')
    }
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '登录失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function verifyMfa() {
  if (!code.value.trim()) return
  submitting.value = true
  try {
    const result = await authPortalApi.verifyMfa(code.value.trim(), context.value?.csrfToken)
    window.location.assign(result.redirectUrl ?? '/')
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '验证码无效或已过期'
  } finally {
    submitting.value = false
  }
}

async function consent(action: 'approve' | 'deny') {
  submitting.value = true
  try {
    const result = await authPortalApi.consent(action, context.value?.csrfToken)
    window.location.assign(result.location)
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '授权请求已失效'
  } finally {
    submitting.value = false
  }
}

function federationUrl(provider: string) {
  if (!context.value?.tenantId) return '#'
  return `/t/${context.value.tenantId}/federation/${provider}/authorize`
}

watch(() => route.path, load)
onMounted(load)
</script>

<template>
  <main class="portal-shell">
    <section class="portal-card" :style="{ '--portal-primary': primary }">
      <header class="portal-header">
        <BrandMark :logo="style?.logo" :title="title" size="lg" />
        <span class="portal-secure">安全连接</span>
      </header>

      <div v-if="toast" class="portal-toast" role="alert" aria-live="assertive">{{ toast }}</div>
      <div v-if="loading" class="portal-state" aria-live="polite"><span class="spinner"></span><span>正在准备安全登录…</span></div>
      <template v-else-if="context?.status === 'expired'">
        <div class="portal-state"><div class="state-icon">!</div><h1>请求已过期</h1><p>请从原应用重新发起登录或授权。</p></div>
      </template>
      <template v-else-if="isConsentRoute || context?.status === 'consent'">
        <div class="portal-copy"><span class="eyebrow">授权确认</span><h1>{{ context?.clientName ?? '应用' }} 请求访问</h1><p>确认后，该应用将获得以下权限。</p></div>
        <ul class="scope-list">
          <li v-for="scope in context?.scopes ?? []" :key="scope"><span class="scope-dot"></span><span>{{ scope }}</span></li>
        </ul>
        <div class="portal-actions"><button class="button button-secondary" :disabled="submitting" @click="consent('deny')">拒绝</button><button class="button button-primary" :disabled="submitting" @click="consent('approve')">{{ submitting ? '处理中…' : '同意并继续' }}</button></div>
      </template>
      <template v-else-if="context?.status === 'mfa'">
        <div class="portal-copy"><span class="eyebrow">第二步验证</span><h1>验证你的身份</h1><p>请输入身份验证器生成的 6 位动态验证码。</p></div>
        <form class="portal-form" @submit.prevent="verifyMfa"><label for="mfa-code">动态验证码</label><input id="mfa-code" v-model="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="000000" autofocus><button class="button button-primary" :disabled="submitting || !code">{{ submitting ? '验证中…' : '验证并继续' }}</button></form>
      </template>
      <template v-else>
        <div class="portal-copy"><span class="eyebrow">安全登录</span><h1>欢迎回来</h1><p>{{ style?.subtitle ?? '企业级身份管理平台' }}</p></div>
        <form class="portal-form" @submit.prevent="login"><label for="username">用户名或邮箱</label><input id="username" v-model="username" autocomplete="username" placeholder="请输入用户名或邮箱" autofocus><label for="password">密码</label><input id="password" v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码"><button class="button button-primary" :disabled="submitting || !username || !password">{{ submitting ? '登录中…' : '登录' }}</button></form>
        <div v-if="style?.socialProviders?.length" class="federation"><span>或使用企业身份源</span><a v-for="provider in style.socialProviders" :key="provider" :href="federationUrl(provider)">使用 {{ provider }} 登录</a></div>
      </template>

      <footer class="portal-footer">本次交互由 Easy1Auth 授权服务器保护 · 请勿在公共设备保存密码</footer>
    </section>
  </main>
</template>
