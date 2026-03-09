import request from '@/utils/request'

export interface PasswordPolicy {
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireNumbers: boolean
  requireSpecialChars: boolean
  specialChars: string
  maxAge: number
  historyCount: number
  preventReuse: boolean
}

export interface PasswordExpiryStatus {
  expired: boolean
  daysUntilExpiry: number
}

export interface MfaStatus {
  enabled: boolean
  type: string | null
}

export interface MfaSetupResult {
  secret: string
  qrCodeUrl: string
  backupCodes: string[]
}

export const securityApi = {
  getPasswordPolicy(): Promise<{ status: string; policy: PasswordPolicy; expiryStatus: PasswordExpiryStatus }> {
    return request.get('/security/password-policy')
  },

  changePassword(data: {
    currentPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<{ status: string; message: string }> {
    return request.post('/security/change-password', data)
  },

  getMfaStatus(): Promise<{ status: string; mfa: MfaStatus }> {
    return request.get('/security/mfa/status')
  },

  setupMfa(): Promise<{ status: string; data: MfaSetupResult }> {
    return request.post('/security/mfa/setup')
  },

  enableMfa(token: string): Promise<{ status: string; message: string }> {
    return request.post('/security/mfa/enable', { token })
  },

  disableMfa(token: string): Promise<{ status: string; message: string }> {
    return request.post('/security/mfa/disable', { token })
  },

  verifyMfa(token: string, type?: string): Promise<{ status: string; message: string }> {
    return request.post('/security/mfa/verify', { token, type })
  },

  sendMfaEmailCode(): Promise<{ status: string; message: string }> {
    return request.post('/security/mfa/send-email-code')
  }
}
