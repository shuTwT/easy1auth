import { shallowRef, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'antdv-next'
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
  const mfaChallenge = shallowRef<string | null>(null)

  const isCountingDown = computed(() => countdown.value > 0)

  async function login(data: LoginRequest) {
    loading.value = true
    try {
      const response = await authApi.login(data)
      if (response.status === 'mfa_required' && response.challengeToken) {
        mfaChallenge.value = response.challengeToken
        message.info('请输入身份验证器中的动态验证码')
        return response
      }
      if (!response.token || !response.refreshToken || !response.user) throw new Error('登录响应无效')
      const user = response.user
      userStore.setSession(response.token, response.refreshToken)
      userStore.setUserInfo(response.user)
      if (response.tenants && response.tenants.length > 0) {
        userStore.setTenants(response.tenants)
        
        let currentTenant = response.tenants.find(t => t.id === user.currentTenantId)
        
        if (!currentTenant) {
          currentTenant = response.tenants[0]
        }
        
        if (currentTenant) {
          userStore.setCurrentTenant(currentTenant)
        }
      }
      message.success('登录成功')
      await router.push('/dashboard')
      return response
    } finally {
      loading.value = false
    }
  }

  async function verifyMfa(code: string) {
    if (!mfaChallenge.value) throw new Error('MFA 挑战不存在或已过期')
    loading.value = true
    try {
      const response = await authApi.verifyMfa(mfaChallenge.value, code)
      if (!response.token || !response.refreshToken || !response.user) throw new Error('MFA 登录响应无效')
      mfaChallenge.value = null
      userStore.setSession(response.token, response.refreshToken)
      userStore.setUserInfo(response.user)
      if (response.tenants?.length) {
        userStore.setTenants(response.tenants)
        userStore.setCurrentTenant(response.tenants.find(t => t.id === response.user?.currentTenantId) || response.tenants[0] || null)
      }
      message.success('登录成功')
      await router.push('/dashboard')
      return response
    } finally { loading.value = false }
  }

  async function sendCode(data: SendCodeRequest) {
    if (isCountingDown.value) return

    sendingCode.value = true
    try {
      const response = await authApi.sendCode(data)
      message.success('验证码已发送')
      startCountdown(60)
      return response
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
      userStore.setSession(response.token, response.refreshToken)
      userStore.setUserInfo(response.user)
      if (response.tenants && response.tenants.length > 0) {
        userStore.setTenants(response.tenants)
        
        let currentTenant = response.tenants.find(t => t.id === response.user.currentTenantId)
        
        if (!currentTenant) {
          currentTenant = response.tenants[0]
        }
        
        if (currentTenant) {
          userStore.setCurrentTenant(currentTenant)
        }
      }
      message.success('注册成功')
      await router.push('/dashboard')
      return response
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
      if (!loginResponse.token || !loginResponse.refreshToken || !loginResponse.user) throw new Error('Passkey 登录响应无效')
      userStore.setSession(loginResponse.token, loginResponse.refreshToken)
      userStore.setUserInfo(loginResponse.user)
      message.success('登录成功')
      await router.push('/dashboard')
      return loginResponse
    } catch (error: any) {
      if (error.name === 'NotAllowedError') {
        message.error('用户取消或认证超时')
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
    } finally {
      loading.value = false
    }
  }

  async function handleSocialCallback(provider: string, code: string, state?: string) {
    loading.value = true
    try {
      const response = await authApi.socialLogin({ provider, code, state })
      userStore.setSession(response.token, response.refreshToken)
      userStore.setUserInfo(response.user)
      
      if (response.isNewUser) {
        message.success('注册成功，欢迎使用')
      } else {
        message.success('登录成功')
      }
      
      await router.push('/dashboard')
      return response
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
    ,mfaChallenge
    ,verifyMfa
  }
}
