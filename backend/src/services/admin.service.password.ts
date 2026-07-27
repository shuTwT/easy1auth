import bcrypt from 'bcryptjs'
import { AppError } from '../middleware/errorHandler'
import type { AdminManagementContext } from '../middleware/admin-management-auth'
import prisma from '../lib/prisma'
import type { ResetPasswordDto } from '../types/admin.types'
import { addPasswordToHistory, checkPasswordHistory, getPasswordPolicy, validatePassword } from './passwordPolicy.service'
import type { AdminDto } from './admin.service.helpers'
import {
  assertCanManageOwner,
  PASSWORD_TARGET_SELECT,
  validateContext
} from './admin.service.helpers'
import { findAdminById } from './admin.service.read'

export async function resetAdminPassword(
  context: AdminManagementContext,
  adminId: string,
  data: ResetPasswordDto
): Promise<AdminDto> {
  validateContext(context)
  const target = await prisma.adminTenant.findUnique({
    where: {
      adminId_tenantId: { adminId, tenantId: context.tenantId }
    },
    select: PASSWORD_TARGET_SELECT
  })
  if (!target) throw new AppError('管理员不存在或不属于当前租户', 404)
  assertCanManageOwner(context, target)

  const policy = getPasswordPolicy()
  const validation = validatePassword(data.newPassword, policy)
  if (!validation.valid) throw new AppError(validation.errors.join(', '), 400)
  const isNotReused = await checkPasswordHistory(
    adminId,
    data.newPassword,
    policy.historyCount
  )
  if (!isNotReused) {
    throw new AppError(`不能使用最近${policy.historyCount}次使用过的密码`, 400)
  }

  const hashedPassword = await bcrypt.hash(data.newPassword, 10)
  await prisma.admin.update({
    where: { id: adminId },
    data: { password: hashedPassword, passwordChangedAt: new Date() }
  })
  await addPasswordToHistory(adminId, target.admin.password)
  return findAdminById(context, adminId)
}
