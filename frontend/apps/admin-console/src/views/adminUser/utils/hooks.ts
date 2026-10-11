import { computed } from 'vue'
import type { Ref } from 'vue'
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import type { AdminMembership, AdminStatus, AdminUser } from '@/types/adminUser'
import type { PermissionCatalogItem } from '@/types/adminRole'
import type { ApiErrorResponse, PermissionCategory, PermissionCategoryItem, SelectOption } from './types'

const adminStatusLabelMap: Record<AdminStatus, string> = {
  active: '正常',
  disabled: '禁用'
}

const tenantRoleLabelMap: Record<string, string> = {
  owner: '用户池所有者',
  admin: '管理员'
}

const permissionCategoryLabelMap: Record<string, string> = {
  'admin-user': '管理员用户管理',
  'admin-role': '管理员角色管理'
}

/**
 * 管理员字典：状态 / 用户池角色 / 管理员角色类型的文本与颜色映射，列表页与弹窗共用。
 */
export function useAdminDict() {
  const adminStatusOptions: SelectOption<AdminStatus>[] = Object.entries(adminStatusLabelMap).map(([value, label]) => {
    return { value: value as AdminStatus, label }
  })

  const adminStatusVariant = (status: AdminStatus) => {
    return status === 'active' ? 'green' : 'red'
  }

  const adminStatusText = (status: AdminStatus) => {
    return status === 'active' ? '正常' : '禁用'
  }

  const tenantRoleText = (role: string | null) => {
    if (!role) {
      return '-'
    }
    return tenantRoleLabelMap[role] || role
  }

  const tenantRoleVariant = (role: string | null) => {
    if (role === 'owner') {
      return 'purple'
    }
    return 'blue'
  }

  const roleTypeVariant = (isSystem: boolean) => {
    return isSystem ? 'purple' : 'blue'
  }

  const permissionsSummary = (permissions: string[]) => {
    if (permissions.includes('*')) {
      return '全部权限'
    }
    if (permissions.length === 0) {
      return '无权限'
    }
    return `${permissions.length} 项权限`
  }

  const formatDate = (date: string | null) => {
    if (!date) {
      return '-'
    }
    return new Date(date).toLocaleString('zh-CN')
  }

  return {
    adminStatusOptions,
    adminStatusVariant,
    adminStatusText,
    tenantRoleText,
    tenantRoleVariant,
    roleTypeVariant,
    permissionsSummary,
    formatDate
  }
}

/**
 * 管理员账号与当前选中用户池的成员关系判断。
 */
export function useAdminMembership() {
  const userStore = useUserStore()
  const currentAdminId = computed<string>(() => {
    return userStore.userInfo?.id || ''
  })

  const isSelf = (row: AdminUser) => {
    return !!currentAdminId.value && row.id === currentAdminId.value
  }

  /** 账号在任一用户池中是所有者，用于保护账号级操作。 */
  const isAccountOwner = (row: AdminUser) => {
    return row.userPools.some((m) => {
      return m.tenantRole === 'owner'
    })
  }

  /** 账号在当前选中用户池中的成员关系。 */
  const currentMembership = (row: AdminUser): AdminMembership | undefined => {
    const tid = userStore.currentUserPool?.id
    if (!tid) {
      return undefined
    }
    return row.userPools.find((m) => {
      return m.userPoolId === tid
    })
  }

  /** 当前用户池成员关系的角色；非成员时返回 null。 */
  const currentUserPoolRole = (row: AdminUser): string | null => {
    return currentMembership(row)?.tenantRole ?? null
  }

  /** 当前成员关系为不可变更的 owner 角色。 */
  const isCurrentMembershipOwner = (row: AdminUser) => {
    return currentMembership(row)?.tenantRole === 'owner'
  }

  return {
    isSelf,
    isAccountOwner,
    currentMembership,
    currentUserPoolRole,
    isCurrentMembershipOwner
  }
}

/**
 * 将权限目录按资源分组。
 */
export function usePermissionCategories(catalog: Ref<PermissionCatalogItem[]>) {
  const permissionCategories = computed<PermissionCategory[]>(() => {
    const groups: Record<string, PermissionCategoryItem[]> = {}
    const menuNames = new Map(catalog.value
      .filter((permission) => {
        return permission.type.toLowerCase() === 'menu'
      })
      .map((permission) => {
        return [permission.resource, permission.name]
      }))
    for (const permission of catalog.value) {
      const prefix = permission.resource || permission.code.split(':')[0] || permission.code
      if (!groups[prefix]) {
        groups[prefix] = []
      }
      groups[prefix].push({ code: permission.code, name: permission.name || permission.code })
    }
    return Object.entries(groups).map(([prefix, items]) => {
      return {
        code: prefix,
        name: permissionCategoryLabelMap[prefix] || menuNames.get(prefix) || prefix,
        items
      }
    })
  })

  return { permissionCategories }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return fallback
  }
  return error.response?.data?.msg ?? fallback
}
