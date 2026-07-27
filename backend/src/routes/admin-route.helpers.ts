import type { Response } from 'express'
import { AppError } from '../middleware/errorHandler'
import type { AdminManagementRequest } from '../middleware/admin-management-auth'

export type AuditChanges = Record<string, string | boolean | string[] | null>

export type MutationConfig<TResult> = {
  readonly audit: (succeeded: boolean, errorMessage?: string) => Promise<void>
  readonly execute: () => Promise<TResult>
  readonly successMessage: string
  readonly failureMessage: string
  readonly statusCode?: number
}

export const readString = (value: unknown): string | undefined =>
  typeof value === 'string' ? value : undefined

export const readNumber = (value: unknown, fallback: number): number => {
  const text = readString(value)
  if (text === undefined) return fallback
  const parsed = Number.parseInt(text, 10)
  return Number.isNaN(parsed) ? fallback : parsed
}

export const readStringList = (value: unknown): string[] | undefined => {
  if (!Array.isArray(value)) return undefined
  const values = value.filter((item): item is string => typeof item === 'string')
  return values.length === value.length ? values : undefined
}

export const getAdminContext = (req: AdminManagementRequest) => {
  if (!req.adminContext) throw new AppError('管理员上下文缺失', 401)
  return req.adminContext
}

export const getRouteId = (
  req: AdminManagementRequest,
  invalidMessage: string
): string => {
  const id = req.params.id
  if (typeof id !== 'string') throw new AppError(invalidMessage, 400)
  return id
}

export const sendRouteError = (
  res: Response,
  error: unknown,
  fallbackMessage: string
): void => {
  if (error instanceof AppError) {
    res.status(error.statusCode).json({ status: 'error', message: error.message })
    return
  }

  console.error(fallbackMessage, error)
  res.status(500).json({ status: 'error', message: fallbackMessage })
}

export const runRead = async <TResult>(
  res: Response,
  execute: () => Promise<TResult>,
  failureMessage: string
): Promise<void> => {
  try {
    res.json({ status: 'success', data: await execute() })
  } catch (error) {
    if (error instanceof Error) {
      sendRouteError(res, error, failureMessage)
      return
    }

    sendRouteError(res, new AppError('服务器内部错误', 500), failureMessage)
  }
}

export const runMutation = async <TResult>(
  res: Response,
  config: MutationConfig<TResult>
): Promise<void> => {
  try {
    const result = await config.execute()
    await config.audit(true)
    const response = { status: 'success', message: config.successMessage }
    res
      .status(config.statusCode ?? 200)
      .json(result === undefined ? response : { ...response, data: result })
  } catch (error) {
    await config.audit(false, error instanceof Error ? error.message : '服务器内部错误')
    sendRouteError(res, error, config.failureMessage)
  }
}
