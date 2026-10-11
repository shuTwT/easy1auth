import type { Tenant } from '@/types/tenant'
import type { SelectOption } from './types'

const statusOptions: SelectOption<Tenant['status']>[] = [
  { label: '启用', value: 'active' },
  { label: '停用', value: 'inactive' },
]

/**
 * 租户字典：状态选项与日期格式化，列表页与弹窗共用。
 */
export function useTenantDict() {
  const formatDate = (date: string) => {
    return new Date(date).toLocaleString('zh-CN', { hour12: false })
  }

  return {
    statusOptions,
    formatDate,
  }
}

/**
 * 从异常中提取错误信息。
 */
export function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}
