<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'antdv-next'
import {
  Building2,
  CheckCircle2,
  ChevronRight,
  Clock3,
  KeyRound,
  Mail,
  Phone,
  Save,
  ShieldCheck,
  Smartphone,
  UserRound,
} from '@lucide/vue'
import { profileApi, type AdminProfile } from '@/api/profile'
import { securityApi, type MfaStatus } from '@/api/security'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const saving = ref(false)
const profile = ref<AdminProfile | null>(null)
const mfaStatus = ref<MfaStatus>({ enabled: false, type: null })
const emailModalOpen = ref(false)
const emailStep = ref<'email' | 'code'>('email')
const emailSending = ref(false)
const emailVerifying = ref(false)
const emailCountdown = ref(0)
const emailChallengeToken = ref('')
let emailCountdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  username: '',
  phone: '',
})

const emailForm = reactive({ email: '', code: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名' },
    { max: 100, message: '用户名不能超过 100 个字符' },
  ],
  phone: [
    { pattern: /^\+?[0-9][0-9 -]{5,31}$/, message: '请输入有效的手机号' },
  ],
}

const displayName = computed(() => profile.value?.username || '管理员')
const avatarLetter = computed(() => displayName.value.charAt(0).toUpperCase())
const currentUserPoolName = computed(() => userStore.currentUserPool?.name || '暂未选择用户池')
const mfaLabel = computed(() => {
  if (!mfaStatus.value.enabled) return '未启用'
  return mfaStatus.value.type === 'totp' ? '身份验证器' : '已启用'
})
const isDirty = computed(() => {
  if (!profile.value) return false
  return form.username.trim() !== profile.value.username
    || form.phone.trim() !== (profile.value.phone || '')
})

function formatDate(value: string | null | undefined) {
  if (!value) return '暂无记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function hydrateForm(value: AdminProfile) {
  form.username = value.username
  form.phone = value.phone || ''
}

function syncStore(value: AdminProfile) {
  userStore.setUserInfo({
    ...(userStore.userInfo || {}),
    id: value.id,
    username: value.username,
    email: value.email,
  })
}

async function loadProfile() {
  loading.value = true
  try {
    const [profileResult, mfaResult] = await Promise.all([
      profileApi.get(),
      securityApi.getMfaStatus(),
    ])
    profile.value = profileResult
    mfaStatus.value = mfaResult
    hydrateForm(profileResult)
    syncStore(profileResult)
  } catch (error) {
    console.error('加载个人资料失败:', error)
    message.error('加载个人资料失败')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!isDirty.value) return
  saving.value = true
  try {
    const updated = await profileApi.update({
      username: form.username.trim(),
      phone: form.phone.trim(),
    })
    profile.value = updated
    hydrateForm(updated)
    syncStore(updated)
    message.success('个人资料已更新')
  } catch (error) {
    console.error('更新个人资料失败:', error)
    message.error('更新个人资料失败')
  } finally {
    saving.value = false
  }
}

function openEmailChange() {
  emailStep.value = 'email'
  emailForm.email = ''
  emailForm.code = ''
  emailChallengeToken.value = ''
  emailModalOpen.value = true
}

function startEmailCountdown() {
  if (emailCountdownTimer) clearInterval(emailCountdownTimer)
  emailCountdown.value = 60
  emailCountdownTimer = setInterval(() => {
    emailCountdown.value--
    if (emailCountdown.value <= 0 && emailCountdownTimer) {
      clearInterval(emailCountdownTimer)
      emailCountdownTimer = null
    }
  }, 1000)
}

async function sendEmailChangeCode() {
  const email = emailForm.email.trim().toLowerCase()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    message.warning('请输入正确的新邮箱地址')
    return
  }
  emailSending.value = true
  try {
    const result = await profileApi.sendEmailChangeCode(email)
    emailChallengeToken.value = result.challengeToken
    emailStep.value = 'code'
    emailForm.code = ''
    startEmailCountdown()
    message.success('验证码已发送到新邮箱')
  } catch (error) {
    console.error('发送邮箱换绑验证码失败:', error)
    message.error('发送邮箱换绑验证码失败')
  } finally {
    emailSending.value = false
  }
}

async function verifyEmailChange() {
  if (!/^\d{6}$/.test(emailForm.code)) {
    message.warning('请输入 6 位邮箱验证码')
    return
  }
  emailVerifying.value = true
  try {
    await profileApi.verifyEmailChange(emailChallengeToken.value, emailForm.code)
    message.success('邮箱换绑成功，请使用新邮箱重新登录')
    userStore.logout()
    await router.replace('/login')
  } catch (error) {
    console.error('验证邮箱换绑验证码失败:', error)
    message.error('验证邮箱换绑验证码失败')
  } finally {
    emailVerifying.value = false
  }
}

onMounted(loadProfile)
onBeforeUnmount(() => {
  if (emailCountdownTimer) clearInterval(emailCountdownTimer)
})
</script>

<template>
  <main class="profile-page page-container">
    <header class="page-header profile-heading">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理您的管理员账户资料与安全状态</p>
      </div>
    </header>

    <div v-if="loading" class="grid gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
      <a-card class="profile-card"><a-skeleton active avatar :paragraph="{ rows: 5 }" /></a-card>
      <div class="space-y-6">
        <a-card><a-skeleton active :paragraph="{ rows: 6 }" /></a-card>
        <a-card><a-skeleton active :paragraph="{ rows: 3 }" /></a-card>
      </div>
    </div>

    <div v-else-if="profile" class="grid items-start gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
      <aside class="space-y-6">
        <a-card class="profile-card overflow-hidden" :styles="{ body: { padding: 0 } }">
          <div class="profile-cover" aria-hidden="true"></div>
          <div class="px-6 pb-6 text-center">
            <a-avatar :size="88" class="profile-avatar" :alt="`${displayName} 的头像`">
              {{ avatarLetter }}
            </a-avatar>
            <h2 class="mt-4 text-xl font-semibold text-[#102A43]">{{ displayName }}</h2>
            <p class="mt-1 break-all text-sm text-[#52677D]">{{ profile.email }}</p>
            <a-tag color="success" class="!mt-3">
              <span class="inline-flex items-center gap-1"><CheckCircle2 class="size-3" />账户正常</span>
            </a-tag>

            <dl class="mt-6 space-y-4 border-t border-[#D7E2EC] pt-5 text-left">
              <div class="profile-meta-row">
                <dt><Building2 class="size-4" />当前用户池</dt>
                <dd>{{ currentUserPoolName }}</dd>
              </div>
              <div class="profile-meta-row">
                <dt><Clock3 class="size-4" />最近登录</dt>
                <dd>{{ formatDate(profile.lastLoginAt) }}</dd>
              </div>
              <div class="profile-meta-row">
                <dt><UserRound class="size-4" />账户创建</dt>
                <dd>{{ formatDate(profile.createdAt) }}</dd>
              </div>
            </dl>
          </div>
        </a-card>

        <a-card title="账户标识" size="small">
          <p class="break-all font-mono text-xs leading-5 text-[#52677D]">{{ profile.id }}</p>
        </a-card>
      </aside>

      <section class="min-w-0 space-y-6">
        <a-card class="profile-card">
          <template #title>
            <div>
              <h2 class="text-base font-semibold text-[#102A43]">基本资料</h2>
              <p class="mt-1 text-sm font-normal text-[#52677D]">这些信息用于识别您的管理员账户</p>
            </div>
          </template>

          <a-form :model="form" :rules="rules" layout="vertical" class="max-w-2xl" @finish="saveProfile">
            <div class="grid gap-x-5 md:grid-cols-2">
              <a-form-item label="用户名" name="username">
                <a-input v-model:value="form.username" autocomplete="username" :maxlength="100" placeholder="请输入用户名">
                  <template #prefix><UserRound class="size-4 text-[#52677D]" /></template>
                </a-input>
              </a-form-item>

              <a-form-item label="手机号" name="phone">
                <a-input v-model:value="form.phone" autocomplete="tel" :maxlength="32" placeholder="选填，用于账户联系">
                  <template #prefix><Phone class="size-4 text-[#52677D]" /></template>
                </a-input>
              </a-form-item>
            </div>

            <a-form-item label="邮箱地址" extra="换绑邮箱需要验证新邮箱，成功后当前登录会话将失效。">
              <div class="flex flex-col gap-3 sm:flex-row">
                <a-input :value="profile.email" readonly class="min-w-0 flex-1">
                  <template #prefix><Mail class="size-4 text-[#52677D]" /></template>
                </a-input>
                <a-button class="shrink-0" @click="openEmailChange">换绑邮箱</a-button>
              </div>
            </a-form-item>

            <div class="flex flex-wrap items-center justify-between gap-3 border-t border-[#D7E2EC] pt-5">
              <p class="text-xs text-[#52677D]">最近更新：{{ formatDate(profile.updatedAt) }}</p>
              <a-button type="primary" html-type="submit" :loading="saving" :disabled="!isDirty">
                <Save class="size-4 mr-1" />保存修改
              </a-button>
            </div>
          </a-form>
        </a-card>

        <a-card class="profile-card">
          <template #title>
            <div>
              <h2 class="text-base font-semibold text-[#102A43]">账户安全</h2>
              <p class="mt-1 text-sm font-normal text-[#52677D]">查看安全状态并进入安全设置完成敏感操作</p>
            </div>
          </template>

          <div class="divide-y divide-border">
            <div class="security-row">
              <div class="security-icon bg-[#0369A1]/10 text-[#0369A1]"><KeyRound class="size-5" /></div>
              <div class="min-w-0 flex-1">
                <h3 class="text-sm font-medium text-[#102A43]">登录密码</h3>
                <p class="mt-1 text-sm text-[#52677D]">定期更换密码可以降低账户泄露风险</p>
              </div>
              <a-tag color="success">已设置</a-tag>
            </div>

            <div class="security-row">
              <div class="security-icon" :class="mfaStatus.enabled ? 'bg-emerald-500/10 text-emerald-500' : 'bg-amber-500/10 text-amber-500'">
                <Smartphone class="size-5" />
              </div>
              <div class="min-w-0 flex-1">
                <h3 class="text-sm font-medium text-[#102A43]">多因素认证</h3>
                <p class="mt-1 text-sm text-[#52677D]">{{ mfaStatus.enabled ? '登录时需要额外身份验证' : '建议启用身份验证器保护账户' }}</p>
              </div>
              <a-tag :color="mfaStatus.enabled ? 'success' : 'warning'">{{ mfaLabel }}</a-tag>
            </div>

            <div class="security-row">
              <div class="security-icon bg-indigo-500/10 text-indigo-500"><ShieldCheck class="size-5" /></div>
              <div class="min-w-0 flex-1">
                <h3 class="text-sm font-medium text-[#102A43]">安全设置</h3>
                <p class="mt-1 text-sm text-[#52677D]">修改密码、配置 MFA 和查看密码策略</p>
              </div>
              <a-button type="link" class="!px-0" @click="router.push('/security')">
                前往设置<ChevronRight class="size-4" />
              </a-button>
            </div>
          </div>
        </a-card>
      </section>
    </div>
  <a-modal v-model:open="emailModalOpen" title="换绑邮箱" :footer="null" :mask-closable="!emailSending && !emailVerifying">
    <div v-if="emailStep === 'email'" class="space-y-5 pt-2">
      <div class="rounded-lg border border-sky-100 bg-sky-50 p-4 text-sm text-sky-900">
        当前邮箱：<span class="font-medium">{{ profile?.email }}</span>
      </div>
      <div class="grid gap-2">
        <label for="new-admin-email" class="text-sm font-medium text-[#102A43]">新邮箱地址</label>
        <a-input id="new-admin-email" v-model:value="emailForm.email" autocomplete="email" :maxlength="320" placeholder="name@company.com">
          <template #prefix><Mail class="size-4 text-[#52677D]" /></template>
        </a-input>
      </div>
      <a-button type="primary" block :loading="emailSending" @click="sendEmailChangeCode">发送验证码</a-button>
    </div>

    <div v-else class="space-y-5 pt-2">
      <div>
        <p class="text-sm text-[#52677D]">验证码已发送至</p>
        <p class="mt-1 break-all font-medium text-[#102A43]">{{ emailForm.email }}</p>
      </div>
      <div class="grid gap-2">
        <label for="admin-email-code" class="text-sm font-medium text-[#102A43]">邮箱验证码</label>
        <a-input id="admin-email-code" v-model:value="emailForm.code" inputmode="numeric" autocomplete="one-time-code" :maxlength="6" placeholder="请输入6位验证码" @press-enter="verifyEmailChange" />
      </div>
      <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        <a-button :disabled="emailCountdown > 0" :loading="emailSending" @click="sendEmailChangeCode">
          {{ emailCountdown > 0 ? `${emailCountdown}s 后可重新发送` : '重新发送' }}
        </a-button>
        <a-button type="primary" :loading="emailVerifying" @click="verifyEmailChange">验证并换绑</a-button>
      </div>
    </div>
  </a-modal>
</main>
</template>

<style scoped>
.profile-page {
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
}

.profile-heading {
  align-items: flex-start;
}

.profile-card {
  border-color: #D7E2EC;
  box-shadow: 0 1px 2px rgba(16, 42, 67, 0.06);
}

.profile-cover {
  height: 104px;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 255, 255, 0.24), transparent 24%),
    linear-gradient(135deg, #075985, #0EA5E9);
}

.profile-avatar {
  margin-top: -44px;
  border: 4px solid white;
  background: #E0F2FE;
  color: #075985;
  font-size: 30px;
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.14);
}

.profile-meta-row {
  display: grid;
  gap: 4px;
}

.profile-meta-row dt {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #7890A5;
  font-size: 13px;
}

.profile-meta-row dd {
  margin-left: 25px;
  color: #102A43;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.security-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 0;
}

.security-row:first-child {
  padding-top: 4px;
}

.security-row:last-child {
  padding-bottom: 4px;
}

.security-icon {
  display: flex;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

@media (max-width: 640px) {
  .profile-page {
    padding: 16px;
  }

  .security-row {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .security-row > :last-child {
    margin-left: 56px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .profile-page * {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
