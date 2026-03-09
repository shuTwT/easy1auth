import { Router, Request, Response } from 'express'
import bcrypt from 'bcryptjs'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { 
  validatePassword, 
  checkPasswordHistory, 
  addPasswordToHistory, 
  checkPasswordExpiry,
  getPasswordPolicy 
} from '../services/passwordPolicy.service'
import {
  setupMfa,
  enableMfa,
  disableMfa,
  verifyMfa,
  sendMfaEmailCode,
  verifyMfaEmailCode
} from '../services/mfa.service'
import prisma from '../lib/prisma'

const router = Router()

router.get('/password-policy', authMiddleware, async (req: Request, res: Response) => {
  try {
    const policy = getPasswordPolicy()
    const authReq = req as AuthRequest
    const expiryStatus = await checkPasswordExpiry(authReq.userId!)

    res.json({
      status: 'success',
      policy,
      expiryStatus
    })
  } catch (error) {
    console.error('获取密码策略错误:', error)
    res.status(500).json({
      status: 'error',
      message: '获取密码策略失败'
    })
  }
})

router.post('/change-password', authMiddleware, async (req: Request, res: Response) => {
  try {
    const { currentPassword, newPassword, confirmPassword } = req.body
    const authReq = req as AuthRequest
    const userId = authReq.userId

    if (!currentPassword || !newPassword || !confirmPassword) {
      return res.status(400).json({
        status: 'error',
        message: '请填写所有密码字段'
      })
    }

    if (newPassword !== confirmPassword) {
      return res.status(400).json({
        status: 'error',
        message: '两次输入的新密码不一致'
      })
    }

    const user = await prisma.admin.findUnique({
      where: { id: userId }
    })

    if (!user) {
      return res.status(404).json({
        status: 'error',
        message: '用户不存在'
      })
    }

    const isValidPassword = await bcrypt.compare(currentPassword, user.password)
    if (!isValidPassword) {
      return res.status(400).json({
        status: 'error',
        message: '当前密码错误'
      })
    }

    const policy = getPasswordPolicy()
    const validation = validatePassword(newPassword, policy)
    if (!validation.valid) {
      return res.status(400).json({
        status: 'error',
        message: validation.errors.join(', ')
      })
    }

    const isNotReused = await checkPasswordHistory(userId!, newPassword, policy.historyCount)
    if (!isNotReused) {
      return res.status(400).json({
        status: 'error',
        message: `不能使用最近${policy.historyCount}次使用过的密码`
      })
    }

    const hashedPassword = await bcrypt.hash(newPassword, 10)
    
    await prisma.admin.update({
      where: { id: userId },
      data: {
        password: hashedPassword,
        passwordChangedAt: new Date()
      }
    })
    
    await addPasswordToHistory(userId!, user.password)

    res.json({
      status: 'success',
      message: '密码修改成功'
    })
  } catch (error) {
    console.error('修改密码错误:', error)
    res.status(500).json({
      status: 'error',
      message: '修改密码失败'
    })
  }
})

router.get('/mfa/status', authMiddleware, async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const user = await prisma.admin.findUnique({
      where: { id: authReq.userId },
      select: {
        mfaEnabled: true,
        mfaType: true
      }
    })

    res.json({
      status: 'success',
      mfa: {
        enabled: user?.mfaEnabled || false,
        type: user?.mfaType || null
      }
    })
  } catch (error) {
    console.error('获取MFA状态错误:', error)
    res.status(500).json({
      status: 'error',
      message: '获取MFA状态失败'
    })
  }
})

router.post('/mfa/setup', authMiddleware, async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const user = await prisma.admin.findUnique({
      where: { id: authReq.userId }
    })

    if (!user) {
      return res.status(404).json({
        status: 'error',
        message: '用户不存在'
      })
    }

    const result = await setupMfa(authReq.userId!, user.email)

    res.json({
      status: 'success',
      data: result
    })
  } catch (error) {
    console.error('设置MFA错误:', error)
    res.status(500).json({
      status: 'error',
      message: '设置MFA失败'
    })
  }
})

router.post('/mfa/enable', authMiddleware, async (req: Request, res: Response) => {
  try {
    const { token } = req.body
    const authReq = req as AuthRequest

    if (!token) {
      return res.status(400).json({
        status: 'error',
        message: '请输入验证码'
      })
    }

    const result = await enableMfa(authReq.userId!, token)

    res.json({
      status: result.success ? 'success' : 'error',
      message: result.message
    })
  } catch (error) {
    console.error('启用MFA错误:', error)
    res.status(500).json({
      status: 'error',
      message: '启用MFA失败'
    })
  }
})

router.post('/mfa/disable', authMiddleware, async (req: Request, res: Response) => {
  try {
    const { token } = req.body
    const authReq = req as AuthRequest

    if (!token) {
      return res.status(400).json({
        status: 'error',
        message: '请输入验证码'
      })
    }

    const result = await disableMfa(authReq.userId!, token)

    res.json({
      status: result.success ? 'success' : 'error',
      message: result.message
    })
  } catch (error) {
    console.error('禁用MFA错误:', error)
    res.status(500).json({
      status: 'error',
      message: '禁用MFA失败'
    })
  }
})

router.post('/mfa/verify', authMiddleware, async (req: Request, res: Response) => {
  try {
    const { token, type } = req.body
    const authReq = req as AuthRequest

    if (!token) {
      return res.status(400).json({
        status: 'error',
        message: '请输入验证码'
      })
    }

    let result
    if (type === 'email') {
      result = await verifyMfaEmailCode(authReq.userId!, token)
    } else {
      result = await verifyMfa(authReq.userId!, token)
    }

    res.json({
      status: result.success ? 'success' : 'error',
      message: result.message
    })
  } catch (error) {
    console.error('验证MFA错误:', error)
    res.status(500).json({
      status: 'error',
      message: '验证MFA失败'
    })
  }
})

router.post('/mfa/send-email-code', authMiddleware, async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const result = await sendMfaEmailCode(authReq.userId!)

    res.json({
      status: result.success ? 'success' : 'error',
      message: result.message
    })
  } catch (error) {
    console.error('发送MFA邮箱验证码错误:', error)
    res.status(500).json({
      status: 'error',
      message: '发送验证码失败'
    })
  }
})

export default router
