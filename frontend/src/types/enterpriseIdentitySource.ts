export interface EnterpriseIdentitySource {
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

export interface EnterpriseIdentityTask {
  id: string
  type: 'full' | 'event'
  status: 'pending' | 'processing' | 'succeeded' | 'partial' | 'failed'
  summary: Record<string, number>
  lastError: string | null
  createdAt: string
  finishedAt: string | null
}

export interface EnterpriseIdentitySourceInput {
  name?: string
  appId?: string
  appSecret?: string
  verificationToken?: string
  encryptKey?: string
  status?: 'active' | 'disabled'
}
