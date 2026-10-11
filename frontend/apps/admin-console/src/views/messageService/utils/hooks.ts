import axios from 'axios'
import type { SelectOption, TemplateType } from './types'

/**
 * 消息模板字典：模板类型选项，列表页与编辑弹窗共用。
 */
export function useTemplateDict() {
  const templateTypeOptions: SelectOption<TemplateType>[] = [
    { value: 'email', label: '邮件' },
    { value: 'sms', label: '短信' }
  ]

  return {
    templateTypeOptions
  }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data?.msg || fallback
  }
  return error instanceof Error ? error.message : fallback
}
