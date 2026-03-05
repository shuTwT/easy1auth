import { Router, Request, Response } from 'express'
import { RoleService } from '../services/role.service'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { AppError } from '../middleware/errorHandler'

const router = Router()
const roleService = new RoleService()

router.use(authMiddleware)

router.get('/stats', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const stats = await roleService.getStats(tenantId)
    res.json(stats)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取统计信息失败' })
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

    const tree = await roleService.getTree(tenantId)
    res.json(tree)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取角色树失败' })
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

    const { search, type } = req.query
    const roles = await roleService.findAll(tenantId, {
      search: search as string,
      type: type as string,
    })
    res.json(roles)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取角色列表失败' })
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

    const role = await roleService.findById(tenantId, req.params.id as string)
    res.json(role)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取角色失败' })
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

    const role = await roleService.create(tenantId, req.body)
    res.status(201).json(role)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '创建角色失败' })
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

    const role = await roleService.update(tenantId, req.params.id as string, req.body)
    res.json(role)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '更新角色失败' })
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

    await roleService.delete(tenantId, req.params.id as string)
    res.status(204).send()
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '删除角色失败' })
    }
  }
})

router.get('/:id/users', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { search } = req.query
    const result = await roleService.getRoleUsers(tenantId, req.params.id as string, {
      search: search as string,
    })
    res.json(result)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取角色用户失败' })
    }
  }
})

router.post('/:id/users', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    await roleService.assignUsers(tenantId, req.params.id as string, req.body)
    res.status(200).json({ message: '分配用户成功' })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '分配用户失败' })
    }
  }
})

router.delete('/:id/users', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    await roleService.removeUsers(tenantId, req.params.id as string, req.body)
    res.status(200).json({ message: '移除用户成功' })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '移除用户失败' })
    }
  }
})

router.get('/user/:userId', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const roles = await roleService.getUserRoles(tenantId, req.params.userId as string)
    res.json(roles)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取用户角色失败' })
    }
  }
})

router.post('/user/:userId', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { roleIds } = req.body
    await roleService.assignRolesToUser(tenantId, req.params.userId as string, roleIds)
    res.status(200).json({ message: '分配角色成功' })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '分配角色失败' })
    }
  }
})

export default router
