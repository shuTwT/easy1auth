export interface Permission {
  id: string
  tenantId: string
  code: string
  name: string
  description: string | null
  type: 'menu' | 'operation' | 'data'
  resource: string
  action: string
  parentId: string | null
  createdAt: string
  updatedAt: string
  parent?: { id: string; name: string; code: string } | null
}

export interface PermissionTree {
  id: string
  code: string
  name: string
  description: string | null
  type: string
  resource: string
  action: string
  children: PermissionTree[]
}

export interface PermissionStats {
  totalPermissions: number
  menuPermissions: number
  operationPermissions: number
  dataPermissions: number
}

export interface CreatePermissionDto {
  name: string
  code: string
  description?: string
  type?: 'menu' | 'operation' | 'data'
  parentId?: string
  resource: string
  action: string
}

export interface UpdatePermissionDto {
  name?: string
  description?: string
  type?: 'menu' | 'operation' | 'data'
  parentId?: string
  resource?: string
  action?: string
}

export interface PermissionQueryDto {
  page?: number
  pageSize?: number
  search?: string
  type?: string
  resource?: string
}

export interface PermissionListResponse {
  permissions: Permission[]
  total: number
  page: number
  pageSize: number
}
