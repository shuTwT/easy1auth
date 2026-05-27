import { Permission } from '@prisma/client'
import prisma from '../lib/prisma'
import {
  CreatePermissionDto,
  UpdatePermissionDto,
  PermissionQueryDto,
  PermissionListResponse,
  PermissionTree,
  PermissionStats,
} from '../types/permission.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'

const PRESET_PERMISSIONS: Array<{ code: string; name: string; type: string; resource: string; action: string; parentCode?: string }> = [
  // 租户管理
  { code: 'tenant:read', name: '查看租户', type: 'operation', resource: 'tenant', action: 'read' },
  { code: 'tenant:create', name: '创建租户', type: 'operation', resource: 'tenant', action: 'create' },
  { code: 'tenant:update', name: '编辑租户', type: 'operation', resource: 'tenant', action: 'update' },
  { code: 'tenant:delete', name: '删除租户', type: 'operation', resource: 'tenant', action: 'delete' },
  // 用户管理
  { code: 'user:read', name: '查看用户', type: 'operation', resource: 'user', action: 'read' },
  { code: 'user:create', name: '创建用户', type: 'operation', resource: 'user', action: 'create' },
  { code: 'user:update', name: '编辑用户', type: 'operation', resource: 'user', action: 'update' },
  { code: 'user:delete', name: '删除用户', type: 'operation', resource: 'user', action: 'delete' },
  // 用户组管理
  { code: 'group:read', name: '查看用户组', type: 'operation', resource: 'group', action: 'read' },
  { code: 'group:create', name: '创建用户组', type: 'operation', resource: 'group', action: 'create' },
  { code: 'group:update', name: '编辑用户组', type: 'operation', resource: 'group', action: 'update' },
  { code: 'group:delete', name: '删除用户组', type: 'operation', resource: 'group', action: 'delete' },
  // 岗位管理
  { code: 'position:read', name: '查看岗位', type: 'operation', resource: 'position', action: 'read' },
  { code: 'position:create', name: '创建岗位', type: 'operation', resource: 'position', action: 'create' },
  { code: 'position:update', name: '编辑岗位', type: 'operation', resource: 'position', action: 'update' },
  { code: 'position:delete', name: '删除岗位', type: 'operation', resource: 'position', action: 'delete' },
  // 应用管理
  { code: 'application:read', name: '查看应用', type: 'operation', resource: 'application', action: 'read' },
  { code: 'application:create', name: '创建应用', type: 'operation', resource: 'application', action: 'create' },
  { code: 'application:update', name: '编辑应用', type: 'operation', resource: 'application', action: 'update' },
  { code: 'application:delete', name: '删除应用', type: 'operation', resource: 'application', action: 'delete' },
  // 角色管理
  { code: 'role:read', name: '查看角色', type: 'operation', resource: 'role', action: 'read' },
  { code: 'role:create', name: '创建角色', type: 'operation', resource: 'role', action: 'create' },
  { code: 'role:update', name: '编辑角色', type: 'operation', resource: 'role', action: 'update' },
  { code: 'role:delete', name: '删除角色', type: 'operation', resource: 'role', action: 'delete' },
  { code: 'role:assign', name: '分配角色', type: 'operation', resource: 'role', action: 'assign' },
  // 权限管理
  { code: 'permission:read', name: '查看权限', type: 'operation', resource: 'permission', action: 'read' },
  { code: 'permission:create', name: '创建权限', type: 'operation', resource: 'permission', action: 'create' },
  { code: 'permission:update', name: '编辑权限', type: 'operation', resource: 'permission', action: 'update' },
  { code: 'permission:delete', name: '删除权限', type: 'operation', resource: 'permission', action: 'delete' },
  // 审计日志
  { code: 'audit:read', name: '查看审计日志', type: 'operation', resource: 'audit', action: 'read' },
  { code: 'audit:export', name: '导出审计日志', type: 'operation', resource: 'audit', action: 'export' },
  { code: 'audit:cleanup', name: '清理审计日志', type: 'operation', resource: 'audit', action: 'cleanup' },
  // 社会化身份源
  { code: 'social-idp:read', name: '查看身份源', type: 'operation', resource: 'social-idp', action: 'read' },
  { code: 'social-idp:create', name: '创建身份源', type: 'operation', resource: 'social-idp', action: 'create' },
  { code: 'social-idp:update', name: '编辑身份源', type: 'operation', resource: 'social-idp', action: 'update' },
  { code: 'social-idp:delete', name: '删除身份源', type: 'operation', resource: 'social-idp', action: 'delete' },
  // 安全设置
  { code: 'security:read', name: '查看安全设置', type: 'operation', resource: 'security', action: 'read' },
  { code: 'security:password-policy', name: '管理密码策略', type: 'operation', resource: 'security', action: 'manage' },
  { code: 'security:mfa', name: '管理MFA', type: 'operation', resource: 'security', action: 'manage' },
  // 个性化设置
  { code: 'personalization:read', name: '查看个性化设置', type: 'operation', resource: 'personalization', action: 'read' },
  { code: 'personalization:update', name: '编辑个性化设置', type: 'operation', resource: 'personalization', action: 'update' },
  // SSO
  { code: 'sso:read', name: '查看SSO配置', type: 'operation', resource: 'sso', action: 'read' },
  { code: 'sso:manage', name: '管理SSO', type: 'operation', resource: 'sso', action: 'manage' },
  // 品牌设置
  { code: 'brand:read', name: '查看品牌设置', type: 'operation', resource: 'brand', action: 'read' },
  { code: 'brand:update', name: '编辑品牌设置', type: 'operation', resource: 'brand', action: 'update' },
  // 数据权限
  { code: 'data:all', name: '全部数据', type: 'data', resource: 'data', action: 'all' },
  { code: 'data:department', name: '本部门数据', type: 'data', resource: 'data', action: 'department' },
  { code: 'data:department-sub', name: '本部门及下级数据', type: 'data', resource: 'data', action: 'department_and_sub' },
  { code: 'data:self', name: '仅本人数据', type: 'data', resource: 'data', action: 'self' },
  // 菜单权限
  { code: 'menu:dashboard', name: '控制台', type: 'menu', resource: 'menu', action: 'dashboard', parentCode: undefined },
  { code: 'menu:tenant', name: '租户管理', type: 'menu', resource: 'menu', action: 'tenant', parentCode: undefined },
  { code: 'menu:user', name: '用户管理', type: 'menu', resource: 'menu', action: 'user', parentCode: undefined },
  { code: 'menu:group', name: '用户组管理', type: 'menu', resource: 'menu', action: 'group', parentCode: undefined },
  { code: 'menu:position', name: '岗位管理', type: 'menu', resource: 'menu', action: 'position', parentCode: undefined },
  { code: 'menu:application', name: '应用管理', type: 'menu', resource: 'menu', action: 'application', parentCode: undefined },
  { code: 'menu:role', name: '角色管理', type: 'menu', resource: 'menu', action: 'role', parentCode: undefined },
  { code: 'menu:permission', name: '权限管理', type: 'menu', resource: 'menu', action: 'permission', parentCode: undefined },
  { code: 'menu:audit', name: '审计日志', type: 'menu', resource: 'menu', action: 'audit', parentCode: undefined },
  { code: 'menu:social-idp', name: '社会化身份源', type: 'menu', resource: 'menu', action: 'social-idp', parentCode: undefined },
  { code: 'menu:security', name: '安全设置', type: 'menu', resource: 'menu', action: 'security', parentCode: undefined },
  { code: 'menu:personalization', name: '个性化设置', type: 'menu', resource: 'menu', action: 'personalization', parentCode: undefined },
  { code: 'menu:sso', name: '单点登录', type: 'menu', resource: 'menu', action: 'sso', parentCode: undefined },
  { code: 'menu:brand', name: '品牌设置', type: 'menu', resource: 'menu', action: 'brand', parentCode: undefined },
]

export class PermissionService {
  async seedIfEmpty(tenantId: string): Promise<void> {
    const count = await prisma.permission.count({ where: { tenantId } })
    if (count > 0) return

    const records = PRESET_PERMISSIONS.map((p) => ({
      id: uuidv4(),
      tenantId,
      code: p.code,
      name: p.name,
      type: p.type,
      resource: p.resource,
      action: p.action,
      parentId: null,
    }))

    await prisma.permission.createMany({ data: records })
  }

  async create(tenantId: string, data: CreatePermissionDto): Promise<Permission> {
    const existing = await prisma.permission.findFirst({
      where: { tenantId, code: data.code },
    })

    if (existing) {
      throw new AppError('权限编码已存在', 400)
    }

    if (data.parentId) {
      const parent = await prisma.permission.findFirst({
        where: { id: data.parentId, tenantId },
      })
      if (!parent) {
        throw new AppError('父级权限不存在', 404)
      }
    }

    return prisma.permission.create({
      data: {
        id: uuidv4(),
        tenantId,
        name: data.name,
        code: data.code,
        description: data.description ?? null,
        type: data.type || 'operation',
        resource: data.resource,
        action: data.action,
        parentId: data.parentId ?? null,
      },
    })
  }

  async findById(tenantId: string, id: string): Promise<Permission> {
    const permission = await prisma.permission.findFirst({
      where: { id, tenantId },
      include: { parent: { select: { id: true, name: true, code: true } } },
    })

    if (!permission) {
      throw new AppError('权限不存在', 404)
    }

    return permission
  }

  async findAll(tenantId: string, query: PermissionQueryDto): Promise<PermissionListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 50
    const skip = (page - 1) * pageSize

    const where: any = { tenantId }

    if (query.search) {
      where.OR = [
        { name: { contains: query.search } },
        { code: { contains: query.search } },
        { resource: { contains: query.search } },
      ]
    }

    if (query.type) {
      where.type = query.type
    }

    if (query.resource) {
      where.resource = query.resource
    }

    const [permissions, total] = await Promise.all([
      prisma.permission.findMany({
        where,
        include: { parent: { select: { id: true, name: true, code: true } } },
        orderBy: [{ resource: 'asc' }, { code: 'asc' }],
        skip,
        take: pageSize,
      }),
      prisma.permission.count({ where }),
    ])

    return { permissions, total, page, pageSize }
  }

  async getTree(tenantId: string): Promise<PermissionTree[]> {
    await this.seedIfEmpty(tenantId)

    const permissions = await prisma.permission.findMany({
      where: { tenantId },
      orderBy: [{ type: 'asc' }, { resource: 'asc' }, { code: 'asc' }],
    })

    const nodeMap = new Map<string, PermissionTree & { parentId: string | null }>()
    const roots: PermissionTree[] = []

    for (const p of permissions) {
      nodeMap.set(p.id, {
        id: p.id,
        code: p.code,
        name: p.name,
        description: p.description,
        type: p.type,
        resource: p.resource,
        action: p.action,
        parentId: p.parentId,
        children: [],
      })
    }

    for (const node of nodeMap.values()) {
      if (node.parentId && nodeMap.has(node.parentId)) {
        nodeMap.get(node.parentId)!.children.push(node)
      } else {
        roots.push(node)
      }
    }

    return roots
  }

  async getStats(tenantId: string): Promise<PermissionStats> {
    await this.seedIfEmpty(tenantId)

    const [totalPermissions, menuCount, operationCount, dataCount] = await Promise.all([
      prisma.permission.count({ where: { tenantId } }),
      prisma.permission.count({ where: { tenantId, type: 'menu' } }),
      prisma.permission.count({ where: { tenantId, type: 'operation' } }),
      prisma.permission.count({ where: { tenantId, type: 'data' } }),
    ])

    return {
      totalPermissions,
      menuPermissions: menuCount,
      operationPermissions: operationCount,
      dataPermissions: dataCount,
    }
  }

  async update(tenantId: string, id: string, data: UpdatePermissionDto): Promise<Permission> {
    const permission = await prisma.permission.findFirst({
      where: { id, tenantId },
    })

    if (!permission) {
      throw new AppError('权限不存在', 404)
    }

    if (data.parentId) {
      if (data.parentId === id) {
        throw new AppError('不能将自己设为父级', 400)
      }
      const parent = await prisma.permission.findFirst({
        where: { id: data.parentId, tenantId },
      })
      if (!parent) {
        throw new AppError('父级权限不存在', 404)
      }
    }

    return prisma.permission.update({
      where: { id },
      data: {
        name: data.name,
        description: data.description,
        type: data.type,
        resource: data.resource,
        action: data.action,
        parentId: data.parentId,
      },
      include: { parent: { select: { id: true, name: true, code: true } } },
    })
  }

  async delete(tenantId: string, id: string): Promise<void> {
    const permission = await prisma.permission.findFirst({
      where: { id, tenantId },
      include: { _count: { select: { children: true } } },
    })

    if (!permission) {
      throw new AppError('权限不存在', 404)
    }

    if (permission._count.children > 0) {
      throw new AppError('该权限下还有子权限，不能删除', 400)
    }

    await prisma.permission.delete({ where: { id } })
  }
}

export default new PermissionService()
