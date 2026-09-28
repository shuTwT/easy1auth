export interface SyncIdentitySource {
  id: string
  name: string
  provider: 'feishu'
  appId: string
  status: 'active' | 'disabled'
  lastSyncAt: string | null
  lastSyncStatus: 'succeeded' | 'partial' | 'failed' | null
  lastError: string | null
  createdAt: string
  updatedAt: string
}

export interface SyncIdentityTask {
  id: string
  type: 'full' | 'event'
  status: 'pending' | 'processing' | 'succeeded' | 'partial' | 'failed'
  summary: Record<string, number>
  lastError: string | null
  createdAt: string
  finishedAt: string | null
}

export interface SyncIdentitySourceInput {
  name?: string
  appId?: string
  appSecret?: string
  verificationToken?: string
  encryptKey?: string
  status?: 'active' | 'disabled'
}

export interface SyncIdentitySourceStats {
  totalSources: number
  activeSources: number
  inactiveSources: number
}

export interface SyncIdentitySourceQuery {
  page?: number
  pageSize?: number
  search?: string
  status?: 'active' | 'disabled'
}

export interface SyncIdentitySourceListResponse {
  items: SyncIdentitySource[]
  total: number
  page: number
  pageSize: number
}
