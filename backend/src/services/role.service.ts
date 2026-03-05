import { PrismaClient, Role } from '@prisma/client'
import prisma from '../lib/prisma'
import { AppError } from '../middleware/errorHandler'
import {
  CreateRoleDto,
  UpdateRoleDto,
  RoleResponse,
  RoleTreeResponse,
  RoleStatsResponse,
  AssignRoleDto,
  RemoveRoleDto,
  RoleUsersResponse,
} from '../types/role.types'

export class RoleService {
  async create(tenantId: string, data: CreateRoleDto): Promise<RoleResponse> {
    const existingRole = await prisma.role.findFirst({
      where: {
        tenantId,
        code: data.code,
      },
    })

    if (existingRole) {
      throw new AppError('角色编码已存在', 400)
    }

    const role = await prisma.role.create({
      data: {
        tenantId,
        name: data.name,
        code: data.code,
        description: data.description,
        type: data.type || 'custom',
        permissions: data.permissions || {},
        dataScope: data.dataScope || 'self',
        parentId: data.parentId,
      },
      include: {
        parent: {
          select: { id: true, name: true, code: true },
        },
      },
    })

    return {
      ...role,
      permissions: role.permissions as Record<string, any>,
      userCount: 0,
    }
  }

  async findAll(
    tenantId: string,
    query?: {
      search?: string
      type?: string
    }
  ): Promise<RoleResponse[]> {
    const where: any = { tenantId }

    if (query?.search) {
      where.OR = [
        { name: { contains: query.search } },
        { code: { contains: query.search } },
        { description: { contains: query.search } },
      ]
    }

    if (query?.type) {
      where.type = query.type
    }

    const roles = await prisma.role.findMany({
      where,
      include: {
        parent: {
          select: { id: true, name: true, code: true },
        },
        _count: {
          select: { users: true },
        },
      },
      orderBy: { createdAt: 'desc' },
    })

    return roles.map((role) => ({
      ...role,
      permissions: role.permissions as Record<string, any>,
      userCount: role._count.users,
    }))
  }

  async getTree(tenantId: string): Promise<RoleTreeResponse[]> {
    const roles = await prisma.role.findMany({
      where: { tenantId },
      include: {
        _count: {
          select: { users: true },
        },
      },
      orderBy: { createdAt: 'asc' },
    })

    const roleMap = new Map<string, RoleTreeResponse>()
    const rootRoles: RoleTreeResponse[] = []

    roles.forEach((role) => {
      roleMap.set(role.id, {
        id: role.id,
        name: role.name,
        code: role.code,
        description: role.description,
        type: role.type,
        userCount: role._count.users,
        children: [],
      })
    })

    roles.forEach((role) => {
      const roleNode = roleMap.get(role.id)!
      if (role.parentId && roleMap.has(role.parentId)) {
        const parent = roleMap.get(role.parentId)!
        if (!parent.children) {
          parent.children = []
        }
        parent.children.push(roleNode)
      } else {
        rootRoles.push(roleNode)
      }
    })

    return rootRoles
  }

  async getStats(tenantId: string): Promise<RoleStatsResponse> {
    const [totalRoles, systemRoles, customRoles, usersWithRoles] = await Promise.all([
      prisma.role.count({ where: { tenantId } }),
      prisma.role.count({ where: { tenantId, type: 'system' } }),
      prisma.role.count({ where: { tenantId, type: 'custom' } }),
      prisma.user.count({
        where: {
          tenantId,
          roles: { some: {} },
        },
      }),
    ])

    return {
      totalRoles,
      systemRoles,
      customRoles,
      totalUsers: usersWithRoles,
    }
  }

  async findById(tenantId: string, id: string): Promise<RoleResponse> {
    const role = await prisma.role.findFirst({
      where: { id, tenantId },
      include: {
        parent: {
          select: { id: true, name: true, code: true },
        },
        _count: {
          select: { users: true },
        },
      },
    })

    if (!role) {
      throw new AppError('角色不存在', 404)
    }

    return {
      ...role,
      permissions: role.permissions as Record<string, any>,
      userCount: role._count.users,
    }
  }

  async update(tenantId: string, id: string, data: UpdateRoleDto): Promise<RoleResponse> {
    const role = await prisma.role.findFirst({
      where: { id, tenantId },
    })

    if (!role) {
      throw new AppError('角色不存在', 404)
    }

    if (role.type === 'system') {
      throw new AppError('系统角色不能修改', 403)
    }

    const updatedRole = await prisma.role.update({
      where: { id },
      data: {
        name: data.name,
        description: data.description,
        permissions: data.permissions,
        dataScope: data.dataScope,
        parentId: data.parentId,
      },
      include: {
        parent: {
          select: { id: true, name: true, code: true },
        },
        _count: {
          select: { users: true },
        },
      },
    })

    return {
      ...updatedRole,
      permissions: updatedRole.permissions as Record<string, any>,
      userCount: updatedRole._count.users,
    }
  }

  async delete(tenantId: string, id: string): Promise<void> {
    const role = await prisma.role.findFirst({
      where: { id, tenantId },
      include: {
        _count: {
          select: { users: true },
        },
      },
    })

    if (!role) {
      throw new AppError('角色不存在', 404)
    }

    if (role.type === 'system') {
      throw new AppError('系统角色不能删除', 403)
    }

    if (role._count.users > 0) {
      throw new AppError('角色下还有用户，不能删除', 400)
    }

    await prisma.role.delete({
      where: { id },
    })
  }

  async getRoleUsers(
    tenantId: string,
    roleId: string,
    query?: { search?: string }
  ): Promise<RoleUsersResponse> {
    const role = await prisma.role.findFirst({
      where: { id: roleId, tenantId },
    })

    if (!role) {
      throw new AppError('角色不存在', 404)
    }

    const where: any = {
      tenantId,
      roles: { some: { id: roleId } },
    }

    if (query?.search) {
      where.OR = [
        { username: { contains: query.search } },
        { email: { contains: query.search } },
        { name: { contains: query.search } },
      ]
    }

    const users = await prisma.user.findMany({
      where,
      select: {
        id: true,
        username: true,
        email: true,
        name: true,
        status: true,
        department: true,
        position: true,
      },
      orderBy: { createdAt: 'desc' },
    })

    return {
      users,
      total: users.length,
    }
  }

  async assignUsers(tenantId: string, roleId: string, data: AssignRoleDto): Promise<void> {
    const role = await prisma.role.findFirst({
      where: { id: roleId, tenantId },
    })

    if (!role) {
      throw new AppError('角色不存在', 404)
    }

    await prisma.$transaction(async (tx) => {
      for (const userId of data.userIds) {
        const user = await tx.user.findFirst({
          where: { id: userId, tenantId },
          include: { roles: { where: { id: roleId } } },
        })

        if (!user) {
          throw new AppError(`用户 ${userId} 不存在`, 404)
        }

        if (user.roles.length > 0) {
          continue
        }

        await tx.user.update({
          where: { id: userId },
          data: {
            roles: {
              connect: { id: roleId },
            },
          },
        })
      }
    })
  }

  async removeUsers(tenantId: string, roleId: string, data: RemoveRoleDto): Promise<void> {
    const role = await prisma.role.findFirst({
      where: { id: roleId, tenantId },
    })

    if (!role) {
      throw new AppError('角色不存在', 404)
    }

    await prisma.$transaction(async (tx) => {
      for (const userId of data.userIds) {
        await tx.user.update({
          where: { id: userId },
          data: {
            roles: {
              disconnect: { id: roleId },
            },
          },
        })
      }
    })
  }

  async getUserRoles(tenantId: string, userId: string): Promise<RoleResponse[]> {
    const user = await prisma.user.findFirst({
      where: { id: userId, tenantId },
      include: {
        roles: {
          include: {
            parent: {
              select: { id: true, name: true, code: true },
            },
            _count: {
              select: { users: true },
            },
          },
        },
      },
    })

    if (!user) {
      throw new AppError('用户不存在', 404)
    }

    return user.roles.map((role) => ({
      ...role,
      permissions: role.permissions as Record<string, any>,
      userCount: role._count.users,
    }))
  }

  async assignRolesToUser(tenantId: string, userId: string, roleIds: string[]): Promise<void> {
    const user = await prisma.user.findFirst({
      where: { id: userId, tenantId },
    })

    if (!user) {
      throw new AppError('用户不存在', 404)
    }

    const roles = await prisma.role.findMany({
      where: {
        id: { in: roleIds },
        tenantId,
      },
    })

    if (roles.length !== roleIds.length) {
      throw new AppError('部分角色不存在', 404)
    }

    await prisma.user.update({
      where: { id: userId },
      data: {
        roles: {
          set: roleIds.map((id) => ({ id })),
        },
      },
    })
  }
}
