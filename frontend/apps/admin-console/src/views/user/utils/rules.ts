import type { ResetPasswordForm } from './types'

export const userFormRules = {
  username: [{ required: true, message: '请输入用户名' }],
  email: [{ required: true, message: '请输入邮箱' }],
  name: [{ required: true, message: '请输入姓名' }]
}

export const userPasswordRules = [{ required: true, message: '请输入密码' }]

/**
 * 重置密码表单规则：确认密码需要与新密码一致，因此依赖表单对象。
 */
export const createResetPasswordRules = (form: ResetPasswordForm) => {
  return {
    newPassword: [{ required: true, message: '请输入新密码' }],
    confirmPassword: [
      { required: true, message: '请确认新密码' },
      {
        validator: (_rule: unknown, value: string) => {
          if (value !== form.newPassword) {
            return Promise.reject('两次输入的密码不一致')
          }
          return Promise.resolve()
        }
      }
    ]
  }
}
