import { Application, Prisma } from '@prisma/client'

export type ApplicationStatus = 'active' | 'disabled'
export type ApplicationType = 'web' | 'native' | 'spa' | 'machine'

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
  applications: Application[]
  total: number
  page: number
  pageSize: number
}

export interface ApplicationStats {
  totalApplications: number
  activeApplications: number
  disabledApplications: number
}

export interface RegenerateSecretDto {
  clientSecret: string
}

export interface UpdateRedirectUrisDto {
  redirectUris: string[]
}

export interface UpdateGrantTypesDto {
  allowedGrantTypes: string[]
}
