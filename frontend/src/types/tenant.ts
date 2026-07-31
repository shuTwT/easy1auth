export interface TenantPackageSummary {
  id: number
  code: string
  name: string
  status: 'active' | 'inactive'
  defaultPackage: boolean
  maxUsers: number
  maxApps: number
  permissionCodes: string[]
}

export interface Tenant {
  id: string
  name: string
  status: TenantStatus
  system: boolean
  tenantPackage: TenantPackageSummary | null
  role: string
}

export interface TenantControl {
  id: string
  name: string
  status: TenantStatus
  tenantPackage: TenantPackageSummary
  administratorAccountId: string | null
}

export type TenantStatus = 'active' | 'suspended' | 'deleted'

export interface CreateTenantDto {
  name: string
  packageId?: number
  administratorAccountId?: string
}

export type UpdateTenantDto = Partial<CreateTenantDto>

export interface TenantQueryDto {
  page?: number
  pageSize?: number
  name?: string
  status?: TenantStatus
}

export interface TenantListResponse {
  items: Tenant[]
  total: number
  page: number
  pageSize: number
}

export interface TenantControlListResponse {
  items: TenantControl[]
  total: number
  page: number
  pageSize: number
}

export interface TenantUpdateDto {
  name?: string
  packageId?: number
}

export interface TenantStats {
  totalUsers: number
  totalApps: number
  activeUsers: number
  storageUsed: number
}
