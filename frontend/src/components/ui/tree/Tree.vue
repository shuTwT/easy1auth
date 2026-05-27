<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import type { TreeNodeData, TreePropsConfig } from './types'
import { cn } from '@/lib/utils'
import TreeNode from './TreeNode.vue'

const props = withDefaults(defineProps<{
  data?: TreeNodeData[]
  props?: TreePropsConfig
  nodeKey?: string
  defaultExpandAll?: boolean
  class?: HTMLAttributes['class']
}>(), {
  data: () => [],
  props: () => ({ children: 'children', label: 'label' }),
  nodeKey: 'id',
  defaultExpandAll: true,
})

const emit = defineEmits<{
  (e: 'node-click', data: TreeNodeData): void
  (e: 'toggle', data: TreeNodeData): void
}>()

const mergedProps = { children: 'children', label: 'label', value: 'id', ...(props.props || {}) }

function handleSelect(node: TreeNodeData) {
  emit('node-click', node)
}
</script>

<template>
  <div
    data-slot="tree"
    :class="cn('flex flex-col gap-px', props.class)"
  >
    <TreeNode
      v-for="(item, i) in data"
      :key="(item as any)[nodeKey] ?? i"
      :node="item"
      :tree-props="mergedProps"
      :depth="0"
      :default-expand-all="defaultExpandAll"
      @select="handleSelect"
      @toggle="emit('toggle', $event)"
    >
      <template #default="slotData">
        <slot name="default" v-bind="slotData" />
      </template>
    </TreeNode>
  </div>
</template>
