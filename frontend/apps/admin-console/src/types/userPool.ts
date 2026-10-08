export interface UserPoolPackageSummary {
  id: number
  code: string
  name: string
  status: 'active' | 'inactive'
  defaultPackage: boolean
  maxUsers: number
  maxApps: number
  permissionCodes: string[]
}

export interface UserPool {
  id: string
  name: string
  status: UserPoolStatus
  system: boolean
  userPoolPackage: UserPoolPackageSummary | null
  role: string
}

export interface UserPoolControl {
  id: string
  name: string
  status: UserPoolStatus
  userPoolPackage: UserPoolPackageSummary
  administratorAccountId: string | null
}

export type UserPoolStatus = 'active' | 'suspended' | 'deleted'

export interface CreateUserPoolDto {
  name: string
  packageId?: number
  administratorAccountId?: string
}

export type UpdateUserPoolDto = Partial<CreateUserPoolDto>

export interface UserPoolQueryDto {
  page?: number
  pageSize?: number
  name?: string
  status?: UserPoolStatus
}

export interface UserPoolListResponse {
  items: UserPool[]
  total: number
  page: number
  pageSize: number
}

export interface UserPoolControlListResponse {
  items: UserPoolControl[]
  total: number
  page: number
  pageSize: number
}

export interface UserPoolUpdateDto {
  name?: string
  packageId?: number
}

export interface UserPoolStats {
  totalUsers: number
  totalApps: number
  activeUsers: number
  storageUsed: number
}
