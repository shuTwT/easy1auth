import { Router, Request, Response } from 'express'
import jwt from 'jsonwebtoken'
import prisma from '../lib/prisma'
import { authMiddleware, AuthRequest } from '../middleware/auth'

const router = Router()

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key-change-in-production'

router.get('/list', authMiddleware, async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const userId = authReq.userId

    if (!userId) {
      return res.status(401).json({
        status: 'error',
        message: '未授权'
      })
    }

    const adminTenants = await prisma.adminTenant.findMany({
      where: { adminId: userId },
      include: {
        tenant: true
      }
    })

    const tenants = adminTenants.map(at => ({
      id: at.tenant.id,
      name: at.tenant.name,
      logo: at.tenant.logo,
      status: at.tenant.status,
      plan: at.tenant.plan,
      role: at.role,
      createdAt: at.tenant.createdAt
    }))

    return res.json({
      status: 'success',
      tenants
    })
  } catch (error) {
    console.error('获取租户列表错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '获取租户列表失败'
    })
  }
})

router.post('/create', authMiddleware, async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const userId = authReq.userId
    const { name, plan = 'basic' } = req.body

    if (!userId) {
      return res.status(401).json({
        status: 'error',
        message: '未授权'
      })
    }

    if (!name) {
      return res.status(400).json({
        status: 'error',
        message: '租户名称不能为空'
      })
    }

    const result = await prisma.$transaction(async (tx) => {
      const tenant = await tx.tenant.create({
        data: {
          name,
          status: 'active',
          plan
        }
      })

      await tx.adminTenant.create({
        data: {
          adminId: userId,
          tenantId: tenant.id,
          role: 'owner'
        }
      })

      return tenant
    })

    return res.json({
      status: 'success',
      message: '租户创建成功',
      tenant: {
        id: result.id,
        name: result.name,
        status: result.status,
        plan: result.plan,
        role: 'owner'
      }
    })
  } catch (error) {
    console.error('创建租户错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '创建租户失败'
    })
  }
})

router.get('/current', authMiddleware, async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const userId = authReq.userId
    const tenantId = authReq.tenantId

    if (!userId || !tenantId) {
      return res.status(401).json({
        status: 'error',
        message: '未授权'
      })
    }

    const adminTenant = await prisma.adminTenant.findFirst({
      where: {
        adminId: userId,
        tenantId
      },
      include: {
        tenant: true
      }
    })

    if (!adminTenant) {
      return res.status(404).json({
        status: 'error',
        message: '租户不存在'
      })
    }

    return res.json({
      status: 'success',
      tenant: {
        id: adminTenant.tenant.id,
        name: adminTenant.tenant.name,
        logo: adminTenant.tenant.logo,
        status: adminTenant.tenant.status,
        plan: adminTenant.tenant.plan,
        role: adminTenant.role
      }
    })
  } catch (error) {
    console.error('获取当前租户错误:', error)
    return res.status(500).json({
      status: 'error',
      message: '获取当前租户失败'
    })
  }
})

export default router
