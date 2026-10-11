export const spaceFormRules = {
  name: [{ required: true, whitespace: true, message: '请输入空间名称' }],
  code: [{ required: true, whitespace: true, message: '请输入空间编码' }]
}

export const resourceFormRules = {
  name: [{ required: true, whitespace: true, message: '请输入资源名称' }],
  code: [{ required: true, whitespace: true, message: '请输入资源编码' }],
  spaceId: [{ required: true, message: '请选择权限空间' }]
}
