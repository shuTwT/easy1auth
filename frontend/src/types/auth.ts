export interface LoginRequest {
  username?: string
  password?: string
  email?: string
  code?: string
  loginType: 'password' | 'email' | 'passkey'
}

export interface LoginResponse {
  token: string
  refreshToken: string
  user: {
    id: string
    username: string
    email: string
    avatar?: string
  }
}

export interface SendCodeRequest {
  email: string
  type: 'login' | 'register'
}

export interface SendCodeResponse {
  status: string
  message: string
  code?: string
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
  }
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
