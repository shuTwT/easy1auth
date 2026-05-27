import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import permissionService from '../services/permission.service'
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

    const stats = await permissionService.getStats(tenantId)
    res.json({ status: 'success', data: stats })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('获取权限统计错误:', error)
      res.status(500).json({ status: 'error', message: '获取权限统计失败' })
    }
  }
})

router.get('/tree', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const tree = await permissionService.getTree(tenantId)
    res.json({ status: 'success', data: tree })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('获取权限树错误:', error)
      res.status(500).json({ status: 'error', message: '获取权限树失败' })
    }
  }
})

router.get('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const query = {
      page: parseInt(req.query.page as string) || 1,
      pageSize: parseInt(req.query.pageSize as string) || 50,
      search: req.query.search as string,
      type: req.query.type as any,
      resource: req.query.resource as string,
    }

    const result = await permissionService.findAll(tenantId, query)
    res.json({ status: 'success', data: result })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('获取权限列表错误:', error)
      res.status(500).json({ status: 'error', message: '获取权限列表失败' })
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

    const permission = await permissionService.findById(tenantId, req.params.id as string)
    res.json({ status: 'success', data: permission })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('获取权限详情错误:', error)
      res.status(500).json({ status: 'error', message: '获取权限详情失败' })
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

    const permission = await permissionService.create(tenantId, req.body)
    res.status(201).json({ status: 'success', data: permission, message: '创建权限成功' })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('创建权限错误:', error)
      res.status(500).json({ status: 'error', message: '创建权限失败' })
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

    const permission = await permissionService.update(tenantId, req.params.id as string, req.body)
    res.json({ status: 'success', data: permission, message: '更新权限成功' })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('更新权限错误:', error)
      res.status(500).json({ status: 'error', message: '更新权限失败' })
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

    await permissionService.delete(tenantId, req.params.id as string)
    res.json({ status: 'success', message: '删除权限成功' })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ status: 'error', message: error.message })
    } else {
      console.error('删除权限错误:', error)
      res.status(500).json({ status: 'error', message: '删除权限失败' })
    }
  }
})

export default router
