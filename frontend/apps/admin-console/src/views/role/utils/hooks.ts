import { ref } from 'vue'
import axios from 'axios'
import { message } from 'antdv-next'
import { permissionApi } from '@/api/permission'
import type { PermissionTree } from '@/types/permission'
import type { SelectOption } from './types'

const roleTypeOptions: SelectOption[] = [
  { value: 'system', label: '内置角色' },
  { value: 'custom', label: '自定义角色' }
]

const dataScopeOptions: SelectOption[] = [
  { value: 'self', label: '仅本人数据' },
  { value: 'department', label: '本部门数据' },
  { value: 'department_and_sub', label: '本部门及下级部门数据' },
  { value: 'all', label: '全部数据' }
]

const dataScopeLabelMap: Record<string, string> = {
  self: '仅本人',
  department: '本部门',
  department_and_sub: '本部门及下级',
  all: '全部'
}

/**
 * 角色字典：类型 / 数据范围的选项与文本映射，列表页与弹窗共用。
 */
export function useRoleDict() {
  const getDataScopeLabel = (scope: string) => {
    return dataScopeLabelMap[scope] ?? scope
  }

  const getTypeText = (type: string) => {
    return type === 'system' ? '系统' : '自定义'
  }

  const getTypeVariant = (type: string) => {
    return type === 'system' ? 'purple' : 'blue'
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString('zh-CN')
  }

  return {
    roleTypeOptions,
    dataScopeOptions,
    getDataScopeLabel,
    getTypeText,
    getTypeVariant,
    formatDate
  }
}

/**
 * 权限树：加载权限树，并在权限编码列表与后端 JSON 结构之间转换。
 */
export function usePermissionTree() {
  const permissionTree = ref<PermissionTree[]>([])

  const loadPermissionTree = async () => {
    try {
      permissionTree.value = await permissionApi.getTree()
    } catch (error: unknown) {
      console.error('加载权限树失败:', error)
      message.error('加载权限树失败')
    }
  }

  const flattenPermissionTree = (nodes: PermissionTree[]): PermissionTree[] => {
    return nodes.reduce((acc: PermissionTree[], node: PermissionTree) => {
      acc.push(node)
      if (node.children?.length) {
        acc.push(...flattenPermissionTree(node.children))
      }
      return acc
    }, [])
  }

  const getPermissionCodesFromJson = (permissions: Record<string, boolean>) => {
    const allCodes = flattenPermissionTree(permissionTree.value).map((item) => item.code)
    return allCodes.filter((code) => permissions[code] === true)
  }

  const buildPermissionsFromCodes = (codes: string[]) => {
    const result: Record<string, boolean> = {}
    codes.forEach((code) => {
      result[code] = true
    })
    return result
  }

  return {
    permissionTree,
    loadPermissionTree,
    getPermissionCodesFromJson,
    buildPermissionsFromCodes
  }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data?.msg || fallback
  }
  return fallback
}
