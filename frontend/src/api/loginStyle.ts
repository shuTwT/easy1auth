import request from '@/utils/request'

export interface LoginStyle {
  id: string
  logo?: string
  logoDark?: string
  backgroundImage?: string
  backgroundColor: string
  primaryColor: string
  title: string
  subtitle: string
  customCSS?: string
  loginMethods?: string[]
  socialProviders?: string[]
  createdAt: string
  updatedAt: string
}

export interface UpdateLoginStyleDto {
  logo?: string
  logoDark?: string
  backgroundImage?: string
  backgroundColor?: string
  primaryColor?: string
  title?: string
  subtitle?: string
  customCSS?: string
  loginMethods?: string[]
  socialProviders?: string[]
}

export const loginStyleApi = {
  get: () => request.get<{ data: LoginStyle }>('/login-style'),
  
  update: (data: UpdateLoginStyleDto) => 
    request.put<{ data: LoginStyle }>('/login-style', data),
  
  getPublic: (domain?: string) => 
    request.get<{ data: LoginStyle }>('/login-style/public', { params: { domain } }),
}
