import request from '@/utils/request'

export interface AdminProfile {
  id: string
  username: string
  email: string
  phone: string | null
  status: 'active' | 'disabled'
  securityVersion: number
  lastTenantId: string | null
  mfaEnabled: boolean
  mfaType: string | null
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UpdateAdminProfile {
  username: string
  phone: string
}

export interface EmailChallengeResponse {
  challengeToken: string
  expiresIn: number
}

export const profileApi = {
  get(): Promise<AdminProfile> {
    return request.get('/security/profile')
  },

  update(input: UpdateAdminProfile): Promise<AdminProfile> {
    return request.put('/security/profile', input)
  },

  sendEmailChangeCode(email: string): Promise<EmailChallengeResponse> {
    return request.post('/security/email/send-code', { email })
  },

  verifyEmailChange(challengeToken: string, code: string): Promise<{ email: string }> {
    return request.post('/security/email/verify', { challengeToken, code })
  },
}
