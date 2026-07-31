import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'antdv-next'
import { useUserStore } from '@/stores/user'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

type ApiResponse<T> = { code: number; data: T | null; msg?: string }
type RetryableRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean }

const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

let refreshPromise: Promise<string> | null = null
let redirectingToLogin = false

const isAnonymousAuthRequest = (url?: string) => /^\/auth\/(login|mfa\/verify|register|send-code|refresh)$/.test(url || '')

async function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) throw new Error('刷新令牌不存在')
  refreshPromise = refreshClient.post<ApiResponse<{ token: string; refreshToken: string }>>('/auth/refresh', { refreshToken })
    .then(({ data }) => {
      if (data.code !== 0 || !data.data?.token || !data.data.refreshToken) throw new Error(data.msg || '刷新登录状态失败')
      useUserStore().setSession(data.data.token, data.data.refreshToken)
      return data.data.token
    })
    .finally(() => { refreshPromise = null })
  return refreshPromise
}

function clearSessionAndRedirect() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  useUserStore().logout()
  message.error('登录已过期，请重新登录')
  window.location.assign('/login')
}

request.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem('accessToken')
    // Login is anonymous. Sending an expired token here lets Spring Security
    // reject the request before the login controller can return its business error.
    if (accessToken && !isAnonymousAuthRequest(config.url)) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    const userStore = useUserStore()
    if (userStore.currentTenant?.id) {
      config.headers['tenant-id'] = userStore.currentTenant.id
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data && typeof data === 'object' && typeof data.code === 'number') {
      if (data.code !== 0) {
        message.error(data.msg || '请求失败')
        return Promise.reject(new Error(data.msg || '请求失败'))
      }
      return data.data
    }
    return data
  },
  async (error: AxiosError) => {
    const { response } = error
    const originalRequest = error.config as RetryableRequestConfig | undefined
    if (response) {
      if (response.status === 401 && originalRequest && !originalRequest._retry && !isAnonymousAuthRequest(originalRequest.url)) {
        originalRequest._retry = true
        try {
          const accessToken = await refreshAccessToken()
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return request(originalRequest)
        } catch {
          clearSessionAndRedirect()
          return Promise.reject(error)
        }
      }
      switch (response.status) {
        case 401:
          clearSessionAndRedirect()
          break
        case 403:
          message.error('没有权限访问')
          break
        case 404:
          message.error('请求资源不存在')
          break
        case 500:
          message.error('服务器错误')
          break
        default:
          message.error((response.data as { msg?: string } | undefined)?.msg || '请求失败')
      }
    } else {
      message.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
