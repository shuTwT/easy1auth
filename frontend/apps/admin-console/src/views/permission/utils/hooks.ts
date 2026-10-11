import { computed } from 'vue'
import type { Ref } from 'vue'
import type { PermissionSpace, PermissionTree } from '@/types/permission'
import type { SelectOption } from './types'

const commonOperationOptions: SelectOption[] = [
  { value: 'read', label: '读取（read）' },
  { value: 'create', label: '创建（create）' },
  { value: 'update', label: '更新（update）' },
  { value: 'delete', label: '删除（delete）' }
]

/**
 * 权限空间：下拉选项与名称查找，列表页与资源弹窗共用。
 */
export function useSpaceOptions(spaces: Ref<PermissionSpace[]>) {
  const spaceOptions = computed(() => {
    return spaces.value.map((space) => {
      return { value: space.id, label: `${space.name}（${space.code}）` }
    })
  })

  const getSpaceName = (id?: string | null) => {
    return spaces.value.find((item) => item.id === id)?.name || '-'
  }

  return { spaceOptions, getSpaceName }
}

/**
 * 资源操作选项：常用操作 + 资源树中已出现的自定义操作 + 当前已选操作。
 */
export function useOperationOptions(resources: Ref<PermissionTree[]>, getSelected: () => string[] | undefined) {
  const operationOptions = computed(() => {
    const options = new Map<string, SelectOption>(commonOperationOptions.map((option) => [option.value, option]))

    const addOperation = (value: string) => {
      const operation = value.trim()
      if (operation && !options.has(operation)) {
        options.set(operation, { value: operation, label: operation })
      }
    }

    const collect = (nodes: PermissionTree[]) => {
      nodes.forEach((node) => {
        ;(node.operations || []).forEach(addOperation)
        if (node.action) {
          addOperation(node.action)
        }
        collect(node.children || [])
      })
    }

    collect(resources.value)
    ;(getSelected() || []).forEach(addOperation)
    return [...options.values()]
  })

  return { operationOptions }
}

/**
 * 按选中的权限空间过滤资源树，未选中时返回全部。
 */
export function useVisibleResources(resources: Ref<PermissionTree[]>, selectedSpaceId: Ref<string | undefined>) {
  const visibleResources = computed(() => {
    if (!selectedSpaceId.value) {
      return resources.value
    }
    const filter = (nodes: PermissionTree[]): PermissionTree[] => {
      return nodes
        .map((node) => ({ ...node, children: filter(node.children || []) }))
        .filter((node) => node.spaceId === selectedSpaceId.value || node.children.length > 0)
    }
    return filter(resources.value)
  })

  return { visibleResources }
}
