<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AuthPortalRenderer } from '@easy1auth/components'
import type { AuthInteractionContext, LoginMethod, LoginStyleConfig, PublicLoginStyle } from '@easy1auth/types'
import { safeCssColor } from '@easy1auth/utils'
import { authPortalApi } from './api'

const route = useRoute()
const context = ref<AuthInteractionContext | null>(null)
const loading = ref(true)
const submitting = ref(false)
const toast = ref('')
const mode = ref<'login' | 'register'>('login')
const loginMethod = ref<'password' | 'email'>('email')
const username = ref('')
const password = ref('')
const emailInput = ref('')
const verifyCode = ref('')
const challengeToken = ref('')
const mfaCode = ref('')
const termsAccepted = ref(false)
const legalDocument = ref<'terms' | 'privacy' | null>(null)
let customStyleElement: HTMLStyleElement | null = null

const isConsentRoute = computed(() => route.path === '/oauth-consent')
const style = computed(() => context.value?.style)
const styleConfig = computed(() => style.value?.config ?? legacyConfig(style.value))
const standard = computed(() => styleConfig.value.standard)
const availableMethods = computed(() => standard.value.enabled ? standard.value.methods.filter(method => method !== 'social') : [])
const registrationEnabled = computed(() => standard.value.enabled && standard.value.registrationEnabled)
const termsRequired = computed(() => standard.value.termsRequired && Boolean(context.value?.style?.termsOfService || context.value?.style?.privacyPolicy))
const rendererMode = computed(() => {
  if (context.value?.status === 'expired') return 'expired'
  if (context.value?.status === 'mfa') return 'mfa'
  if (isConsentRoute.value || context.value?.status === 'consent') return 'consent'
  return mode.value
})

function legacyConfig(value?: PublicLoginStyle | null): LoginStyleConfig {
  const methods = (value?.loginMethods ?? ['password', 'email']).map(method => method === 'oidc' ? 'social' : method).filter((method): method is LoginMethod => ['password', 'email', 'social'].includes(method))
  return {
    schemaVersion: 1,
    global: {
      title: value?.title ?? 'Easy1Auth',
      subtitle: value?.subtitle ?? '企业级身份管理平台',
      logoUrl: value?.logo ?? null,
      logoDarkUrl: value?.logoDark ?? null,
      background: { mode: value?.backgroundImage ? 'image' : 'solid', color: value?.backgroundColor ?? '#f5f7fa', imageUrl: value?.backgroundImage ?? null, overlayOpacity: 0 },
      primaryColor: value?.primaryColor ?? '#0369A1',
      language: 'zh-CN',
      card: { width: 480, radius: 16, shadow: true, position: 'center' },
      customCss: value?.customCss ?? null,
    },
    standard: { enabled: true, methods: methods.length ? methods : ['email'], registrationEnabled: value?.registrationEnabled === true, termsRequired: false },
    qr: { enabled: false, title: '扫码登录', subtitle: '使用手机扫码继续', iconUrl: null },
  }
}

function setCustomCss(css?: string | null) {
  customStyleElement?.remove()
  customStyleElement = null
  if (!css?.trim()) return
  customStyleElement = document.createElement('style')
  customStyleElement.id = 'auth-portal-custom-style'
  customStyleElement.textContent = `#auth-portal-root { ${css} }`
  document.head.appendChild(customStyleElement)
}

function setTheme() {
  document.documentElement.style.setProperty('--easy1auth-primary', safeCssColor(styleConfig.value.global.primaryColor, '#0369A1'))
  document.documentElement.style.setProperty('--easy1auth-background', safeCssColor(styleConfig.value.global.background.color, '#F5F7FA'))
  setCustomCss(styleConfig.value.global.customCss)
}

async function load() {
  loading.value = true
  toast.value = ''
  try {
    context.value = isConsentRoute.value ? await authPortalApi.getConsent() : await authPortalApi.getInteraction()
    toast.value = context.value.message ?? ''
    const firstMethod = standard.value.methods.find(method => method === 'password' || method === 'email')
    if (firstMethod) loginMethod.value = firstMethod
    setTheme()
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '页面加载失败，请重新发起请求'
  } finally {
    loading.value = false
  }
}

function ensureTerms() {
  if (termsRequired.value && !termsAccepted.value) {
    toast.value = '请先阅读并同意服务条款和隐私条款'
    return false
  }
  return true
}

function clearForm() {
  toast.value = ''
  username.value = ''
  password.value = ''
  emailInput.value = ''
  verifyCode.value = ''
  challengeToken.value = ''
  termsAccepted.value = false
}

function switchMode(target: 'login' | 'register') {
  mode.value = target
  clearForm()
}

async function passwordLogin() {
  if (!ensureTerms() || !username.value.trim() || !password.value) return
  submitting.value = true
  try {
    const result = await authPortalApi.login(username.value.trim(), password.value, context.value?.csrfToken)
    if (result.status === 'mfa_required' && context.value) {
      context.value = { ...context.value, status: 'mfa' }
      toast.value = '请输入动态验证码继续登录'
      return
    }
    window.location.assign(result.redirectUrl ?? '/')
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '登录失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function sendCode(target: 'login' | 'register') {
  if (!ensureTerms()) return
  const email = emailInput.value.trim()
  if (!email) { toast.value = '请输入邮箱'; return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { toast.value = '邮箱格式不正确'; return }
  submitting.value = true
  try {
    const result = target === 'register'
      ? await authPortalApi.registerSendCode(email, context.value?.csrfToken)
      : await authPortalApi.emailLoginSendCode(email, context.value?.csrfToken)
    challengeToken.value = result.token
    toast.value = '验证码已发送至邮箱'
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '验证码发送失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function verifyAndComplete(target: 'login' | 'register') {
  if (!verifyCode.value.trim()) { toast.value = '请输入验证码'; return }
  submitting.value = true
  try {
    const result = target === 'register'
      ? await authPortalApi.registerVerify(challengeToken.value, verifyCode.value.trim(), context.value?.csrfToken)
      : await authPortalApi.emailLoginVerify(challengeToken.value, verifyCode.value.trim(), context.value?.csrfToken)
    window.location.assign(result.redirectUrl ?? '/')
  } catch (error) {
    toast.value = error instanceof Error ? error.message : '验证失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function verifyMfa() {
  if (!mfaCode.value.trim()) return
  submitting.value = true
  try {
    const result = await authPortalApi.verifyMfa(mfaCode.value.trim(), context.value?.csrfToken)
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

function socialUrl(sourceId: string) {
  return context.value?.tenantId ? `/t/${context.value.tenantId}/social/${sourceId}/authorize` : '#'
}

const legalTitle = computed(() => legalDocument.value === 'terms' ? '服务条款' : '隐私政策')
const legalContent = computed(() => legalDocument.value === 'terms' ? style.value?.termsOfService : style.value?.privacyPolicy)

watch(() => route.path, load)
onMounted(load)
onBeforeUnmount(() => customStyleElement?.remove())
</script>

<template>
  <AuthPortalRenderer
    id="auth-portal-root"
    :config="styleConfig"
    :mode="rendererMode"
    :title-override="context?.clientName && rendererMode === 'consent' ? `${context.clientName} 请求访问` : undefined"
    :subtitle-override="rendererMode === 'consent' ? '确认后，该应用将获得以下权限。' : undefined"
  >
    <template #alert>
      <div v-if="toast" class="portal-toast" role="alert" aria-live="assertive">{{ toast }}</div>
    </template>

    <template #content>
      <div v-if="loading" class="portal-loading"><span class="spinner"></span><span>正在加载安全登录环境…</span></div>
      <template v-else-if="rendererMode === 'consent'">
        <ul class="scope-list"><li v-for="scope in context?.scopes ?? []" :key="scope"><span class="scope-dot"></span><span>{{ scope }}</span></li></ul>
        <div class="portal-actions"><button class="button button-secondary" :disabled="submitting" @click="consent('deny')">拒绝</button><button class="button button-primary" :disabled="submitting" @click="consent('approve')">{{ submitting ? '处理中…' : '同意并继续' }}</button></div>
      </template>
      <form v-else-if="rendererMode === 'mfa'" class="portal-form" @submit.prevent="verifyMfa"><label for="mfa-code">动态验证码</label><input id="mfa-code" v-model="mfaCode" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="000000" autofocus><button class="button button-primary" :disabled="submitting || !mfaCode">{{ submitting ? '验证中…' : '验证并继续' }}</button></form>
      <template v-else-if="mode === 'login'">
        <div v-if="availableMethods.length > 1" class="login-method-switch"><button v-for="method in availableMethods" :key="method" :class="{ active: loginMethod === method }" type="button" @click="loginMethod = method">{{ method === 'password' ? '密码登录' : '邮箱登录' }}</button></div>
        <form v-if="availableMethods.includes('password') && loginMethod === 'password' && !challengeToken" class="portal-form" @submit.prevent="passwordLogin"><label for="login-username">账号</label><input id="login-username" v-model="username" autocomplete="username" placeholder="请输入账号或邮箱"><label for="login-password">密码</label><input id="login-password" v-model="password" type="password" autocomplete="current-password" placeholder="请输入登录密码"><button class="button button-primary" :disabled="submitting || !username || !password">{{ submitting ? '登录中…' : '登录' }}</button></form>
        <form v-else-if="availableMethods.includes('email') && !challengeToken" class="portal-form" @submit.prevent="sendCode('login')"><label for="login-email">邮箱</label><input id="login-email" v-model="emailInput" type="email" autocomplete="email" placeholder="请输入邮箱"><button class="button button-primary" :disabled="submitting || !emailInput">{{ submitting ? '发送中…' : '下一步' }}</button></form>
        <form v-else-if="challengeToken" class="portal-form" @submit.prevent="verifyAndComplete('login')"><label for="login-code">验证码</label><input id="login-code" v-model="verifyCode" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="请输入6位验证码"><button class="button button-primary" :disabled="submitting || !verifyCode">{{ submitting ? '验证中…' : '登录' }}</button></form>
        <div v-else-if="!style?.socialSources?.length || !standard.methods.includes('social')" class="portal-empty">当前未启用可用的登录方式，请联系管理员。</div>
        <div v-if="registrationEnabled" class="portal-switch">还没有账号？<a href="#" @click.prevent="switchMode('register')">立即注册</a></div>
        <div v-if="style?.socialSources?.length && standard.methods.includes('social')" class="federation"><span>或使用社会化登录</span><a v-for="source in style.socialSources" :key="source.id" :href="socialUrl(source.id)">使用 {{ source.name }} 登录</a></div>
      </template>
      <template v-else>
        <form v-if="!challengeToken" class="portal-form" @submit.prevent="sendCode('register')"><label for="register-email">邮箱</label><input id="register-email" v-model="emailInput" type="email" autocomplete="email" placeholder="请输入邮箱"><button class="button button-primary" :disabled="submitting || !emailInput">{{ submitting ? '发送中…' : '发送验证码' }}</button></form>
        <form v-else class="portal-form" @submit.prevent="verifyAndComplete('register')"><label for="register-code">验证码</label><input id="register-code" v-model="verifyCode" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="请输入6位验证码"><button class="button button-primary" :disabled="submitting || !verifyCode">{{ submitting ? '注册中…' : '完成注册' }}</button></form>
        <div class="portal-switch">已有账号？<a href="#" @click.prevent="switchMode('login')">返回登录</a></div>
      </template>
    </template>

    <template #legal>
      <label v-if="context?.style?.termsOfService || context?.style?.privacyPolicy" class="portal-legal"><input v-model="termsAccepted" type="checkbox"><span>我已阅读并同意 <a v-if="context?.style?.termsOfService" href="#" @click.prevent="legalDocument = 'terms'">服务条款</a><span v-if="context?.style?.termsOfService && context?.style?.privacyPolicy">和</span><a v-if="context?.style?.privacyPolicy" href="#" @click.prevent="legalDocument = 'privacy'">隐私政策</a></span></label>
    </template>
  </AuthPortalRenderer>
  <div v-if="legalDocument" class="legal-dialog-backdrop" role="presentation" @click.self="legalDocument = null">
    <section class="legal-dialog" role="dialog" aria-modal="true" :aria-label="legalTitle"><header><h2>{{ legalTitle }}</h2><button type="button" aria-label="关闭" @click="legalDocument = null">×</button></header><div class="legal-content">{{ legalContent }}</div></section>
  </div>
</template>
