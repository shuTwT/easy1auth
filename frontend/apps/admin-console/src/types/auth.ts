export interface LoginRequest {
  username?: string
  password?: string
  email?: string
  code?: string
  challengeToken?: string
  loginType: 'password' | 'email' | 'passkey'
}

export interface LoginResponse {
  status?: 'success' | 'mfa_required'
  token?: string
  refreshToken?: string
  challengeToken?: string
  methods?: Array<'totp' | 'email'>
  expiresIn?: number
  user?: {
    id: string
    username: string
    email: string
    avatar?: string
    currentTenantId?: string
  }
  tenants?: TenantInfo[]
}

export interface SendCodeRequest {
  email: string
  type: 'login' | 'register'
}

export interface SendCodeResponse {
  code?: string
  challengeToken?: string
}

export interface RegisterRequest {
  email: string
  password: string
  code: string
  username?: string
}

export interface RegisterResponse {
  token: string
  refreshToken: string
  user: {
    id: string
    username: string
    email: string
    avatar?: string
    currentTenantId?: string
  }
  tenants?: TenantInfo[]
}

export interface TenantInfo {
  id: string
  name: string
  role: string
  logo?: string
  status?: string
  plan?: string
}

export interface PasskeyLoginStartResponse {
  challenge: string
  rpId: string
  allowCredentials: Array<{
    id: string
    type: string
    transports?: string[]
  }>
}

export interface PasskeyLoginFinishRequest {
  credential: {
    id: string
    rawId: string
    response: {
      clientDataJSON: string
      authenticatorData: string
      signature: string
      userHandle?: string
    }
    type: string
  }
}

export interface SocialLoginRequest {
  provider: string
  code: string
  state?: string
}

export interface SocialLoginResponse {
  token: string
  refreshToken: string
  user: {
    id: string
    username: string
    email: string
    avatar?: string
  }
  isNewUser: boolean
}
