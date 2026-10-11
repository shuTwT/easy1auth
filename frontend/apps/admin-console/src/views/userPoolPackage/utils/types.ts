import type { UserPoolPackage } from '@/types/userPoolPackage'

export type TreeNodeData = Record<string, unknown>

export interface PermissionTreeNode extends TreeNodeData {
  id: string
  label: string
  code?: string
  type: 'group' | 'directory' | 'menu' | 'action'
  children: PermissionTreeNode[]
  ancestorCodes: string[]
}

export interface PackageEditDialogExpose {
  open: (item?: UserPoolPackage) => void
  reopen: () => void
  setPermissionCodes: (codes: string[]) => void
}

export interface PackagePermissionDialogExpose {
  open: (item: UserPoolPackage) => void
  openForSelect: (codes: string[]) => void
}

export interface PackageDeleteDialogExpose {
  open: (item: UserPoolPackage) => void
}
