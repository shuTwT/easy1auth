import type { InitializeForm } from './types'

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,128}$/

export const accountRules = [
  { required: true, message: '请输入系统管理员账号' },
  { type: 'email' as const, message: '请输入有效的邮箱地址' },
  { max: 320, message: '邮箱地址不能超过 320 个字符' }
]

export const passwordRules = [
  { required: true, message: '请输入管理员密码' },
  {
    pattern: passwordPattern,
    message: '密码需为 8–128 位，并包含大写字母、小写字母和数字'
  }
]

/**
 * 确认密码规则：需要与密码字段一致，因此依赖表单对象。
 */
export const createConfirmPasswordRules = (model: InitializeForm) => {
  return [
    { required: true, message: '请再次输入密码' },
    {
      validator: async (_rule: unknown, value: string) => {
        if (value === model.password) {
          return Promise.resolve()
        }
        return Promise.reject(new Error('两次输入的密码不一致'))
      }
    }
  ]
}
