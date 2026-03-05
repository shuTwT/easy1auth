export type SocialProviderType = 'wechat' | 'qq' | 'feishu' | 'github' | 'gitee' | 'dingtalk' | 'wechat_work' | 'custom'

export interface SocialIdentityProvider {
  id: string
  tenantId: string
  name: string
  type: SocialProviderType
  clientId: string
  clientSecret: string
  authorizationEndpoint: string
  tokenEndpoint: string
  userInfoEndpoint: string
  scope: string[]
  attributeMapping: Record<string, string> | null
  status: 'active' | 'inactive'
  createdAt: string
  updatedAt: string
}

export interface CreateSocialIdentityProviderDto {
  name: string
  type: SocialProviderType
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
  redirectUri: string
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

export const PROVIDER_CONFIGS: Record<SocialProviderType, { name: string; icon: string; color: string }> = {
  wechat: { name: '微信', icon: 'ChatDotRound', color: '#07C160' },
  qq: { name: 'QQ', icon: 'ChatRound', color: '#12B7F5' },
  feishu: { name: '飞书', icon: 'Connection', color: '#00D6B9' },
  github: { name: 'GitHub', icon: 'Platform', color: '#24292E' },
  gitee: { name: 'Gitee', icon: 'Platform', color: '#C71D23' },
  dingtalk: { name: '钉钉', icon: 'ChatDotRound', color: '#0089FF' },
  wechat_work: { name: '企业微信', icon: 'ChatDotRound', color: '#2B7EFF' },
  custom: { name: '自定义', icon: 'Setting', color: '#909399' },
}
