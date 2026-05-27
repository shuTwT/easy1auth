import type { HTMLAttributes } from 'vue'

export interface TreeNodeData {
  [key: string]: any
  children?: TreeNodeData[]
}

export interface TreePropsConfig {
  children?: string
  label?: string
  value?: string
  disabled?: string
  icon?: string
}

export interface TreeProps {
  data?: TreeNodeData[]
  props?: TreePropsConfig
  nodeKey?: string
  defaultExpandAll?: boolean
  expandedKeys?: (string | number)[]
  class?: HTMLAttributes['class']
}

export interface TreeSelectProps {
  modelValue?: string | number
  data?: TreeNodeData[]
  props?: TreePropsConfig
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  class?: HTMLAttributes['class']
}
