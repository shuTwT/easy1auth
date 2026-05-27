<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover'
import { Input } from '@/components/ui/input'

const props = withDefaults(defineProps<{
  defaultValue?: string
  modelValue?: string
  class?: HTMLAttributes['class']
  presets?: string[]
  showInput?: boolean
  disabled?: boolean
}>(), {
  presets: () => [
    '#0369A1', '#0EA5E9', '#10B981', '#F59E0B',
    '#EF4444', '#8B5CF6', '#EC4899', '#0F172A',
    '#64748B', '#F1F5F9', '#FFFFFF',
  ],
  showInput: true,
})

const emits = defineEmits<{
  (e: 'update:modelValue', payload: string): void
}>()

const modelValue = useVModel(props, 'modelValue', emits, {
  passive: true,
  defaultValue: props.defaultValue || '#000000',
})
</script>

<template>
  <Popover>
    <PopoverTrigger as-child :disabled="disabled">
      <button
        data-slot="color-picker-trigger"
        :class="cn(
          'border-input inline-flex h-8 w-10 items-center justify-center rounded-lg border bg-clip-padding transition-colors hover:opacity-80 disabled:pointer-events-none disabled:opacity-50',
          props.class,
        )"
      >
        <span
          class="h-5 w-5 rounded-sm border border-border"
          :style="{ backgroundColor: modelValue }"
        />
      </button>
    </PopoverTrigger>
    <PopoverContent class="w-64 p-3" align="start">
      <div class="flex flex-col gap-3">
        <div class="flex items-center gap-2">
          <div
            class="h-8 w-8 shrink-0 rounded-md border border-border"
            :style="{ backgroundColor: modelValue }"
          />
          <input
            v-if="!showInput"
            type="color"
            :value="modelValue"
            class="h-8 w-full cursor-pointer rounded-md border-0 bg-transparent p-0"
            @input="modelValue = ($event.target as HTMLInputElement).value"
          />
          <Input
            v-if="showInput"
            :model-value="modelValue"
            class="h-8 font-mono text-xs"
            placeholder="#000000"
            @update:model-value="(val: any) => { const v = String(val); if (/^#[0-9a-fA-F]{0,6}$/.test(v)) modelValue = v }"
          />
        </div>
        <div class="grid grid-cols-11 gap-1.5">
          <button
            v-for="preset in presets"
            :key="preset"
            type="button"
            class="h-5 w-5 cursor-pointer rounded-sm border border-border transition-transform hover:scale-125"
            :class="{ 'ring-ring ring-2 ring-offset-1': modelValue?.toLowerCase() === preset.toLowerCase() }"
            :style="{ backgroundColor: preset }"
            :title="preset"
            @click="modelValue = preset"
          />
        </div>
        <div class="flex items-center gap-2">
          <input
            type="color"
            :value="modelValue"
            class="h-7 w-7 cursor-pointer rounded border-0 bg-transparent p-0"
            @input="modelValue = ($event.target as HTMLInputElement).value"
          />
          <span class="text-muted-foreground text-xs">自定义</span>
        </div>
      </div>
    </PopoverContent>
  </Popover>
</template>
