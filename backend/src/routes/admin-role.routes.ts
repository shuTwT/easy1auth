import { Router, type Response } from 'express'
import {
  adminManagementAuth,
  type AdminManagementRequest
} from '../middleware/admin-management-auth'
import adminRoleService from '../services/admin-role.service'
import auditService from '../services/audit.service'
import type { AdminRoleQueryDto } from '../types/admin-role.types'
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

type RoleChangeInput = {
  readonly name?: unknown
  readonly description?: unknown
  readonly permissions?: unknown
}

type MutationAudit = {
  readonly request: AdminManagementRequest
  readonly action: string
  readonly resourceId?: string
  readonly succeeded: boolean
  readonly changes: AuditChanges
  readonly errorMessage?: string
}

const readBoolean = (value: unknown): boolean | undefined => {
  const text = readString(value)
  if (text === 'true') return true
  if (text === 'false') return false
  return undefined
}

const getRoleChanges = (body: RoleChangeInput): AuditChanges => {
  const permissions = readStringList(body.permissions)
  return {
    ...(typeof body.name === 'string' ? { name: body.name } : {}),
    ...(typeof body.description === 'string' ? { description: body.description } : {}),
    ...(permissions === undefined ? {} : { permissions })
  }
}

const getRoleQuery = (req: AdminManagementRequest): AdminRoleQueryDto => {
  const name = readString(req.query.name)
  const isSystem = readBoolean(req.query.isSystem)
  return {
    page: readNumber(req.query.page, 1),
    pageSize: readNumber(req.query.pageSize, 10),
    ...(name === undefined ? {} : { name }),
    ...(isSystem === undefined ? {} : { isSystem })
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
      type: 'role',
      action,
      resource: 'admin-role',
      ...(resourceId === undefined ? {} : { resourceId }),
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
      '管理员角色操作审计日志写入失败:',
      auditError instanceof Error ? auditError.message : auditError
    )
  }
}

router.get('/stats', adminManagementAuth(['admin-role:read']), (
  req: AdminManagementRequest,
  res: Response
) => runRead(
  res,
  () => adminRoleService.getStats(getAdminContext(req).tenantId),
  '获取管理员角色统计失败'
))

router.get('/permissions/catalog', adminManagementAuth(['admin-role:read']), (
  req: AdminManagementRequest,
  res: Response
) => runRead(
  res,
  () => adminRoleService.getPermissionCatalog(getAdminContext(req).tenantId),
  '获取管理员权限目录失败'
))

router.get('/', adminManagementAuth(['admin-role:read']), (
  req: AdminManagementRequest,
  res: Response
) => runRead(
  res,
  () => adminRoleService.findAll(getAdminContext(req).tenantId, getRoleQuery(req)),
  '获取管理员角色列表失败'
))

router.get('/:id', adminManagementAuth(['admin-role:read']), (
  req: AdminManagementRequest,
  res: Response
) => runRead(
  res,
  () => adminRoleService.findById(
    getAdminContext(req).tenantId,
    getRouteId(req, '管理员角色标识无效')
  ),
  '获取管理员角色详情失败'
))

router.post('/', adminManagementAuth(['admin-role:create']), (
  req: AdminManagementRequest,
  res: Response
) => runMutation(res, {
  audit: (succeeded, errorMessage) => writeMutationAudit({
    request: req,
    action: 'create',
    succeeded,
    changes: getRoleChanges(req.body),
    ...(errorMessage === undefined ? {} : { errorMessage })
  }),
  execute: () => adminRoleService.create(getAdminContext(req).tenantId, req.body),
  successMessage: '管理员角色创建成功',
  failureMessage: '创建管理员角色失败',
  statusCode: 201
}))

router.put('/:id', adminManagementAuth(['admin-role:update']), (
  req: AdminManagementRequest,
  res: Response
) => {
  const id = getRouteId(req, '管理员角色标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'update',
      resourceId: id,
      succeeded,
      changes: getRoleChanges(req.body),
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminRoleService.update(getAdminContext(req).tenantId, id, req.body),
    successMessage: '管理员角色更新成功',
    failureMessage: '更新管理员角色失败'
  })
})

router.delete('/:id', adminManagementAuth(['admin-role:delete']), (
  req: AdminManagementRequest,
  res: Response
) => {
  const id = getRouteId(req, '管理员角色标识无效')
  return runMutation(res, {
    audit: (succeeded, errorMessage) => writeMutationAudit({
      request: req,
      action: 'delete',
      resourceId: id,
      succeeded,
      changes: { deleted: succeeded },
      ...(errorMessage === undefined ? {} : { errorMessage })
    }),
    execute: () => adminRoleService.delete(getAdminContext(req).tenantId, id),
    successMessage: '管理员角色删除成功',
    failureMessage: '删除管理员角色失败'
  })
})

export default router
