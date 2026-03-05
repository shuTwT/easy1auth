import prisma from '../lib/prisma'
import { AppError } from '../middleware/errorHandler'
import {
  CreateSocialIdentityProviderDto,
  UpdateSocialIdentityProviderDto,
  SocialIdentityProviderResponse,
  SocialIdentityProviderStats,
  OAuthAuthorizeUrlResponse,
  OAuthCallbackDto,
  SocialLoginResponse,
} from '../types/socialIdentityProvider.types'
import axios from 'axios'
import crypto from 'crypto'
import jwt from 'jsonwebtoken'
import { v4 as uuidv4 } from 'uuid'

const OAUTH_CONFIGS: Record<string, any> = {
  wechat: {
    authorizationEndpoint: 'https://open.weixin.qq.com/connect/qrconnect',
    tokenEndpoint: 'https://api.weixin.qq.com/sns/oauth2/access_token',
    userInfoEndpoint: 'https://api.weixin.qq.com/sns/userinfo',
    scope: ['snsapi_login'],
  },
  qq: {
    authorizationEndpoint: 'https://graph.qq.com/oauth2.0/authorize',
    tokenEndpoint: 'https://graph.qq.com/oauth2.0/token',
    userInfoEndpoint: 'https://graph.qq.com/user/get_user_info',
    scope: ['get_user_info'],
  },
  feishu: {
    authorizationEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/authorize',
    tokenEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/oidc/access_token',
    userInfoEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/user_info',
    scope: ['contact:user.base:readonly'],
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
  dingtalk: {
    authorizationEndpoint: 'https://login.dingtalk.com/oauth2/auth',
    tokenEndpoint: 'https://api.dingtalk.com/v1.0/oauth2/userAccessToken',
    userInfoEndpoint: 'https://api.dingtalk.com/v1.0/contact/users/me',
    scope: ['openid'],
  },
  wechat_work: {
    authorizationEndpoint: 'https://open.work.weixin.qq.com/wwopen/sso/qrConnect',
    tokenEndpoint: 'https://qyapi.weixin.qq.com/cgi-bin/miniprogram/jscode2session',
    userInfoEndpoint: 'https://qyapi.weixin.qq.com/cgi-bin/user/get',
    scope: ['snsapi_base'],
  },
}

export class SocialIdentityProviderService {
  async create(tenantId: string, data: CreateSocialIdentityProviderDto): Promise<SocialIdentityProviderResponse> {
    const existingProvider = await prisma.socialIdentityProvider.findFirst({
      where: {
        tenantId,
        type: data.type,
      },
    })

    if (existingProvider) {
      throw new AppError('该类型的社会化身份源已存在', 400)
    }

    const config = OAUTH_CONFIGS[data.type] || {}

    const provider = await prisma.socialIdentityProvider.create({
      data: {
        tenantId,
        name: data.name,
        type: data.type,
        clientId: data.clientId,
        clientSecret: data.clientSecret,
        authorizationEndpoint: data.authorizationEndpoint || config.authorizationEndpoint || '',
        tokenEndpoint: data.tokenEndpoint || config.tokenEndpoint || '',
        userInfoEndpoint: data.userInfoEndpoint || config.userInfoEndpoint || '',
        scope: data.scope || config.scope || [],
        attributeMapping: data.attributeMapping || {},
        status: 'active',
      },
    })

    return {
      ...provider,
      scope: provider.scope as string[],
      attributeMapping: provider.attributeMapping as Record<string, string>,
    }
  }

  async findAll(tenantId: string, query?: { type?: string; status?: string }): Promise<SocialIdentityProviderResponse[]> {
    const where: any = { tenantId }

    if (query?.type) {
      where.type = query.type
    }

    if (query?.status) {
      where.status = query.status
    }

    const providers = await prisma.socialIdentityProvider.findMany({
      where,
      orderBy: { createdAt: 'desc' },
    })

    return providers.map((provider) => ({
      ...provider,
      scope: provider.scope as string[],
      attributeMapping: provider.attributeMapping as Record<string, string>,
    }))
  }

  async getStats(tenantId: string): Promise<SocialIdentityProviderStats> {
    const providers = await prisma.socialIdentityProvider.findMany({
      where: { tenantId },
    })

    const totalProviders = providers.length
    const activeProviders = providers.filter((p) => p.status === 'active').length
    const inactiveProviders = providers.filter((p) => p.status === 'inactive').length

    const byType: Record<string, number> = {}
    providers.forEach((p) => {
      byType[p.type] = (byType[p.type] || 0) + 1
    })

    return {
      totalProviders,
      activeProviders,
      inactiveProviders,
      byType,
    }
  }

  async findById(tenantId: string, id: string): Promise<SocialIdentityProviderResponse> {
    const provider = await prisma.socialIdentityProvider.findFirst({
      where: { id, tenantId },
    })

    if (!provider) {
      throw new AppError('社会化身份源不存在', 404)
    }

    return {
      ...provider,
      scope: provider.scope as string[],
      attributeMapping: provider.attributeMapping as Record<string, string>,
    }
  }

  async update(
    tenantId: string,
    id: string,
    data: UpdateSocialIdentityProviderDto
  ): Promise<SocialIdentityProviderResponse> {
    const provider = await prisma.socialIdentityProvider.findFirst({
      where: { id, tenantId },
    })

    if (!provider) {
      throw new AppError('社会化身份源不存在', 404)
    }

    const updatedProvider = await prisma.socialIdentityProvider.update({
      where: { id },
      data: {
        name: data.name,
        clientId: data.clientId,
        clientSecret: data.clientSecret,
        authorizationEndpoint: data.authorizationEndpoint,
        tokenEndpoint: data.tokenEndpoint,
        userInfoEndpoint: data.userInfoEndpoint,
        scope: data.scope,
        attributeMapping: data.attributeMapping,
        status: data.status,
      },
    })

    return {
      ...updatedProvider,
      scope: updatedProvider.scope as string[],
      attributeMapping: updatedProvider.attributeMapping as Record<string, string>,
    }
  }

  async delete(tenantId: string, id: string): Promise<void> {
    const provider = await prisma.socialIdentityProvider.findFirst({
      where: { id, tenantId },
    })

    if (!provider) {
      throw new AppError('社会化身份源不存在', 404)
    }

    await prisma.socialIdentityProvider.delete({
      where: { id },
    })
  }

  async getAuthorizeUrl(
    tenantId: string,
    providerType: string,
    redirectUri: string
  ): Promise<OAuthAuthorizeUrlResponse> {
    const provider = await prisma.socialIdentityProvider.findFirst({
      where: { tenantId, type: providerType, status: 'active' },
    })

    if (!provider) {
      throw new AppError('社会化身份源不存在或未启用', 404)
    }

    const state = crypto.randomBytes(16).toString('hex')
    const scope = (provider.scope as string[]).join(' ')

    const params = new URLSearchParams({
      client_id: provider.clientId,
      redirect_uri: redirectUri,
      response_type: 'code',
      scope: scope,
      state: state,
    })

    const authorizeUrl = `${provider.authorizationEndpoint}?${params.toString()}`

    await prisma.auditLog.create({
      data: {
        tenantId,
        type: 'oauth',
        action: 'authorize_url_generated',
        resource: 'social_identity_provider',
        resourceId: provider.id,
        ip: '0.0.0.0',
        status: 'success',
      },
    })

    return {
      authorizeUrl,
      state,
    }
  }

  async handleCallback(
    tenantId: string,
    providerType: string,
    data: OAuthCallbackDto,
    redirectUri: string
  ): Promise<SocialLoginResponse> {
    const provider = await prisma.socialIdentityProvider.findFirst({
      where: { tenantId, type: providerType, status: 'active' },
    })

    if (!provider) {
      throw new AppError('社会化身份源不存在或未启用', 404)
    }

    let accessToken: string
    let refreshToken: string | undefined
    let providerUserId: string
    let userProfile: Record<string, any>

    try {
      const tokenResponse = await this.exchangeCodeForToken(provider, data.code, redirectUri)
      accessToken = tokenResponse.access_token
      refreshToken = tokenResponse.refresh_token
      providerUserId = tokenResponse.user_id

      userProfile = await this.fetchUserProfile(provider, accessToken, providerUserId)
    } catch (error) {
      throw new AppError('OAuth认证失败', 400)
    }

    let socialAccount = await prisma.socialAccount.findFirst({
      where: {
        provider: providerType,
        providerId: providerUserId,
      },
      include: { user: true },
    })

    let isNewUser = false
    let user

    if (socialAccount) {
      user = socialAccount.user

      await prisma.socialAccount.update({
        where: { id: socialAccount.id },
        data: {
          accessToken,
          refreshToken,
          profile: userProfile,
        },
      })
    } else {
      isNewUser = true
      const username = `social_${providerType}_${providerUserId}`
      const email = userProfile.email || `${username}@social.local`

      user = await prisma.user.create({
        data: {
          tenantId,
          username,
          email,
          name: userProfile.name || userProfile.nickname || username,
          avatar: userProfile.avatar_url || userProfile.avatar || userProfile.headimgurl,
          password: null,
          status: 'active',
          emailVerified: !!userProfile.email,
          socialAccounts: {
            create: {
              provider: providerType,
              providerId: providerUserId,
              accessToken,
              refreshToken,
              profile: userProfile,
            },
          },
        },
      })
    }

    const token = jwt.sign(
      {
        userId: user.id,
        tenantId: user.tenantId,
        username: user.username,
      },
      process.env.JWT_SECRET || 'your-secret-key',
      { expiresIn: '7d' }
    )

    await prisma.auditLog.create({
      data: {
        tenantId,
        userId: user.id,
        username: user.username,
        type: 'oauth',
        action: isNewUser ? 'social_register' : 'social_login',
        resource: 'user',
        resourceId: user.id,
        ip: '0.0.0.0',
        status: 'success',
      },
    })

    return {
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        name: user.name,
        avatar: user.avatar,
      },
      isNewUser,
    }
  }

  private async exchangeCodeForToken(
    provider: any,
    code: string,
    redirectUri: string
  ): Promise<{ access_token: string; refresh_token?: string; user_id: string }> {
    const params = new URLSearchParams({
      client_id: provider.clientId,
      client_secret: provider.clientSecret,
      code: code,
      redirect_uri: redirectUri,
      grant_type: 'authorization_code',
    })

    const response = await axios.post(provider.tokenEndpoint, params.toString(), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Accept: 'application/json',
      },
    })

    const data = response.data

    return {
      access_token: data.access_token || data.accessToken,
      refresh_token: data.refresh_token || data.refreshToken,
      user_id: data.openid || data.userid || data.id || data.uid,
    }
  }

  private async fetchUserProfile(
    provider: any,
    accessToken: string,
    userId: string
  ): Promise<Record<string, any>> {
    const response = await axios.get(provider.userInfoEndpoint, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'application/json',
      },
      params: {
        access_token: accessToken,
        openid: userId,
      },
    })

    return response.data
  }
}
