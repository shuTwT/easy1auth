import { computed, ref } from 'vue'
import type { Ref } from 'vue'
import { message } from 'antdv-next'
import { permissionApi } from '@/api/permission'
import type { PermissionSpace, PermissionTree } from '@/types/permission'
import type { SelectOption } from './types'

const commonOperationOptions: SelectOption[] = [
  { value: 'read', label: '读取（read）' },
  { value: 'create', label: '创建（create）' },
  { value: 'update', label: '更新（update）' },
  { value: 'delete', label: '删除（delete）' }
]

/**
 * 权限空间列表：加载、保存后本地合并与名称查找，权限空间页与数据资源页共用。
 */
export function useSpaceList() {
  const spaces = ref<PermissionSpace[]>([])
  const spacesLoading = ref(false)
  let spaceLoadVersion = 0

  const loadSpaces = async (search?: string) => {
    const version = ++spaceLoadVersion
    spacesLoading.value = true
    try {
      const rows = await permissionApi.getSpaces(search)
      if (version === spaceLoadVersion) {
        spaces.value = rows
      }
    } catch (error: unknown) {
      console.error('加载权限空间失败:', error)
      if (version === spaceLoadVersion) {
        message.error('加载权限空间失败，请重试')
      }
    } finally {
      if (version === spaceLoadVersion) {
        spacesLoading.value = false
      }
    }
  }

  /** 保存权限空间后先本地合并，避免下拉出现空窗，再重新拉取。 */
  const mergeSpace = async (saved: PermissionSpace) => {
    spaceLoadVersion += 1
    spacesLoading.value = false
    spaces.value = [...spaces.value.filter((space) => space.id !== saved.id), saved]
    await loadSpaces()
  }

  return { spaces, spacesLoading, loadSpaces, mergeSpace }
}

/**
 * 权限空间：下拉选项与名称查找。
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
