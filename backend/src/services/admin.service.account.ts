import type { Prisma } from '@prisma/client'
import { AppError } from '../middleware/errorHandler'
import type { AdminManagementContext } from '../middleware/admin-management-auth'
import prisma from '../lib/prisma'
import type {
  ChangeAdminStatusDto,
  UpdateAdminDto
} from '../types/admin.types'
import type { AdminDto } from './admin.service.helpers'
import {
  assertCanManageOwner,
  isAdminStatus,
  OWNER_ROLE,
  requireTargetMembership,
  TARGET_MEMBERSHIP_SELECT,
  validateContext
} from './admin.service.helpers'
import { findAdminById } from './admin.service.read'

export async function updateAdmin(
  context: AdminManagementContext,
  adminId: string,
  data: UpdateAdminDto
): Promise<AdminDto> {
  validateContext(context)
  const target = await requireTargetMembership(context, adminId)
  assertCanManageOwner(context, target)

  const updateData: Prisma.AdminUpdateInput = {}
  const duplicateChecks: Prisma.AdminWhereInput[] = []
  if (data.username !== undefined) {
    if (!data.username.trim()) throw new AppError('用户名不能为空', 400)
    updateData.username = data.username
    duplicateChecks.push({ username: data.username })
  }
  if (data.email !== undefined) {
    if (!data.email.trim()) throw new AppError('邮箱不能为空', 400)
    updateData.email = data.email
    duplicateChecks.push({ email: data.email })
  }
  if (data.phone !== undefined) updateData.phone = data.phone.trim() || null

  if (duplicateChecks.length > 0) {
    const existingAdmin = await prisma.admin.findFirst({
      where: { id: { not: adminId }, OR: duplicateChecks },
      select: { username: true, email: true }
    })
    if (existingAdmin?.username === data.username) {
      throw new AppError('用户名已存在', 400)
    }
    if (existingAdmin?.email === data.email) {
      throw new AppError('邮箱已存在', 400)
    }
  }
  if (Object.keys(updateData).length === 0) {
    throw new AppError('没有可更新的管理员信息', 400)
  }

  await prisma.admin.update({ where: { id: adminId }, data: updateData })
  return findAdminById(context, adminId)
}

export async function updateAdminStatus(
  context: AdminManagementContext,
  adminId: string,
  data: ChangeAdminStatusDto
): Promise<AdminDto> {
  validateContext(context)
  if (!isAdminStatus(data.status)) {
    throw new AppError('管理员状态无效', 400)
  }
  if (adminId === context.adminId) throw new AppError('不能禁用自己', 400)

  await prisma.$transaction(async (tx) => {
    const target = await tx.adminTenant.findUnique({
      where: {
        adminId_tenantId: { adminId, tenantId: context.tenantId }
      },
      select: TARGET_MEMBERSHIP_SELECT
    })
    if (!target) throw new AppError('管理员不存在或不属于当前租户', 404)
    assertCanManageOwner(context, target)

    if (
      data.status === 'disabled' &&
      target.admin.status === 'active' &&
      target.role === OWNER_ROLE
    ) {
      const activeOwnerCount = await tx.adminTenant.count({
        where: {
          tenantId: context.tenantId,
          role: OWNER_ROLE,
          admin: { status: 'active' }
        }
      })
      if (activeOwnerCount <= 1) {
        throw new AppError('不能禁用当前租户最后一个可用所有者', 400)
      }
    }
    await tx.admin.update({ where: { id: adminId }, data: { status: data.status } })
  })

  return findAdminById(context, adminId)
}

export async function resetAdminMfa(
  context: AdminManagementContext,
  adminId: string
): Promise<AdminDto> {
  validateContext(context)
  const target = await requireTargetMembership(context, adminId)
  assertCanManageOwner(context, target)
  await prisma.admin.update({
    where: { id: adminId },
    data: { mfaEnabled: false, mfaSecret: null, mfaType: null }
  })
  return findAdminById(context, adminId)
}
