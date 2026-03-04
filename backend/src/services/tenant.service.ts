import prisma from '../lib/prisma'
import { 
  Tenant, 
  CreateTenantDto, 
  UpdateTenantDto, 
  TenantQueryDto, 
  TenantListResponse,
  TenantStats,
  TenantStatus 
} from '../types/tenant.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'

export class TenantService {
  async create(data: CreateTenantDto): Promise<Tenant> {
    if (data.domain) {
      const existingTenant = await prisma.tenant.findUnique({
        where: { domain: data.domain }
      })
      if (existingTenant) {
        throw new AppError('域名已被使用', 400)
      }
    }

    const tenant = await prisma.tenant.create({
      data: {
        id: uuidv4(),
        name: data.name,
        logo: data.logo,
        domain: data.domain,
        plan: data.plan || 'basic',
        maxUsers: data.maxUsers || 100,
        maxApps: data.maxApps || 10,
        status: 'active'
      }
    })

    return tenant
  }

  async findById(id: string): Promise<Tenant> {
    const tenant = await prisma.tenant.findUnique({
      where: { id }
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    return tenant
  }

  async findByDomain(domain: string): Promise<Tenant | null> {
    return await prisma.tenant.findUnique({
      where: { domain }
    })
  }

  async findAll(query: TenantQueryDto): Promise<TenantListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 10
    const skip = (page - 1) * pageSize

    const where: any = {}

    if (query.name) {
      where.name = {
        contains: query.name,
        mode: 'insensitive'
      }
    }

    if (query.status) {
      where.status = query.status
    }

    if (query.plan) {
      where.plan = query.plan
    }

    if (query.domain) {
      where.domain = {
        contains: query.domain,
        mode: 'insensitive'
      }
    }

    const [tenants, total] = await Promise.all([
      prisma.tenant.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { createdAt: 'desc' }
      }),
      prisma.tenant.count({ where })
    ])

    return {
      tenants,
      total,
      page,
      pageSize
    }
  }

  async update(id: string, data: UpdateTenantDto): Promise<Tenant> {
    await this.findById(id)

    if (data.domain) {
      const existingTenant = await prisma.tenant.findFirst({
        where: {
          domain: data.domain,
          NOT: { id }
        }
      })
      if (existingTenant) {
        throw new AppError('域名已被使用', 400)
      }
    }

    const tenant = await prisma.tenant.update({
      where: { id },
      data: {
        ...data,
        updatedAt: new Date()
      }
    })

    return tenant
  }

  async delete(id: string): Promise<void> {
    await this.findById(id)

    await prisma.tenant.update({
      where: { id },
      data: { status: 'deleted' }
    })
  }

  async updateStatus(id: string, status: TenantStatus): Promise<Tenant> {
    await this.findById(id)

    const tenant = await prisma.tenant.update({
      where: { id },
      data: {
        status,
        updatedAt: new Date()
      }
    })

    return tenant
  }

  async getStats(id: string): Promise<TenantStats> {
    await this.findById(id)

    const [totalUsers, totalApps, activeUsers] = await Promise.all([
      prisma.user.count({
        where: { tenantId: id }
      }),
      prisma.application.count({
        where: { tenantId: id }
      }),
      prisma.user.count({
        where: {
          tenantId: id,
          status: 'active'
        }
      })
    ])

    return {
      totalUsers,
      totalApps,
      activeUsers,
      storageUsed: 0
    }
  }

  async checkDomainAvailability(domain: string): Promise<boolean> {
    const tenant = await prisma.tenant.findUnique({
      where: { domain }
    })
    return !tenant
  }

  async validateTenantLimits(id: string): Promise<{
    canAddUser: boolean
    canAddApp: boolean
    currentUsers: number
    currentApps: number
  }> {
    const tenant = await this.findById(id)
    
    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    const [currentUsers, currentApps] = await Promise.all([
      prisma.user.count({ where: { tenantId: id } }),
      prisma.application.count({ where: { tenantId: id } })
    ])

    return {
      canAddUser: currentUsers < tenant.maxUsers,
      canAddApp: currentApps < tenant.maxApps,
      currentUsers,
      currentApps
    }
  }
}

export default new TenantService()
