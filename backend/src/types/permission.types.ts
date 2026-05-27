import { Permission } from '@prisma/client'

export type PermissionType = 'menu' | 'operation' | 'data'

export interface CreatePermissionDto {
  name: string
  code: string
  description?: string
  type?: PermissionType
  parentId?: string
  resource: string
  action: string
}

export interface UpdatePermissionDto {
  name?: string
  description?: string
  type?: PermissionType
  parentId?: string
  resource?: string
  action?: string
}

export interface PermissionQueryDto {
  page?: number
  pageSize?: number
  search?: string
  type?: PermissionType
  resource?: string
}

export interface PermissionListResponse {
  permissions: Permission[]
  total: number
  page: number
  pageSize: number
}

export interface PermissionTree extends Pick<Permission, 'id' | 'code' | 'name' | 'description' | 'type' | 'resource' | 'action'> {
  children: PermissionTree[]
}

export interface PermissionStats {
  totalPermissions: number
  menuPermissions: number
  operationPermissions: number
  dataPermissions: number
}
