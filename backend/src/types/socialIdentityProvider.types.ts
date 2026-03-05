export interface CreateSocialIdentityProviderDto {
  name: string
  type: 'wechat' | 'qq' | 'feishu' | 'github' | 'gitee' | 'dingtalk' | 'wechat_work' | 'custom'
  clientId: string
  clientSecret: string
  authorizationEndpoint?: string
  tokenEndpoint?: string
  userInfoEndpoint?: string
  scope?: string[]
  attributeMapping?: Record<string, string>
}

export interface UpdateSocialIdentityProviderDto {
  name?: string
  clientId?: string
  clientSecret?: string
  authorizationEndpoint?: string
  tokenEndpoint?: string
  userInfoEndpoint?: string
  scope?: string[]
  attributeMapping?: Record<string, string>
  status?: 'active' | 'inactive'
}

export interface SocialIdentityProviderResponse {
  id: string
  tenantId: string
  name: string
  type: string
  clientId: string
  clientSecret: string
  authorizationEndpoint: string
  tokenEndpoint: string
  userInfoEndpoint: string
  scope: string[]
  attributeMapping: Record<string, string> | null
  status: string
  createdAt: Date
  updatedAt: Date
}

export interface SocialIdentityProviderListResponse {
  providers: SocialIdentityProviderResponse[]
  total: number
}

export interface SocialIdentityProviderStats {
  totalProviders: number
  activeProviders: number
  inactiveProviders: number
  byType: Record<string, number>
}

export interface OAuthAuthorizeUrlResponse {
  authorizeUrl: string
  state: string
}

export interface OAuthCallbackDto {
  code: string
  state: string
}

export interface SocialLoginResponse {
  token: string
  user: {
    id: string
    username: string
    email: string
    name: string
    avatar: string | null
  }
  isNewUser: boolean
}

export interface SocialAccountResponse {
  id: string
  userId: string
  provider: string
  providerId: string
  createdAt: Date
  updatedAt: Date
  profile: Record<string, any> | null
}
