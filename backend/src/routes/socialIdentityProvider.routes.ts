import { Router, Request, Response } from 'express'
import { SocialIdentityProviderService } from '../services/socialIdentityProvider.service'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { AppError } from '../middleware/errorHandler'

const router = Router()
const providerService = new SocialIdentityProviderService()

router.use(authMiddleware)

router.get('/stats', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const stats = await providerService.getStats(tenantId)
    res.json(stats)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取统计信息失败' })
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

    const { type, status } = req.query
    const providers = await providerService.findAll(tenantId, {
      type: type as string,
      status: status as string,
    })
    res.json(providers)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取身份源列表失败' })
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

    const provider = await providerService.findById(tenantId, req.params.id as string)
    res.json(provider)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取身份源失败' })
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

    const provider = await providerService.create(tenantId, req.body)
    res.status(201).json(provider)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '创建身份源失败' })
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

    const provider = await providerService.update(tenantId, req.params.id as string, req.body)
    res.json(provider)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '更新身份源失败' })
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

    await providerService.delete(tenantId, req.params.id as string)
    res.status(204).send()
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '删除身份源失败' })
    }
  }
})

router.post('/:type/authorize', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { redirectUri } = req.body
    const result = await providerService.getAuthorizeUrl(tenantId, req.params.type as string, redirectUri)
    res.json(result)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : '获取授权URL失败' })
    }
  }
})

router.post('/:type/callback', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const { code, state, redirectUri } = req.body
    const result = await providerService.handleCallback(
      tenantId,
      req.params.type as string,
      { code, state },
      redirectUri
    )
    res.json(result)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({ error: error.message })
    } else {
      res.status(500).json({ error: error instanceof Error ? error.message : 'OAuth回调处理失败' })
    }
  }
})

export default router
