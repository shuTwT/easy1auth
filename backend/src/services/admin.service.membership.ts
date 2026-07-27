import { AppError } from '../middleware/errorHandler'
import type { AdminManagementContext } from '../middleware/admin-management-auth'
import prisma from '../lib/prisma'
import type { AssignRolesDto } from '../types/admin.types'
import type { AdminDto } from './admin.service.helpers'
import {
  assertCanManageOwner,
  canAssignOwnRoles,
  OWNER_ROLE,
  requireTargetMembership,
  TARGET_MEMBERSHIP_SELECT,
  validateContext
} from './admin.service.helpers'
import { findAdminById } from './admin.service.read'

export async function assignAdminRoles(
  context: AdminManagementContext,
  adminId: string,
  data: AssignRolesDto
): Promise<AdminDto> {
  validateContext(context)
  const target = await requireTargetMembership(context, adminId)
  assertCanManageOwner(context, target)
  if (
    !Array.isArray(data.roleIds) ||
    data.roleIds.some(
      (roleId) => typeof roleId !== 'string' || !roleId.trim()
    )
  ) {
    throw new AppError('角色参数无效', 400)
  }

  const roleIds = [...new Set(data.roleIds)]
  await prisma.$transaction(async (tx) => {
    const roles = await tx.adminRole.findMany({
      where: { tenantId: context.tenantId, id: { in: roleIds } },
      select: { id: true, permissions: true }
    })
    if (roles.length !== roleIds.length) {
      throw new AppError('部分角色不存在或不属于当前租户', 400)
    }
    if (adminId === context.adminId && !canAssignOwnRoles(context, roles)) {
      throw new AppError('不能为自己授予当前不具备的权限', 403)
    }

    const currentRoles = await tx.adminRole.findMany({
      where: {
        tenantId: context.tenantId,
        admins: { some: { id: adminId } }
      },
      select: { id: true }
    })
    const currentRoleIds = new Set(currentRoles.map(({ id }) => id))
    const requestedRoleIds = new Set(roleIds)
    await tx.admin.update({
      where: { id: adminId },
      data: {
        roles: {
          disconnect: currentRoles
            .filter(({ id }) => !requestedRoleIds.has(id))
            .map(({ id }) => ({ id })),
          connect: roles
            .filter(({ id }) => !currentRoleIds.has(id))
            .map(({ id }) => ({ id }))
        }
      }
    })
  })
  return findAdminById(context, adminId)
}

export async function removeAdminFromTenant(
  context: AdminManagementContext,
  adminId: string
): Promise<void> {
  validateContext(context)
  if (adminId === context.adminId) {
    throw new AppError('不能将自己移出租户', 400)
  }

  await prisma.$transaction(async (tx) => {
    const target = await tx.adminTenant.findUnique({
      where: {
        adminId_tenantId: { adminId, tenantId: context.tenantId }
      },
      select: TARGET_MEMBERSHIP_SELECT
    })
    if (!target) throw new AppError('管理员不存在或不属于当前租户', 404)
    assertCanManageOwner(context, target)

    if (target.role === OWNER_ROLE && target.admin.status === 'active') {
      const activeOwnerCount = await tx.adminTenant.count({
        where: {
          tenantId: context.tenantId,
          role: OWNER_ROLE,
          admin: { status: 'active' }
        }
      })
      if (activeOwnerCount <= 1) {
        throw new AppError('不能移除当前租户最后一个可用所有者', 400)
      }
    }

    await tx.adminTenant.delete({
      where: {
        adminId_tenantId: { adminId, tenantId: context.tenantId }
      }
    })
    const remainingMemberships = await tx.adminTenant.count({ where: { adminId } })
    if (remainingMemberships === 0) {
      await tx.admin.update({
        where: { id: adminId },
        data: { currentTenantId: null }
      })
    }
  })
}
