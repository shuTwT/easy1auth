import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import positionService from '../services/position.service'
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
      code: req.query.code as string,
      departmentId: req.query.departmentId as string,
      level: req.query.level ? parseInt(req.query.level as string) : undefined
    }

    const result = await positionService.findAll(tenantId, query)

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
      console.error('获取岗位列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取岗位列表失败'
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

    const result = await positionService.getStats(tenantId)

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
      console.error('获取岗位统计错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取岗位统计失败'
      })
    }
  }
})

router.get('/:id', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const result = await positionService.findById(tenantId, req.params.id as string)

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
      console.error('获取岗位详情错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取岗位详情失败'
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

    const { name, code, description, departmentId, level, sequence, maxCount } = req.body

    if (!name || !code) {
      throw new AppError('岗位名称和编码为必填项', 400)
    }

    const result = await positionService.create(tenantId, {
      name,
      code,
      description,
      departmentId,
      level,
      sequence,
      maxCount
    })

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
      console.error('创建岗位错误:', error)
      res.status(500).json({
        status: 'error',
        message: '创建岗位失败'
      })
    }
  }
})

router.put('/:id', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { name, code, description, departmentId, level, sequence, maxCount } = req.body

    const result = await positionService.update(tenantId, req.params.id as string, {
      name,
      code,
      description,
      departmentId,
      level,
      sequence,
      maxCount
    })

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
      console.error('更新岗位错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新岗位失败'
      })
    }
  }
})

router.delete('/:id', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    await positionService.delete(tenantId, req.params.id as string)

    res.json({
      status: 'success',
      message: '删除成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('删除岗位错误:', error)
      res.status(500).json({
        status: 'error',
        message: '删除岗位失败'
      })
    }
  }
})

export default router
