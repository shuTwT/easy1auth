export const profileFormRules = {
  username: [
    { required: true, message: '请输入用户名' },
    { max: 100, message: '用户名不能超过 100 个字符' }
  ],
  phone: [
    { pattern: /^\+?[0-9][0-9 -]{5,31}$/, message: '请输入有效的手机号' }
  ]
}

/**
 * 邮箱换绑校验：邮箱格式与 6 位数字验证码。
 */
export const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const emailCodePattern = /^\d{6}$/

export const emailRuleMessages = {
  emailInvalid: '请输入正确的新邮箱地址',
  codeInvalid: '请输入 6 位邮箱验证码'
}
