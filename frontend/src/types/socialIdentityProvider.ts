export const SOCIAL_PROVIDER_TYPES = ['oidc'] as const
export type SocialProviderType = (typeof SOCIAL_PROVIDER_TYPES)[number]
export interface SocialIdentityProvider {
  id: string; tenantId: string; name: string; type: SocialProviderType; issuer: string
  clientId: string; clientSecret?: string | null; scope: string[]
  attributeMapping: Record<string,string> | null; jitProvisioning: boolean
  status: 'active' | 'disabled'; createdAt: string; updatedAt: string
}
export interface CreateSocialIdentityProviderDto {
  name: string; type: SocialProviderType; issuer: string; clientId: string; clientSecret: string
  scope?: string[]; attributeMapping?: Record<string,string>; jitProvisioning?: boolean
}
export interface UpdateSocialIdentityProviderDto {
  name?: string; issuer?: string; clientId?: string; clientSecret?: string
  scope?: string[]; attributeMapping?: Record<string,string>; jitProvisioning?: boolean
  status?: 'active' | 'disabled'
}
export interface SocialIdentityProviderListResponse { items: SocialIdentityProvider[]; total: number; page: number; pageSize: number }
export interface SocialIdentityProviderStats { totalProviders: number; activeProviders: number; inactiveProviders: number; byType: Record<string,number> }
export interface OAuthAuthorizeUrlResponse { authorizeUrl: string; state: string }
export interface OAuthCallbackDto { code: string; state: string; redirectUri: string }
export interface SocialLoginResponse { token: string; user: { id:string;username:string;email:string;name:string;avatar:string|null };isNewUser:boolean }
export const PROVIDER_CONFIGS = {
  oidc: { name:'通用 OIDC', color:'#0369A1', defaultScopes:['openid','profile','email'], availableScopes:['openid','profile','email','phone','offline_access'] }
} as const
export const isSocialProviderType=(value:unknown):value is SocialProviderType=>value==='oidc'
