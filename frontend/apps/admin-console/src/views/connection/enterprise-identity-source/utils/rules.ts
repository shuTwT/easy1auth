import { ENTERPRISE_SOURCE_TYPES, SOURCE_CONFIGS } from '@/types/loginIdentitySource'

const ENTERPRISE_TYPE = ENTERPRISE_SOURCE_TYPES[0]

/**
 * 企业身份源表单规则：类型固定为飞书，凭证字段名取自类型配置。
 */
export const createSourceFormRules = (isEditing: boolean) => {
  const config = SOURCE_CONFIGS[ENTERPRISE_TYPE]
  return {
    name: [{ required: true, whitespace: true, message: '请输入身份源名称' }],
    clientId: [{ required: true, whitespace: true, message: `请输入${config.clientIdLabel}` }],
    clientSecret: [{ required: !isEditing, whitespace: true, message: `请输入${config.clientSecretLabel}` }]
  }
}
