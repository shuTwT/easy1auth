export const SOCIAL_SOURCE_TYPES = ['wechat_qr', 'wechat_mp', 'github', 'gitee', 'feishu_web'] as const
export type SocialSourceType = (typeof SOCIAL_SOURCE_TYPES)[number]

export interface SocialIdentitySource {
  id: string; tenantId: string; name: string; type: SocialSourceType; mode: string | null
  clientId: string; clientSecret?: string | null
  status: 'active' | 'disabled'; createdAt: string; updatedAt: string
}
export interface CreateSocialIdentitySourceDto {
  name: string; type: SocialSourceType; mode?: string | null; clientId: string; clientSecret: string
}
export interface UpdateSocialIdentitySourceDto {
  name?: string; type?: SocialSourceType; mode?: string | null
  clientId?: string; clientSecret?: string
  status?: 'active' | 'disabled'
}
export interface SocialIdentitySourceListResponse { items: SocialIdentitySource[]; total: number; page: number; pageSize: number }
export interface SocialIdentitySourceStats { totalSources: number; activeSources: number; inactiveSources: number; byType: Record<string, number> }

export const SOURCE_CONFIGS = {
  wechat_qr: { name: '微信扫码', color: '#07C160', clientIdLabel: 'AppID', clientSecretLabel: 'AppSecret', supportsMode: false },
  wechat_mp: { name: '微信公众号', color: '#07C160', clientIdLabel: 'AppID', clientSecretLabel: 'AppSecret', supportsMode: false },
  github: { name: 'GitHub', color: '#24292F', clientIdLabel: 'Client ID', clientSecretLabel: 'Client Secret', supportsMode: false },
  gitee: { name: 'Gitee', color: '#C71D23', clientIdLabel: 'Client ID', clientSecretLabel: 'Client Secret', supportsMode: false },
  feishu_web: { name: '飞书网页授权', color: '#00D6B9', clientIdLabel: 'App ID', clientSecretLabel: 'App Secret', supportsMode: false },
} as const

export const isSocialSourceType = (value: unknown): value is SocialSourceType =>
  SOCIAL_SOURCE_TYPES.includes(value as SocialSourceType)
