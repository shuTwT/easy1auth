import type { UserPoolPackage } from '@/types/userPoolPackage'

export interface ManagementMenu {
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

export interface AuthorizationContext {
  userPoolId: string
  membershipRole: string
  permissions: string[]
  userPoolPackage: UserPoolPackage | null
  menus: ManagementMenu[]
}

export interface MenuCatalogResponse {
  menus: ManagementMenu[]
  permissions: ManagementMenu[]
}
