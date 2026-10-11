/**
 * 租户编码格式：1 至 100 位小写字母、数字、下划线或连字符。
 */
export const tenantCodePattern = /^[a-z0-9_-]{1,100}$/

export const tenantRuleMessages = {
  nameRequired: '请输入租户名称',
  codeInvalid: '租户编码须为1至100位小写字母、数字、下划线或连字符',
}
