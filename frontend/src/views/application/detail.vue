<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { toast } from 'vue-sonner'
import {
  ArrowLeft,
  Check,
  Copy,
  Eye,
  EyeOff,
  Minus,
  Plus,
  RefreshCw,
  Trash2
} from '@lucide/vue'
import { applicationApi } from '@/api/application'
import type {
  Application,
  ApplicationStatus,
  ApplicationType,
  UpdateApplicationDto
} from '@/types/application'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { NumberField, NumberFieldContent, NumberFieldDecrement, NumberFieldIncrement, NumberFieldInput } from '@/components/ui/number-field'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'

type ApiErrorResponse = {
  message?: string
}

type ConfigForm = {
  name: string
  logo: string
  description: string
  type: ApplicationType
  status: ApplicationStatus
}

type LoginForm = {
  redirectUris: string[]
  allowedGrantTypes: string[]
  accessTokenLifetime: number
  refreshTokenLifetime: number
}

const typeLabels: Record<ApplicationType, string> = {
  web: 'Web 应用',
  native: '原生应用',
  spa: '单页应用',
  machine: '机器对机器'
}

const statusLabels: Record<ApplicationStatus, string> = {
  active: '启用',
  disabled: '禁用'
}

const grantTypeOptions = [
  { value: 'authorization_code', label: '授权码模式', description: '适用于有用户交互的 Web 和原生应用' },
  { value: 'client_credentials', label: '客户端凭证模式', description: '适用于机器对机器通信' },
  { value: 'refresh_token', label: '刷新令牌', description: '允许应用续期已过期的访问令牌' }
] as const

const route = useRoute()
const router = useRouter()
const baseUrl = window.location.origin

const applicationId = computed(() => {
  const { id } = route.params
  return Array.isArray(id) ? id[0] ?? '' : id ? String(id) : ''
})

const application = ref<Application | null>(null)
const loading = ref(true)
const loadError = ref('')
const notFound = ref(false)
const secretVisible = ref(false)
const configSaving = ref(false)
const loginSaving = ref(false)
const redirectUriInput = ref('')

const configForm = reactive<ConfigForm>({
  name: '',
  logo: '',
  description: '',
  type: 'web',
  status: 'active'
})

const loginForm = reactive<LoginForm>({
  redirectUris: [],
  allowedGrantTypes: ['authorization_code'],
  accessTokenLifetime: 3600,
  refreshTokenLifetime: 2592000
})

const authorizationEndpoint = `${baseUrl}/oauth2/authorize`
const tokenEndpoint = `${baseUrl}/api/oauth2/token`
const userinfoEndpoint = `${baseUrl}/api/oauth2/userinfo`

const clientSecretDisplay = computed(() => {
  if (secretVisible.value) return application.value?.clientSecret ?? ''
  return '•'.repeat(24)
})

const selectedRedirectUri = computed(() => loginForm.redirectUris[0] ?? '')
const hasRedirectUri = computed(() => Boolean(selectedRedirectUri.value))
const codeClientId = computed(() => application.value?.clientId ?? 'YOUR_CLIENT_ID')
const codeRedirectUri = computed(() => selectedRedirectUri.value || 'YOUR_REDIRECT_URI')
const codeClientSecret = computed(() => {
  if (secretVisible.value && application.value) return application.value.clientSecret
  return 'YOUR_CLIENT_SECRET'
})

const endpoints = computed(() => [
  { label: 'Authorization endpoint', value: authorizationEndpoint },
  { label: 'Token endpoint', value: tokenEndpoint },
  { label: 'Userinfo endpoint', value: userinfoEndpoint }
])

const oauth2Examples = computed(() => [
  {
    title: '1. 获取授权码',
    description: '将用户重定向到授权端点：',
    code: `${authorizationEndpoint}?client_id=${codeClientId.value}&redirect_uri=${codeRedirectUri.value}&response_type=code&scope=openid%20profile%20email&state=RANDOM_STATE`
  },
  {
    title: '2. 使用授权码换取 Token',
    description: '向 Token 端点发送 POST 请求：',
    code: `POST ${tokenEndpoint}\nContent-Type: application/x-www-form-urlencoded\n\ngrant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=${codeRedirectUri.value}&client_id=${codeClientId.value}&client_secret=${codeClientSecret.value}`
  },
  {
    title: '3. 获取用户信息',
    description: '使用 Access Token 获取用户信息：',
    code: `GET ${userinfoEndpoint}\nAuthorization: Bearer ACCESS_TOKEN`
  }
])

const pkceExamples = computed(() => [
  {
    title: '1. 生成 Code Verifier 和 Code Challenge',
    description: 'Code Verifier 为 43-128 位随机字符串，Code Challenge 为 BASE64URL(SHA256(code_verifier))。',
    code: 'Code Verifier: RANDOM_CODE_VERIFIER\nCode Challenge: BASE64URL(SHA256(CODE_VERIFIER))'
  },
  {
    title: '2. 授权请求',
    description: '在授权请求中加入 PKCE 参数：',
    code: `${authorizationEndpoint}?client_id=${codeClientId.value}&redirect_uri=${codeRedirectUri.value}&response_type=code&scope=openid%20profile%20email&state=RANDOM_STATE&code_challenge=CODE_CHALLENGE&code_challenge_method=S256`
  },
  {
    title: '3. Token 请求',
    description: '使用 code_verifier 换取 Token，无需在客户端保存密钥：',
    code: `POST ${tokenEndpoint}\nContent-Type: application/x-www-form-urlencoded\n\ngrant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=${codeRedirectUri.value}&client_id=${codeClientId.value}&code_verifier=CODE_VERIFIER`
  }
])

const clientCredentialsExamples = computed(() => [
  {
    title: '适用于机器对机器通信',
    description: '服务端使用客户端凭证获取访问令牌：',
    code: `POST ${tokenEndpoint}\nContent-Type: application/x-www-form-urlencoded\n\ngrant_type=client_credentials&client_id=${codeClientId.value}&client_secret=${codeClientSecret.value}&scope=read%20write`
  }
])

const getApiErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return error instanceof Error ? error.message : fallback
}

const populateForms = (app: Application) => {
  Object.assign(configForm, {
    name: app.name,
    logo: app.logo ?? '',
    description: app.description ?? '',
    type: app.type,
    status: app.status
  })
  Object.assign(loginForm, {
    redirectUris: [...(app.redirectUris ?? [])],
    allowedGrantTypes: [...(app.allowedGrantTypes ?? [])],
    accessTokenLifetime: app.accessTokenLifetime,
    refreshTokenLifetime: app.refreshTokenLifetime
  })
}

const loadApplication = async () => {
  loading.value = true
  loadError.value = ''
  notFound.value = false

  if (!applicationId.value) {
    notFound.value = true
    loading.value = false
    return
  }

  try {
    const response = await applicationApi.getById(applicationId.value)
    application.value = response.data
    populateForms(response.data)
    document.title = `${response.data.name} - 应用详情`
  } catch (error: unknown) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      notFound.value = true
    } else {
      loadError.value = getApiErrorMessage(error, '加载应用详情失败')
    }
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/application')
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.success('已复制到剪贴板')
  } catch (error: unknown) {
    console.error('复制失败:', error)
    toast.error('复制失败')
  }
}

const addRedirectUri = () => {
  const uri = redirectUriInput.value.trim()
  if (!uri) return
  if (loginForm.redirectUris.includes(uri)) {
    toast.warning('该回调地址已存在')
    return
  }
  loginForm.redirectUris.push(uri)
  redirectUriInput.value = ''
}

const removeRedirectUri = (index: number) => {
  loginForm.redirectUris.splice(index, 1)
}

const toggleGrantType = (grantType: string) => {
  const index = loginForm.allowedGrantTypes.indexOf(grantType)
  if (index === -1) {
    loginForm.allowedGrantTypes.push(grantType)
  } else {
    loginForm.allowedGrantTypes.splice(index, 1)
  }
}

const saveConfig = async () => {
  if (!application.value) return
  if (!configForm.name.trim()) {
    toast.error('请输入应用名称')
    return
  }

  configSaving.value = true
  const payload: UpdateApplicationDto = {
    name: configForm.name.trim(),
    logo: configForm.logo.trim(),
    description: configForm.description.trim(),
    type: configForm.type,
    status: configForm.status
  }

  try {
    const response = await applicationApi.update(applicationId.value, payload)
    application.value = response.data
    document.title = `${response.data.name} - 应用详情`
    toast.success('应用配置已保存')
  } catch (error: unknown) {
    console.error('保存应用配置失败:', error)
    toast.error(getApiErrorMessage(error, '保存应用配置失败'))
  } finally {
    configSaving.value = false
  }
}

const saveLoginControl = async () => {
  if (!application.value) return
  if (loginForm.allowedGrantTypes.length === 0) {
    toast.error('请至少选择一种授权类型')
    return
  }
  if (loginForm.accessTokenLifetime < 60) {
    toast.error('访问令牌有效期不能小于 60 秒')
    return
  }
  if (loginForm.refreshTokenLifetime < 3600) {
    toast.error('刷新令牌有效期不能小于 3600 秒')
    return
  }

  loginSaving.value = true
  const payload: UpdateApplicationDto = {
    redirectUris: [...loginForm.redirectUris],
    allowedGrantTypes: [...loginForm.allowedGrantTypes],
    accessTokenLifetime: loginForm.accessTokenLifetime,
    refreshTokenLifetime: loginForm.refreshTokenLifetime
  }

  try {
    const response = await applicationApi.update(applicationId.value, payload)
    application.value = response.data
    toast.success('登录控制已保存')
  } catch (error: unknown) {
    console.error('保存登录控制失败:', error)
    toast.error(getApiErrorMessage(error, '保存登录控制失败'))
  } finally {
    loginSaving.value = false
  }
}

const regenerateSecret = async () => {
  if (!application.value) return
  if (!window.confirm('重新生成密钥后，旧密钥将立即失效。确定要重新生成吗？')) return

  try {
    const response = await applicationApi.regenerateSecret(applicationId.value)
    application.value = { ...application.value, clientSecret: response.data.clientSecret }
    secretVisible.value = true
    toast.success('密钥重新生成成功，请立即复制并妥善保管')
  } catch (error: unknown) {
    console.error('重新生成密钥失败:', error)
    toast.error(getApiErrorMessage(error, '重新生成密钥失败'))
  }
}

onMounted(loadApplication)
</script>

<template>
  <div class="min-h-full p-4 sm:p-6">
    <div v-if="loading" class="mx-auto flex max-w-6xl flex-col gap-6">
      <div class="flex items-center gap-3">
        <Skeleton class="h-9 w-24" />
        <div class="flex flex-col gap-2">
          <Skeleton class="h-8 w-64" />
          <Skeleton class="h-4 w-40" />
        </div>
      </div>
      <Card>
        <CardHeader>
          <Skeleton class="h-6 w-32" />
          <Skeleton class="h-4 w-72" />
        </CardHeader>
        <CardContent class="grid gap-6 md:grid-cols-2">
          <Skeleton class="h-10 w-full" />
          <Skeleton class="h-10 w-full" />
          <Skeleton class="h-10 w-full" />
          <Skeleton class="h-24 w-full" />
        </CardContent>
      </Card>
    </div>

    <Card v-else-if="notFound" class="mx-auto max-w-lg">
      <CardHeader>
        <CardTitle>应用不存在</CardTitle>
        <CardDescription>找不到对应的应用，可能已被删除或你没有访问权限。</CardDescription>
      </CardHeader>
      <CardFooter>
        <Button type="button" @click="goBack">返回应用列表</Button>
      </CardFooter>
    </Card>

    <Card v-else-if="loadError" class="mx-auto max-w-lg">
      <CardHeader>
        <CardTitle>加载失败</CardTitle>
        <CardDescription>{{ loadError }}</CardDescription>
      </CardHeader>
      <CardFooter class="flex gap-2">
        <Button type="button" variant="outline" @click="loadApplication">重新加载</Button>
        <Button type="button" @click="goBack">返回应用列表</Button>
      </CardFooter>
    </Card>

    <div v-else-if="application" class="mx-auto flex max-w-6xl flex-col gap-6">
      <header class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div class="flex items-start gap-3">
          <Button type="button" variant="ghost" class="-ml-3 shrink-0" @click="goBack">
            <ArrowLeft data-icon="inline-start" />
            返回
          </Button>
          <div class="flex min-w-0 items-start gap-3">
            <img
              v-if="application.logo"
              :src="application.logo"
              :alt="`${application.name} Logo`"
              width="48"
              height="48"
              class="size-12 rounded-lg border object-cover"
            />
            <div v-else class="flex size-12 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-lg font-semibold text-primary">
              {{ application.name.slice(0, 1).toUpperCase() }}
            </div>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h1 class="truncate text-2xl font-semibold tracking-tight">{{ application.name }}</h1>
                <Badge :variant="application.status === 'active' ? 'default' : 'secondary'">
                  {{ statusLabels[application.status] }}
                </Badge>
                <Badge variant="outline">{{ typeLabels[application.type] }}</Badge>
              </div>
              <p class="mt-1 truncate font-mono text-xs text-muted-foreground">{{ application.clientId }}</p>
            </div>
          </div>
        </div>
      </header>

      <Tabs default-value="config" class="w-full">
        <TabsList class="grid h-auto w-full max-w-xl grid-cols-3">
          <TabsTrigger value="config">应用配置</TabsTrigger>
          <TabsTrigger value="login">登录控制</TabsTrigger>
          <TabsTrigger value="access">访问授权</TabsTrigger>
        </TabsList>

        <TabsContent value="config" class="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>应用配置</CardTitle>
              <CardDescription>管理应用的基本信息和客户端凭证。</CardDescription>
            </CardHeader>
            <CardContent class="flex flex-col gap-6">
              <div class="grid gap-4 rounded-lg border bg-muted/30 p-4 md:grid-cols-2">
                <div class="grid gap-2">
                  <label for="application-client-id" class="text-sm font-medium">AppID / Client ID</label>
                  <div class="flex gap-2">
                    <Input id="application-client-id" :model-value="application.clientId" readonly class="font-mono text-sm" />
                    <Button type="button" variant="outline" size="icon" aria-label="复制 AppID" @click="copyToClipboard(application.clientId)">
                      <Copy data-icon="inline-start" />
                    </Button>
                  </div>
                </div>
                <div class="grid gap-2">
                  <label for="application-client-secret" class="text-sm font-medium">AppSecret / Client Secret</label>
                  <div class="flex gap-2">
                    <Input id="application-client-secret" :model-value="clientSecretDisplay" readonly class="font-mono text-sm" />
                    <Button type="button" variant="outline" size="icon" :aria-label="secretVisible ? '隐藏 AppSecret' : '显示 AppSecret'" @click="secretVisible = !secretVisible">
                      <EyeOff v-if="secretVisible" data-icon="inline-start" />
                      <Eye v-else data-icon="inline-start" />
                    </Button>
                    <Button type="button" variant="outline" size="icon" aria-label="复制 AppSecret" @click="copyToClipboard(application.clientSecret)">
                      <Copy data-icon="inline-start" />
                    </Button>
                    <Button type="button" variant="outline" size="icon" aria-label="重新生成 AppSecret" @click="regenerateSecret">
                      <RefreshCw data-icon="inline-start" />
                    </Button>
                  </div>
                  <p class="text-xs text-muted-foreground">重新生成后，旧密钥会立即失效。</p>
                </div>
              </div>

              <form class="grid gap-6" @submit.prevent="saveConfig">
                <div class="grid gap-4 md:grid-cols-2">
                  <div class="grid gap-2">
                    <label for="application-name" class="text-sm font-medium">应用名称</label>
                    <Input id="application-name" v-model="configForm.name" placeholder="请输入应用名称" />
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">应用类型</label>
                    <Select v-model="configForm.type">
                      <SelectTrigger>
                        <SelectValue placeholder="请选择应用类型" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="web">Web 应用</SelectItem>
                        <SelectItem value="native">原生应用</SelectItem>
                        <SelectItem value="spa">单页应用</SelectItem>
                        <SelectItem value="machine">机器对机器</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div class="grid gap-2">
                    <label for="application-logo" class="text-sm font-medium">应用 Logo</label>
                    <Input id="application-logo" v-model="configForm.logo" placeholder="请输入 Logo URL" />
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">应用状态</label>
                    <Select v-model="configForm.status">
                      <SelectTrigger>
                        <SelectValue placeholder="请选择应用状态" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="active">启用</SelectItem>
                        <SelectItem value="disabled">禁用</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div class="grid gap-2">
                  <label for="application-description" class="text-sm font-medium">应用描述</label>
                  <Textarea id="application-description" v-model="configForm.description" :rows="4" placeholder="请输入应用描述" />
                </div>
                <div class="flex justify-end">
                  <Button type="submit" :disabled="configSaving">
                    <Check v-if="!configSaving" data-icon="inline-start" />
                    {{ configSaving ? '保存中...' : '保存配置' }}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="login" class="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>登录控制</CardTitle>
              <CardDescription>配置 OAuth 登录的回调地址、授权类型和令牌有效期。</CardDescription>
            </CardHeader>
            <CardContent>
              <Alert class="mb-6">
                <AlertTitle>OAuth 登录配置</AlertTitle>
                <AlertDescription>新创建的应用需要配置回调地址和授权类型后才能正常使用 OAuth 登录</AlertDescription>
              </Alert>

              <form class="grid gap-6" @submit.prevent="saveLoginControl">
                <div class="grid gap-2">
                  <label for="redirect-uri" class="text-sm font-medium">回调地址</label>
                  <div class="flex gap-2">
                    <Input id="redirect-uri" v-model="redirectUriInput" class="flex-1" placeholder="https://example.com/oauth/callback" @keyup.enter.prevent="addRedirectUri" />
                    <Button type="button" variant="outline" @click="addRedirectUri">
                      <Plus data-icon="inline-start" />
                      添加
                    </Button>
                  </div>
                  <div v-if="loginForm.redirectUris.length" class="flex flex-wrap gap-2 pt-1">
                    <Badge v-for="(uri, index) in loginForm.redirectUris" :key="uri" variant="secondary" class="max-w-full cursor-pointer" @click="removeRedirectUri(index)">
                      <span class="truncate">{{ uri }}</span>
                      <Trash2 data-icon="inline-end" />
                    </Badge>
                  </div>
                  <p v-else class="text-sm text-muted-foreground">尚未配置回调地址，授权码流程将无法完成回调。</p>
                </div>

                <fieldset class="grid gap-3">
                  <legend class="text-sm font-medium">允许的授权类型</legend>
                  <div class="grid gap-3 md:grid-cols-3">
                    <label v-for="grantType in grantTypeOptions" :key="grantType.value" class="flex cursor-pointer items-start gap-3 rounded-lg border p-3 transition-colors hover:bg-muted/50">
                      <Checkbox :checked="loginForm.allowedGrantTypes.includes(grantType.value)" @update:checked="toggleGrantType(grantType.value)" />
                      <span class="grid gap-1">
                        <span class="text-sm font-medium">{{ grantType.label }}</span>
                        <span class="text-xs text-muted-foreground">{{ grantType.description }}</span>
                      </span>
                    </label>
                  </div>
                </fieldset>

                <div class="grid gap-4 md:grid-cols-2">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">访问令牌有效期</label>
                    <div class="flex items-center gap-2">
                      <NumberField v-model="loginForm.accessTokenLifetime" :min="60" :max="86400">
                        <NumberFieldContent>
                          <NumberFieldDecrement><Minus data-icon="inline-start" /></NumberFieldDecrement>
                          <NumberFieldInput />
                          <NumberFieldIncrement><Plus data-icon="inline-start" /></NumberFieldIncrement>
                        </NumberFieldContent>
                      </NumberField>
                      <span class="shrink-0 text-sm text-muted-foreground">秒</span>
                    </div>
                    <p class="text-xs text-muted-foreground">最短 60 秒。</p>
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">刷新令牌有效期</label>
                    <div class="flex items-center gap-2">
                      <NumberField v-model="loginForm.refreshTokenLifetime" :min="3600" :max="31536000">
                        <NumberFieldContent>
                          <NumberFieldDecrement><Minus data-icon="inline-start" /></NumberFieldDecrement>
                          <NumberFieldInput />
                          <NumberFieldIncrement><Plus data-icon="inline-start" /></NumberFieldIncrement>
                        </NumberFieldContent>
                      </NumberField>
                      <span class="shrink-0 text-sm text-muted-foreground">秒</span>
                    </div>
                    <p class="text-xs text-muted-foreground">最短 3600 秒。</p>
                  </div>
                </div>

                <div class="flex justify-end">
                  <Button type="submit" :disabled="loginSaving">
                    <Check v-if="!loginSaving" data-icon="inline-start" />
                    {{ loginSaving ? '保存中...' : '保存登录控制' }}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="access" class="mt-6 flex flex-col gap-6">
          <Card>
            <CardHeader>
              <CardTitle>OAuth2 / OIDC 端点</CardTitle>
              <CardDescription>以下端点可直接用于当前应用的 OAuth2 和 OpenID Connect 集成。</CardDescription>
            </CardHeader>
            <CardContent class="grid gap-3">
              <div v-for="endpoint in endpoints" :key="endpoint.value" class="flex flex-col gap-2 rounded-lg border p-3 sm:flex-row sm:items-center sm:justify-between">
                <div class="grid gap-1">
                  <span class="text-sm font-medium">{{ endpoint.label }}</span>
                  <code class="break-all text-xs text-muted-foreground">{{ endpoint.value }}</code>
                </div>
                <Button type="button" variant="outline" size="sm" class="shrink-0" @click="copyToClipboard(endpoint.value)">
                  <Copy data-icon="inline-start" />
                  复制
                </Button>
              </div>
            </CardContent>
          </Card>

          <Alert v-if="!hasRedirectUri">
            <AlertTitle>尚未配置回调地址</AlertTitle>
            <AlertDescription>请先在「登录控制」标签页配置回调地址</AlertDescription>
          </Alert>

          <Card>
            <CardHeader>
              <CardTitle>集成指南</CardTitle>
              <CardDescription>代码示例已使用当前应用的 Client ID；Client Secret 仅在你显示密钥后写入示例。</CardDescription>
            </CardHeader>
            <CardContent>
              <Tabs default-value="oauth2">
                <TabsList class="grid h-auto w-full grid-cols-1 md:grid-cols-3">
                  <TabsTrigger value="oauth2">OAuth2 授权码流程</TabsTrigger>
                  <TabsTrigger value="pkce">PKCE 安全增强</TabsTrigger>
                  <TabsTrigger value="client">客户端凭证流程</TabsTrigger>
                </TabsList>

                <TabsContent value="oauth2" class="mt-6 grid gap-6">
                  <div v-for="example in oauth2Examples" :key="example.title" class="grid gap-2">
                    <h3 class="text-base font-semibold">{{ example.title }}</h3>
                    <p class="text-sm text-muted-foreground">{{ example.description }}</p>
                    <div class="relative">
                      <pre class="max-h-80 overflow-auto whitespace-pre-wrap break-all rounded-lg bg-muted p-4 pr-14 font-mono text-xs leading-6">{{ example.code }}</pre>
                      <Button type="button" variant="ghost" size="icon" class="absolute right-2 top-2" :aria-label="`复制${example.title}`" @click="copyToClipboard(example.code)">
                        <Copy data-icon="inline-start" />
                      </Button>
                    </div>
                  </div>
                </TabsContent>

                <TabsContent value="pkce" class="mt-6 grid gap-6">
                  <div v-for="example in pkceExamples" :key="example.title" class="grid gap-2">
                    <h3 class="text-base font-semibold">{{ example.title }}</h3>
                    <p class="text-sm text-muted-foreground">{{ example.description }}</p>
                    <div class="relative">
                      <pre class="max-h-80 overflow-auto whitespace-pre-wrap break-all rounded-lg bg-muted p-4 pr-14 font-mono text-xs leading-6">{{ example.code }}</pre>
                      <Button type="button" variant="ghost" size="icon" class="absolute right-2 top-2" :aria-label="`复制${example.title}`" @click="copyToClipboard(example.code)">
                        <Copy data-icon="inline-start" />
                      </Button>
                    </div>
                  </div>
                </TabsContent>

                <TabsContent value="client" class="mt-6 grid gap-6">
                  <div v-for="example in clientCredentialsExamples" :key="example.title" class="grid gap-2">
                    <h3 class="text-base font-semibold">{{ example.title }}</h3>
                    <p class="text-sm text-muted-foreground">{{ example.description }}</p>
                    <div class="relative">
                      <pre class="max-h-80 overflow-auto whitespace-pre-wrap break-all rounded-lg bg-muted p-4 pr-14 font-mono text-xs leading-6">{{ example.code }}</pre>
                      <Button type="button" variant="ghost" size="icon" class="absolute right-2 top-2" :aria-label="`复制${example.title}`" @click="copyToClipboard(example.code)">
                        <Copy data-icon="inline-start" />
                      </Button>
                    </div>
                  </div>
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  </div>
</template>
