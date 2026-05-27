<script setup lang="ts">
import { ref, computed } from 'vue'
import type { TreeNodeData, TreePropsConfig } from './types'
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible'
import { cn } from '@/lib/utils'
import { ChevronRight } from '@lucide/vue'

const props = withDefaults(defineProps<{
  node: TreeNodeData
  treeProps: TreePropsConfig
  depth: number
  defaultExpandAll?: boolean
  selectedKey?: string | number | null
}>(), {
  depth: 0,
})

const emit = defineEmits<{
  (e: 'toggle', node: TreeNodeData): void
  (e: 'select', node: TreeNodeData): void
}>()

const childrenField = computed(() => props.treeProps.children || 'children')
const labelField = computed(() => props.treeProps.label || 'label')

const children = computed(() => {
  const ch = props.node[childrenField.value]
  return Array.isArray(ch) ? ch : []
})

const hasChildren = computed(() => children.value.length > 0)

const isOpen = ref(props.defaultExpandAll && hasChildren.value)

function toggle() {
  if (!hasChildren.value) return
  isOpen.value = !isOpen.value
  emit('toggle', props.node)
}

function handleSelect() {
  emit('select', props.node)
}
</script>

<template>
  <div data-slot="tree-node" :class="cn('group/tree-node')">
    <Collapsible v-if="hasChildren" v-model:open="isOpen">
      <div
        :class="cn(
          'flex cursor-pointer items-center gap-1 rounded-md px-2 py-1 text-sm transition-colors hover:bg-muted/50',
          selectedKey !== undefined && selectedKey === node[treeProps.value || 'id']
            ? 'bg-primary/10 text-primary'
            : 'text-foreground',
        )"
        :style="{ paddingLeft: `${depth * 16 + 8}px` }"
      >
        <CollapsibleTrigger as-child @click.stop="toggle">
          <button
            type="button"
            class="text-muted-foreground hover:text-foreground flex size-4 shrink-0 cursor-pointer items-center justify-center rounded transition-transform data-open:rotate-90"
          >
            <ChevronRight class="size-3.5" />
          </button>
        </CollapsibleTrigger>
        <span class="flex-1" @click="handleSelect">
          <slot name="default" :data="node" :depth="depth">
            {{ node[labelField] }}
          </slot>
        </span>
      </div>
      <CollapsibleContent>
        <TreeNode
          v-for="(child, i) in children"
          :key="child[treeProps.value || 'id'] || i"
          :node="child"
          :tree-props="treeProps"
          :depth="depth + 1"
          :default-expand-all="defaultExpandAll"
          :selected-key="selectedKey"
          @toggle="emit('toggle', $event)"
          @select="emit('select', $event)"
        >
          <slot name="default" :data="child" :depth="depth + 1" />
        </TreeNode>
      </CollapsibleContent>
    </Collapsible>

    <div
      v-else
      :class="cn(
        'flex cursor-pointer items-center gap-1 rounded-md px-2 py-1 text-sm transition-colors hover:bg-muted/50',
        selectedKey !== undefined && selectedKey === node[treeProps.value || 'id']
          ? 'bg-primary/10 text-primary'
          : 'text-foreground',
      )"
      :style="{ paddingLeft: `${depth * 16 + 8}px` }"
      @click="handleSelect"
    >
      <span class="size-4 shrink-0" />
      <span class="flex-1">
        <slot name="default" :data="node" :depth="depth">
          {{ node[labelField] }}
        </slot>
      </span>
    </div>
  </div>
</template>
