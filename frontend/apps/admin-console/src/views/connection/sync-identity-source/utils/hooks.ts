import axios from 'axios'
import type { SelectOption, SourceStatus } from './types'

const statusLabelMap: Record<string, string> = {
  active: '启用',
  disabled: '停用',
  succeeded: '成功',
  partial: '部分完成',
  failed: '失败',
  pending: '排队中',
  processing: '同步中'
}

const successStatuses = ['active', 'succeeded']
const warningStatuses = ['partial', 'processing', 'pending']

/**
 * 同步身份源字典：状态选项、文本、颜色与时间格式化，列表页与抽屉共用。
 */
export function useSyncDict() {
  const statusOptions: SelectOption<SourceStatus>[] = [
    { value: 'active', label: '启用' },
    { value: 'disabled', label: '停用' }
  ]

  const statusColor = (value: string) => {
    if (successStatuses.includes(value)) {
      return 'success'
    }
    if (warningStatuses.includes(value)) {
      return 'warning'
    }
    return 'error'
  }

  const statusText = (value: string | null) => {
    return statusLabelMap[value ?? ''] ?? '-'
  }

  const formatDate = (value: string | null) => {
    return value ? new Date(value).toLocaleString() : '-'
  }

  return {
    statusOptions,
    statusColor,
    statusText,
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
