export type MenuNodeType = 'directory' | 'menu' | 'action'

export interface MenuTreeNode {
  id: string
  label: string
  code: string
  resource: string
  scope: string
  type: MenuNodeType
  children: MenuTreeNode[]
}
