import type { SyncIdentitySource } from '@/types/syncIdentitySource'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export type SourceStatus = 'active' | 'disabled'

export interface SourceEditForm {
  name: string
  appId: string
  appSecret: string
  verificationToken: string
  encryptKey: string
  status: SourceStatus
}

export interface SourceEditDrawerExpose {
  open: (row?: SyncIdentitySource) => void
}

export interface TaskDrawerExpose {
  open: (row: SyncIdentitySource) => void
}
