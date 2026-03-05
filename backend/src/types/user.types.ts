import { User, Prisma } from '@prisma/client'

export type UserStatus = 'active' | 'disabled' | 'locked'

export interface CreateUserDto {
  username: string
  email: string
  password?: string
  phone?: string
  name: string
  avatar?: string
  department?: string
  position?: string
  customAttributes?: Record<string, any>
}

export interface UpdateUserDto {
  username?: string
  email?: string
  phone?: string
  name?: string
  avatar?: string
  department?: string
  position?: string
  status?: UserStatus
  customAttributes?: Record<string, any>
}

export interface UserQueryDto {
  page?: number
  pageSize?: number
  username?: string
  email?: string
  phone?: string
  name?: string
  status?: UserStatus
  department?: string
}

export interface UserListResponse {
  users: User[]
  total: number
  page: number
  pageSize: number
}

export interface UserStats {
  totalUsers: number
  activeUsers: number
  disabledUsers: number
  lockedUsers: number
}

export interface ChangePasswordDto {
  oldPassword: string
  newPassword: string
}

export interface ResetPasswordDto {
  newPassword: string
}

export interface AssignRolesDto {
  roleIds: string[]
}

export interface AssignGroupsDto {
  groupIds: string[]
}

export interface UserImportDto {
  username: string
  email: string
  password?: string
  phone?: string
  name: string
  department?: string
  position?: string
}

export interface UserImportResult {
  success: number
  failed: number
  total: number
  errors: Array<{
    row: number
    username?: string
    error: string
  }>
  importedUsers: Array<{
    username: string
    email: string
    name: string
  }>
}
