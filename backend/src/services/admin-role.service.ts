import prisma from '../lib/prisma'
import {
  ADMIN_PERMISSION_CATALOG,
  ADMIN_PERMISSION_WILDCARD
} from '../lib/admin-permissions'
import { AppError } from '../middleware/errorHandler'
import type { AdminRole } from '@prisma/client'
import type {
  AdminRoleListResponse,
  AdminRoleQueryDto,
  AdminRoleResponse,
  AdminRoleStats,
  CreateAdminRoleDto,
  PermissionCatalogResponse,
  UpdateAdminRoleDto
} from '../types/admin-role.types'

type AdminRoleWithAdminCount = AdminRole & {
  _count: {
    admins: number
  }
}

const VALID_PERMISSION_CODES = new Set<string>([
  ...ADMIN_PERMISSION_CATALOG,
  ADMIN_PERMISSION_WILDCARD
])

export class AdminRoleService {
  async getStats(tenantId: string): Promise<AdminRoleStats> {
    const [totalRoles, systemRoles, customRoles, totalAdmins] = await Promise.all([
      prisma.adminRole.count({ where: { tenantId } }),
      prisma.adminRole.count({ where: { tenantId, isSystem: true } }),
      prisma.adminRole.count({ where: { tenantId, isSystem: false } }),
      prisma.admin.count({
        where: {
          tenants: { some: { tenantId } }
        }
      })
    ])

    return { totalRoles, systemRoles, customRoles, totalAdmins }
  }

  async findAll(
    tenantId: string,
    query?: AdminRoleQueryDto
  ): Promise<AdminRoleListResponse> {
    const page = query?.page ?? 1
    const pageSize = query?.pageSize ?? 10

    if (page < 1 || pageSize < 1) {
      throw new AppError('分页参数必须大于零', 400)
    }

    const name = query?.name?.trim()
    const where = {
      tenantId,
      ...(name ? { name: { contains: name } } : {}),
      ...(query?.isSystem === undefined ? {} : { isSystem: query.isSystem })
    }

    const [roles, total] = await Promise.all([
      prisma.adminRole.findMany({
        where,
        skip: (page - 1) * pageSize,
        take: pageSize,
        include: { _count: { select: { admins: true } } },
        orderBy: { createdAt: 'desc' }
      }),
      prisma.adminRole.count({ where })
    ])

    return {
      roles: roles.map((role) => this.toResponse(role)),
      total,
      page,
      pageSize
    }
  }

  async findById(tenantId: string, id: string): Promise<AdminRoleResponse> {
    const role = await prisma.adminRole.findFirst({
      where: { id, tenantId },
      include: { _count: { select: { admins: true } } }
    })

    if (!role) {
      throw new AppError('管理员角色不存在', 404)
    }

    return this.toResponse(role)
  }

  async create(tenantId: string, data: CreateAdminRoleDto): Promise<AdminRoleResponse> {
    const name = data.name.trim()
    this.validateName(name)
    this.validatePermissions(data.permissions)

    const existingRole = await prisma.adminRole.findUnique({
      where: { tenantId_name: { tenantId, name } }
    })

    if (existingRole) {
      throw new AppError('管理员角色名称已存在', 400)
    }

    const role = await prisma.adminRole.create({
      data: {
        tenantId,
        name,
        description: data.description ?? null,
        permissions: data.permissions,
        isSystem: false
      },
      include: { _count: { select: { admins: true } } }
    })

    return this.toResponse(role)
  }

  async update(
    tenantId: string,
    id: string,
    data: UpdateAdminRoleDto
  ): Promise<AdminRoleResponse> {
    const role = await prisma.adminRole.findFirst({ where: { id, tenantId } })

    if (!role) {
      throw new AppError('管理员角色不存在', 404)
    }

    if (role.isSystem) {
      throw new AppError('系统角色不能修改', 403)
    }

    const name = data.name === undefined ? undefined : data.name.trim()
    if (name !== undefined) {
      this.validateName(name)
      if (name !== role.name) {
        const existingRole = await prisma.adminRole.findUnique({
          where: { tenantId_name: { tenantId, name } }
        })

        if (existingRole) {
          throw new AppError('管理员角色名称已存在', 400)
        }
      }
    }

    if (data.permissions !== undefined) {
      this.validatePermissions(data.permissions)
    }

    const updatedRole = await prisma.adminRole.update({
      where: { id },
      data: {
        ...(name === undefined ? {} : { name }),
        ...(data.description === undefined ? {} : { description: data.description }),
        ...(data.permissions === undefined ? {} : { permissions: data.permissions })
      },
      include: { _count: { select: { admins: true } } }
    })

    return this.toResponse(updatedRole)
  }

  async delete(tenantId: string, id: string): Promise<void> {
    const role = await prisma.adminRole.findFirst({
      where: { id, tenantId },
      include: { _count: { select: { admins: true } } }
    })

    if (!role) {
      throw new AppError('管理员角色不存在', 404)
    }

    if (role.isSystem) {
      throw new AppError('系统角色不能删除', 403)
    }

    if (role._count.admins > 0) {
      throw new AppError('角色下还有管理员，不能删除', 400)
    }

    await prisma.adminRole.delete({ where: { id } })
  }

  async getPermissionCatalog(_tenantId: string): Promise<PermissionCatalogResponse> {
    return { permissions: [...ADMIN_PERMISSION_CATALOG] }
  }

  private toResponse(role: AdminRoleWithAdminCount): AdminRoleResponse {
    return {
      ...role,
      adminCount: role._count.admins
    }
  }

  private validateName(name: string): void {
    if (name.length === 0) {
      throw new AppError('管理员角色名称不能为空', 400)
    }
  }

  private validatePermissions(permissions: readonly string[]): void {
    const invalidPermissions = permissions.filter(
      (permission) => !VALID_PERMISSION_CODES.has(permission)
    )

    if (invalidPermissions.length > 0) {
      throw new AppError(`存在未知的管理员权限: ${invalidPermissions.join(', ')}`, 400)
    }
  }
}

export default new AdminRoleService()
