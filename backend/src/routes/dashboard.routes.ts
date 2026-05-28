import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import dashboardService from '../services/dashboard.service'
import { AppError } from '../middleware/errorHandler'

const router = Router()

router.use(authMiddleware)

router.get('/stats', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const data = await dashboardService.getDashboardData(tenantId)

    res.json({
      status: 'success',
      data
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取控制台数据错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取控制台数据失败'
      })
    }
  }
})

export default router
