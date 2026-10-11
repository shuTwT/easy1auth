import type { Application, ApplicationStatus, ApplicationType } from '@/types/application'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export type EditDialogMode = 'create' | 'edit'

export interface ApplicationEditForm {
  id: string | undefined
  name: string | undefined
  type: ApplicationType | undefined
  description: string | undefined
  logo: string | undefined
  redirectUris: string[]
  allowedGrantTypes: string[]
  accessTokenLifetime: number | undefined
  refreshTokenLifetime: number | undefined
}

export interface ConfigForm {
  name: string
  logo: string
  description: string
  type: ApplicationType
  status: ApplicationStatus
}

export interface LoginForm {
  redirectUris: string[]
  allowedGrantTypes: string[]
  accessTokenLifetime: number
  refreshTokenLifetime: number
}

export interface ApiErrorResponse {
  msg?: string
}

export interface ApplicationEditDialogExpose {
  open: (mode: EditDialogMode, row?: Partial<Application>) => void
}

export interface SecretDialogExpose {
  open: (secret: string) => void
}
