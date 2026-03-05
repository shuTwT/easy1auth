import { shallowRef, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type {
  LoginRequest,
  RegisterRequest,
  SendCodeRequest
} from '@/types/auth'

export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  const loading = shallowRef(false)
  const sendingCode = shallowRef(false)
  const countdown = shallowRef(0)

  const isCountingDown = computed(() => countdown.value > 0)

  async function login(data: LoginRequest) {
    loading.value = true
    try {
      const response = await authApi.login(data)
      userStore.setToken(response.token)
      userStore.setUserInfo(response.user)
      if (response.tenants) {
        userStore.setTenants(response.tenants)
        const currentTenant = response.tenants.find(t => t.id === response.user.currentTenantId)
        if (currentTenant) {
          userStore.setCurrentTenant(currentTenant)
        }
      }
      ElMessage.success('登录成功')
      await router.push('/dashboard')
      return response
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '登录失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  async function sendCode(data: SendCodeRequest) {
    if (isCountingDown.value) return

    sendingCode.value = true
    try {
      const response = await authApi.sendCode(data)
      ElMessage.success(response.message || '验证码已发送')
      startCountdown(60)
      return response
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '发送验证码失败')
      throw error
    } finally {
      sendingCode.value = false
    }
  }

  function startCountdown(seconds: number) {
    countdown.value = seconds
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  }

  async function register(data: RegisterRequest) {
    loading.value = true
    try {
      const response = await authApi.register(data)
      userStore.setToken(response.token)
      userStore.setUserInfo(response.user)
      if (response.tenants) {
        userStore.setTenants(response.tenants)
        const currentTenant = response.tenants.find(t => t.id === response.user.currentTenantId)
        if (currentTenant) {
          userStore.setCurrentTenant(currentTenant)
        }
      }
      ElMessage.success('注册成功')
      await router.push('/dashboard')
      return response
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '注册失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  async function passkeyLogin() {
    loading.value = true
    try {
      const startResponse = await authApi.passkeyLoginStart()
      
      const credential = await navigator.credentials.get({
        publicKey: {
          challenge: Uint8Array.from(atob(startResponse.challenge), c => c.charCodeAt(0)),
          rpId: startResponse.rpId,
          allowCredentials: startResponse.allowCredentials.map(cred => ({
            id: Uint8Array.from(atob(cred.id), c => c.charCodeAt(0)),
            type: 'public-key' as PublicKeyCredentialType,
            transports: cred.transports as AuthenticatorTransport[]
          })),
          userVerification: 'preferred'
        }
      }) as PublicKeyCredential

      if (!credential) {
        throw new Error('未获取到认证凭证')
      }

      const finishData = {
        credential: {
          id: credential.id,
          rawId: btoa(String.fromCharCode(...new Uint8Array(credential.rawId))),
          response: {
            clientDataJSON: btoa(String.fromCharCode(...new Uint8Array((credential.response as AuthenticatorAssertionResponse).clientDataJSON))),
            authenticatorData: btoa(String.fromCharCode(...new Uint8Array((credential.response as AuthenticatorAssertionResponse).authenticatorData))),
            signature: btoa(String.fromCharCode(...new Uint8Array((credential.response as AuthenticatorAssertionResponse).signature))),
            userHandle: (credential.response as AuthenticatorAssertionResponse).userHandle 
              ? btoa(String.fromCharCode(...new Uint8Array((credential.response as AuthenticatorAssertionResponse).userHandle!))) 
              : undefined
          },
          type: credential.type
        }
      }

      const loginResponse = await authApi.passkeyLoginFinish(finishData)
      userStore.setToken(loginResponse.token)
      userStore.setUserInfo(loginResponse.user)
      ElMessage.success('登录成功')
      await router.push('/dashboard')
      return loginResponse
    } catch (error: any) {
      if (error.name === 'NotAllowedError') {
        ElMessage.error('用户取消或认证超时')
      } else {
        ElMessage.error(error.response?.data?.message || 'Passkey 登录失败')
      }
      throw error
    } finally {
      loading.value = false
    }
  }

  async function socialLogin(provider: string) {
    loading.value = true
    try {
      const { url } = await authApi.getSocialLoginUrl(provider)
      window.location.href = url
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '获取授权链接失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  async function handleSocialCallback(provider: string, code: string, state?: string) {
    loading.value = true
    try {
      const response = await authApi.socialLogin({ provider, code, state })
      userStore.setToken(response.token)
      userStore.setUserInfo(response.user)
      
      if (response.isNewUser) {
        ElMessage.success('注册成功，欢迎使用')
      } else {
        ElMessage.success('登录成功')
      }
      
      await router.push('/dashboard')
      return response
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '第三方登录失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    sendingCode,
    countdown,
    isCountingDown,
    login,
    sendCode,
    register,
    passkeyLogin,
    socialLogin,
    handleSocialCallback
  }
}
