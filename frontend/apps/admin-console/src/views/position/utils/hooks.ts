import type { SelectOption } from './types'

const levelOptions: SelectOption<number>[] = [
  { value: 1, label: '员工 (1-2级)' },
  { value: 3, label: '主管 (3-4级)' },
  { value: 5, label: '经理 (5-6级)' },
  { value: 7, label: '总监 (7-8级)' },
  { value: 9, label: '高管 (9-10级)' }
]

/**
 * 岗位字典：级别选项、级别文本与颜色，列表页与编辑弹窗共用。
 */
export function usePositionDict() {
  const getLevelColor = (level?: number) => {
    if (!level) return 'default'
    if (level >= 9) return 'red'
    if (level >= 7) return 'purple'
    if (level >= 5) return 'blue'
    if (level >= 3) return 'green'
    return 'cyan'
  }

  const getLevelText = (level?: number) => {
    if (!level) return '未指定'
    if (level >= 9) return '高管'
    if (level >= 7) return '总监'
    if (level >= 5) return '经理'
    if (level >= 3) return '主管'
    return '员工'
  }

  const formatDate = (date: string) => {
    return new Date(date).toLocaleString()
  }

  return {
    levelOptions,
    getLevelColor,
    getLevelText,
    formatDate
  }
}
