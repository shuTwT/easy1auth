import { Request, Response, NextFunction } from 'express'
import tenantService from '../services/tenant.service'
import { AppError } from '../middleware/errorHandler'
import { CreateTenantDto, UpdateTenantDto, TenantQueryDto } from '../types/tenant.types'

export class TenantController {
  async create(req: Request, res: Response, next: NextFunction) {
    try {
      const data: CreateTenantDto = req.body
      const tenant = await tenantService.create(data)
      
      res.status(201).json({
        status: 'success',
        message: '租户创建成功',
        data: tenant
      })
    } catch (error) {
      next(error)
    }
  }

  async getById(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string
      const tenant = await tenantService.findById(id)
      
      res.json({
        status: 'success',
        data: tenant
      })
    } catch (error) {
      next(error)
    }
  }

  async getList(req: Request, res: Response, next: NextFunction) {
    try {
      const query: TenantQueryDto = {
        page: parseInt(req.query.page as string) || 1,
        pageSize: parseInt(req.query.pageSize as string) || 10,
        name: req.query.name as string,
        status: req.query.status as any,
        plan: req.query.plan as any,
        domain: req.query.domain as string
      }
      
      const result = await tenantService.findAll(query)
      
      res.json({
        status: 'success',
        data: result
      })
    } catch (error) {
      next(error)
    }
  }

  async update(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string
      const data: UpdateTenantDto = req.body
      const tenant = await tenantService.update(id, data)
      
      res.json({
        status: 'success',
        message: '租户更新成功',
        data: tenant
      })
    } catch (error) {
      next(error)
    }
  }

  async delete(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string
      await tenantService.delete(id)
      
      res.json({
        status: 'success',
        message: '租户删除成功'
      })
    } catch (error) {
      next(error)
    }
  }

  async updateStatus(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string
      const { status } = req.body
      
      if (!['active', 'suspended', 'deleted'].includes(status)) {
        throw new AppError('无效的租户状态', 400)
      }
      
      const tenant = await tenantService.updateStatus(id, status)
      
      res.json({
        status: 'success',
        message: '租户状态更新成功',
        data: tenant
      })
    } catch (error) {
      next(error)
    }
  }

  async getStats(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string
      const stats = await tenantService.getStats(id)
      
      res.json({
        status: 'success',
        data: stats
      })
    } catch (error) {
      next(error)
    }
  }

  async checkDomain(req: Request, res: Response, next: NextFunction) {
    try {
      const domain = req.query.domain as string
      
      if (!domain) {
        throw new AppError('域名不能为空', 400)
      }
      
      const available = await tenantService.checkDomainAvailability(domain)
      
      res.json({
        status: 'success',
        data: { available }
      })
    } catch (error) {
      next(error)
    }
  }

  async validateLimits(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string
      const limits = await tenantService.validateTenantLimits(id)
      
      res.json({
        status: 'success',
        data: limits
      })
    } catch (error) {
      next(error)
    }
  }
}

export default new TenantController()
