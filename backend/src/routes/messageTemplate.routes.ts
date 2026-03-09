import { Router, Request, Response } from 'express'
import { MessageTemplateService } from '../services/messageTemplate.service'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import { AppError } from '../middleware/errorHandler'

const router = Router()
const messageTemplateService = new MessageTemplateService()

router.use(authMiddleware)

router.get('/', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId
    const { type } = req.query

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const templates = await messageTemplateService.listTemplates(
      tenantId,
      type as 'email' | 'sms' | undefined
    )
    res.json({
      status: 'success',
      data: templates,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('获取消息模板列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取消息模板列表失败',
      })
    }
  }
})

router.post('/init', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    await messageTemplateService.initDefaultTemplates(tenantId)
    res.json({
      status: 'success',
      message: '默认模板初始化成功',
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('初始化默认模板错误:', error)
      res.status(500).json({
        status: 'error',
        message: '初始化默认模板失败',
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

    const template = await messageTemplateService.getTemplate(tenantId, id)
    res.json({
      status: 'success',
      data: template,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('获取消息模板错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取消息模板失败',
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

    const template = await messageTemplateService.createTemplate(tenantId, req.body)
    res.status(201).json({
      status: 'success',
      message: '模板创建成功',
      data: template,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('创建消息模板错误:', error)
      res.status(500).json({
        status: 'error',
        message: '创建消息模板失败',
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

    const template = await messageTemplateService.updateTemplate(tenantId, id, req.body)
    res.json({
      status: 'success',
      message: '模板更新成功',
      data: template,
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('更新消息模板错误:', error)
      res.status(500).json({
        status: 'error',
        message: '更新消息模板失败',
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

    await messageTemplateService.deleteTemplate(tenantId, id)
    res.json({
      status: 'success',
      message: '模板删除成功',
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message,
      })
    } else {
      console.error('删除消息模板错误:', error)
      res.status(500).json({
        status: 'error',
        message: '删除消息模板失败',
      })
    }
  }
})

export default router
