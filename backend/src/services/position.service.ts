import { Position } from '@prisma/client'
import prisma from '../lib/prisma'
import {
  CreatePositionDto,
  UpdatePositionDto,
  PositionQueryDto,
  PositionListResponse,
  PositionStats
} from '../types/position.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'

export class PositionService {
  async create(tenantId: string, data: CreatePositionDto): Promise<Position> {
    const existingPosition = await prisma.position.findFirst({
      where: {
        tenantId,
        code: data.code
      }
    })

    if (existingPosition) {
      throw new AppError('岗位编码已存在', 400)
    }

    const position = await prisma.position.create({
      data: {
        id: uuidv4(),
        tenantId,
        name: data.name,
        code: data.code,
        description: data.description ?? null,
        departmentId: data.departmentId ?? null,
        level: data.level ?? 1,
        sequence: data.sequence ?? null,
        maxCount: data.maxCount ?? null,
        userCount: 0
      }
    })

    return position
  }

  async findById(tenantId: string, id: string): Promise<Position> {
    const position = await prisma.position.findFirst({
      where: {
        id,
        tenantId
      }
    })

    if (!position) {
      throw new AppError('岗位不存在', 404)
    }

    return position
  }

  async findAll(tenantId: string, query: PositionQueryDto): Promise<PositionListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 10
    const skip = (page - 1) * pageSize

    const where: any = {
      tenantId
    }

    if (query.name) {
      where.name = {
        contains: query.name
      }
    }

    if (query.code) {
      where.code = {
        contains: query.code
      }
    }

    if (query.departmentId) {
      where.departmentId = query.departmentId
    }

    if (query.level) {
      where.level = query.level
    }

    const [positions, total] = await Promise.all([
      prisma.position.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: [
          { level: 'asc' },
          { createdAt: 'desc' }
        ]
      }),
      prisma.position.count({ where })
    ])

    return {
      positions,
      total,
      page,
      pageSize
    }
  }

  async update(tenantId: string, id: string, data: UpdatePositionDto): Promise<Position> {
    await this.findById(tenantId, id)

    if (data.code) {
      const existingPosition = await prisma.position.findFirst({
        where: {
          tenantId,
          code: data.code,
          NOT: { id }
        }
      })

      if (existingPosition) {
        throw new AppError('岗位编码已存在', 400)
      }
    }

    const position = await prisma.position.update({
      where: { id },
      data: {
        name: data.name,
        code: data.code,
        description: data.description,
        departmentId: data.departmentId === undefined ? undefined : (data.departmentId || null),
        level: data.level,
        sequence: data.sequence,
        maxCount: data.maxCount
      }
    })

    return position
  }

  async delete(tenantId: string, id: string): Promise<void> {
    await this.findById(tenantId, id)

    const position = await prisma.position.findUnique({
      where: { id }
    })

    if (position && position.userCount > 0) {
      throw new AppError('该岗位下还有用户，无法删除', 400)
    }

    await prisma.position.delete({
      where: { id }
    })
  }

  async getStats(tenantId: string): Promise<PositionStats> {
    const positions = await prisma.position.findMany({
      where: { tenantId },
      select: {
        userCount: true,
        level: true,
        maxCount: true
      }
    })

    const totalPositions = positions.length
    const filledPositions = positions.filter(p => p.userCount > 0).length
    const vacantPositions = positions.filter(p => p.userCount === 0 || (p.maxCount && p.userCount < p.maxCount)).length
    const averageLevel = positions.length > 0 
      ? positions.reduce((sum, p) => sum + p.level, 0) / positions.length 
      : 0

    return {
      totalPositions,
      filledPositions,
      vacantPositions,
      averageLevel: Math.round(averageLevel * 100) / 100
    }
  }

  async incrementUserCount(id: string): Promise<void> {
    await prisma.position.update({
      where: { id },
      data: {
        userCount: {
          increment: 1
        }
      }
    })
  }

  async decrementUserCount(id: string): Promise<void> {
    await prisma.position.update({
      where: { id },
      data: {
        userCount: {
          decrement: 1
        }
      }
    })
  }
}

export default new PositionService()
