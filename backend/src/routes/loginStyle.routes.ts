import { Router, Request, Response } from 'express'
import { LoginStyleService } from '../services/loginStyle.service'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { AppError } from '../middleware/errorHandler'

const router = Router()
const loginStyleService = new LoginStyleService()

router.get('/public', async (req: Request, res: Response) => {
  try {
    const { domain } = req.query
    const style = await loginStyleService.getPublicLoginStyle(domain as string)
    res.json({
      status: 'success',
      data: style,
    })
  } catch (error) {
    console.error('获取公开登录样式错误:', error)
    res.status(500).json({
      status: 'error',
      message: '获取登录样式失败',
    })
  }
})

router.use(authMiddleware)

router.get('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const style = await loginStyleService.getLoginStyle(tenantId)
    res.json({
      status: 'success',
      data: style,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('获取登录样式错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取登录样式失败',
      })
    }
  }
})

router.put('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const style = await loginStyleService.updateLoginStyle(tenantId, req.body)
    res.json({
      status: 'success',
      message: '登录样式更新成功',
      data: style,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('更新登录样式错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新登录样式失败',
      })
    }
  }
})

export default router
