import type { AdminManagementContext } from '../middleware/admin-management-auth'
import type {
  AdminQueryDto,
  AdminStats,
  AssignRolesDto,
  ChangeAdminStatusDto,
  ResetPasswordDto,
  UpdateAdminDto
} from '../types/admin.types'
import {
  findAllAdmins,
  findAdminById,
  getAdminStats
} from './admin.service.read'
import {
  resetAdminMfa,
  updateAdmin,
  updateAdminStatus
} from './admin.service.account'
import { resetAdminPassword } from './admin.service.password'
import {
  assignAdminRoles,
  removeAdminFromTenant
} from './admin.service.membership'
import type { AdminDto, AdminListResult } from './admin.service.helpers'

export type { AdminDto, AdminListResult } from './admin.service.helpers'

export class AdminService {
  async findAll(
    context: AdminManagementContext,
    query: AdminQueryDto
  ): Promise<AdminListResult> {
    return findAllAdmins(context, query)
  }

  async findById(
    context: AdminManagementContext,
    adminId: string
  ): Promise<AdminDto> {
    return findAdminById(context, adminId)
  }

  async getStats(context: AdminManagementContext): Promise<AdminStats> {
    return getAdminStats(context)
  }

  async update(
    context: AdminManagementContext,
    adminId: string,
    data: UpdateAdminDto
  ): Promise<AdminDto> {
    return updateAdmin(context, adminId, data)
  }

  async updateStatus(
    context: AdminManagementContext,
    adminId: string,
    data: ChangeAdminStatusDto
  ): Promise<AdminDto> {
    return updateAdminStatus(context, adminId, data)
  }

  async resetPassword(
    context: AdminManagementContext,
    adminId: string,
    data: ResetPasswordDto
  ): Promise<AdminDto> {
    return resetAdminPassword(context, adminId, data)
  }

  async resetMfa(
    context: AdminManagementContext,
    adminId: string
  ): Promise<AdminDto> {
    return resetAdminMfa(context, adminId)
  }

  async assignRoles(
    context: AdminManagementContext,
    adminId: string,
    data: AssignRolesDto
  ): Promise<AdminDto> {
    return assignAdminRoles(context, adminId, data)
  }

  async removeFromTenant(
    context: AdminManagementContext,
    adminId: string
  ): Promise<void> {
    return removeAdminFromTenant(context, adminId)
  }
}
