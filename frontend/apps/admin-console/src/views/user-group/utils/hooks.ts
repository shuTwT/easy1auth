import axios from 'axios'
import type { GroupType } from '@/types/user-group'
import type { SelectOption } from './types'

const typeLabelMap: Record<GroupType, string> = {
  team: '团队',
  department: '部门',
  project: '项目',
  organization: '组织'
}

const typeColorMap: Record<GroupType, string> = {
  team: 'blue',
  department: 'green',
  project: 'purple',
  organization: 'orange'
}

/**
 * 用户组字典：类型选项、文本与颜色，列表页、详情页与弹窗共用。
 */
export function useGroupDict() {
  const groupTypeOptions: SelectOption<GroupType>[] = Object.entries(typeLabelMap).map(([value, label]) => {
    return { value: value as GroupType, label }
  })

  const getTypeText = (type: string) => {
    return typeLabelMap[type as GroupType] ?? '未知'
  }

  const getTypeVariant = (type: string) => {
    return typeColorMap[type as GroupType] ?? 'default'
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString()
  }

  return {
    groupTypeOptions,
    getTypeText,
    getTypeVariant,
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
