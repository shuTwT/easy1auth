import { Router, Request, Response } from 'express'
import { BrandSettingsService } from '../services/brandSettings.service'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { AppError } from '../middleware/errorHandler'

const router = Router()
const brandSettingsService = new BrandSettingsService()

router.use(authMiddleware)

router.get('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const result = await brandSettingsService.getBrandSettings(tenantId)
    res.json({
      status: 'success',
      data: result,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('获取品牌设置错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取品牌设置失败',
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

    if (req.body.customDomain) {
      const isValid = await brandSettingsService.validateCustomDomain(
        tenantId,
        req.body.customDomain
      )
      if (!isValid) {
        throw new AppError('该域名已被使用', 400)
      }
    }

    const result = await brandSettingsService.updateBrandSettings(tenantId, req.body)
    res.json({
      status: 'success',
      message: '品牌设置更新成功',
      data: result,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('更新品牌设置错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新品牌设置失败',
      })
    }
  }
})

router.get('/public', async (req: Request, res: Response) => {
  try {
    const domain = req.query.domain as string
    const result = await brandSettingsService.getPublicBrandSettings(domain)
    res.json({
      status: 'success',
      data: result,
    })
  } catch (error) {
    console.error('获取公开品牌设置错误:', error)
    res.status(500).json({
      status: 'error',
      message: '获取品牌设置失败',
    })
  }
})

export default router
