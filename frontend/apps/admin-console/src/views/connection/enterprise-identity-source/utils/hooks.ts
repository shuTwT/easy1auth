import axios from 'axios'
import type { SelectOption, SourceStatus } from './types'

const statusLabelMap: Record<SourceStatus, string> = {
  active: '已启用',
  disabled: '已禁用'
}

/**
 * 企业身份源字典：状态选项与展示文案，列表页与弹窗共用。
 */
export function useEnterpriseSourceDict() {
  const statusOptions: SelectOption<SourceStatus>[] = [
    { value: 'active', label: statusLabelMap.active },
    { value: 'disabled', label: statusLabelMap.disabled }
  ]

  const statusText = (status: SourceStatus) => {
    return statusLabelMap[status] ?? status
  }

  const statusColor = (status: SourceStatus) => {
    return status === 'active' ? 'green' : 'red'
  }

  const formatDate = (value: string) => {
    return new Date(value).toLocaleString('zh-CN')
  }

  return {
    statusOptions,
    statusText,
    statusColor,
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
