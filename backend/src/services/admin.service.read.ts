import type { Prisma } from '@prisma/client'
import { AppError } from '../middleware/errorHandler'
import type { AdminManagementContext } from '../middleware/admin-management-auth'
import prisma from '../lib/prisma'
import type { AdminQueryDto, AdminStats } from '../types/admin.types'
import type { AdminDto, AdminListResult } from './admin.service.helpers'
import {
  buildPublicMembershipSelect,
  OWNER_ROLE,
  toDto,
  validateContext,
  validatePagination
} from './admin.service.helpers'

export async function findAllAdmins(
  context: AdminManagementContext,
  query: AdminQueryDto
): Promise<AdminListResult> {
  validateContext(context)

  const page = query.page ?? 1
  const pageSize = query.pageSize ?? 10
  validatePagination(page, pageSize)

  const adminWhere: Prisma.AdminWhereInput = {}
  if (query.username) adminWhere.username = { contains: query.username }
  if (query.email) adminWhere.email = { contains: query.email }
  if (query.phone) adminWhere.phone = { contains: query.phone }
  if (query.status) adminWhere.status = query.status
  if (query.roleId) {
    adminWhere.roles = {
      some: { id: query.roleId, tenantId: context.tenantId }
    }
  }

  const where: Prisma.AdminTenantWhereInput = {
    tenantId: context.tenantId,
    admin: adminWhere
  }
  const [memberships, total] = await Promise.all([
    prisma.adminTenant.findMany({
      where,
      skip: (page - 1) * pageSize,
      take: pageSize,
      orderBy: { createdAt: 'desc' },
      select: buildPublicMembershipSelect(context.tenantId)
    }),
    prisma.adminTenant.count({ where })
  ])

  return {
    admins: memberships.map(toDto),
    total,
    page,
    pageSize
  }
}

export async function findAdminById(
  context: AdminManagementContext,
  adminId: string
): Promise<AdminDto> {
  validateContext(context)
  const membership = await prisma.adminTenant.findUnique({
    where: {
      adminId_tenantId: { adminId, tenantId: context.tenantId }
    },
    select: buildPublicMembershipSelect(context.tenantId)
  })

  if (!membership) {
    throw new AppError('管理员不存在或不属于当前租户', 404)
  }

  return toDto(membership)
}

export async function getAdminStats(
  context: AdminManagementContext
): Promise<AdminStats> {
  validateContext(context)
  const tenantWhere: Prisma.AdminTenantWhereInput = {
    tenantId: context.tenantId
  }
  const [
    totalAdmins,
    activeAdmins,
    disabledAdmins,
    mfaEnabledAdmins,
    ownerCount
  ] = await Promise.all([
    prisma.adminTenant.count({ where: tenantWhere }),
    prisma.adminTenant.count({
      where: { ...tenantWhere, admin: { status: 'active' } }
    }),
    prisma.adminTenant.count({
      where: { ...tenantWhere, admin: { status: 'disabled' } }
    }),
    prisma.adminTenant.count({
      where: { ...tenantWhere, admin: { mfaEnabled: true } }
    }),
    prisma.adminTenant.count({
      where: { ...tenantWhere, role: OWNER_ROLE }
    })
  ])

  return { totalAdmins, activeAdmins, disabledAdmins, mfaEnabledAdmins, ownerCount }
}
