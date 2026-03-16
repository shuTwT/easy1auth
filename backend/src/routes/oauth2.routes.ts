import { Router, Request, Response } from 'express'
import oauth2Service from '../services/oauth2.service'
import tokenService from '../services/token.service'
import { AppError } from '../middleware/errorHandler'
import prisma from '../lib/prisma'
import jwt from 'jsonwebtoken'
import bcrypt from 'bcryptjs'

const router = Router()

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key-change-in-production'

// OAuth2 获取应用信息 - 参数只有 clientId，返回 client 和 scopes
router.get('/authorize', async (req: Request, res: Response) => {
  try {
    const { client_id } = req.query

    if (!client_id) {
      throw new AppError('invalid_request: 缺少 client_id 参数', 400)
    }

    const application = await prisma.application.findUnique({
      where: { clientId: client_id as string }
    })

    if (!application) {
      throw new AppError('invalid_client', 401)
    }

    if (application.status !== 'active') {
      throw new AppError('unauthorized_client', 403)
    }

    const client = {
      clientId: application.clientId,
      name: application.name,
      logo: application.logo,
      description: application.description,
      redirectUris: application.redirectUris
    }

    const scopes = ['openid', 'profile', 'email', 'phone']

    res.json({ client, scopes })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        error: error.message
      })
    } else {
      console.error('获取应用信息错误:', error)
      res.status(500).json({
        error: 'server_error'
      })
    }
  }
})

// OAuth2 授权提交 - 需要用户登录
router.post('/authorize', async (req: Request, res: Response) => {
  try {
    const { token } = req.body
    
    if (!token) {
      throw new AppError('unauthorized', 401)
    }

    // 验证用户 token
    let decoded: any
    try {
      decoded = jwt.verify(token, JWT_SECRET)
    } catch {
      throw new AppError('invalid_token', 401)
    }

    const {
      client_id,
      redirect_uri,
      response_type,
      scope,
      state,
      code_challenge,
      code_challenge_method
    } = req.body

    if (!client_id || !redirect_uri || !response_type) {
      throw new AppError('invalid_request: 缺少必要参数', 400)
    }

    const application = await prisma.application.findUnique({
      where: { clientId: client_id as string }
    })

    if (!application) {
      throw new AppError('invalid_client', 401)
    }

    const tenantId = application.tenantId

    // 获取用户
    const user = await prisma.user.findFirst({
      where: {
        id: decoded.userId,
        tenantId,
        status: 'active'
      }
    })

    if (!user) {
      throw new AppError('unauthorized', 401)
    }

    const code = await oauth2Service.generateAuthorizationCode(
      application,
      user,
      redirect_uri as string,
      scope as string,
      code_challenge as string,
      code_challenge_method as string
    )

    const redirectUrl = new URL(redirect_uri as string)
    redirectUrl.searchParams.set('code', code)
    if (state) {
      redirectUrl.searchParams.set('state', state as string)
    }

    res.json({
      redirectUrl: redirectUrl.toString()
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        error: error.message
      })
    } else {
      console.error('授权错误:', error)
      res.status(500).json({
        error: 'server_error'
      })
    }
  }
})

router.post('/token', async (req: Request, res: Response) => {
  try {
    const {
      grant_type,
      code,
      redirect_uri,
      client_id,
      client_secret,
      refresh_token,
      code_verifier,
      scope
    } = req.body

    if (!grant_type) {
      throw new AppError('invalid_request: 缺少 grant_type', 400)
    }

    let tokenResponse

    if (grant_type === 'authorization_code') {
      if (!code) {
        throw new AppError('invalid_request: 缺少 code 参数', 400)
      }
      
      const authCode = await tokenService.validateAuthorizationCode(code)
      if (!authCode) {
        throw new AppError('invalid_grant: 授权码无效或已过期', 400)
      }
      
      tokenResponse = await oauth2Service.exchangeCodeForToken(
        {
          grant_type,
          code,
          redirect_uri,
          client_id,
          client_secret,
          code_verifier
        },
        authCode.tenantId
      )
    } else if (grant_type === 'refresh_token') {
      tokenResponse = await oauth2Service.refreshToken(
        {
          grant_type,
          refresh_token,
          client_id,
          client_secret
        },
        req.headers['x-tenant-id'] as string
      )
    } else if (grant_type === 'client_credentials') {
      tokenResponse = await oauth2Service.clientCredentialsGrant(
        {
          grant_type,
          client_id,
          client_secret,
          scope
        },
        req.headers['x-tenant-id'] as string
      )
    } else {
      throw new AppError('unsupported_grant_type', 400)
    }

    res.json(tokenResponse)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        error: error.message
      })
    } else {
      console.error('Token 错误:', error)
      res.status(500).json({
        error: 'server_error'
      })
    }
  }
})

router.get('/userinfo', async (req: Request, res: Response) => {
  try {
    const authHeader = req.headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AppError('invalid_token: 缺少访问令牌', 401)
    }

    const accessToken = authHeader.substring(7)
    const userInfo = await oauth2Service.getUserInfo(accessToken)

    res.json(userInfo)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        error: error.message
      })
    } else {
      console.error('UserInfo错误:', error)
      res.status(500).json({
        error: 'server_error'
      })
    }
  }
})

router.post('/revoke', async (req: Request, res: Response) => {
  try {
    const { token, token_type_hint } = req.body

    if (!token) {
      throw new AppError('invalid_request: 缺少token参数', 400)
    }

    if (token_type_hint === 'refresh_token') {
      await tokenService.revokeRefreshToken(token)
    } else if (token_type_hint === 'access_token') {
      const decoded = tokenService.decodeAccessToken(token)
      if (decoded && decoded.jti) {
        await tokenService.revokeRefreshToken(`access_token:${decoded.jti}`)
      }
    } else {
      await tokenService.revokeRefreshToken(token)
      const decoded = tokenService.decodeAccessToken(token)
      if (decoded && decoded.jti) {
        await tokenService.revokeRefreshToken(`access_token:${decoded.jti}`)
      }
    }

    res.status(200).send()
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        error: error.message
      })
    } else {
      console.error('Revoke错误:', error)
      res.status(500).json({
        error: 'server_error'
      })
    }
  }
})

router.get('/keys', async (req: Request, res: Response) => {
  try {
    const publicKey = process.env.JWT_PUBLIC_KEY || ''
    
    res.json({
      keys: [
        {
          kty: 'RSA',
          use: 'sig',
          alg: 'RS256',
          kid: 'key-1',
          n: publicKey
        }
      ]
    })
  } catch (error) {
    console.error('Keys错误:', error)
    res.status(500).json({
      error: 'server_error'
    })
  }
})

router.get('/.well-known/openid-configuration', async (req: Request, res: Response) => {
  try {
    const issuer = process.env.ISSUER || 'https://auth.easy1auth.com'
    
    res.json({
      issuer,
      authorization_endpoint: `${issuer}/oauth2/authorize`,
      token_endpoint: `${issuer}/oauth2/token`,
      userinfo_endpoint: `${issuer}/oauth2/userinfo`,
      jwks_uri: `${issuer}/oauth2/keys`,
      revocation_endpoint: `${issuer}/oauth2/revoke`,
      response_types_supported: ['code', 'token'],
      grant_types_supported: ['authorization_code', 'client_credentials', 'refresh_token'],
      subject_types_supported: ['public'],
      id_token_signing_alg_values_supported: ['RS256'],
      scopes_supported: ['openid', 'profile', 'email', 'phone'],
      token_endpoint_auth_methods_supported: ['client_secret_basic', 'client_secret_post'],
      claims_supported: ['sub', 'name', 'email', 'email_verified', 'phone_number', 'phone_number_verified', 'picture', 'tenant_id', 'tenant_name', 'roles', 'groups']
    })
  } catch (error) {
    console.error('OpenID配置错误:', error)
    res.status(500).json({
      error: 'server_error'
    })
  }
})

export default router
