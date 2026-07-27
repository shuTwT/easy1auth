import jwt, { JsonWebTokenError, type JwtPayload } from 'jsonwebtoken'
import type { NextFunction, Request, Response } from 'express'
import { AppError } from './errorHandler'
import prisma from '../lib/prisma'

const JWT_SECRET = process.env.JWT_SECRET
if (!JWT_SECRET) {
  throw new Error('JWT_SECRET environment variable is required')
}

export interface AdminManagementContext {
  readonly adminId: string
  readonly tenantId: string
  readonly isOwner: boolean
  readonly permissions: readonly string[]
}

export interface AdminManagementRequest extends Request {
  adminContext?: AdminManagementContext
}

type AdminTokenPayload = {
  readonly userId: string
  readonly type: 'admin'
}

const isAdminTokenPayload = (
  decoded: string | JwtPayload
): decoded is AdminTokenPayload => {
  if (typeof decoded === 'string') {
    return false
  }

  return typeof decoded.userId === 'string' && decoded.type === 'admin'
}

export const adminManagementAuth = (
  requiredPermissionCodes: readonly string[] = []
) => {
  return async (
    req: AdminManagementRequest,
    _res: Response,
    next: NextFunction
  ) => {
    try {
      const authHeader = req.headers.authorization
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        throw new AppError('未提供认证令牌', 401)
      }

      const token = authHeader.substring(7).trim()
      if (!token) {
        throw new AppError('未提供认证令牌', 401)
      }

      let decoded: string | JwtPayload
      try {
        decoded = jwt.verify(token, JWT_SECRET)
      } catch (error) {
        if (error instanceof JsonWebTokenError) {
          throw new AppError('无效的认证令牌', 401)
        }

        throw error
      }

      if (!isAdminTokenPayload(decoded)) {
        throw new AppError('无效的认证令牌', 401)
      }

      const admin = await prisma.admin.findUnique({
        where: { id: decoded.userId }
      })

      if (!admin) {
        throw new AppError('管理员不存在', 404)
      }

      if (admin.status !== 'active') {
        throw new AppError('管理员账号已被禁用', 403)
      }

      const tenantHeader = req.headers['tenant-id']
      const tenantId = typeof tenantHeader === 'string' ? tenantHeader.trim() : ''
      if (!tenantId) {
        throw new AppError('租户信息缺失', 400)
      }

      const adminTenant = await prisma.adminTenant.findUnique({
        where: {
          adminId_tenantId: {
            adminId: admin.id,
            tenantId
          }
        },
        include: { tenant: true }
      })

      if (!adminTenant) {
        throw new AppError('无权访问该租户', 403)
      }

      if (adminTenant.tenant.status === 'suspended') {
        throw new AppError('租户已被暂停', 403)
      }

      if (adminTenant.tenant.status === 'deleted') {
        throw new AppError('租户已被删除', 403)
      }

      const roles = await prisma.adminRole.findMany({
        where: {
          tenantId,
          admins: { some: { id: admin.id } }
        },
        select: { permissions: true }
      })

      const permissions = [
        ...new Set(
          roles.flatMap(({ permissions: rolePermissions }) => {
            if (!Array.isArray(rolePermissions)) {
              return []
            }

            return rolePermissions.filter(
              (permission: unknown): permission is string =>
                typeof permission === 'string'
            )
          })
        )
      ]
      const isOwner = adminTenant.role === 'owner'
      const hasWildcard = permissions.includes('*')
      const hasRequiredPermissions = requiredPermissionCodes.every((code) =>
        permissions.includes(code)
      )

      if (!isOwner && !hasWildcard && !hasRequiredPermissions) {
        throw new AppError('没有执行此操作的权限', 403)
      }

      req.adminContext = {
        adminId: admin.id,
        tenantId,
        isOwner,
        permissions
      }

      next()
    } catch (error) {
      if (error instanceof Error) {
        next(error)
        return
      }

      next(new AppError('服务器内部错误', 500))
    }
  }
}
