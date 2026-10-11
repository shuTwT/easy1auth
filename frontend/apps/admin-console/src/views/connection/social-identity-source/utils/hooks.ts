import axios from 'axios'
import { SOURCE_CONFIGS, SOCIAL_SOURCE_TYPES } from '@/types/loginIdentitySource'
import type { LoginSourceType } from '@/types/loginIdentitySource'
import type { SelectOption, SourceStatus } from './types'

const statusLabelMap: Record<SourceStatus, string> = {
  active: '已启用',
  disabled: '已禁用'
}

/**
 * 社会化身份源字典：类型与状态选项、展示文案，列表页与弹窗共用。
 */
export function useSocialSourceDict() {
  const typeOptions: SelectOption[] = SOCIAL_SOURCE_TYPES.map((value) => ({
    value,
    label: SOURCE_CONFIGS[value].name
  }))

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

  const typeName = (type: LoginSourceType) => {
    return SOURCE_CONFIGS[type]?.name ?? type
  }

  const typeColor = (type: LoginSourceType) => {
    return SOURCE_CONFIGS[type]?.color
  }

  const formatDate = (value: string) => {
    return new Date(value).toLocaleString('zh-CN')
  }

  return {
    typeOptions,
    statusOptions,
    statusText,
    statusColor,
    typeName,
    typeColor,
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
