import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { message } from 'antdv-next'
import { useUserStore } from '@/stores/user'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    // Login is anonymous. Sending an expired token here lets Spring Security
    // reject the request before the login controller can return its business error.
    const isAnonymousAuthRequest = /^\/auth\/(login|mfa\/verify|register|send-code|refresh)$/.test(config.url || '')
    if (token && !isAnonymousAuthRequest) {
      config.headers.Authorization = `Bearer ${token}`
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
  (error) => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          message.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          localStorage.removeItem('currentTenantId')
          useUserStore().logout()
          window.location.href = '/login'
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
          message.error(response.data?.msg || '请求失败')
      }
    } else {
      message.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
