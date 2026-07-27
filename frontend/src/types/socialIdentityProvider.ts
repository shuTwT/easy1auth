export const SOCIAL_PROVIDER_TYPES = [
  'wechat_qr',
  'wechat_mini_program_qr',
  'wechat_official_account',
  'wechat_mini_program',
  'github',
  'gitee',
  'feishu',
] as const

export type SocialProviderType = (typeof SOCIAL_PROVIDER_TYPES)[number]

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
  scope?: string[]
  attributeMapping?: Record<string, string>
}

export interface UpdateSocialIdentityProviderDto {
  name?: string
  clientId?: string
  clientSecret?: string
  scope?: string[]
  attributeMapping?: Record<string, string>
  status?: 'active' | 'inactive'
}

export interface SocialIdentityProviderListResponse {
  providers: SocialIdentityProvider[]
  total: number
  page: number
  pageSize: number
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

type ProviderConfig = {
  readonly name: string
  readonly color: string
  readonly defaultScopes: readonly string[]
  readonly availableScopes: readonly string[]
}

export const PROVIDER_CONFIGS = {
  wechat_qr: {
    name: '微信扫码',
    color: '#07C160',
    defaultScopes: ['snsapi_login'],
    availableScopes: ['snsapi_login'],
  },
  wechat_mini_program_qr: {
    name: '小程序扫码',
    color: '#07C160',
    defaultScopes: ['snsapi_login'],
    availableScopes: ['snsapi_login'],
  },
  wechat_official_account: {
    name: '公众号网页授权',
    color: '#07C160',
    defaultScopes: ['snsapi_userinfo'],
    availableScopes: ['snsapi_base', 'snsapi_userinfo'],
  },
  wechat_mini_program: {
    name: '微信小程序',
    color: '#07C160',
    defaultScopes: [],
    availableScopes: [],
  },
  github: {
    name: 'GitHub',
    color: '#24292E',
    defaultScopes: ['user:email'],
    availableScopes: ['user', 'user:email', 'repo', 'read:org'],
  },
  gitee: {
    name: 'Gitee',
    color: '#C71D23',
    defaultScopes: ['user_info', 'emails'],
    availableScopes: ['user_info', 'emails', 'projects', 'pull_requests', 'issues'],
  },
  feishu: {
    name: '飞书网页授权',
    color: '#3370FF',
    defaultScopes: ['contact:user.base:readonly'],
    availableScopes: ['contact:user.base:readonly', 'contact:user.email:readonly'],
  },
} as const satisfies Record<SocialProviderType, ProviderConfig>

export const isSocialProviderType = (value: unknown): value is SocialProviderType =>
  typeof value === 'string' && SOCIAL_PROVIDER_TYPES.some((type) => type === value)
