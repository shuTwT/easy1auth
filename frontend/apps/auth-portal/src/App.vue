<script setup lang="ts">
import { Button, Checkbox, ConfigProvider, Empty, Form, FormItem, Input, Modal, Result, Segmented, Spin, message } from 'antdv-next'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AuthPortalRenderer } from '@easy1auth/components'
import type { AuthInteractionContext, LoginMethod, LoginStyleConfig, PublicLoginStyle } from '@easy1auth/types'
import { safeCssColor } from '@easy1auth/utils'
import { authPortalApi } from './api'

const route = useRoute()
const context = ref<AuthInteractionContext | null>(null)
const loading = ref(true)
const submitting = ref(false)
const mode = ref<'login' | 'register'>('login')
const loginMethod = ref<'password' | 'email'>('email')
const passwordForm = reactive({ username: '', password: '' })
const emailForm = reactive({ email: '' })
const verifyForm = reactive({ code: '' })
const mfaForm = reactive({ code: '' })
const socialProvisionForm = reactive({ username: '' })
const socialProfile = ref<{ name?: string | null; email?: string | null; suggestedUsername?: string | null; canProvision: boolean } | null>(null)
const socialAction = ref<'select' | 'create' | 'bind'>('select')
const challengeToken = ref('')
const termsAccepted = ref(false)
const legalDocument = ref<'terms' | 'privacy' | null>(null)
let customStyleElement: HTMLStyleElement | null = null

const isConsentRoute = computed(() => route.path === '/oauth-consent')
const isSocialCallbackRoute = computed(() => route.path === '/oauth-login/social/callback')
const style = computed(() => context.value?.style)
const styleConfig = computed(() => style.value?.config ?? legacyConfig(style.value))
const standard = computed(() => styleConfig.value.standard)
const availableMethods = computed(() => standard.value.enabled ? standard.value.methods.filter(method => method !== 'social') : [])
const registrationEnabled = computed(() => standard.value.enabled && standard.value.registrationEnabled)
const termsRequired = computed(() => standard.value.termsRequired && Boolean(context.value?.style?.termsOfService || context.value?.style?.privacyPolicy))
const loginMethodOptions = computed(() => availableMethods.value.map(method => ({
  label: method === 'password' ? '密码登录' : '邮箱登录',
  value: method,
})))
const antdTheme = computed(() => {
  const primary = safeCssColor(styleConfig.value.global.primaryColor, '#0369A1')
  return {
    token: {
      colorPrimary: primary,
      colorInfo: primary,
      colorSuccess: '#15803D',
      colorWarning: '#B45309',
      colorError: '#B91C1C',
      colorText: '#102A43',
      colorTextSecondary: '#52677D',
      colorBorder: '#D7E2EC',
      fontFamily: '"Fira Sans", ui-sans-serif, system-ui, sans-serif',
      borderRadius: 8,
      controlHeight: 40,
    },
  }
})
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
  try {
    if (!isSocialCallbackRoute.value) socialProfile.value = null
    context.value = isConsentRoute.value ? await authPortalApi.getConsent() : await authPortalApi.getInteraction()
    if (context.value.message) message.error(context.value.message)
    const firstMethod = standard.value.methods.find(method => method === 'password' || method === 'email')
    if (firstMethod) loginMethod.value = firstMethod
    setTheme()
    if (isSocialCallbackRoute.value) await completeSocialCallback()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '页面加载失败，请重新发起请求')
  } finally {
    loading.value = false
  }
}

function ensureTerms() {
  if (termsRequired.value && !termsAccepted.value) {
    message.warning('请先阅读并同意服务条款和隐私条款')
    return false
  }
  return true
}

function clearForm() {
  passwordForm.username = ''
  passwordForm.password = ''
  emailForm.email = ''
  verifyForm.code = ''
  mfaForm.code = ''
  challengeToken.value = ''
  termsAccepted.value = false
  socialProvisionForm.username = ''
}

function switchMode(target: 'login' | 'register') {
  mode.value = target
  clearForm()
}

async function passwordLogin() {
  if (!ensureTerms() || !passwordForm.username.trim() || !passwordForm.password) return
  submitting.value = true
  try {
    const result = await authPortalApi.login(passwordForm.username.trim(), passwordForm.password, context.value?.csrfToken)
    if (result.status === 'mfa_required' && context.value) {
      context.value = { ...context.value, status: 'mfa' }
      message.info('请输入动态验证码继续登录')
      return
    }
    completeLogin(result.redirectUrl)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '登录失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function sendCode(target: 'login' | 'register') {
  if (!ensureTerms()) return
  const email = emailForm.email.trim()
  if (!email) { message.warning('请输入邮箱'); return }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { message.warning('邮箱格式不正确'); return }
  submitting.value = true
  try {
    const result = target === 'register'
      ? await authPortalApi.registerSendCode(email, context.value?.csrfToken)
      : await authPortalApi.emailLoginSendCode(email, context.value?.csrfToken)
    challengeToken.value = result.token
    message.success('验证码已发送至邮箱')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '验证码发送失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function verifyAndComplete(target: 'login' | 'register') {
  if (!verifyForm.code.trim()) { message.warning('请输入验证码'); return }
  submitting.value = true
  try {
    const result = target === 'register'
      ? await authPortalApi.registerVerify(challengeToken.value, verifyForm.code.trim(), context.value?.csrfToken)
      : await authPortalApi.emailLoginVerify(challengeToken.value, verifyForm.code.trim(), context.value?.csrfToken)
    completeLogin(result.redirectUrl)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '验证失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function verifyMfa() {
  if (!mfaForm.code.trim()) return
  submitting.value = true
  try {
    const result = await authPortalApi.verifyMfa(mfaForm.code.trim(), context.value?.csrfToken)
    completeLogin(result.redirectUrl)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '验证码无效或已过期')
  } finally {
    submitting.value = false
  }
}

async function completeSocialCallback() {
  const code = Array.isArray(route.query.code) ? route.query.code[0] : route.query.code
  const state = Array.isArray(route.query.state) ? route.query.state[0] : route.query.state
  if (!code || !state) {
    message.error('社会化登录回调参数无效')
    return
  }
  const result = await authPortalApi.socialCallback(code, state, context.value?.csrfToken)
  if (result.status === 'success') {
    completeLogin(result.redirectUrl)
    return
  }
  socialProfile.value = {
    name: result.name,
    email: result.email,
    suggestedUsername: result.suggestedUsername,
    canProvision: result.canProvision === true,
  }
  socialAction.value = 'select'
  socialProvisionForm.username = result.suggestedUsername ?? ''
}

async function provisionSocialUser() {
  if (!socialProvisionForm.username.trim()) {
    message.warning('请输入用户名')
    return
  }
  submitting.value = true
  try {
    const result = await authPortalApi.socialProvision(socialProvisionForm.username.trim(), context.value?.csrfToken)
    completeLogin(result.redirectUrl)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '创建用户失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function consent(action: 'approve' | 'deny') {
  submitting.value = true
  try {
    const result = await authPortalApi.consent(action, context.value?.csrfToken)
    const form = document.createElement('form')
    form.method = 'post'
    form.action = result.location
    const parameters: Array<[string, string]> = [
      ['client_id', result.clientId],
      ['state', result.state],
      ...result.scopes.map(scope => ['scope', scope] as [string, string])
    ]
    for (const [name, value] of parameters) {
      const input = document.createElement('input')
      input.type = 'hidden'
      input.name = name
      input.value = value
      form.appendChild(input)
    }
    document.body.appendChild(form)
    form.submit()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '授权请求已失效')
  } finally {
    submitting.value = false
  }
}

function completeLogin(redirectUrl?: string) {
  if (!redirectUrl || redirectUrl === '/') {
    message.success('登录成功')
    return
  }
  window.location.assign(redirectUrl)
}

function socialUrl(sourceId: string) {
  return context.value?.tenantId ? `/t/${context.value.tenantId}/social/${sourceId}/authorize` : '#'
}

const legalTitle = computed(() => legalDocument.value === 'terms' ? '服务条款' : '隐私政策')
const legalContent = computed(() => legalDocument.value === 'terms' ? style.value?.termsOfService : style.value?.privacyPolicy)
const legalModalOpen = computed({
  get: () => legalDocument.value !== null,
  set: (open: boolean) => {
    if (!open) legalDocument.value = null
  },
})

watch(() => route.path, load)
onMounted(load)
onBeforeUnmount(() => customStyleElement?.remove())
</script>

<template>
  <ConfigProvider :theme="antdTheme">
    <AuthPortalRenderer
    id="auth-portal-root"
    :config="styleConfig"
    :mode="rendererMode"
    :title-override="context?.clientName && rendererMode === 'consent' ? `${context.clientName} 请求访问` : undefined"
    :subtitle-override="rendererMode === 'consent' ? '确认后，该应用将获得以下权限。' : undefined"
    >
    <template #content>
      <div v-if="loading" class="portal-loading"><Spin size="large" description="正在加载安全登录环境…" /></div>
      <template v-else-if="rendererMode === 'consent'">
        <ul class="scope-list"><li v-for="scope in context?.scopes ?? []" :key="scope"><span class="scope-dot"></span><span>{{ scope }}</span></li></ul>
        <div class="portal-actions"><Button :disabled="submitting" @click="consent('deny')">拒绝</Button><Button type="primary" :loading="submitting" @click="consent('approve')">同意并继续</Button></div>
      </template>
      <Form v-else-if="rendererMode === 'mfa'" class="portal-form" :model="mfaForm" layout="vertical" @finish="verifyMfa"><FormItem label="动态验证码" name="code"><Input v-model:value="mfaForm.code" inputmode="numeric" autocomplete="one-time-code" :maxlength="6" placeholder="000000" autofocus /></FormItem><Button html-type="submit" type="primary" block :loading="submitting" :disabled="!mfaForm.code">验证并继续</Button></Form>
      <template v-else-if="socialProfile">
        <Result v-if="socialAction === 'select'" status="info" title="选择账号处理方式" :sub-title="socialProfile.name ? `已验证社会化账号：${socialProfile.name}` : '已验证社会化账号'">
          <template #extra><div class="social-account-actions"><Button type="primary" :disabled="!socialProfile.canProvision" title="身份源未提供邮箱时无法创建用户" @click="socialAction = 'create'">创建新用户</Button><Button @click="socialAction = 'bind'">绑定已有用户</Button></div></template>
        </Result>
        <Form v-else-if="socialAction === 'create'" class="portal-form" :model="socialProvisionForm" layout="vertical" @finish="provisionSocialUser"><FormItem label="用户名" name="username"><Input v-model:value="socialProvisionForm.username" autocomplete="username" placeholder="请输入用户名" /></FormItem><div class="portal-actions"><Button @click="socialAction = 'select'">返回</Button><Button html-type="submit" type="primary" :loading="submitting" :disabled="!socialProvisionForm.username">创建并继续</Button></div></Form>
        <Form v-else class="portal-form" :model="passwordForm" layout="vertical" @finish="passwordLogin"><FormItem label="账号" name="username"><Input v-model:value="passwordForm.username" autocomplete="username" placeholder="请输入账号或邮箱" /></FormItem><FormItem label="密码" name="password"><Input v-model:value="passwordForm.password" type="password" autocomplete="current-password" placeholder="请输入登录密码" /></FormItem><div class="portal-actions"><Button @click="socialAction = 'select'">返回</Button><Button html-type="submit" type="primary" :loading="submitting" :disabled="!passwordForm.username || !passwordForm.password">验证并绑定</Button></div></Form>
      </template>
      <template v-else-if="mode === 'login'">
        <Segmented v-if="availableMethods.length > 1" v-model:value="loginMethod" class="login-method-switch" block :options="loginMethodOptions" />
        <Form v-if="availableMethods.includes('password') && loginMethod === 'password' && !challengeToken" class="portal-form" :model="passwordForm" layout="vertical" @finish="passwordLogin"><FormItem label="账号" name="username"><Input v-model:value="passwordForm.username" autocomplete="username" placeholder="请输入账号或邮箱" /></FormItem><FormItem label="密码" name="password"><Input v-model:value="passwordForm.password" type="password" autocomplete="current-password" placeholder="请输入登录密码" /></FormItem><Button html-type="submit" type="primary" block :loading="submitting" :disabled="!passwordForm.username || !passwordForm.password">登录</Button></Form>
        <Form v-else-if="availableMethods.includes('email') && !challengeToken" class="portal-form" :model="emailForm" layout="vertical" @finish="sendCode('login')"><FormItem label="邮箱" name="email"><Input v-model:value="emailForm.email" type="email" autocomplete="email" placeholder="请输入邮箱" /></FormItem><Button html-type="submit" type="primary" block :loading="submitting" :disabled="!emailForm.email">下一步</Button></Form>
        <Form v-else-if="challengeToken" class="portal-form" :model="verifyForm" layout="vertical" @finish="verifyAndComplete('login')"><FormItem label="验证码" name="code"><Input v-model:value="verifyForm.code" inputmode="numeric" autocomplete="one-time-code" :maxlength="6" placeholder="请输入6位验证码" /></FormItem><Button html-type="submit" type="primary" block :loading="submitting" :disabled="!verifyForm.code">登录</Button></Form>
        <Empty v-else-if="!style?.socialSources?.length || !standard.methods.includes('social')" class="portal-empty" description="暂无可用登录方式" />
        <div v-if="registrationEnabled" class="portal-switch">还没有账号？<a href="#" @click.prevent="switchMode('register')">立即注册</a></div>
        <div v-if="style?.socialSources?.length && standard.methods.includes('social')" class="federation"><span>或使用社会化登录</span><a v-for="source in style.socialSources" :key="source.id" :href="socialUrl(source.id)">使用 {{ source.name }} 登录</a></div>
      </template>
      <template v-else>
        <Form v-if="!challengeToken" class="portal-form" :model="emailForm" layout="vertical" @finish="sendCode('register')"><FormItem label="邮箱" name="email"><Input v-model:value="emailForm.email" type="email" autocomplete="email" placeholder="请输入邮箱" /></FormItem><Button html-type="submit" type="primary" block :loading="submitting" :disabled="!emailForm.email">发送验证码</Button></Form>
        <Form v-else class="portal-form" :model="verifyForm" layout="vertical" @finish="verifyAndComplete('register')"><FormItem label="验证码" name="code"><Input v-model:value="verifyForm.code" inputmode="numeric" autocomplete="one-time-code" :maxlength="6" placeholder="请输入6位验证码" /></FormItem><Button html-type="submit" type="primary" block :loading="submitting" :disabled="!verifyForm.code">完成注册</Button></Form>
        <div class="portal-switch">已有账号？<a href="#" @click.prevent="switchMode('login')">返回登录</a></div>
      </template>
    </template>

    <template #legal>
      <Checkbox v-if="context?.style?.termsOfService || context?.style?.privacyPolicy" v-model:checked="termsAccepted" class="portal-legal">我已阅读并同意 <a v-if="context?.style?.termsOfService" href="#" @click.prevent="legalDocument = 'terms'">服务条款</a><span v-if="context?.style?.termsOfService && context?.style?.privacyPolicy">和</span><a v-if="context?.style?.privacyPolicy" href="#" @click.prevent="legalDocument = 'privacy'">隐私政策</a></Checkbox>
    </template>
  </AuthPortalRenderer>
    <Modal v-model:open="legalModalOpen" :title="legalTitle" :footer="null"><div class="legal-content">{{ legalContent }}</div></Modal>
  </ConfigProvider>
</template>
