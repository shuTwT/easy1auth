import request from '@/utils/request'
import type { LoginStyleConfig, LoginStyleLegalDocuments } from '@easy1auth/types'

export type { LoginStyleLegalDocuments } from '@easy1auth/types'

export interface LoginStyleDraftResponse {
  config: LoginStyleConfig
  socialProviderIds: string[]
  legalDocuments: LoginStyleLegalDocuments
  draftUpdatedAt: string
  publishedAt?: string | null
}

export const loginStyleApi = {
  getDraft: (): Promise<LoginStyleDraftResponse> => request.get('/login-style/draft'),

  saveDraft: (data: { config: LoginStyleConfig; socialProviderIds: string[]; legalDocuments: LoginStyleLegalDocuments }): Promise<LoginStyleDraftResponse> =>
    request.put('/login-style/draft', data),

  publish: (): Promise<LoginStyleDraftResponse> => request.post('/login-style/publish'),

  reset: (): Promise<LoginStyleDraftResponse> => request.post('/login-style/reset'),
}
