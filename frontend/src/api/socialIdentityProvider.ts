import request from '../utils/request'
import type {
  SocialIdentityProvider,
  SocialIdentityProviderListResponse,
  CreateSocialIdentityProviderDto,
  UpdateSocialIdentityProviderDto,
  SocialIdentityProviderStats,
  OAuthAuthorizeUrlResponse,
  OAuthCallbackDto,
  SocialLoginResponse,
} from '../types/socialIdentityProvider'

export const socialIdentityProviderApi = {
  async getStats(): Promise<SocialIdentityProviderStats> {
    const response: any = await request.get('/social-identity-providers/stats')
    return response.data
  },

  async getList(params?: { type?: string; status?: string; search?: string; page?: number; pageSize?: number }): Promise<SocialIdentityProviderListResponse> {
    const response: any = await request.get('/social-identity-providers', { params })
    return { ...response.data, providers: response.data.providers.map(mapProvider) }
  },

  async getById(id: string): Promise<SocialIdentityProvider> {
    const response: any = await request.get(`/social-identity-providers/${id}`)
    return mapProvider(response.data)
  },

  async create(data: CreateSocialIdentityProviderDto): Promise<SocialIdentityProvider> {
    const response: any = await request.post('/social-identity-providers', wire(data))
    return mapProvider(response.data)
  },

  async update(id: string, data: UpdateSocialIdentityProviderDto): Promise<SocialIdentityProvider> {
    const response: any = await request.put(`/social-identity-providers/${id}`, wire(data))
    return mapProvider(response.data)
  },

  delete(id: string): Promise<void> {
    return request.delete(`/social-identity-providers/${id}`)
  },

  getAuthorizeUrl(type: string, redirectUri: string): Promise<OAuthAuthorizeUrlResponse> {
    return request.post(`/social-identity-providers/${type}/authorize`, {
      redirectUri,
    })
  },

  handleCallback(type: string, data: OAuthCallbackDto): Promise<SocialLoginResponse> {
    return request.post(`/social-identity-providers/${type}/callback`, data)
  },
}

const wire = (data: CreateSocialIdentityProviderDto | UpdateSocialIdentityProviderDto) => ({
  name:data.name, issuer:data.issuer, clientId:data.clientId, clientSecret:data.clientSecret,
  scopes:data.scope, claimMapping:data.attributeMapping, jitProvisioning:data.jitProvisioning,
  status:'status' in data && data.status === ('inactive' as any) ? 'disabled' : ('status' in data ? data.status : undefined),
})
const mapProvider = (data:any):SocialIdentityProvider => ({
  ...data, type:'oidc', scope:data.scopes || [], attributeMapping:data.claimMapping || null,
  status:data.status === 'inactive' ? 'disabled' : data.status,
})
