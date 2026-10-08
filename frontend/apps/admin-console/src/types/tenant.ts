export interface Tenant {
  id: string
  userPoolId: string
  name: string
  code: string
  description: string | null
  status: 'active' | 'inactive'
  createdAt: string
  updatedAt: string
}

export interface TenantInput {
  name: string
  code: string
  description?: string
  status: Tenant['status']
}

export interface TenantQuery {
  page: number
  pageSize: number
  name?: string
  status?: Tenant['status']
}

export interface TenantList {
  items: Tenant[]
  total: number
  page: number
  pageSize: number
}
