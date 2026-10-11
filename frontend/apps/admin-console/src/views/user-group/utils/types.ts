import type { GroupType, UserGroup } from '@/types/user-group'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface GroupEditForm {
  name: string
  description: string
  type: GroupType
}

export interface GroupEditDialogExpose {
  open: (row?: UserGroup) => void
}

export interface AddMembersDialogExpose {
  open: () => void
  close: () => void
}
