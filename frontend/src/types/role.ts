export interface Role {
  id: string
  tenantId: string
  name: string
  code: string
  description: string | null
  type: string
  permissions: Record<string, any>
  dataScope: string
  parentId: string | null
  createdAt: string
  updatedAt: string
  userCount?: number
  parent?: { id: string; name: string; code: string } | null
  children?: Role[]
}

export interface RoleTree {
  id: string
  name: string
  code: string
  description: string | null
  type: string
  userCount?: number
  children?: RoleTree[]
}

export interface RoleStats {
  totalRoles: number
  systemRoles: number
  customRoles: number
  totalUsers: number
}

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

export interface AssignRoleDto {
  userIds: string[]
}

export interface RemoveRoleDto {
  userIds: string[]
}

export interface RoleUser {
  id: string
  username: string
  email: string
  name: string
  status: string
  department: string | null
  position: string | null
}

export interface RoleUsersResponse {
  users: RoleUser[]
  total: number
}
