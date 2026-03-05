import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import userService from '../services/user.service'
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
      username: req.query.username as string,
      email: req.query.email as string,
      phone: req.query.phone as string,
      name: req.query.name as string,
      status: req.query.status as any,
      department: req.query.department as string
    }

    const result = await userService.findAll(tenantId, query)

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
      console.error('获取用户列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户列表失败'
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

    const { username, email, password, phone, name, avatar, department, position, customAttributes } = req.body

    if (!username || !email || !name) {
      throw new AppError('用户名、邮箱和姓名为必填项', 400)
    }

    const user = await userService.create(tenantId, {
      username,
      email,
      password,
      phone,
      name,
      avatar,
      department,
      position,
      customAttributes
    })

    res.json({
      status: 'success',
      message: '用户创建成功',
      data: user
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('创建用户错误:', error)
      res.status(500).json({
        status: 'error',
        message: '创建用户失败'
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

    const stats = await userService.getStats(tenantId)

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
      console.error('获取用户统计错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户统计失败'
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

    const user = await userService.findById(tenantId, id)

    res.json({
      status: 'success',
      data: user
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取用户详情错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户详情失败'
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

    const { username, email, phone, name, avatar, department, position, status, customAttributes } = req.body

    const user = await userService.update(tenantId, id, {
      username,
      email,
      phone,
      name,
      avatar,
      department,
      position,
      status,
      customAttributes
    })

    res.json({
      status: 'success',
      message: '用户更新成功',
      data: user
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('更新用户错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新用户失败'
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

    await userService.delete(tenantId, id)

    res.json({
      status: 'success',
      message: '用户删除成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('删除用户错误:', error)
      res.status(500).json({
        status: 'error',
        message: '删除用户失败'
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

    const user = await userService.updateStatus(tenantId, id, status)

    res.json({
      status: 'success',
      message: '状态更新成功',
      data: user
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('更新用户状态错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新用户状态失败'
      })
    }
  }
})

router.post('/:id/reset-password', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string
    const { newPassword } = req.body

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    if (!newPassword) {
      throw new AppError('新密码不能为空', 400)
    }

    await userService.resetPassword(tenantId, id, { newPassword })

    res.json({
      status: 'success',
      message: '密码重置成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('重置密码错误:', error)
      res.status(500).json({
        status: 'error',
        message: '重置密码失败'
      })
    }
  }
})

router.post('/:id/change-password', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string
    const { oldPassword, newPassword } = req.body

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    if (!oldPassword || !newPassword) {
      throw new AppError('原密码和新密码不能为空', 400)
    }

    await userService.changePassword(tenantId, id, { oldPassword, newPassword })

    res.json({
      status: 'success',
      message: '密码修改成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('修改密码错误:', error)
      res.status(500).json({
        status: 'error',
        message: '修改密码失败'
      })
    }
  }
})

router.get('/:id/roles', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const roles = await userService.getUserRoles(tenantId, id)

    res.json({
      status: 'success',
      data: roles
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取用户角色错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户角色失败'
      })
    }
  }
})

router.post('/:id/roles', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string
    const { roleIds } = req.body

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    if (!Array.isArray(roleIds)) {
      throw new AppError('角色ID列表格式错误', 400)
    }

    await userService.assignRoles(tenantId, id, { roleIds })

    res.json({
      status: 'success',
      message: '角色分配成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('分配角色错误:', error)
      res.status(500).json({
        status: 'error',
        message: '分配角色失败'
      })
    }
  }
})

router.get('/:id/groups', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const groups = await userService.getUserGroups(tenantId, id)

    res.json({
      status: 'success',
      data: groups
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取用户组错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取用户组失败'
      })
    }
  }
})

router.post('/:id/groups', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const id = req.params.id as string
    const { groupIds } = req.body

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    if (!Array.isArray(groupIds)) {
      throw new AppError('用户组ID列表格式错误', 400)
    }

    await userService.assignGroups(tenantId, id, { groupIds })

    res.json({
      status: 'success',
      message: '用户组分配成功'
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('分配用户组错误:', error)
      res.status(500).json({
        status: 'error',
        message: '分配用户组失败'
      })
    }
  }
})

export default router
