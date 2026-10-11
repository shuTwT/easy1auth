export const roleFormRules = {
  name: [{ required: true, message: '请输入角色名称' }],
  code: [{ required: true, message: '请输入角色编码' }],
  type: [{ required: true, message: '请选择角色类型' }],
  dataScope: [{ required: true, message: '请选择数据范围' }]
}
