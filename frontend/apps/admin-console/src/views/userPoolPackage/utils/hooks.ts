import { computed } from 'vue'
import type { ManagementPermission, UserPoolPackageStatus } from '@/types/userPoolPackage'
import type { PermissionTreeNode } from './types'

const permissionTypeLabelMap: Record<string, string> = {
  DIRECTORY: '目录',
  MENU: '菜单',
  ACTION: '按钮',
}

/**
 * 用户池套餐字典：权限类型 / 状态的文本与颜色映射，以及日期、配额格式化。
 */
export function useUserPoolPackageDict() {
  const permissionTypeLabel = (type: string) => {
    return permissionTypeLabelMap[type.toUpperCase()] || type
  }

  const permissionTypeVariant = (type: string): string => {
    return type.toUpperCase() === 'MENU' ? 'blue' : 'green'
  }

  const statusVariant = (status: UserPoolPackageStatus): string => {
    return status === 'active' ? 'green' : 'red'
  }

  const formatDate = (value: string | null) => {
    return value ? new Date(value).toLocaleString() : '-'
  }

  const formatLimit = (value: number) => {
    return value >= 2147483647 ? '不限' : value.toLocaleString()
  }

  return {
    permissionTypeLabel,
    permissionTypeVariant,
    statusVariant,
    formatDate,
    formatLimit,
  }
}

/**
 * 收集节点自身及全部子孙节点的权限编码。
 */
export function descendantCodes(node: PermissionTreeNode): string[] {
  return [node.code, ...node.children.flatMap(descendantCodes)].filter((code): code is string => {
    return !!code
  })
}

/**
 * 将权限目录构建为按菜单层级组织的权限树，仅保留用户池作用域的启用权限。
 */
export function usePermissionTree(getPermissions: () => ManagementPermission[]) {
  const userPoolPermissions = computed(() => {
    return getPermissions()
      .filter((permission) => {
        return permission.active && permission.scope.toUpperCase() === 'USER_POOL'
      })
      .sort((a, b) => {
        return a.sortOrder - b.sortOrder || a.code.localeCompare(b.code)
      })
  })

  const permissionTree = computed<PermissionTreeNode[]>(() => {
    const menuNodes = new Map<string, PermissionTreeNode>()
    const rootMenus: PermissionTreeNode[] = []
    const otherActions: PermissionTreeNode[] = []

    const menuPermissions = userPoolPermissions.value.filter((permission) => {
      return ['DIRECTORY', 'MENU'].includes(permission.type.toUpperCase())
    })

    menuPermissions.forEach((permission) => {
      menuNodes.set(permission.code, {
        id: permission.code,
        label: permission.name,
        code: permission.code,
        type: permission.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
        children: [],
        ancestorCodes: [],
      })
    })

    menuPermissions.forEach((permission) => {
      const node = menuNodes.get(permission.code)!
      const parent = permission.parentCode ? menuNodes.get(permission.parentCode) : undefined
      if (parent) {
        node.ancestorCodes = [...parent.ancestorCodes, parent.code!]
        parent.children.push(node)
      } else {
        rootMenus.push(node)
      }
    })

    userPoolPermissions.value
      .filter((permission) => {
        return permission.type.toUpperCase() === 'ACTION'
      })
      .forEach((permission) => {
        const parent = permission.parentCode ? menuNodes.get(permission.parentCode) : undefined
        const node: PermissionTreeNode = {
          id: permission.code,
          label: permission.name,
          code: permission.code,
          type: 'action',
          children: [],
          ancestorCodes: parent ? [...parent.ancestorCodes, parent.code!] : [],
        }
        if (parent) {
          parent.children.push(node)
        } else {
          otherActions.push(node)
        }
      })

    const groups: PermissionTreeNode[] = []
    if (rootMenus.length > 0) {
      groups.push({ id: 'group:menus', label: '菜单权限', type: 'group', children: rootMenus, ancestorCodes: [] })
    }
    if (otherActions.length > 0) {
      groups.push({ id: 'group:actions', label: '其他操作权限', type: 'group', children: otherActions, ancestorCodes: [] })
    }
    return groups
  })

  return { permissionTree }
}
