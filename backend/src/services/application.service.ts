import { Application, Prisma } from '@prisma/client'
import prisma from '../lib/prisma'
import { 
  CreateApplicationDto, 
  UpdateApplicationDto, 
  ApplicationQueryDto, 
  ApplicationListResponse,
  ApplicationStats,
  ApplicationStatus,
  RegenerateSecretDto
} from '../types/application.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'
import crypto from 'crypto'

export class ApplicationService {
  private generateClientId(): string {
    return `app_${uuidv4().replace(/-/g, '')}`
  }

  private generateClientSecret(): string {
    return crypto.randomBytes(32).toString('hex')
  }

  async create(tenantId: string, data: CreateApplicationDto): Promise<Application> {
    const existingApp = await prisma.application.findFirst({
      where: {
        tenantId,
        name: data.name
      }
    })

    if (existingApp) {
      throw new AppError('应用名称已存在', 400)
    }

    const tenant = await prisma.tenant.findUnique({
      where: { id: tenantId }
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    const appCount = await prisma.application.count({
      where: { tenantId }
    })

    if (appCount >= tenant.maxApps) {
      throw new AppError('已达到租户应用上限', 400)
    }

    const clientId = this.generateClientId()
    const clientSecret = this.generateClientSecret()

    const application = await prisma.application.create({
      data: {
        id: uuidv4(),
        tenantId,
        name: data.name,
        logo: data.logo ?? null,
        description: data.description ?? null,
        type: data.type ?? 'web',
        clientId,
        clientSecret,
        redirectUris: data.redirectUris ?? [],
        allowedGrantTypes: data.allowedGrantTypes ?? ['authorization_code'],
        accessTokenLifetime: data.accessTokenLifetime ?? 3600,
        refreshTokenLifetime: data.refreshTokenLifetime ?? 2592000,
        status: 'active'
      }
    })

    return application
  }

  async findById(tenantId: string, id: string): Promise<Application> {
    const application = await prisma.application.findFirst({
      where: {
        id,
        tenantId
      }
    })

    if (!application) {
      throw new AppError('应用不存在', 404)
    }

    return application
  }

  async findByClientId(clientId: string): Promise<Application | null> {
    return prisma.application.findUnique({
      where: { clientId }
    })
  }

  async findAll(tenantId: string, query: ApplicationQueryDto): Promise<ApplicationListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 10
    const skip = (page - 1) * pageSize

    const where: Prisma.ApplicationWhereInput = {
      tenantId
    }

    if (query.name) {
      where.name = {
        contains: query.name
      }
    }

    if (query.type) {
      where.type = query.type
    }

    if (query.status) {
      where.status = query.status
    }

    const [applications, total] = await Promise.all([
      prisma.application.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { createdAt: 'desc' }
      }),
      prisma.application.count({ where })
    ])

    return {
      applications,
      total,
      page,
      pageSize
    }
  }

  async update(tenantId: string, id: string, data: UpdateApplicationDto): Promise<Application> {
    await this.findById(tenantId, id)

    if (data.name) {
      const existingApp = await prisma.application.findFirst({
        where: {
          tenantId,
          NOT: { id },
          name: data.name
        }
      })

      if (existingApp) {
        throw new AppError('应用名称已存在', 400)
      }
    }

    const updateData: Prisma.ApplicationUpdateInput = {
      ...data,
      updatedAt: new Date()
    }

    if (data.logo !== undefined) {
      updateData.logo = data.logo ?? null
    }

    if (data.description !== undefined) {
      updateData.description = data.description ?? null
    }

    if (data.redirectUris !== undefined) {
      updateData.redirectUris = data.redirectUris
    }

    if (data.allowedGrantTypes !== undefined) {
      updateData.allowedGrantTypes = data.allowedGrantTypes
    }

    const application = await prisma.application.update({
      where: { id },
      data: updateData
    })

    return application
  }

  async delete(tenantId: string, id: string): Promise<void> {
    await this.findById(tenantId, id)

    await prisma.application.delete({
      where: { id }
    })
  }

  async updateStatus(tenantId: string, id: string, status: ApplicationStatus): Promise<Application> {
    await this.findById(tenantId, id)

    const application = await prisma.application.update({
      where: { id },
      data: {
        status,
        updatedAt: new Date()
      }
    })

    return application
  }

  async regenerateSecret(tenantId: string, id: string): Promise<RegenerateSecretDto> {
    await this.findById(tenantId, id)

    const newSecret = this.generateClientSecret()

    await prisma.application.update({
      where: { id },
      data: {
        clientSecret: newSecret,
        updatedAt: new Date()
      }
    })

    return {
      clientSecret: newSecret
    }
  }

  async getStats(tenantId: string): Promise<ApplicationStats> {
    const [totalApplications, activeApplications, disabledApplications] = await Promise.all([
      prisma.application.count({
        where: { tenantId }
      }),
      prisma.application.count({
        where: {
          tenantId,
          status: 'active'
        }
      }),
      prisma.application.count({
        where: {
          tenantId,
          status: 'disabled'
        }
      })
    ])

    return {
      totalApplications,
      activeApplications,
      disabledApplications
    }
  }

  async validateClient(clientId: string, clientSecret: string): Promise<Application> {
    const application = await this.findByClientId(clientId)

    if (!application) {
      throw new AppError('应用不存在', 404)
    }

    if (application.status !== 'active') {
      throw new AppError('应用已禁用', 403)
    }

    if (application.clientSecret !== clientSecret) {
      throw new AppError('客户端密钥错误', 401)
    }

    return application
  }
}

export default new ApplicationService()
