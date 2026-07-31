<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { CircleX, LoaderCircle } from '@lucide/vue'
import { useAuth } from '@/composables/useAuth'
import { isSocialProviderType } from '@/types/socialIdentityProvider'
const route = useRoute()
const router = useRouter()
const { handleSocialCallback } = useAuth()
const errorMessage = ref('')

const firstValue = (value: unknown): string | undefined => {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && typeof value[0] === 'string') return value[0]
  return undefined
}

const getErrorMessage = (error: unknown): string => {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data.msg ?? '社会化登录失败，请重试'
  }
  return error instanceof Error ? error.message : '社会化登录失败，请重试'
}

onMounted(async () => {
  const provider = firstValue(route.params.provider)
  const code = firstValue(route.query.code)
  const state = firstValue(route.query.state)
  const providerError = firstValue(route.query.error_description) ?? firstValue(route.query.error)

  if (providerError) {
    errorMessage.value = providerError
    return
  }

  if (!isSocialProviderType(provider)) {
    errorMessage.value = '不支持的社会化身份源'
    return
  }

  if (!code) {
    errorMessage.value = '授权回调缺少必要的授权码'
    return
  }

  try {
    await handleSocialCallback(provider, code, state)
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  }
})
</script>

<template>
  <main class="flex min-h-[100dvh] items-center justify-center bg-muted/40 p-4">
    <Card class="w-full max-w-md">
      <div class="text-center">
        <h3>{{ errorMessage ? '授权登录失败' : '正在完成授权' }}</h3>
        <p>
          {{ errorMessage ? '请返回登录页重新发起授权' : '正在验证第三方账号信息，请稍候' }}
        </p>
      </div>
      <div class="flex flex-col gap-4">
        <Alert v-if="errorMessage" color="error">
          <CircleX />
          <h3>无法完成登录</h3>
          <p>{{ errorMessage }}</p>
        </Alert>
        <div v-else class="flex items-center justify-center gap-3 py-6 text-muted-foreground" aria-live="polite">
          <LoaderCircle class="animate-spin" />
          <span>处理授权结果...</span>
        </div>
        <Button v-if="errorMessage" @click="router.replace('/login')">
          返回登录页
        </Button>
      </div>
    </Card>
  </main>
</template>
