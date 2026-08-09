import { computed, ref } from 'vue'
import { loginStyleApi, type LoginStyleDraftResponse, type LoginStyleLegalDocuments } from '@/api/loginStyle'
import type { LoginStyleConfig } from '@easy1auth/types'

export const DEFAULT_LOGIN_STYLE_CONFIG: LoginStyleConfig = {
  schemaVersion: 1,
  global: {
    title: 'Easy1Auth',
    subtitle: '企业级身份管理平台',
    logoUrl: null,
    logoDarkUrl: null,
    background: { mode: 'solid', color: '#f5f7fa', imageUrl: null, overlayColor: '#ffffff', overlayOpacity: 0 },
    primaryColor: '#0369A1',
    language: 'zh-CN',
    card: { width: 480, radius: 16, shadow: true, position: 'center' },
    customCss: null,
  },
  standard: { enabled: true, methods: ['password', 'email'], registrationEnabled: true, termsRequired: false },
  qr: { enabled: false, title: '扫码登录', subtitle: '使用手机扫码继续', iconUrl: null },
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function mergeConfig(value?: Partial<LoginStyleConfig> | null): LoginStyleConfig {
  const source = (value ?? {}) as Partial<LoginStyleConfig>
  const global = (source.global ?? {}) as Partial<LoginStyleConfig['global']>
  const background = (global.background ?? {}) as Partial<LoginStyleConfig['global']['background']>
  const card = (global.card ?? {}) as Partial<LoginStyleConfig['global']['card']>
  const standard = (source.standard ?? {}) as Partial<LoginStyleConfig['standard']>
  const qr = (source.qr ?? {}) as Partial<LoginStyleConfig['qr']>
  return {
    schemaVersion: 1,
    global: {
      ...clone(DEFAULT_LOGIN_STYLE_CONFIG.global),
      ...global,
      background: { ...clone(DEFAULT_LOGIN_STYLE_CONFIG.global.background), ...background },
      card: { ...clone(DEFAULT_LOGIN_STYLE_CONFIG.global.card), ...card },
    },
    standard: { ...clone(DEFAULT_LOGIN_STYLE_CONFIG.standard), ...standard, methods: (standard.methods ?? DEFAULT_LOGIN_STYLE_CONFIG.standard.methods).filter((method: string) => ['password', 'email', 'social'].includes(method)) as LoginStyleConfig['standard']['methods'] },
    qr: { ...clone(DEFAULT_LOGIN_STYLE_CONFIG.qr), ...qr },
  }
}

function normalizeLegal(value?: LoginStyleLegalDocuments | null): LoginStyleLegalDocuments {
  return {
    termsOfService: value?.termsOfService ?? null,
    privacyPolicy: value?.privacyPolicy ?? null,
  }
}

export function useLoginStyleDraft() {
  const config = ref<LoginStyleConfig>(clone(DEFAULT_LOGIN_STYLE_CONFIG))
  const legalDocuments = ref<LoginStyleLegalDocuments>(normalizeLegal())
  const draftUpdatedAt = ref<string | null>(null)
  const publishedAt = ref<string | null>(null)
  const loading = ref(false)
  const saving = ref(false)
  const publishing = ref(false)
  const resetting = ref(false)
  const dirty = ref(false)
  const status = computed(() => dirty.value ? '未保存修改' : '草稿已保存')

  function apply(response: LoginStyleDraftResponse) {
    config.value = mergeConfig(response.config)
    legalDocuments.value = normalizeLegal(response.legalDocuments)
    draftUpdatedAt.value = response.draftUpdatedAt ?? null
    publishedAt.value = response.publishedAt ?? null
    dirty.value = false
  }

  async function load() {
    loading.value = true
    try {
      apply(await loginStyleApi.getDraft())
    } finally {
      loading.value = false
    }
  }

  function markDirty() {
    dirty.value = true
  }

  async function save() {
    saving.value = true
    try {
      apply(await loginStyleApi.saveDraft({ config: clone(config.value), legalDocuments: clone(legalDocuments.value) }))
    } finally {
      saving.value = false
    }
  }

  async function publish() {
    publishing.value = true
    try {
      apply(await loginStyleApi.publish())
    } finally {
      publishing.value = false
    }
  }

  async function reset() {
    resetting.value = true
    try {
      apply(await loginStyleApi.reset())
    } finally {
      resetting.value = false
    }
  }

  return { config, legalDocuments, draftUpdatedAt, publishedAt, loading, saving, publishing, resetting, dirty, status, load, markDirty, save, publish, reset }
}
