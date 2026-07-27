import type { Prisma } from '@prisma/client'
import { AppError } from '../middleware/errorHandler'
import type { AdminManagementContext } from '../middleware/admin-management-auth'
import prisma from '../lib/prisma'
import type { AdminStatus } from '../types/admin.types'

export interface AdminRoleSummary {
  readonly id: string
  readonly name: string
  readonly permissions: readonly string[]
}

export interface AdminDto {
  readonly id: string
  readonly tenantId: string
  readonly tenantRole: string
  readonly currentTenantId: string | null
  readonly username: string
  readonly email: string
  readonly phone: string | null
  readonly status: AdminStatus
  readonly mfaEnabled: boolean
  readonly lastLoginAt: Date | null
  readonly createdAt: Date
  readonly updatedAt: Date
  readonly roles: readonly AdminRoleSummary[]
}

export interface AdminListResult {
  readonly admins: readonly AdminDto[]
  readonly total: number
  readonly page: number
  readonly pageSize: number
}

export const OWNER_ROLE = 'owner'
const ADMIN_STATUSES = ['active', 'disabled'] as const

export const buildPublicMembershipSelect = (tenantId: string) => ({
  tenantId: true,
  role: true,
  admin: {
    select: {
      id: true,
      currentTenantId: true,
      username: true,
      email: true,
      phone: true,
      status: true,
      mfaEnabled: true,
      lastLoginAt: true,
      createdAt: true,
      updatedAt: true,
      roles: {
        where: { tenantId },
        select: {
          id: true,
          name: true,
          permissions: true
        }
      }
    }
  }
}) satisfies Prisma.AdminTenantSelect

export const TARGET_MEMBERSHIP_SELECT = {
  role: true,
  admin: {
    select: {
      id: true,
      status: true
    }
  }
} satisfies Prisma.AdminTenantSelect

export const PASSWORD_TARGET_SELECT = {
  role: true,
  admin: {
    select: {
      id: true,
      status: true,
      password: true
    }
  }
} satisfies Prisma.AdminTenantSelect

export type PublicMembership = Prisma.AdminTenantGetPayload<{
  select: ReturnType<typeof buildPublicMembershipSelect>
}>

export type TargetMembership = Prisma.AdminTenantGetPayload<{
  select: typeof TARGET_MEMBERSHIP_SELECT
}>

export const toPermissionList = (
  permissions: Prisma.JsonValue
): readonly string[] => {
  if (!Array.isArray(permissions)) {
    return []
  }

  return permissions.filter(
    (permission): permission is string => typeof permission === 'string'
  )
}

export const toDto = (membership: PublicMembership): AdminDto => ({
  id: membership.admin.id,
  tenantId: membership.tenantId,
  tenantRole: membership.role,
  currentTenantId: membership.admin.currentTenantId,
  username: membership.admin.username,
  email: membership.admin.email,
  phone: membership.admin.phone,
  status: membership.admin.status as AdminStatus,
  mfaEnabled: membership.admin.mfaEnabled,
  lastLoginAt: membership.admin.lastLoginAt,
  createdAt: membership.admin.createdAt,
  updatedAt: membership.admin.updatedAt,
  roles: membership.admin.roles.map((role) => ({
    id: role.id,
    name: role.name,
    permissions: toPermissionList(role.permissions)
  }))
})

export const validateContext = (context: AdminManagementContext): void => {
  if (!context.adminId.trim() || !context.tenantId.trim()) {
    throw new AppError('管理员上下文无效', 400)
  }
}

export const validatePagination = (page: number, pageSize: number): void => {
  if (
    !Number.isInteger(page) ||
    !Number.isInteger(pageSize) ||
    page < 1 ||
    pageSize < 1 ||
    pageSize > 100
  ) {
    throw new AppError('分页参数无效', 400)
  }
}

export const isAdminStatus = (status: AdminStatus): status is AdminStatus =>
  ADMIN_STATUSES.includes(status)

export const requireTargetMembership = async (
  context: AdminManagementContext,
  adminId: string
): Promise<TargetMembership> => {
  const membership = await prisma.adminTenant.findUnique({
    where: {
      adminId_tenantId: {
        adminId,
        tenantId: context.tenantId
      }
    },
    select: TARGET_MEMBERSHIP_SELECT
  })

  if (!membership) {
    throw new AppError('管理员不存在或不属于当前租户', 404)
  }

  return membership
}

export const assertCanManageOwner = (
  context: AdminManagementContext,
  target: Pick<TargetMembership, 'role'>
): void => {
  if (target.role === OWNER_ROLE && !context.isOwner) {
    throw new AppError('只有租户所有者可以管理租户所有者', 403)
  }
}

export const canAssignOwnRoles = (
  context: AdminManagementContext,
  roles: readonly { readonly permissions: Prisma.JsonValue }[]
): boolean => {
  if (context.permissions.includes('*')) {
    return true
  }

  const currentPermissions = new Set(context.permissions)
  return roles.every((role) =>
    toPermissionList(role.permissions).every((permission) =>
      currentPermissions.has(permission)
    )
  )
}
