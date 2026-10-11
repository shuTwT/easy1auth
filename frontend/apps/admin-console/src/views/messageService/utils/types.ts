import type { MessageTemplate } from '@/api/messageTemplate'

export type TemplateType = MessageTemplate['type']

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface TemplateEditForm {
  type: TemplateType
  code: string
  name: string
  subject: string
  content: string
  isDefault: boolean
}

export interface TemplateEditDialogExpose {
  open: (type: TemplateType, row?: MessageTemplate) => void
}
