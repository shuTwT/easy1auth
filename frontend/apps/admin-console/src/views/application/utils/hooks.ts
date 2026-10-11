import axios from 'axios'
import { message } from 'antdv-next'
import type { ApplicationStatus, ApplicationType } from '@/types/application'
import type { ApiErrorResponse, SelectOption } from './types'

const typeLabelMap: Record<ApplicationType, string> = {
  web: 'Web应用',
  native: '原生应用',
  spa: '单页应用',
  machine: '机器对机器'
}

const statusLabelMap: Record<ApplicationStatus, string> = {
  active: '正常',
  disabled: '禁用'
}

const statusColorMap: Record<ApplicationStatus, string> = {
  active: 'green',
  disabled: 'orange'
}

const grantTypeOptions = [
  { value: 'authorization_code', label: '授权码模式', description: '适用于有用户交互的 Web 和原生应用' },
  { value: 'client_credentials', label: '客户端凭证模式', description: '适用于机器对机器通信' },
  { value: 'refresh_token', label: '刷新令牌', description: '允许应用续期已过期的访问令牌' }
] as const

/**
 * 应用字典：类型 / 状态 / 授权类型的选项与文本映射，列表页、详情页与弹窗共用。
 */
export function useApplicationDict() {
  const applicationTypeOptions: SelectOption<ApplicationType>[] = Object.entries(typeLabelMap).map(([value, label]) => {
    return { value: value as ApplicationType, label }
  })

  const applicationStatusOptions: SelectOption<ApplicationStatus>[] = Object.entries(statusLabelMap).map(([value, label]) => {
    return { value: value as ApplicationStatus, label }
  })

  const getTypeText = (type: string) => {
    return typeLabelMap[type as ApplicationType] ?? '未知'
  }

  const getStatusText = (status: string) => {
    return statusLabelMap[status as ApplicationStatus] ?? '未知'
  }

  const getStatusVariant = (status: string) => {
    return statusColorMap[status as ApplicationStatus] ?? 'red'
  }

  const formatLifetime = (seconds?: number) => {
    if (!seconds) {
      return 'N/A'
    }
    if (seconds < 3600) {
      return `${Math.floor(seconds / 60)} 分钟`
    }
    if (seconds < 86400) {
      return `${Math.floor(seconds / 3600)} 小时`
    }
    return `${Math.floor(seconds / 86400)} 天`
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString()
  }

  return {
    applicationTypeOptions,
    applicationStatusOptions,
    grantTypeOptions,
    getTypeText,
    getStatusText,
    getStatusVariant,
    formatLifetime,
    formatDate
  }
}

/**
 * 复制文本到剪贴板，并通过 message 提示结果。
 */
export function useClipboard() {
  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text)
      message.success('已复制到剪贴板')
    } catch (error: unknown) {
      console.error('复制失败:', error)
      message.error('复制失败')
    }
  }

  return { copyToClipboard }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.msg ?? fallback
  }
  return error instanceof Error ? error.message : fallback
}
