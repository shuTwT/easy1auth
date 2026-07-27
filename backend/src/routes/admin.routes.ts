import { Router, type Response } from 'express'
import {
  adminManagementAuth,
  type AdminManagementRequest
} from '../middleware/admin-management-auth'
import { AdminService } from '../services/admin.service'
import auditService from '../services/audit.service'
import type { AdminQueryDto, AdminStatus } from '../types/admin.types'
import {
  getAdminContext,
  getRouteId,
  readNumber,
  readString,
  readStringList,
  runMutation,
  runRead,
  type AuditChanges
} from './admin-route.helpers'

const router: Router = Router()
const adminService = new AdminService()

type AdminChangeInput = {
  readonly username?: unknown
  readonly email?: unknown
  readonly phone?: unknown
}

type MutationAudit = {
  readonly request: AdminManagementRequest
  readonly action: string
  readonly resourceId: string
  readonly succeeded: boolean
  readonly changes: AuditChanges
  readonly errorMessage?: string
}

const readAdminStatus = (value: unknown): AdminStatus | undefined => {
  const status = readString(value)
  return status === 'active' || status === 'disabled' ? status : undefined
}

const getAdminChanges = (body: AdminChangeInput): AuditChanges => ({
  ...(typeof body.username === 'string' ? { username: body.username } : {}),
  ...(typeof body.email === 'string' ? { email: body.email } : {}),
  ...(typeof body.phone === 'string' ? { phone: body.phone } : {})
})

const getAdminQuery = (req: AdminManagementRequest): AdminQueryDto => {
  const username = readString(req.query.username)
  const email = readString(req.query.email)
  const phone = readString(req.query.phone)
  const roleId = readString(req.query.roleId)
  const status = readAdminStatus(req.query.status)
  return {
    page: readNumber(req.query.page, 1),
    pageSize: readNumber(req.query.pageSize, 10),
    ...(username === undefined ? {} : { username }),
    ...(email === undefined ? {} : { email }),
    ...(phone === undefined ? {} : { phone }),
    ...(roleId === undefined ? {} : { roleId }),
    ...(status === undefined ? {} : { status })
  }
}

const writeMutationAudit = async ({
  request,
  action,
  resourceId,
  succeeded,
  changes,
  errorMessage
}: MutationAudit): Promise<void> => {
  const context = request.adminContext
  if (!context) return

  try {
    await auditService.create(context.tenantId, {
      userId: context.adminId,
      type: 'system',
      action,
      resource: 'admin-user',
      resourceId,
      method: request.method,
      ip: request.ip ?? 'unknown',
      ...(typeof request.headers['user-agent'] === 'string'
        ? { userAgent: request.headers['user-agent'] }
        : {}),
      status: succeeded ? 'success' : 'failed',
      changes,
      ...(errorMessage === undefined ? {} : { errorMessage })
    })
  } catch (auditError) {
    console.error(
      '管理员操作审计日志写入失败:',
      auditError instanceof Error ? auditError.message : auditError
    )
  }
}

router.get('/stats', adminManagementAuth(['admin-user:read']), (req: AdminManagementRequest, res: Response) => runRead(
  res,
  () => adminService.getStats(getAdminContext(req)),
  '获取管理员统计失败'
))

router.get('/', adminManagementAuth(['admin-user:read']), (req: AdminManagementRequest, res: Response) => runRead(
  res,
  () => adminService.findAll(getAdminContext(req), getAdminQuery(req)),
  '获取管理员列表失败'
))

router.get('/:id', adminManagementAuth(['admin-user:read']), (req: AdminManagementRequest, res: Response) => runRead(
  res,
  () => adminService.findById(
    getAdminContext(req),
    getRouteId(req, '管理员标识无效')
  ),
  '获取管理员详情失败'
))

router.put('/:id', adminManagementAuth(['admin-user:update']), (req: AdminManagementRequest, res: Response) => {
  const id = getRouteId(req, '管理员标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'update',
      resourceId: id,
      succeeded,
      changes: getAdminChanges(req.body),
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminService.update(getAdminContext(req), id, req.body),
    successMessage: '管理员信息更新成功',
    failureMessage: '更新管理员失败'
  })
})

router.put('/:id/status', adminManagementAuth(['admin-user:status']), (req: AdminManagementRequest, res: Response) => {
  const id = getRouteId(req, '管理员标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'status',
      resourceId: id,
      succeeded,
      changes: {
        ...(typeof req.body?.status === 'string' ? { status: req.body.status } : {})
      },
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminService.updateStatus(getAdminContext(req), id, req.body),
    successMessage: '管理员状态更新成功',
    failureMessage: '更新管理员状态失败'
  })
})

router.post('/:id/reset-password', adminManagementAuth(['admin-user:reset-password']), (req: AdminManagementRequest, res: Response) => {
  const id = getRouteId(req, '管理员标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'reset-password',
      resourceId: id,
      succeeded,
      changes: { reset: succeeded },
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminService.resetPassword(getAdminContext(req), id, req.body),
    successMessage: '管理员密码重置成功',
    failureMessage: '重置管理员密码失败'
  })
})

router.post('/:id/reset-mfa', adminManagementAuth(['admin-user:reset-mfa']), (req: AdminManagementRequest, res: Response) => {
  const id = getRouteId(req, '管理员标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'reset-mfa',
      resourceId: id,
      succeeded,
      changes: { reset: succeeded },
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminService.resetMfa(getAdminContext(req), id),
    successMessage: '管理员 MFA 重置成功',
    failureMessage: '重置管理员 MFA 失败'
  })
})

router.put('/:id/roles', adminManagementAuth(['admin-user:assign-role']), (req: AdminManagementRequest, res: Response) => {
  const id = getRouteId(req, '管理员标识无效')
  const roleIds = readStringList(req.body?.roleIds)
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'assign-role',
      resourceId: id,
      succeeded,
      changes: roleIds === undefined ? {} : { roleIds },
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminService.assignRoles(getAdminContext(req), id, req.body),
    successMessage: '管理员角色分配成功',
    failureMessage: '分配管理员角色失败'
  })
})

router.delete('/:id/tenant', adminManagementAuth(['admin-user:remove-tenant']), (req: AdminManagementRequest, res: Response) => {
  const id = getRouteId(req, '管理员标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'remove-tenant',
      resourceId: id,
      succeeded,
      changes: { removed: succeeded },
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminService.removeFromTenant(getAdminContext(req), id),
    successMessage: '管理员已移出租户',
    failureMessage: '移除管理员失败'
  })
})

export default router
