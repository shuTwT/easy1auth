export const applicationEditRules = {
  name: [{ required: true, message: '请输入应用名称' }],
  type: [{ required: true, message: '请选择应用类型' }]
}

export const configFormRules = {
  name: [{ required: true, whitespace: true, message: '请输入应用名称' }]
}
