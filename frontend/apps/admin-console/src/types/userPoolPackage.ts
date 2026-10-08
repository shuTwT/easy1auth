export type UserPoolPackageStatus = 'active' | 'inactive'

export interface UserPoolPackage {
  id: number
  code: string
  name: string
  status: UserPoolPackageStatus
  defaultPackage: boolean
  maxUsers: number
  maxApps: number
  permissionCodes: string[]
  createdAt: string | null
  updatedAt: string | null
}

export interface UserPoolPackageMutation {
  code: string
  name: string
  maxUsers: number
  maxApps: number
  permissionCodes: string[]
}

export interface ManagementPermission {
  code: string
  type: string
  scope: string
  name: string
  parentCode: string | null
  resource: string
  action: string
  sortOrder: number
  active: boolean
}
