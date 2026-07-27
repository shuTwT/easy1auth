import { Router, Request, Response } from 'express'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import prisma from '../lib/prisma'
import { ensureDefaultAdminRoles } from '../lib/admin-permissions'
import { VerificationCodeService } from '../services/verification-code.service'

const router = Router()

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key-change-in-production'

router.post('/login', async (req: Request, res: Response) => {
  try {
    const { username, password, email, code, loginType } = req.body

    if (loginType === 'password') {
      const user = await prisma.admin.findFirst({
        where: {
          OR: [
            { username: username || '' },
            { email: username || '' }
          ]
        },
        include: {
          tenants: {
            include: {
              tenant: true
            }
          }
        }
      })

      if (!user) {
        return res.status(400).json({
          status: 'error',
          message: '用户名或密码错误'
        })
      }

      if (user.status !== 'active') {
        return res.status(403).json({
          status: 'error',
          message: '账号已被禁用'
        })
      }

      const isValidPassword = await bcrypt.compare(password, user.password)
      if (!isValidPassword) {
        return res.status(400).json({
          status: 'error',
          message: '用户名或密码错误'
        })
      }

      const currentTenantId = user.currentTenantId || user.tenants[0]?.tenantId

      if (!currentTenantId) {
        return res.status(403).json({
          status: 'error',
          message: '该账号没有关联的租户'
        })
      }

      const token = jwt.sign(
        { 
          userId: user.id,
          type: 'admin'
        },
        JWT_SECRET,
        { expiresIn: '7d' }
      )

      const refreshToken = jwt.sign(
        { 
          userId: user.id, 
          type: 'refresh'
        },
        JWT_SECRET,
        { expiresIn: '30d' }
      )

      return res.json({
        token,
        refreshToken,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          avatar: null,
          currentTenantId
        },
        tenants: user.tenants.map(t => ({
          id: t.tenant.id,
          name: t.tenant.name,
          role: t.role
        }))
      })
    } else if (loginType === 'email') {
      return res.status(501).json({
        status: 'error',
        message: '邮箱验证码登录功能暂未实现'
      })
    } else if (loginType === 'passkey') {
      return res.status(501).json({
        status: 'error',
        message: 'Passkey 登录功能暂未实现'
      })
    } else {
      return res.status(400).json({
        status: 'error',
        message: '无效的登录方式'
      })
    }
  } catch (error) {
    console.error('登录错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '登录失败'
    })
  }
})

router.post('/user-login', async (req: Request, res: Response) => {
  try {
    const { username, password, tenantId } = req.body

    if (!username || !password) {
      return res.status(400).json({
        status: 'error',
        message: '用户名和密码不能为空'
      })
    }

    let userWhere: any = {
      OR: [
        { username: username },
        { email: username }
      ],
      status: 'active'
    }

    if (tenantId) {
      userWhere.tenantId = tenantId
    }

    const user = await prisma.user.findFirst({
      where: userWhere,
      include: {
        tenant: true
      }
    })

    if (!user) {
      return res.status(400).json({
        status: 'error',
        message: '用户名或密码错误'
      })
    }

    if (!user.password) {
      return res.status(400).json({
        status: 'error',
        message: '该账号未设置密码，请使用其他登录方式'
      })
    }

    const isValidPassword = await bcrypt.compare(password, user.password)
    if (!isValidPassword) {
      return res.status(400).json({
        status: 'error',
        message: '用户名或密码错误'
      })
    }

    await prisma.user.update({
      where: { id: user.id },
      data: { lastLoginAt: new Date() }
    })

    const token = jwt.sign(
      {
        userId: user.id,
        tenantId: user.tenantId,
        type: 'user'
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    )

    const refreshToken = jwt.sign(
      {
        userId: user.id,
        tenantId: user.tenantId,
        type: 'refresh'
      },
      JWT_SECRET,
      { expiresIn: '30d' }
    )

    return res.json({
      token,
      refreshToken,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        name: user.name,
        phone: user.phone,
        avatar: user.avatar,
        tenantId: user.tenantId,
        tenantName: user.tenant.name
      }
    })
  } catch (error) {
    console.error('用户登录错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '登录失败'
    })
  }
})

router.post('/send-code', async (req: Request, res: Response) => {
  try {
    const { email, type } = req.body

    if (!email) {
      return res.status(400).json({
        status: 'error',
        message: '邮箱地址不能为空'
      })
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        status: 'error',
        message: '邮箱格式不正确'
      })
    }

    if (type === 'register') {
      const existingUser = await prisma.admin.findUnique({
        where: { email }
      })
      if (existingUser) {
        return res.status(400).json({
          status: 'error',
          message: '该邮箱已被注册'
        })
      }
    }

    const result = await VerificationCodeService.sendCode(email)

    return res.json({
      status: 'success',
      message: result.message,
      code: result.code
    })
  } catch (error) {
    console.error('发送验证码错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '发送验证码失败'
    })
  }
})

router.post('/register', async (req: Request, res: Response) => {
  try {
    const { email, password, code, username } = req.body

    if (!email || !password || !code) {
      return res.status(400).json({
        status: 'error',
        message: '邮箱、密码和验证码不能为空'
      })
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      return res.status(400).json({
        status: 'error',
        message: '邮箱格式不正确'
      })
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/
    if (!passwordRegex.test(password)) {
      return res.status(400).json({
        status: 'error',
        message: '密码必须至少8位，包含大小写字母和数字'
      })
    }

    const isValidCode = await VerificationCodeService.verifyCode(email, code)
    if (!isValidCode) {
      return res.status(400).json({
        status: 'error',
        message: '验证码错误或已过期'
      })
    }

    const existingUser = await prisma.admin.findUnique({
      where: { email }
    })
    if (existingUser) {
      return res.status(400).json({
        status: 'error',
        message: '该邮箱已被注册'
      })
    }

    const hashedPassword = await bcrypt.hash(password, 10)
    const newUsername = username || email.split('@')[0]

    const result = await prisma.$transaction(async (tx) => {
      const tenant = await tx.tenant.create({
        data: {
          name: `${newUsername}的租户`,
          status: 'active',
          plan: 'basic'
        }
      })

      const user = await tx.admin.create({
        data: {
          id: `admin-${Date.now()}`,
          currentTenantId: tenant.id,
          username: newUsername,
          email,
          password: hashedPassword,
          status: 'active'
        }
      })

      await tx.adminTenant.create({
        data: {
          adminId: user.id,
          tenantId: tenant.id,
          role: 'owner'
        }
      })

      const { superAdmin } = await ensureDefaultAdminRoles(tenant.id, {
        db: tx,
        includeReadOnly: true
      })

      await tx.admin.update({
        where: { id: user.id },
        data: {
          roles: {
            connect: { id: superAdmin.id }
          }
        }
      })

      return { user, tenant }
    })

    const token = jwt.sign(
      { 
        userId: result.user.id,
        type: 'admin'
      },
      JWT_SECRET,
      { expiresIn: '7d' }
    )

    const refreshToken = jwt.sign(
      { 
        userId: result.user.id, 
        type: 'refresh'
      },
      JWT_SECRET,
      { expiresIn: '30d' }
    )

    return res.json({
      token,
      refreshToken,
      user: {
        id: result.user.id,
        username: result.user.username,
        email: result.user.email,
        avatar: null,
        currentTenantId: result.tenant.id
      },
      tenants: [{
        id: result.tenant.id,
        name: result.tenant.name,
        role: 'owner'
      }]
    })
  } catch (error) {
    console.error('注册错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '注册失败'
    })
  }
})

router.post('/passkey/start', async (req: Request, res: Response) => {
  try {
    return res.status(501).json({
      status: 'error',
      message: 'Passkey 功能暂未实现'
    })
  } catch (error) {
    return res.status(500).json({
      status: 'error',
      message: 'Passkey 登录失败'
    })
  }
})

router.post('/passkey/finish', async (req: Request, res: Response) => {
  try {
    return res.status(501).json({
      status: 'error',
      message: 'Passkey 功能暂未实现'
    })
  } catch (error) {
    return res.status(500).json({
      status: 'error',
      message: 'Passkey 登录失败'
    })
  }
})

router.get('/social/:provider/url', async (req: Request, res: Response) => {
  try {
    return res.status(501).json({
      status: 'error',
      message: '第三方登录功能暂未实现'
    })
  } catch (error) {
    return res.status(500).json({
      status: 'error',
      message: '获取授权链接失败'
    })
  }
})

router.post('/social', async (req: Request, res: Response) => {
  try {
    return res.status(501).json({
      status: 'error',
      message: '第三方登录功能暂未实现'
    })
  } catch (error) {
    return res.status(500).json({
      status: 'error',
      message: '第三方登录失败'
    })
  }
})

router.post('/refresh', async (req: Request, res: Response) => {
  try {
    const { refreshToken } = req.body
    
    return res.status(501).json({
      status: 'error',
      message: 'Token 刷新功能暂未实现'
    })
  } catch (error) {
    return res.status(500).json({
      status: 'error',
      message: 'Token 刷新失败'
    })
  }
})

router.post('/logout', async (req: Request, res: Response) => {
  try {
    return res.json({
      status: 'success',
      message: '登出成功'
    })
  } catch (error) {
    return res.status(500).json({
      status: 'error',
      message: '登出失败'
    })
  }
})

export default router
