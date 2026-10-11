/**
 * 同步身份源表单规则：新建时密钥类字段必填，编辑时留空表示不修改。
 */
export const createSourceFormRules = (isEditing: boolean) => {
  return {
    name: [{ required: true, whitespace: true, message: '请输入身份源名称' }],
    appId: [{ required: true, whitespace: true, message: '请输入飞书 App ID' }],
    appSecret: [{ required: !isEditing, message: '请输入 App Secret' }],
    verificationToken: [{ required: !isEditing, message: '请输入 Verification Token' }],
    encryptKey: [{ required: !isEditing, message: '请输入 Encrypt Key' }]
  }
}
