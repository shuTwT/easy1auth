export interface CreateRoleDto {
  name: string
  code: string
  description?: string
  type?: string
  permissions?: Record<string, any>
  dataScope?: string
  parentId?: string
}

export interface UpdateRoleDto {
  name?: string
  description?: string
  permissions?: Record<string, any>
  dataScope?: string
  parentId?: string
}

export interface RoleResponse {
  id: string
  tenantId: string
  name: string
  code: string
  description: string | null
  type: string
  permissions: Record<string, any>
  dataScope: string
  parentId: string | null
  createdAt: Date
  updatedAt: Date
  userCount?: number
  parent?: { id: string; name: string; code: string } | null
  children?: RoleResponse[]
}

export interface RoleTreeResponse {
  id: string
  name: string
  code: string
  description: string | null
  type: string
  userCount?: number
  children?: RoleTreeResponse[]
}

export interface RoleStatsResponse {
  totalRoles: number
  systemRoles: number
  customRoles: number
  totalUsers: number
}

export interface AssignRoleDto {
  userIds: string[]
}

export interface RemoveRoleDto {
  userIds: string[]
}

export interface RoleUsersResponse {
  users: Array<{
    id: string
    username: string
    email: string
    name: string
    status: string
    department: string | null
    position: string | null
  }>
  total: number
}
