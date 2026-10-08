export interface UserGroup {
  id: string
  userPoolId: string
  name: string
  description?: string
  type: GroupType
  createdAt: string
  updatedAt: string
  _count?: {
    members: number
  }
}

export type GroupType = 'team' | 'department' | 'project' | 'organization'

export interface CreateGroupDto {
  name: string
  description?: string
  type?: GroupType
}

export interface UpdateGroupDto {
  name?: string
  description?: string
  type?: GroupType
}

export interface GroupQueryDto {
  page?: number
  pageSize?: number
  name?: string
  type?: GroupType
}

export interface GroupListResponse {
  items: UserGroup[]
  total: number
  page: number
  pageSize: number
}

export interface AddMembersDto {
  userIds: string[]
}

export interface RemoveMembersDto {
  userIds: string[]
}

export interface GroupMembersResponse {
  members: User[]
  total: number
}

export interface GroupStats {
  totalGroups: number
  teamGroups: number
  departmentGroups: number
  projectGroups: number
  organizationGroups: number
}

export interface User {
  id: string
  userPoolId: string
  username: string
  email: string | null
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
