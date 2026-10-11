import type { EnterpriseIdentitySource, EnterpriseSourceType } from '@/types/enterpriseIdentitySource'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export type SourceStatus = 'active' | 'disabled'

export interface SourceEditForm {
  name: string
  type: EnterpriseSourceType
  mode: string | null
  clientId: string
  clientSecret: string
}

export interface SourceEditModalExpose {
  open: (row?: EnterpriseIdentitySource) => void
}
