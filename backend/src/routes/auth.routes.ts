import { Router, Request, Response } from 'express'
import bcrypt from 'bcryptjs'
import jwt from 'jsonwebtoken'
import prisma from '../lib/prisma'
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
        }
      })

      if (!user) {
        return res.status(401).json({
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
        return res.status(401).json({
          status: 'error',
          message: '用户名或密码错误'
        })
      }

      const token = jwt.sign(
        { 
          userId: user.id, 
          tenantId: user.tenantId,
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
          avatar: null
        }
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

    const defaultTenant = await prisma.tenant.findFirst({
      where: { id: 'default-tenant' }
    })

    if (!defaultTenant) {
      return res.status(500).json({
        status: 'error',
        message: '系统配置错误：默认租户不存在'
      })
    }

    const hashedPassword = await bcrypt.hash(password, 10)
    const newUsername = username || email.split('@')[0]

    const user = await prisma.admin.create({
      data: {
        id: `admin-${Date.now()}`,
        tenantId: defaultTenant.id,
        username: newUsername,
        email,
        password: hashedPassword,
        status: 'active'
      }
    })

    const token = jwt.sign(
      { 
        userId: user.id, 
        tenantId: user.tenantId,
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
        avatar: null
      }
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
