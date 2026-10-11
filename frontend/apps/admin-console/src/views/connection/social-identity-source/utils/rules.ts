import { SOURCE_CONFIGS } from '@/types/loginIdentitySource'
import type { SocialSourceType } from '@/types/socialIdentitySource'

/**
 * 社会化身份源表单规则：凭证字段名随类型变化，新建时密钥必填，编辑时留空表示保持原值。
 */
export const createSourceFormRules = (type: SocialSourceType, isEditing: boolean) => {
  const config = SOURCE_CONFIGS[type]
  return {
    name: [{ required: true, whitespace: true, message: '请输入身份源名称' }],
    type: [{ required: true, message: '请选择身份源类型' }],
    clientId: [{ required: true, whitespace: true, message: `请输入${config.clientIdLabel}` }],
    clientSecret: [{ required: !isEditing, whitespace: true, message: `请输入${config.clientSecretLabel}` }]
  }
}
