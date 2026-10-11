import type { PasswordForm } from './types'

/**
 * 修改密码表单规则：确认密码需要与新密码一致，因此依赖表单对象。
 */
export const createPasswordRules = (form: PasswordForm) => {
  return {
    currentPassword: [{ required: true, message: '请输入当前密码' }],
    newPassword: [{ required: true, message: '请输入新密码' }],
    confirmPassword: [
      { required: true, message: '请确认新密码' },
      {
        validator: (_rule: unknown, value: string) => {
          if (value !== form.newPassword) {
            return Promise.reject('两次输入的新密码不一致')
          }
          return Promise.resolve()
        }
      }
    ]
  }
}
