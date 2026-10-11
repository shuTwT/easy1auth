import type { RuleObject } from 'antdv-next'
import type { ResetPasswordForm } from './types'

export const ROLE_CODE_PATTERN = /^[a-z][a-z0-9_]{1,99}$/

export const roleFormRules = {
  name: [{ required: true, message: '请输入角色名称' }],
  code: [
    { required: true, message: '请输入角色编码' },
    { pattern: ROLE_CODE_PATTERN, message: '请输入2至100位小写字母、数字或下划线，且以字母开头' }
  ]
}

/**
 * 重置密码表单规则：确认密码需与新密码一致，因此依赖表单实例。
 */
export const createResetPasswordRules = (form: ResetPasswordForm) => {
  return {
    newPassword: [{ required: true, message: '请输入新密码' }],
    confirmPassword: [
      { required: true, message: '请确认密码' },
      {
        validator: (_rule: RuleObject, value: string) => {
          if (value === form.newPassword) {
            return Promise.resolve()
          }
          return Promise.reject('两次输入的密码不一致')
        }
      }
    ]
  }
}
