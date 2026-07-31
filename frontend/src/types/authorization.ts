import type { TenantPackage } from '@/types/tenantPackage'

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
  tenantId: string
  membershipRole: string
  permissions: string[]
  tenantPackage: TenantPackage | null
  menus: ManagementMenu[]
}

export interface MenuCatalogResponse {
  menus: ManagementMenu[]
  permissions: ManagementMenu[]
}
