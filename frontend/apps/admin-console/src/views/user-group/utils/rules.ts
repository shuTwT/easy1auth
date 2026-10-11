export const groupFormRules = {
  name: [{ required: true, message: '请输入用户组名称' }]
}

export const groupDetailFormRules = {
  name: [{ required: true, whitespace: true, message: '请输入用户组名称' }],
  type: [{ required: true, message: '请选择类型' }]
}
