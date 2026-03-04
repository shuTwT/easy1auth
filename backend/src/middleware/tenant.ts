import { Request, Response, NextFunction } from 'express'
import { AppError } from './errorHandler'
import prisma from '../lib/prisma'

export interface TenantRequest extends Request {
  tenantId?: string
  userId?: string
  tenant?: any
}

export const tenantMiddleware = async (
  req: TenantRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    if (!req.tenantId) {
      throw new AppError('租户信息缺失', 400)
    }

    const tenant = await prisma.tenant.findUnique({
      where: { id: req.tenantId }
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    if (tenant.status === 'suspended') {
      throw new AppError('租户已被暂停', 403)
    }

    if (tenant.status === 'deleted') {
      throw new AppError('租户已被删除', 403)
    }

    req.tenant = tenant

    next()
  } catch (error) {
    next(error)
  }
}

export const tenantDataFilter = (req: TenantRequest, res: Response, next: NextFunction) => {
  if (!req.tenantId) {
    return next(new AppError('租户信息缺失', 400))
  }

  req.body.tenantId = req.tenantId
  
  next()
}

export const checkTenantLimits = (resourceType: 'users' | 'apps') => {
  return async (req: TenantRequest, res: Response, next: NextFunction) => {
    try {
      if (!req.tenantId) {
        throw new AppError('租户信息缺失', 400)
      }

      const tenant = await prisma.tenant.findUnique({
        where: { id: req.tenantId }
      })

      if (!tenant) {
        throw new AppError('租户不存在', 404)
      }

      if (resourceType === 'users') {
        const currentCount = await prisma.user.count({
          where: { tenantId: req.tenantId }
        })
        
        if (currentCount >= tenant.maxUsers) {
          throw new AppError('已达到用户数量上限', 403)
        }
      }

      if (resourceType === 'apps') {
        const currentCount = await prisma.application.count({
          where: { tenantId: req.tenantId }
        })
        
        if (currentCount >= tenant.maxApps) {
          throw new AppError('已达到应用数量上限', 403)
        }
      }

      next()
    } catch (error) {
      next(error)
    }
  }
}
