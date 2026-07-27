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

type OAuthProviderConfig = {
  readonly authorizationEndpoint: string
  readonly tokenEndpoint: string
  readonly userInfoEndpoint: string
  readonly scope: readonly string[]
}

export const SOCIAL_PROVIDER_CONFIGS = {
  wechat_qr: {
    authorizationEndpoint: 'https://open.weixin.qq.com/connect/qrconnect',
    tokenEndpoint: 'https://api.weixin.qq.com/sns/oauth2/access_token',
    userInfoEndpoint: 'https://api.weixin.qq.com/sns/userinfo',
    scope: ['snsapi_login'],
  },
  wechat_mini_program_qr: {
    authorizationEndpoint: 'https://open.weixin.qq.com/connect/qrconnect',
    tokenEndpoint: 'https://api.weixin.qq.com/sns/oauth2/access_token',
    userInfoEndpoint: 'https://api.weixin.qq.com/sns/userinfo',
    scope: ['snsapi_login'],
  },
  wechat_official_account: {
    authorizationEndpoint: 'https://open.weixin.qq.com/connect/oauth2/authorize',
    tokenEndpoint: 'https://api.weixin.qq.com/sns/oauth2/access_token',
    userInfoEndpoint: 'https://api.weixin.qq.com/sns/userinfo',
    scope: ['snsapi_userinfo'],
  },
  wechat_mini_program: {
    authorizationEndpoint: '',
    tokenEndpoint: 'https://api.weixin.qq.com/sns/jscode2session',
    userInfoEndpoint: '',
    scope: [],
  },
  github: {
    authorizationEndpoint: 'https://github.com/login/oauth/authorize',
    tokenEndpoint: 'https://github.com/login/oauth/access_token',
    userInfoEndpoint: 'https://api.github.com/user',
    scope: ['user:email'],
  },
  gitee: {
    authorizationEndpoint: 'https://gitee.com/oauth/authorize',
    tokenEndpoint: 'https://gitee.com/oauth/token',
    userInfoEndpoint: 'https://gitee.com/api/v5/user',
    scope: ['user_info', 'emails'],
  },
  feishu: {
    authorizationEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/authorize',
    tokenEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/oidc/access_token',
    userInfoEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/user_info',
    scope: ['contact:user.base:readonly'],
  },
} as const satisfies Record<SocialProviderType, OAuthProviderConfig>

export const isSocialProviderType = (value: unknown): value is SocialProviderType =>
  typeof value === 'string' && SOCIAL_PROVIDER_TYPES.some((type) => type === value)
