export interface Tenant {
  id: string
  name: string
  logo?: string
  domain?: string
  status: TenantStatus
  plan: TenantPlan
  maxUsers: number
  maxApps: number
  createdAt: Date
  updatedAt: Date
}

export type TenantStatus = 'active' | 'suspended' | 'deleted'
export type TenantPlan = 'basic' | 'professional' | 'enterprise'

export interface CreateTenantDto {
  name: string
  logo?: string
  domain?: string
  plan?: TenantPlan
  maxUsers?: number
  maxApps?: number
}

export interface UpdateTenantDto {
  name?: string
  logo?: string
  domain?: string
  plan?: TenantPlan
  maxUsers?: number
  maxApps?: number
  status?: TenantStatus
}

export interface TenantQueryDto {
  page?: number
  pageSize?: number
  name?: string
  status?: TenantStatus
  plan?: TenantPlan
  domain?: string
}

export interface TenantListResponse {
  tenants: Tenant[]
  total: number
  page: number
  pageSize: number
}

export interface TenantStats {
  totalUsers: number
  totalApps: number
  activeUsers: number
  storageUsed: number
}
