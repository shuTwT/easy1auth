export const positionFormRules = {
  name: [{ required: true, whitespace: true, message: '请输入岗位名称' }],
  code: [{ required: true, whitespace: true, message: '请输入岗位编码' }],
  level: [{ required: true, message: '请选择岗位级别' }]
}
