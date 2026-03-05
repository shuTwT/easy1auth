import { Application, User } from '@prisma/client'
import prisma from '../lib/prisma'
import tokenService from './token.service'
import { 
  AuthorizationCodeDto, 
  TokenRequestDto, 
  TokenResponse, 
  UserInfo,
  OAuth2Error 
} from '../types/oauth2.types'
import { AppError } from '../middleware/errorHandler'
import crypto from 'crypto'

export class OAuth2Service {
  async validateAuthorizationRequest(
    dto: AuthorizationCodeDto,
    tenantId: string
  ): Promise<Application> {
    if (dto.response_type !== 'code') {
      throw new AppError('unsupported_response_type', 400)
    }

    const application = await prisma.application.findUnique({
      where: { clientId: dto.client_id }
    })

    if (!application) {
      throw new AppError('invalid_client', 401)
    }

    if (application.tenantId !== tenantId) {
      throw new AppError('invalid_client', 401)
    }

    if (application.status !== 'active') {
      throw new AppError('unauthorized_client', 403)
    }

    const redirectUris = application.redirectUris as string[]
    if (!redirectUris.includes(dto.redirect_uri)) {
      throw new AppError('invalid_redirect_uri', 400)
    }

    const allowedGrantTypes = application.allowedGrantTypes as string[]
    if (!allowedGrantTypes.includes('authorization_code')) {
      throw new AppError('unauthorized_client', 403)
    }

    return application
  }

  async generateAuthorizationCode(
    application: Application,
    user: User,
    redirectUri: string,
    scope?: string,
    codeChallenge?: string,
    codeChallengeMethod?: string
  ): Promise<string> {
    return tokenService.generateAuthorizationCode(
      application.clientId,
      redirectUri,
      user.id,
      user.tenantId,
      scope,
      codeChallenge,
      codeChallengeMethod
    )
  }

  async exchangeCodeForToken(
    dto: TokenRequestDto,
    tenantId: string
  ): Promise<TokenResponse> {
    if (dto.grant_type !== 'authorization_code') {
      throw new AppError('unsupported_grant_type', 400)
    }

    if (!dto.code || !dto.redirect_uri || !dto.client_id || !dto.client_secret) {
      throw new AppError('invalid_request', 400)
    }

    const application = await prisma.application.findUnique({
      where: { clientId: dto.client_id }
    })

    if (!application) {
      throw new AppError('invalid_client', 401)
    }

    if (application.tenantId !== tenantId) {
      throw new AppError('invalid_client', 401)
    }

    if (application.clientSecret !== dto.client_secret) {
      throw new AppError('invalid_client', 401)
    }

    const authCode = await tokenService.validateAuthorizationCode(dto.code)
    if (!authCode) {
      throw new AppError('invalid_grant', 400)
    }

    if (authCode.clientId !== dto.client_id) {
      throw new AppError('invalid_grant', 400)
    }

    if (authCode.redirectUri !== dto.redirect_uri) {
      throw new AppError('invalid_grant', 400)
    }

    if (authCode.codeChallenge && dto.code_verifier) {
      if (!this.validatePKCE(authCode.codeChallenge, dto.code_verifier, authCode.codeChallengeMethod)) {
        throw new AppError('invalid_grant', 400)
      }
    }

    const user = await prisma.user.findUnique({
      where: { id: authCode.userId }
    })

    if (!user) {
      throw new AppError('invalid_grant', 400)
    }

    await tokenService.revokeAuthorizationCode(dto.code)

    const { accessToken, expiresIn } = await tokenService.generateAccessToken(
      application,
      user,
      authCode.scope
    )

    const refreshToken = await tokenService.generateRefreshToken(application, user)

    const tenant = await prisma.tenant.findUnique({
      where: { id: user.tenantId }
    })

    const userRoles = await prisma.user.findUnique({
      where: { id: user.id },
      include: { roles: true }
    })

    const userGroups = await prisma.user.findUnique({
      where: { id: user.id },
      include: { groups: true }
    })

    const idToken = await tokenService.generateIdToken(
      application,
      user,
      tenant?.name || '',
      userRoles?.roles.map(r => r.code) || [],
      userGroups?.groups.map(g => g.name) || []
    )

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: expiresIn,
      refresh_token: refreshToken,
      scope: authCode.scope,
      id_token: idToken
    }
  }

  async refreshToken(
    dto: TokenRequestDto,
    tenantId: string
  ): Promise<TokenResponse> {
    if (dto.grant_type !== 'refresh_token') {
      throw new AppError('unsupported_grant_type', 400)
    }

    if (!dto.refresh_token || !dto.client_id || !dto.client_secret) {
      throw new AppError('invalid_request', 400)
    }

    const application = await prisma.application.findUnique({
      where: { clientId: dto.client_id }
    })

    if (!application) {
      throw new AppError('invalid_client', 401)
    }

    if (application.tenantId !== tenantId) {
      throw new AppError('invalid_client', 401)
    }

    if (application.clientSecret !== dto.client_secret) {
      throw new AppError('invalid_client', 401)
    }

    const refreshToken = await tokenService.validateRefreshToken(dto.refresh_token)
    if (!refreshToken) {
      throw new AppError('invalid_grant', 400)
    }

    if (refreshToken.clientId !== dto.client_id) {
      throw new AppError('invalid_grant', 400)
    }

    const user = await prisma.user.findUnique({
      where: { id: refreshToken.userId }
    })

    if (!user) {
      throw new AppError('invalid_grant', 400)
    }

    await tokenService.revokeRefreshToken(dto.refresh_token)

    const { accessToken, expiresIn } = await tokenService.generateAccessToken(
      application,
      user,
      refreshToken.scope
    )

    const newRefreshToken = await tokenService.generateRefreshToken(application, user)

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: expiresIn,
      refresh_token: newRefreshToken,
      scope: refreshToken.scope
    }
  }

  async clientCredentialsGrant(
    dto: TokenRequestDto,
    tenantId: string
  ): Promise<TokenResponse> {
    if (dto.grant_type !== 'client_credentials') {
      throw new AppError('unsupported_grant_type', 400)
    }

    if (!dto.client_id || !dto.client_secret) {
      throw new AppError('invalid_request', 400)
    }

    const application = await prisma.application.findUnique({
      where: { clientId: dto.client_id }
    })

    if (!application) {
      throw new AppError('invalid_client', 401)
    }

    if (application.tenantId !== tenantId) {
      throw new AppError('invalid_client', 401)
    }

    if (application.clientSecret !== dto.client_secret) {
      throw new AppError('invalid_client', 401)
    }

    const allowedGrantTypes = application.allowedGrantTypes as string[]
    if (!allowedGrantTypes.includes('client_credentials')) {
      throw new AppError('unauthorized_client', 403)
    }

    const { accessToken, expiresIn } = await tokenService.generateAccessToken(
      application,
      { id: 'client', tenantId: application.tenantId } as User,
      dto.scope
    )

    return {
      access_token: accessToken,
      token_type: 'Bearer',
      expires_in: expiresIn,
      scope: dto.scope
    }
  }

  async getUserInfo(accessToken: string): Promise<UserInfo> {
    const tokenData = await tokenService.validateAccessToken(accessToken)
    if (!tokenData) {
      throw new AppError('invalid_token', 401)
    }

    const user = await prisma.user.findUnique({
      where: { id: tokenData.userId },
      include: {
        roles: true,
        groups: true,
        tenant: true
      }
    })

    if (!user) {
      throw new AppError('invalid_token', 401)
    }

    return {
      sub: user.id,
      name: user.name,
      email: user.email,
      email_verified: user.emailVerified,
      phone: user.phone || undefined,
      phone_verified: user.phoneVerified,
      avatar: user.avatar || undefined,
      department: user.department || undefined,
      position: user.position || undefined,
      tenant_id: user.tenantId,
      tenant_name: user.tenant.name,
      roles: user.roles.map(r => r.code),
      groups: user.groups.map(g => g.name)
    }
  }

  private validatePKCE(
    codeChallenge: string,
    codeVerifier: string,
    method?: string
  ): boolean {
    if (method === 'S256') {
      const hash = crypto
        .createHash('sha256')
        .update(codeVerifier)
        .digest('base64')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '')
      return codeChallenge === hash
    } else if (method === 'plain') {
      return codeChallenge === codeVerifier
    }
    return false
  }
}

export default new OAuth2Service()
