import jwt from 'jsonwebtoken'
import { v4 as uuidv4 } from 'uuid'
import redis from '../lib/redis'
import { AccessToken, RefreshToken, AuthorizationCode } from '../types/oauth2.types'
import { Application, User } from '@prisma/client'
import prisma from '../lib/prisma'

export class TokenService {
  private readonly JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key'
  private readonly AUTHORIZATION_CODE_EXPIRY = 600 // 10 minutes
  private readonly ACCESS_TOKEN_EXPIRY = 3600 // 1 hour
  private readonly REFRESH_TOKEN_EXPIRY = 2592000 // 30 days

  async generateAuthorizationCode(
    clientId: string,
    redirectUri: string,
    userId: string,
    tenantId: string,
    scope?: string,
    codeChallenge?: string,
    codeChallengeMethod?: string
  ): Promise<string> {
    const code = uuidv4().replace(/-/g, '')
    const authCode: AuthorizationCode = {
      code,
      clientId,
      redirectUri,
      userId,
      tenantId,
      scope,
      expiresAt: new Date(Date.now() + this.AUTHORIZATION_CODE_EXPIRY * 1000),
      codeChallenge,
      codeChallengeMethod
    }

    await redis.setex(
      `auth_code:${code}`,
      this.AUTHORIZATION_CODE_EXPIRY,
      JSON.stringify(authCode)
    )

    return code
  }

  async validateAuthorizationCode(code: string): Promise<AuthorizationCode | null> {
    const data = await redis.get(`auth_code:${code}`)
    if (!data) return null

    const authCode: AuthorizationCode = JSON.parse(data)
    if (new Date() > new Date(authCode.expiresAt)) {
      await redis.del(`auth_code:${code}`)
      return null
    }

    return authCode
  }

  async revokeAuthorizationCode(code: string): Promise<void> {
    await redis.del(`auth_code:${code}`)
  }

  async generateAccessToken(
    application: Application,
    user: User,
    scope?: string
  ): Promise<{ accessToken: string; expiresIn: number }> {
    const tokenId = uuidv4()
    const expiresIn = application.accessTokenLifetime || this.ACCESS_TOKEN_EXPIRY
    const expiresAt = new Date(Date.now() + expiresIn * 1000)

    const payload = {
      jti: tokenId,
      sub: user.id,
      aud: application.clientId,
      iss: process.env.ISSUER || 'https://auth.easy1auth.com',
      exp: Math.floor(expiresAt.getTime() / 1000),
      iat: Math.floor(Date.now() / 1000),
      scope,
      tenant_id: user.tenantId,
      username: user.username,
      email: user.email,
      name: user.name
    }

    const accessToken = jwt.sign(payload, this.JWT_SECRET)

    const tokenData: AccessToken = {
      token: accessToken,
      clientId: application.clientId,
      userId: user.id,
      tenantId: user.tenantId,
      scope,
      expiresAt
    }

    await redis.setex(
      `access_token:${tokenId}`,
      expiresIn,
      JSON.stringify(tokenData)
    )

    return { accessToken, expiresIn }
  }

  async generateRefreshToken(
    application: Application,
    user: User
  ): Promise<string> {
    const tokenId = uuidv4()
    const expiresIn = application.refreshTokenLifetime || this.REFRESH_TOKEN_EXPIRY
    const expiresAt = new Date(Date.now() + expiresIn * 1000)

    const refreshToken = uuidv4().replace(/-/g, '') + '.' + Buffer.from(application.clientId).toString('base64')

    const tokenData: RefreshToken = {
      token: refreshToken,
      clientId: application.clientId,
      userId: user.id,
      tenantId: user.tenantId,
      expiresAt
    }

    await redis.setex(
      `refresh_token:${refreshToken}`,
      expiresIn,
      JSON.stringify(tokenData)
    )

    return refreshToken
  }

  async validateAccessToken(token: string): Promise<AccessToken | null> {
    try {
      const decoded = jwt.verify(token, this.JWT_SECRET) as any
      const tokenId = decoded.jti

      const data = await redis.get(`access_token:${tokenId}`)
      if (!data) return null

      const accessToken: AccessToken = JSON.parse(data)
      if (new Date() > new Date(accessToken.expiresAt)) {
        await redis.del(`access_token:${tokenId}`)
        return null
      }

      return accessToken
    } catch (error) {
      return null
    }
  }

  async validateRefreshToken(token: string): Promise<RefreshToken | null> {
    const data = await redis.get(`refresh_token:${token}`)
    if (!data) return null

    const refreshToken: RefreshToken = JSON.parse(data)
    if (new Date() > new Date(refreshToken.expiresAt)) {
      await redis.del(`refresh_token:${token}`)
      return null
    }

    return refreshToken
  }

  async revokeRefreshToken(token: string): Promise<void> {
    await redis.del(`refresh_token:${token}`)
  }

  async revokeAllUserTokens(userId: string, tenantId: string): Promise<void> {
    const keys = await redis.keys(`access_token:*`)
    for (const key of keys) {
      const data = await redis.get(key)
      if (data) {
        const token = JSON.parse(data)
        if (token.userId === userId && token.tenantId === tenantId) {
          await redis.del(key)
        }
      }
    }

    const refreshKeys = await redis.keys(`refresh_token:*`)
    for (const key of refreshKeys) {
      const data = await redis.get(key)
      if (data) {
        const token = JSON.parse(data)
        if (token.userId === userId && token.tenantId === tenantId) {
          await redis.del(key)
        }
      }
    }
  }

  async generateIdToken(
    application: Application,
    user: User,
    tenantName: string,
    roles: string[],
    groups: string[]
  ): Promise<string> {
    const payload = {
      iss: process.env.ISSUER || 'https://auth.easy1auth.com',
      sub: user.id,
      aud: application.clientId,
      exp: Math.floor((Date.now() + 3600000) / 1000),
      iat: Math.floor(Date.now() / 1000),
      name: user.name,
      email: user.email,
      email_verified: user.emailVerified,
      phone_number: user.phone,
      phone_number_verified: user.phoneVerified,
      picture: user.avatar,
      tenant_id: user.tenantId,
      tenant_name: tenantName,
      roles,
      groups
    }

    return jwt.sign(payload, this.JWT_SECRET)
  }

  decodeAccessToken(token: string): any {
    try {
      return jwt.decode(token)
    } catch (error) {
      return null
    }
  }
}

export default new TokenService()
