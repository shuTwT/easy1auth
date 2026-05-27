import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { toast } from 'vue-sonner'
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
    if (token) {
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
    return data
  },
  (error) => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          if (window.location.pathname !== '/login') {
            toast.error('登录已过期，请重新登录')
            localStorage.removeItem('token')
            localStorage.removeItem('currentTenantId')
            const userStore = useUserStore()
            userStore.logout()
            window.location.href = '/login'
          }
          break
        case 403:
          toast.error('没有权限访问')
          break
        case 404:
          toast.error('请求资源不存在')
          break
        case 500:
          toast.error('服务器错误')
          break
        default:
          toast.error(response.data?.message || '请求失败')
      }
    } else {
      toast.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
