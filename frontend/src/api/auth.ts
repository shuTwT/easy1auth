import request from '@/utils/request'
import type {
  LoginRequest,
  LoginResponse,
  SendCodeRequest,
  SendCodeResponse,
  RegisterRequest,
  RegisterResponse,
  PasskeyLoginStartResponse,
  PasskeyLoginFinishRequest,
  SocialLoginRequest,
  SocialLoginResponse
} from '@/types/auth'

export const authApi = {
  login(data: LoginRequest): Promise<LoginResponse> {
    return request.post('/auth/login', data)
  },

  sendCode(data: SendCodeRequest): Promise<SendCodeResponse> {
    return request.post('/auth/send-code', data)
  },

  register(data: RegisterRequest): Promise<RegisterResponse> {
    return request.post('/auth/register', data)
  },

  passkeyLoginStart(): Promise<PasskeyLoginStartResponse> {
    return request.post('/auth/passkey/start')
  },

  passkeyLoginFinish(data: PasskeyLoginFinishRequest): Promise<LoginResponse> {
    return request.post('/auth/passkey/finish', data)
  },

  socialLogin(data: SocialLoginRequest): Promise<SocialLoginResponse> {
    return request.post('/auth/social', data)
  },

  getSocialLoginUrl(provider: string): Promise<{ url: string }> {
    return request.get(`/auth/social/${provider}/url`)
  },

  refreshToken(refreshToken: string): Promise<{ token: string }> {
    return request.post('/auth/refresh', { refreshToken })
  },

  logout(): Promise<void> {
    return request.post('/auth/logout')
  }
}
