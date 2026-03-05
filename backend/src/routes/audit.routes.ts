import { Router, Request, Response } from 'express'
import { authMiddleware, AuthRequest } from '../middleware/auth'
import auditService from '../services/audit.service'
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
      pageSize: parseInt(req.query.pageSize as string) || 20,
      userId: req.query.userId as string,
      username: req.query.username as string,
      type: req.query.type as any,
      action: req.query.action as string,
      resource: req.query.resource as string,
      resourceId: req.query.resourceId as string,
      status: req.query.status as any,
      startDate: req.query.startDate as string,
      endDate: req.query.endDate as string,
      ip: req.query.ip as string
    }

    const result = await auditService.findAll(tenantId, query)

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
      console.error('获取审计日志列表错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取审计日志列表失败'
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

    const stats = await auditService.getStats(tenantId)

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
      console.error('获取审计日志统计错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取审计日志统计失败'
      })
    }
  }
})

router.get('/export', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const format = (req.query.format as 'csv' | 'json') || 'json'
    const query = {
      type: req.query.type as any,
      userId: req.query.userId as string,
      startDate: req.query.startDate as string,
      endDate: req.query.endDate as string
    }

    const content = await auditService.exportLogs(tenantId, format, query)

    const filename = `audit-logs-${new Date().toISOString().split('T')[0]}.${format}`
    const contentType = format === 'csv' ? 'text/csv' : 'application/json'

    res.setHeader('Content-Type', contentType)
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`)
    res.send(content)
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('导出审计日志错误:', error)
      res.status(500).json({
        status: 'error',
        message: '导出审计日志失败'
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

    const log = await auditService.findById(tenantId, id)

    if (!log) {
      throw new AppError('审计日志不存在', 404)
    }

    res.json({
      status: 'success',
      data: log
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('获取审计日志详情错误:', error)
      res.status(500).json({
        status: 'error',
        message: '获取审计日志详情失败'
      })
    }
  }
})

router.delete('/cleanup', async (req: Request, res: Response) => {
  try {
    const authReq = req as AuthRequest
    const tenantId = authReq.tenantId

    if (!tenantId) {
      throw new AppError('缺少租户信息', 400)
    }

    const daysToKeep = parseInt(req.query.days as string) || 90
    const deletedCount = await auditService.deleteOldLogs(tenantId, daysToKeep)

    res.json({
      status: 'success',
      message: `已清理 ${deletedCount} 条审计日志`
    })
  } catch (error) {
    if (error instanceof AppError) {
      res.status(error.statusCode).json({
        status: 'error',
        message: error.message
      })
    } else {
      console.error('清理审计日志错误:', error)
      res.status(500).json({
        status: 'error',
        message: '清理审计日志失败'
      })
    }
  }
})

export default router
