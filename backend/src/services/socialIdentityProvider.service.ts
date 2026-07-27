import prisma from '../lib/prisma'
import type { Prisma, SocialIdentityProvider } from '@prisma/client'
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
import {
  SOCIAL_PROVIDER_CONFIGS,
  isSocialProviderType,
} from '../config/socialIdentityProvider'

export class SocialIdentityProviderService {
  async create(tenantId: string, data: CreateSocialIdentityProviderDto): Promise<SocialIdentityProviderResponse> {
    if (!isSocialProviderType(data.type)) {
      throw new AppError('不支持的社会化身份源类型', 400)
    }

    const existingProvider = await prisma.socialIdentityProvider.findFirst({
      where: {
        tenantId,
        type: data.type,
      },
    })

    if (existingProvider) {
      throw new AppError('该类型的社会化身份源已存在', 400)
    }

    const config = SOCIAL_PROVIDER_CONFIGS[data.type]

    const provider = await prisma.socialIdentityProvider.create({
      data: {
        tenantId,
        name: data.name,
        type: data.type,
        clientId: data.clientId,
        clientSecret: data.clientSecret,
        authorizationEndpoint: config.authorizationEndpoint,
        tokenEndpoint: config.tokenEndpoint,
        userInfoEndpoint: config.userInfoEndpoint,
        scope: data.scope || [...config.scope],
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

  async findAll(
    tenantId: string,
    query?: { type?: string; status?: string; page?: number; pageSize?: number }
  ): Promise<{ providers: SocialIdentityProviderResponse[]; total: number; page: number; pageSize: number }> {
    const page = query?.page || 1
    const pageSize = query?.pageSize || 10
    const skip = (page - 1) * pageSize

    const where: Prisma.SocialIdentityProviderWhereInput = { tenantId }

    if (query?.type) {
      where.type = query.type
    }

    if (query?.status) {
      where.status = query.status
    }

    const [providers, total] = await Promise.all([
      prisma.socialIdentityProvider.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { createdAt: 'desc' },
      }),
      prisma.socialIdentityProvider.count({ where }),
    ])

    return {
      providers: providers.map((provider) => ({
        ...provider,
        scope: provider.scope as string[],
        attributeMapping: provider.attributeMapping as Record<string, string>,
      })),
      total,
      page,
      pageSize,
    }
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
        tenantId,
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
              tenantId,
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
    provider: SocialIdentityProvider,
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
    provider: SocialIdentityProvider,
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
