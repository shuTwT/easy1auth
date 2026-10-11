import type { AuditLogStatus, AuditLogType } from '@/types/audit'
import type { SelectOption } from './types'

const typeLabelMap: Record<AuditLogType, string> = {
  auth: '认证',
  user: '用户',
  application: '应用',
  tenant: '用户池',
  role: '角色',
  group: '用户组',
  system: '系统'
}

const typeColorMap: Record<AuditLogType, string> = {
  auth: 'blue',
  user: 'green',
  application: 'purple',
  tenant: 'red',
  role: 'orange',
  group: 'cyan',
  system: 'default'
}

const actionLabelMap: Record<string, string> = {
  login: '登录',
  logout: '登出',
  login_failed: '登录失败',
  create: '创建',
  update: '更新',
  delete: '删除',
  view: '查看',
  enable: '启用',
  disable: '禁用',
  lock: '锁定',
  unlock: '解锁',
  assign: '分配',
  revoke: '撤销',
  password_change: '修改密码',
  password_reset: '重置密码',
  token_generate: '生成令牌',
  token_revoke: '撤销令牌',
  config_change: '配置变更'
}

const statusLabelMap: Record<AuditLogStatus, string> = {
  success: '成功',
  failed: '失败'
}

/**
 * 审计日志字典：类型 / 操作 / 状态的文本与颜色映射，列表页与详情弹窗共用。
 */
export function useAuditDict() {
  const auditTypeOptions: SelectOption<AuditLogType>[] = Object.entries(typeLabelMap).map(([value, label]) => {
    return { value: value as AuditLogType, label }
  })

  const auditStatusOptions: SelectOption<AuditLogStatus>[] = Object.entries(statusLabelMap).map(([value, label]) => {
    return { value: value as AuditLogStatus, label }
  })

  const getTypeText = (type: string) => {
    return typeLabelMap[type as AuditLogType] ?? type
  }

  const getTypeVariant = (type: string) => {
    return typeColorMap[type as AuditLogType] ?? 'default'
  }

  const getActionText = (action: string) => {
    return actionLabelMap[action] ?? action
  }

  const getStatusText = (status: string) => {
    return status === 'success' ? '成功' : '失败'
  }

  const getStatusVariant = (status: string) => {
    return status === 'success' ? 'green' : 'red'
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString()
  }

  return {
    auditTypeOptions,
    auditStatusOptions,
    getTypeText,
    getTypeVariant,
    getActionText,
    getStatusText,
    getStatusVariant,
    formatDate
  }
}
