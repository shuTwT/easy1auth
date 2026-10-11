import axios from 'axios'
import type { SelectOption } from './types'

const domainStatusLabelMap: Record<string, string> = {
  verified: '已验证',
  pending: '待验证',
  verifying: '验证中'
}

const domainStatusColorMap: Record<string, string> = {
  verified: 'green',
  pending: 'gold',
  verifying: 'default'
}

/**
 * 自定义域名字典：域名状态、SSL 状态、验证方式的文本与颜色，列表页与弹窗共用。
 */
export function useDomainDict() {
  const verificationMethodOptions: SelectOption<'dns' | 'file'>[] = [
    { value: 'dns', label: 'DNS记录验证' },
    { value: 'file', label: '文件验证' }
  ]

  const getDomainStatusText = (status: string) => {
    return domainStatusLabelMap[status] ?? '验证失败'
  }

  const getDomainStatusColor = (status: string) => {
    return domainStatusColorMap[status] ?? 'red'
  }

  const getSslStatusText = (status: string) => {
    return status === 'active' ? '已配置' : '未配置'
  }

  const getSslStatusColor = (status: string) => {
    return status === 'active' ? 'green' : 'default'
  }

  const getVerificationMethodText = (method: string) => {
    return method === 'dns' ? 'DNS记录' : '文件验证'
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString('zh-CN')
  }

  return {
    verificationMethodOptions,
    getDomainStatusText,
    getDomainStatusColor,
    getSslStatusText,
    getSslStatusColor,
    getVerificationMethodText,
    formatDate
  }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data?.msg || fallback
  }
  return error instanceof Error ? error.message : fallback
}
