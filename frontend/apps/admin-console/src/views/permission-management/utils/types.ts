import type { CreatePermissionDto, PermissionSpace, PermissionSpaceInput, PermissionTree } from '@/types/permission'

export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export type ResourceEditForm = CreatePermissionDto

export type SpaceEditForm = Required<PermissionSpaceInput>

export interface ResourceEditDialogExpose {
  open: (resource?: PermissionTree, defaultSpaceId?: string) => void
  selectSpace: (spaceId: string) => void
}

export interface SpaceEditDialogExpose {
  open: (space?: PermissionSpace) => void
}
