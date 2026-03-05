import { Request, Response, NextFunction } from 'express'
import jwt from 'jsonwebtoken'
import { AppError } from './errorHandler'

export interface AuthRequest extends Request {
  userId?: string
  tenantId?: string
}

export const authMiddleware = (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const authHeader = req.headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AppError('未提供认证令牌', 401)
    }

    const token = authHeader.substring(7)
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'secret') as {
      userId: string
      type: string
    }

    req.userId = decoded.userId

    const tenantId = req.headers['tenant-id'] as string
    if (tenantId) {
      req.tenantId = tenantId
    }

    next()
  } catch (error) {
    next(new AppError('无效的认证令牌', 401))
  }
}
