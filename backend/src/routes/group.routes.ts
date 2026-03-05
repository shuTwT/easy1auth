import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import groupService from '../services/group.service'
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
      parentId: req.query.parentId as string
    }

    const result = await groupService.findAll(tenantId, query)

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
      console.error('获取用户组列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户组列表失败'
      })
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

    const result = await groupService.getTree(tenantId)

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
      console.error('获取用户组树错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户组树失败'
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

    const result = await groupService.getStats(tenantId)

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
      console.error('获取用户组统计错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户组统计失败'
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

    const result = await groupService.findById(tenantId, req.params.id as string)

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
      console.error('获取用户组详情错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户组详情失败'
      })
    }
  }
})

router.get('/:id/members', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const result = await groupService.getMembers(tenantId, req.params.id as string)

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
      console.error('获取用户组成员错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户组成员失败'
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

    const { name, description, type, parentId } = req.body

    if (!name) {
      throw new AppError('用户组名称为必填项', 400)
    }

    const result = await groupService.create(tenantId, {
      name,
      description,
      type,
      parentId
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
      console.error('创建用户组错误:', error)
      res.status(500).json({
        status: 'error',
        message: '创建用户组失败'
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

    const { name, description, type, parentId } = req.body

    const result = await groupService.update(tenantId, req.params.id as string, {
      name,
      description,
      type,
      parentId
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
      console.error('更新用户组错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新用户组失败'
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

    await groupService.delete(tenantId, req.params.id as string)

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
      console.error('删除用户组错误:', error)
      res.status(500).json({
        status: 'error',
        message: '删除用户组失败'
      })
    }
  }
})

router.post('/:id/members', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { userIds } = req.body

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      throw new AppError('用户ID列表为必填项', 400)
    }

    await groupService.addMembers(tenantId, req.params.id as string, { userIds })

    res.json({
      status: 'success',
      message: '添加成员成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('添加成员错误:', error)
      res.status(500).json({
        status: 'error',
        message: '添加成员失败'
      })
    }
  }
})

router.delete('/:id/members', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { userIds } = req.body

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      throw new AppError('用户ID列表为必填项', 400)
    }

    await groupService.removeMembers(tenantId, req.params.id as string, { userIds })

    res.json({
      status: 'success',
      message: '移除成员成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('移除成员错误:', error)
      res.status(500).json({
        status: 'error',
        message: '移除成员失败'
      })
    }
  }
})

router.post('/:id/admins', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { userIds } = req.body

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      throw new AppError('用户ID列表为必填项', 400)
    }

    await groupService.addAdmins(tenantId, req.params.id as string, { userIds })

    res.json({
      status: 'success',
      message: '添加管理员成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('添加管理员错误:', error)
      res.status(500).json({
        status: 'error',
        message: '添加管理员失败'
      })
    }
  }
})

router.delete('/:id/admins', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { userIds } = req.body

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      throw new AppError('用户ID列表为必填项', 400)
    }

    await groupService.removeAdmins(tenantId, req.params.id as string, { userIds })

    res.json({
      status: 'success',
      message: '移除管理员成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('移除管理员错误:', error)
      res.status(500).json({
        status: 'error',
        message: '移除管理员失败'
      })
    }
  }
})

export default router
