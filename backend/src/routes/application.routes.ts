import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import applicationService from '../services/application.service'
import { AppError } from '../middleware/errorHandler'

const router = Router()

router.use(authMiddleware)

router.get('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const query = {
      page: parseInt(req.query.page as string) || 1,
      pageSize: parseInt(req.query.pageSize as string) || 10,
      name: req.query.name as string,
      type: req.query.type as any,
      status: req.query.status as any
    }

    const result = await applicationService.findAll(tenantId, query)

    res.json({
      status: 'success',
      data: result
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取应用列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取应用列表失败'
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

    const { name, logo, description, type, redirectUris, allowedGrantTypes, accessTokenLifetime, refreshTokenLifetime } = req.body

    if (!name) {
      throw new AppError('应用名称为必填项', 400)
    }

    const application = await applicationService.create(tenantId, {
      name,
      logo,
      description,
      type,
      redirectUris,
      allowedGrantTypes,
      accessTokenLifetime,
      refreshTokenLifetime
    })

    res.json({
      status: 'success',
      message: '应用创建成功',
      data: application
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('创建应用错误:', error)
      res.status(500).json({
        status: 'error',
        message: '创建应用失败'
      })
    }
  }
})

router.get('/stats', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const stats = await applicationService.getStats(tenantId)

    res.json({
      status: 'success',
      data: stats
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取应用统计错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取应用统计失败'
      })
    }
  }
})

router.get('/:id', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const application = await applicationService.findById(tenantId, id)

    res.json({
      status: 'success',
      data: application
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取应用详情错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取应用详情失败'
      })
    }
  }
})

router.put('/:id', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { name, logo, description, type, redirectUris, allowedGrantTypes, accessTokenLifetime, refreshTokenLifetime, status } = req.body

    const application = await applicationService.update(tenantId, id, {
      name,
      logo,
      description,
      type,
      redirectUris,
      allowedGrantTypes,
      accessTokenLifetime,
      refreshTokenLifetime,
      status
    })

    res.json({
      status: 'success',
      message: '应用更新成功',
      data: application
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('更新应用错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新应用失败'
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

    await applicationService.delete(tenantId, id)

    res.json({
      status: 'success',
      message: '应用删除成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('删除应用错误:', error)
      res.status(500).json({
        status: 'error',
        message: '删除应用失败'
      })
    }
  }
})

router.put('/:id/status', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string
    const { status } = req.body

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    if (!status) {
      throw new AppError('状态不能为空', 400)
    }

    const application = await applicationService.updateStatus(tenantId, id, status)

    res.json({
      status: 'success',
      message: '状态更新成功',
      data: application
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('更新应用状态错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新应用状态失败'
      })
    }
  }
})

router.post('/:id/regenerate-secret', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const result = await applicationService.regenerateSecret(tenantId, id)

    res.json({
      status: 'success',
      message: '密钥重新生成成功',
      data: result
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('重新生成密钥错误:', error)
      res.status(500).json({
        status: 'error',
        message: '重新生成密钥失败'
      })
    }
  }
})

export default router
