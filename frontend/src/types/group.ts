export interface UserGroup {
  id: string
  tenantId: string
  name: string
  description?: string
  type: GroupType
  parentId?: string
  createdAt: string
  updatedAt: string
  parent?: UserGroup | null
  _count?: {
    members: number
    admins: number
    children: number
  }
}

export type GroupType = 'team' | 'department' | 'project' | 'organization'

export interface CreateGroupDto {
  name: string
  description?: string
  type?: GroupType
  parentId?: string
}

export interface UpdateGroupDto {
  name?: string
  description?: string
  type?: GroupType
  parentId?: string
}

export interface GroupQueryDto {
  page?: number
  pageSize?: number
  name?: string
  type?: GroupType
  parentId?: string
}

export interface GroupListResponse {
  groups: UserGroup[]
  total: number
  page: number
  pageSize: number
}

export interface GroupTreeResponse {
  id: string
  name: string
  description?: string | null
  type: string
  parentId?: string | null
  children: GroupTreeResponse[]
  memberCount: number
  adminCount: number
}

export interface AddMembersDto {
  userIds: string[]
}

export interface RemoveMembersDto {
  userIds: string[]
}

export interface AddAdminsDto {
  userIds: string[]
}

export interface RemoveAdminsDto {
  userIds: string[]
}

export interface GroupMembersResponse {
  members: User[]
  admins: User[]
  total: number
}

export interface GroupStats {
  totalGroups: number
  teamGroups: number
  departmentGroups: number
  projectGroups: number
  organizationGroups: number
  rootGroups: number
}

export interface User {
  id: string
  tenantId: string
  username: string
  email: string
  phone?: string
  name: string
  avatar?: string
  status: string
  emailVerified: boolean
  phoneVerified: boolean
  department?: string
  position?: string
  customAttributes?: Record<string, any>
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}
