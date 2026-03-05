import { User, Prisma } from '@prisma/client'
import prisma from '../lib/prisma'
import { 
  CreateUserDto, 
  UpdateUserDto, 
  UserQueryDto, 
  UserListResponse,
  UserStats,
  UserStatus,
  ChangePasswordDto,
  ResetPasswordDto,
  AssignRolesDto,
  AssignGroupsDto
} from '../types/user.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'
import bcrypt from 'bcryptjs'

export class UserService {
  async create(tenantId: string, data: CreateUserDto): Promise<User> {
    const existingUser = await prisma.user.findFirst({
      where: {
        tenantId,
        OR: [
          { username: data.username },
          { email: data.email }
        ]
      }
    })

    if (existingUser) {
      if (existingUser.username === data.username) {
        throw new AppError('用户名已存在', 400)
      }
      if (existingUser.email === data.email) {
        throw new AppError('邮箱已存在', 400)
      }
    }

    const tenant = await prisma.tenant.findUnique({
      where: { id: tenantId }
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    const userCount = await prisma.user.count({
      where: { tenantId }
    })

    if (userCount >= tenant.maxUsers) {
      throw new AppError('已达到租户用户上限', 400)
    }

    let hashedPassword: string | undefined
    if (data.password) {
      hashedPassword = await bcrypt.hash(data.password, 10)
    }

    const user = await prisma.user.create({
      data: {
        id: uuidv4(),
        tenantId,
        username: data.username,
        email: data.email,
        password: hashedPassword,
        phone: data.phone ?? null,
        name: data.name,
        avatar: data.avatar ?? null,
        department: data.department ?? null,
        position: data.position ?? null,
        customAttributes: data.customAttributes ?? Prisma.JsonNull,
        status: 'active'
      }
    })

    return user
  }

  async findById(tenantId: string, id: string): Promise<User> {
    const user = await prisma.user.findFirst({
      where: {
        id,
        tenantId
      }
    })

    if (!user) {
      throw new AppError('用户不存在', 404)
    }

    return user
  }

  async findAll(tenantId: string, query: UserQueryDto): Promise<UserListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 10
    const skip = (page - 1) * pageSize

    const where: Prisma.UserWhereInput = {
      tenantId
    }

    if (query.username) {
      where.username = {
        contains: query.username
      }
    }

    if (query.email) {
      where.email = {
        contains: query.email
      }
    }

    if (query.phone) {
      where.phone = {
        contains: query.phone
      }
    }

    if (query.name) {
      where.name = {
        contains: query.name
      }
    }

    if (query.status) {
      where.status = query.status
    }

    if (query.department) {
      where.department = {
        contains: query.department
      }
    }

    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { createdAt: 'desc' }
      }),
      prisma.user.count({ where })
    ])

    return {
      users,
      total,
      page,
      pageSize
    }
  }

  async update(tenantId: string, id: string, data: UpdateUserDto): Promise<User> {
    await this.findById(tenantId, id)

    if (data.username || data.email) {
      const existingUser = await prisma.user.findFirst({
        where: {
          tenantId,
          NOT: { id },
          OR: [
            ...(data.username ? [{ username: data.username }] : []),
            ...(data.email ? [{ email: data.email }] : [])
          ]
        }
      })

      if (existingUser) {
        if (existingUser.username === data.username) {
          throw new AppError('用户名已存在', 400)
        }
        if (existingUser.email === data.email) {
          throw new AppError('邮箱已存在', 400)
        }
      }
    }

    const updateData: Prisma.UserUpdateInput = {
      ...data,
      updatedAt: new Date()
    }

    if (data.avatar !== undefined) {
      updateData.avatar = data.avatar ?? null
    }

    if (data.phone !== undefined) {
      updateData.phone = data.phone ?? null
    }

    if (data.department !== undefined) {
      updateData.department = data.department ?? null
    }

    if (data.position !== undefined) {
      updateData.position = data.position ?? null
    }

    if (data.customAttributes !== undefined) {
      updateData.customAttributes = data.customAttributes ?? Prisma.JsonNull
    }

    const user = await prisma.user.update({
      where: { id },
      data: updateData
    })

    return user
  }

  async delete(tenantId: string, id: string): Promise<void> {
    await this.findById(tenantId, id)

    await prisma.user.delete({
      where: { id }
    })
  }

  async updateStatus(tenantId: string, id: string, status: UserStatus): Promise<User> {
    await this.findById(tenantId, id)

    const user = await prisma.user.update({
      where: { id },
      data: {
        status,
        updatedAt: new Date()
      }
    })

    return user
  }

  async getStats(tenantId: string): Promise<UserStats> {
    const [totalUsers, activeUsers, disabledUsers, lockedUsers] = await Promise.all([
      prisma.user.count({
        where: { tenantId }
      }),
      prisma.user.count({
        where: {
          tenantId,
          status: 'active'
        }
      }),
      prisma.user.count({
        where: {
          tenantId,
          status: 'disabled'
        }
      }),
      prisma.user.count({
        where: {
          tenantId,
          status: 'locked'
        }
      })
    ])

    return {
      totalUsers,
      activeUsers,
      disabledUsers,
      lockedUsers
    }
  }

  async changePassword(tenantId: string, id: string, data: ChangePasswordDto): Promise<void> {
    const user = await this.findById(tenantId, id)

    if (!user.password) {
      throw new AppError('该用户未设置密码', 400)
    }

    const isValid = await bcrypt.compare(data.oldPassword, user.password)
    if (!isValid) {
      throw new AppError('原密码错误', 400)
    }

    const hashedPassword = await bcrypt.hash(data.newPassword, 10)

    await prisma.user.update({
      where: { id },
      data: {
        password: hashedPassword,
        updatedAt: new Date()
      }
    })
  }

  async resetPassword(tenantId: string, id: string, data: ResetPasswordDto): Promise<void> {
    await this.findById(tenantId, id)

    const hashedPassword = await bcrypt.hash(data.newPassword, 10)

    await prisma.user.update({
      where: { id },
      data: {
        password: hashedPassword,
        updatedAt: new Date()
      }
    })
  }

  async assignRoles(tenantId: string, id: string, data: AssignRolesDto): Promise<void> {
    await this.findById(tenantId, id)

    if (data.roleIds.length > 0) {
      const roles = await prisma.role.findMany({
        where: {
          id: { in: data.roleIds },
          tenantId
        }
      })

      if (roles.length !== data.roleIds.length) {
        throw new AppError('部分角色不存在或不属于当前租户', 400)
      }
    }

    await prisma.user.update({
      where: { id },
      data: {
        roles: {
          set: data.roleIds.map(roleId => ({ id: roleId }))
        }
      }
    })
  }

  async assignGroups(tenantId: string, id: string, data: AssignGroupsDto): Promise<void> {
    await this.findById(tenantId, id)

    if (data.groupIds.length > 0) {
      const groups = await prisma.userGroup.findMany({
        where: {
          id: { in: data.groupIds },
          tenantId
        }
      })

      if (groups.length !== data.groupIds.length) {
        throw new AppError('部分用户组不存在或不属于当前租户', 400)
      }
    }

    await prisma.user.update({
      where: { id },
      data: {
        groups: {
          set: data.groupIds.map(groupId => ({ id: groupId }))
        }
      }
    })
  }

  async getUserRoles(tenantId: string, id: string) {
    await this.findById(tenantId, id)

    const user = await prisma.user.findUnique({
      where: { id },
      include: {
        roles: true
      }
    })

    return user?.roles || []
  }

  async getUserGroups(tenantId: string, id: string) {
    await this.findById(tenantId, id)

    const user = await prisma.user.findUnique({
      where: { id },
      include: {
        groups: true
      }
    })

    return user?.groups || []
  }
}

export default new UserService()
