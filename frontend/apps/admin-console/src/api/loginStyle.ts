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
  get: ():Promise<LoginStyle> => request.get('/login-style'),
  
  update: (data: UpdateLoginStyleDto):Promise<LoginStyle> => 
    request.put('/login-style', data),
  
  getPublic: (domain?: string):Promise<LoginStyle> => 
    request.get('/login-style/public', { params: { domain } }),
}
