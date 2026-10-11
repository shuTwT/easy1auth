import type { SocialIdentitySource, SocialSourceType } from '@/types/socialIdentitySource'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export type SourceStatus = 'active' | 'disabled'

export interface SourceEditForm {
  name: string
  type: SocialSourceType
  mode: string | null
  clientId: string
  clientSecret: string
}

export interface SourceEditModalExpose {
  open: (row?: SocialIdentitySource) => void
}
