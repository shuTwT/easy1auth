import type { UserPool } from '@/types/userPool'
import type { UserPoolStatusOption } from './types'

const userPoolStatusOptions: UserPoolStatusOption[] = [
  { value: 'active', label: '正常' },
  { value: 'suspended', label: '停用' },
  { value: 'deleted', label: '已删除' },
]

/**
 * 用户池字典：状态 / 角色文本映射与配额格式化，列表页与弹窗共用。
 */
export function useUserPoolDict() {
  const formatLimit = (value?: number) => {
    if (value === undefined) {
      return '-'
    }
    return value >= 2147483647 ? '不限' : value.toLocaleString()
  }

  const statusLabel = (status: UserPool['status']) => {
    if (status === 'active') {
      return '正常'
    }
    if (status === 'suspended') {
      return '停用'
    }
    return '已删除'
  }

  const roleLabel = (role: string) => {
    if (role === 'super_admin') {
      return '超级管理员'
    }
    if (role === 'tenant_admin') {
      return '用户池管理员'
    }
    return '普通成员'
  }

  return {
    userPoolStatusOptions,
    formatLimit,
    statusLabel,
    roleLabel,
  }
}
