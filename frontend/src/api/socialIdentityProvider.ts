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
  getStats() {
    return request.get<SocialIdentityProviderStats>('/social-identity-providers/stats')
  },

  getList(params?: { type?: string; status?: string }) {
    return request.get<SocialIdentityProvider[]>('/social-identity-providers', { params })
  },

  getById(id: string) {
    return request.get<SocialIdentityProvider>(`/social-identity-providers/${id}`)
  },

  create(data: CreateSocialIdentityProviderDto) {
    return request.post<SocialIdentityProvider>('/social-identity-providers', data)
  },

  update(id: string, data: UpdateSocialIdentityProviderDto) {
    return request.put<SocialIdentityProvider>(`/social-identity-providers/${id}`, data)
  },

  delete(id: string) {
    return request.delete(`/social-identity-providers/${id}`)
  },

  getAuthorizeUrl(type: string, redirectUri: string) {
    return request.post<OAuthAuthorizeUrlResponse>(`/social-identity-providers/${type}/authorize`, {
      redirectUri,
    })
  },

  handleCallback(type: string, data: OAuthCallbackDto) {
    return request.post<SocialLoginResponse>(`/social-identity-providers/${type}/callback`, data)
  },
}
