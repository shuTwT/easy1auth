import { computed, ref } from 'vue'
import type { Ref } from 'vue'
import { message } from 'antdv-next'
import { authorizationApi } from '@/api/authorization'
import type { ManagementMenu } from '@/types/authorization'
import type { MenuNodeType, MenuTreeNode } from './types'

const typeLabelMap: Record<MenuNodeType, string> = {
  directory: '目录',
  menu: '菜单',
  action: '按钮'
}

const typeColorMap: Record<MenuNodeType, string> = {
  directory: 'purple',
  menu: 'blue',
  action: 'green'
}

/**
 * 菜单字典：节点类型与作用域的文本、颜色。
 */
export function useMenuDict() {
  const getTypeText = (type: MenuNodeType) => {
    return typeLabelMap[type]
  }

  const getTypeColor = (type: MenuNodeType) => {
    return typeColorMap[type]
  }

  const scopeLabel = (scope: string) => {
    return scope.toUpperCase() === 'PLATFORM' ? '平台' : '用户池'
  }

  return {
    getTypeText,
    getTypeColor,
    scopeLabel
  }
}

const sortMenus = (left: ManagementMenu, right: ManagementMenu) => {
  return left.sortOrder - right.sortOrder || left.code.localeCompare(right.code)
}

/**
 * 菜单权限目录：加载目录数据，并组装为「目录 / 菜单 / 按钮」树，支持关键字过滤。
 */
export function useMenuCatalog(search: Ref<string>) {
  const loading = ref(false)
  const menus = ref<ManagementMenu[]>([])
  const permissions = ref<ManagementMenu[]>([])

  const menuTree = computed<MenuTreeNode[]>(() => {
    const nodes = new Map<string, MenuTreeNode>()
    const roots: MenuTreeNode[] = []

    menus.value
      .slice()
      .sort(sortMenus)
      .forEach((menu) => {
        nodes.set(menu.code, {
          id: menu.code,
          label: menu.name,
          code: menu.code,
          resource: menu.resource,
          scope: menu.scope,
          type: menu.type.toUpperCase() === 'DIRECTORY' ? 'directory' : 'menu',
          children: []
        })
      })

    menus.value.forEach((menu) => {
      const node = nodes.get(menu.code)
      if (!node) {
        return
      }
      const parent = menu.parentCode ? nodes.get(menu.parentCode) : undefined
      if (parent) {
        parent.children.push(node)
      } else {
        roots.push(node)
      }
    })

    permissions.value
      .filter((permission) => {
        return permission.type.toUpperCase() === 'ACTION' && permission.parentCode && nodes.has(permission.parentCode)
      })
      .sort(sortMenus)
      .forEach((permission) => {
        const parent = permission.parentCode ? nodes.get(permission.parentCode) : undefined
        parent?.children.push({
          id: permission.code,
          label: permission.name,
          code: permission.code,
          resource: permission.resource,
          scope: permission.scope,
          type: 'action',
          children: []
        })
      })

    return roots
  })

  const filteredTree = computed(() => {
    const keyword = search.value.trim().toLowerCase()
    if (!keyword) {
      return menuTree.value
    }
    const filter = (node: MenuTreeNode): MenuTreeNode | null => {
      const children = node.children.map(filter).filter((item): item is MenuTreeNode => item !== null)
      const matched = [node.label, node.code, node.resource].some((value) => value.toLowerCase().includes(keyword))
      return matched || children.length > 0 ? { ...node, children } : null
    }
    return menuTree.value.map(filter).filter((item): item is MenuTreeNode => item !== null)
  })

  const loadCatalog = async () => {
    loading.value = true
    try {
      const response = await authorizationApi.getMenuCatalog()
      menus.value = response.menus
      permissions.value = response.permissions
    } catch (error: unknown) {
      console.error('加载菜单权限目录失败:', error)
      message.error('加载菜单权限目录失败')
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    filteredTree,
    loadCatalog
  }
}
