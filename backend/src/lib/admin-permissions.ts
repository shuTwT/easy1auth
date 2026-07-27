import type { AdminRole } from '@prisma/client'
import prisma from './prisma'

export const ADMIN_PERMISSION_CATALOG = [
  'admin-user:read',
  'admin-user:update',
  'admin-user:status',
  'admin-user:reset-password',
  'admin-user:reset-mfa',
  'admin-user:assign-role',
  'admin-user:remove-tenant',
  'admin-role:read',
  'admin-role:create',
  'admin-role:update',
  'admin-role:delete',
  'admin-role:assign'
] as const

export type AdminPermissionCode = (typeof ADMIN_PERMISSION_CATALOG)[number]

export const ADMIN_PERMISSION_WILDCARD = '*'

export const ADMIN_READ_ONLY_PERMISSION_CODES = Object.freeze(
  ADMIN_PERMISSION_CATALOG.filter((permission) => permission.endsWith(':read'))
)

export const DEFAULT_ADMIN_ROLE_NAMES = {
  superAdmin: '超级管理员',
  readOnlyAdmin: '只读管理员'
} as const

type AdminRoleDatabase = Pick<typeof prisma, 'adminRole'>

export type EnsureDefaultAdminRolesOptions = {
  readonly includeReadOnly?: boolean
  readonly db?: AdminRoleDatabase
}

export type DefaultAdminRoles = {
  readonly superAdmin: AdminRole
  readonly readOnlyAdmin?: AdminRole
}

export function hasAdminPermission(
  permissions: readonly string[],
  requiredPermission: string
): boolean {
  return permissions.includes(ADMIN_PERMISSION_WILDCARD) || permissions.includes(requiredPermission)
}

export async function ensureDefaultAdminRoles(
  tenantId: string,
  options: EnsureDefaultAdminRolesOptions = {}
): Promise<DefaultAdminRoles> {
  const db = options.db ?? prisma
  const superAdmin = await db.adminRole.upsert({
    where: {
      tenantId_name: {
        tenantId,
        name: DEFAULT_ADMIN_ROLE_NAMES.superAdmin
      }
    },
    update: {
      description: '拥有所有权限的系统管理员',
      permissions: [ADMIN_PERMISSION_WILDCARD],
      isSystem: true
    },
    create: {
      tenantId,
      name: DEFAULT_ADMIN_ROLE_NAMES.superAdmin,
      description: '拥有所有权限的系统管理员',
      permissions: [ADMIN_PERMISSION_WILDCARD],
      isSystem: true
    }
  })

  if (!options.includeReadOnly) {
    return { superAdmin }
  }

  const readOnlyAdmin = await db.adminRole.upsert({
    where: {
      tenantId_name: {
        tenantId,
        name: DEFAULT_ADMIN_ROLE_NAMES.readOnlyAdmin
      }
    },
    update: {
      description: '仅拥有管理员和角色查看权限的系统管理员',
      permissions: [...ADMIN_READ_ONLY_PERMISSION_CODES],
      isSystem: true
    },
    create: {
      tenantId,
      name: DEFAULT_ADMIN_ROLE_NAMES.readOnlyAdmin,
      description: '仅拥有管理员和角色查看权限的系统管理员',
      permissions: [...ADMIN_READ_ONLY_PERMISSION_CODES],
      isSystem: true
    }
  })

  return { superAdmin, readOnlyAdmin }
}
