export type ApplicationStatus = 'active' | 'disabled'
export type ApplicationType = 'web' | 'native' | 'spa' | 'machine'

export interface Application {
  id: string
  tenantId: string
  name: string
  logo: string | null
  description: string | null
  type: ApplicationType
  clientId: string
  /** Only present in create/regenerate responses. */
  clientSecret?: string | null
  redirectUris: string[]
  postLogoutRedirectUris?: string[]
  allowedGrantTypes: string[]
  scopes?: string[]
  requirePkce?: boolean
  requireConsent?: boolean
  accessTokenLifetime: number
  refreshTokenLifetime: number
  status: ApplicationStatus
  createdAt: string
  updatedAt: string
}

export interface CreateApplicationDto {
  name: string
  logo?: string
  description?: string
  type?: ApplicationType
  redirectUris?: string[]
  allowedGrantTypes?: string[]
  accessTokenLifetime?: number
  refreshTokenLifetime?: number
}

export interface UpdateApplicationDto {
  name?: string
  logo?: string
  description?: string
  type?: ApplicationType
  redirectUris?: string[]
  allowedGrantTypes?: string[]
  accessTokenLifetime?: number
  refreshTokenLifetime?: number
  status?: ApplicationStatus
}

export interface ApplicationQueryDto {
  page?: number
  pageSize?: number
  name?: string
  type?: ApplicationType
  status?: ApplicationStatus
}

export interface ApplicationListResponse {
  items: Application[]
  total: number
  page: number
  pageSize: number
}

export interface ApplicationStats {
  totalApplications: number
  activeApplications: number
  disabledApplications: number
}

export interface RegenerateSecretResponse {
  clientSecret: string
}
