import request from '../utils/request'
import type {
  SocialIdentityProvider,
  CreateSocialIdentityProviderDto,
  UpdateSocialIdentityProviderDto,
  SocialIdentityProviderStats,
  OAuthAuthorizeUrlResponse,
  OAuthCallbackDto,
  SocialLoginResponse,
} from '../types/socialIdentityProvider'

export const socialIdentityProviderApi = {
  getStats(): Promise<SocialIdentityProviderStats> {
    return request.get('/social-identity-providers/stats')
  },

  getList(params?: { type?: string; status?: string }): Promise<SocialIdentityProvider[]> {
    return request.get('/social-identity-providers', { params })
  },

  getById(id: string): Promise<SocialIdentityProvider> {
    return request.get(`/social-identity-providers/${id}`)
  },

  create(data: CreateSocialIdentityProviderDto): Promise<SocialIdentityProvider> {
    return request.post('/social-identity-providers', data)
  },

  update(id: string, data: UpdateSocialIdentityProviderDto): Promise<SocialIdentityProvider> {
    return request.put(`/social-identity-providers/${id}`, data)
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
