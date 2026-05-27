<script setup lang="ts">
import { computed, ref } from 'vue'
import type { HTMLAttributes } from 'vue'
import { useVModel } from '@vueuse/core'
import type { TreeNodeData, TreePropsConfig } from './types'
import { cn } from '@/lib/utils'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { X, ChevronDown } from '@lucide/vue'
import Tree from './Tree.vue'

const props = withDefaults(defineProps<{
  defaultValue?: string | number
  modelValue?: string | number
  data?: TreeNodeData[]
  props?: TreePropsConfig
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  class?: HTMLAttributes['class']
}>(), {
  data: () => [],
  props: () => ({ children: 'children', label: 'label', value: 'id' }),
  placeholder: '请选择',
  clearable: true,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | undefined): void
}>()

const modelValue = useVModel(props, 'modelValue', emit, {
  passive: true,
  defaultValue: props.defaultValue,
})

const open = ref(false)
const mergedProps = computed(() => ({ children: 'children', label: 'label', value: 'id', ...(props.props || {}) }))
const valueField = computed(() => mergedProps.value.value || 'id')
const labelField = computed(() => mergedProps.value.label || 'label')
const childrenField = computed(() => mergedProps.value.children || 'children')

function findLabel(data: TreeNodeData[], value: string | number | undefined): string {
  if (value === undefined || value === null || value === '') return ''
  for (const node of data) {
    if (node[valueField.value] === value) return node[labelField.value] || ''
    const children = node[childrenField.value]
    if (Array.isArray(children) && children.length) {
      const found = findLabel(children, value)
      if (found) return found
    }
  }
  return ''
}

const selectedLabel = computed(() => findLabel(props.data, modelValue.value))

function handleSelect(node: TreeNodeData) {
  modelValue.value = node[valueField.value]
  open.value = false
}

function handleClear() {
  modelValue.value = undefined
}
</script>

<template>
  <Popover v-model:open="open">
    <PopoverTrigger as-child :disabled="disabled">
      <button
        type="button"
        role="combobox"
        :aria-expanded="open"
        data-slot="tree-select-trigger"
        :class="cn(
          'border-input focus-visible:border-ring focus-visible:ring-ring/50 h-8 w-full rounded-lg border bg-transparent px-2.5 py-1 text-sm transition-colors focus-visible:ring-3 outline-none disabled:pointer-events-none disabled:opacity-50 flex items-center gap-1',
          props.class,
        )"
      >
        <span :class="cn('flex-1 text-left truncate', !modelValue && 'text-muted-foreground')">
          {{ selectedLabel || placeholder }}
        </span>
        <X
          v-if="clearable && modelValue"
          class="text-muted-foreground hover:text-foreground size-3.5 shrink-0 cursor-pointer"
          @click.stop="handleClear"
        />
        <ChevronDown class="text-muted-foreground size-4 shrink-0 opacity-50" />
      </button>
    </PopoverTrigger>
    <PopoverContent class="w-[var(--reka-popover-trigger-width)] p-1" align="start">
      <Tree
        :data="data"
        :props="mergedProps"
        :default-expand-all="true"
        @node-click="handleSelect"
      >
        <template #default="slotData">
          <slot name="default" v-bind="slotData">
            <span>{{ slotData.data[labelField] }}</span>
          </slot>
        </template>
      </Tree>
    </PopoverContent>
  </Popover>
</template>
