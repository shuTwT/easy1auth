import { Router, Request, Response } from 'express'
import { CustomDomainService } from '../services/customDomain.service'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { AppError } from '../middleware/errorHandler'

const router = Router()
const customDomainService = new CustomDomainService()

router.use(authMiddleware)

router.get('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const domains = await customDomainService.listDomains(tenantId)
    res.json({
      status: 'success',
      data: domains,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('获取自定义域名列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取自定义域名列表失败',
      })
    }
  }
})

router.post('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const domain = await customDomainService.createDomain(tenantId, req.body)
    res.status(201).json({
      status: 'success',
      message: '域名添加成功，请完成验证',
      data: domain,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('添加自定义域名错误:', error)
      res.status(500).json({
        status: 'error',
        message: '添加自定义域名失败',
      })
    }
  }
})

router.post('/:id/verify', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const domain = await customDomainService.verifyDomain(tenantId, id)
    res.json({
      status: 'success',
      message: '域名验证成功',
      data: domain,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('验证域名错误:', error)
      res.status(500).json({
        status: 'error',
        message: '验证域名失败',
      })
    }
  }
})

router.put('/:id/ssl', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const domain = await customDomainService.updateSSL(tenantId, id, req.body)
    res.json({
      status: 'success',
      message: 'SSL证书配置成功',
      data: domain,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('配置SSL证书错误:', error)
      res.status(500).json({
        status: 'error',
        message: '配置SSL证书失败',
      })
    }
  }
})

router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    await customDomainService.deleteDomain(tenantId, id)
    res.json({
      status: 'success',
      message: '域名删除成功',
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('删除域名错误:', error)
      res.status(500).json({
        status: 'error',
        message: '删除域名失败',
      })
    }
  }
})

export default router
