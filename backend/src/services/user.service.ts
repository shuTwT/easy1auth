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
  AssignGroupsDto,
  UserImportDto,
  UserImportResult
} from '../types/user.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'
import bcrypt from 'bcryptjs'
import * as XLSX from 'xlsx'

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

  async generateImportTemplate(): Promise<Buffer> {
    const workbook = XLSX.utils.book_new()
    const worksheet = XLSX.utils.aoa_to_sheet([
      ['用户名*', '邮箱*', '密码', '手机号', '姓名*', '部门', '岗位'],
      ['zhangsan', 'zhangsan@example.com', 'Password123', '13800138000', '张三', '技术部', '工程师'],
      ['lisi', 'lisi@example.com', 'Password123', '13900139000', '李四', '产品部', '产品经理']
    ])

    XLSX.utils.book_append_sheet(workbook, worksheet, '用户导入模板')

    const buffer = XLSX.write(workbook, { type: 'buffer', bookType: 'xlsx' })

    return buffer
  }

  async importUsers(tenantId: string, users: any[]): Promise<UserImportResult> {
    const result: UserImportResult = {
      success: 0,
      failed: 0,
      total: users.length,
      errors: [],
      importedUsers: []
    }

    const tenant = await prisma.tenant.findUnique({
      where: { id: tenantId }
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    const currentUserCount = await prisma.user.count({
      where: { tenantId }
    })

    if (currentUserCount + users.length > tenant.maxUsers) {
      throw new AppError(`导入后将超过租户用户上限（当前：${currentUserCount}，上限：${tenant.maxUsers}）`, 400)
    }

    for (let i = 0; i < users.length; i++) {
      const user = users[i]
      const row = i + 2

      try {
        if (!user.username || !user.email || !user.name) {
          result.errors.push({
            row,
            username: user.username,
            error: '用户名、邮箱和姓名为必填项'
          })
          result.failed++
          continue
        }

        const existingUser = await prisma.user.findFirst({
          where: {
            tenantId,
            OR: [
              { username: user.username },
              { email: user.email }
            ]
          }
        })

        if (existingUser) {
          if (existingUser.username === user.username) {
            result.errors.push({
              row,
              username: user.username,
              error: '用户名已存在'
            })
          } else {
            result.errors.push({
              row,
              username: user.username,
              error: '邮箱已存在'
            })
          }
          result.failed++
          continue
        }

        const hashedPassword = user.password 
          ? await bcrypt.hash(user.password, 10)
          : await bcrypt.hash('Password123', 10)

        const createdUser = await prisma.user.create({
          data: {
            id: uuidv4(),
            tenantId,
            username: user.username,
            email: user.email,
            password: hashedPassword,
            phone: user.phone ?? null,
            name: user.name,
            department: user.department || null,
            position: user.position || null,
            status: 'active'
          }
        })

        result.success++
        result.importedUsers.push({
          username: createdUser.username,
          email: createdUser.email,
          name: createdUser.name
        })
      } catch (error) {
        result.errors.push({
          row,
          username: user.username,
          error: error instanceof Error ? error.message : '导入失败'
        })
        result.failed++
      }
    }

    return result
  }
}

export default new UserService()
