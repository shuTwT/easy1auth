<script setup lang="ts">
import { ref } from 'vue'
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'
import type { UploadFile } from './index'

export type { UploadFile }

const props = withDefaults(defineProps<{
  class?: HTMLAttributes['class']
  accept?: string
  multiple?: boolean
  disabled?: boolean
  drag?: boolean
  showFileList?: boolean
  autoUpload?: boolean
  /** File validation. Return false or string to reject. */
  beforeUpload?: (file: UploadFile) => boolean | Promise<boolean>
  /** Custom upload handler. Default: read as dataURL. */
  httpRequest?: (options: { file: UploadFile; onProgress?: (pct: number) => void }) => Promise<string | void>
  modelValue?: UploadFile[]
}>(), {
  showFileList: true,
  autoUpload: true,
})

const emits = defineEmits<{
  (e: 'update:modelValue', files: UploadFile[]): void
  (e: 'change', file: UploadFile, files: UploadFile[]): void
  (e: 'success', response: any, file: UploadFile): void
  (e: 'error', error: Error, file: UploadFile): void
  (e: 'progress', percent: number, file: UploadFile): void
}>()

const inputRef = ref<HTMLInputElement>()
const fileList = ref<UploadFile[]>(props.modelValue || [])
const dragOver = ref(false)
const uploading = ref(false)

const defaultHttpRequest = async (options: { file: UploadFile }) => {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      resolve(e.target?.result as string)
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(options.file.raw)
  })
}

async function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files?.length) return
  await processFiles(Array.from(target.files))
  target.value = ''
}

async function processFiles(files: File[]) {
  for (const file of files) {
    const uploadFile: UploadFile = {
      name: file.name,
      size: file.size,
      type: file.type,
      raw: file,
    }

    // Validation
    if (props.beforeUpload) {
      const valid = await props.beforeUpload(uploadFile)
      if (valid === false) continue
    }

    fileList.value.push(uploadFile)
    emits('change', uploadFile, fileList.value)

    if (props.autoUpload) {
      await startUpload(uploadFile)
    }
  }
  emits('update:modelValue', fileList.value)
}

async function startUpload(file: UploadFile) {
  file.status = 'uploading'
  uploading.value = true

  try {
    const uploadFn = props.httpRequest || defaultHttpRequest
    const result = await uploadFn({ file })
    file.status = 'success'
    if (typeof result === 'string') file.url = result
    emits('success', result, file)
  } catch (error: any) {
    file.status = 'fail'
    emits('error', error, file)
  } finally {
    uploading.value = false
  }
}

function removeFile(index: number) {
  fileList.value.splice(index, 1)
  emits('update:modelValue', fileList.value)
}

function handleDragOver(e: DragEvent) {
  if (!props.drag) return
  e.preventDefault()
  dragOver.value = true
}

function handleDragLeave() {
  dragOver.value = false
}

async function handleDrop(e: DragEvent) {
  e.preventDefault()
  dragOver.value = false
  if (!e.dataTransfer?.files?.length) return
  await processFiles(Array.from(e.dataTransfer.files))
}

function openFileDialog() {
  if (props.disabled || uploading.value) return
  inputRef.value?.click()
}

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div
    data-slot="upload"
    :class="cn(
      'flex flex-col gap-2',
      props.class,
    )"
  >
    <!-- Drop zone or trigger -->
    <div
      :class="cn(
        'relative',
        drag && [
          'flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 transition-colors',
          dragOver
            ? 'border-primary bg-primary/5'
            : 'border-border hover:border-muted-foreground/50',
          disabled && 'pointer-events-none opacity-50',
        ],
      )"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
      @click="!drag && openFileDialog()"
    >
      <!-- Hidden file input -->
      <input
        ref="inputRef"
        type="file"
        :accept="accept"
        :multiple="multiple"
        class="hidden"
        @change="handleFileSelect"
      />

      <!-- Drag mode: click to open also -->
      <button
        v-if="drag"
        type="button"
        :disabled="disabled"
        class="absolute inset-0 cursor-pointer bg-transparent"
        @click="openFileDialog"
      />

      <!-- Default slot content -->
      <slot :uploading="uploading" :drag-over="dragOver" :open="openFileDialog">
        <div v-if="drag" class="pointer-events-none flex flex-col items-center gap-2 text-center">
          <svg
            class="text-muted-foreground size-8"
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <span class="text-sm font-medium">将文件拖到此处，或<em class="not-italic text-primary">点击上传</em></span>
          <span v-if="accept" class="text-muted-foreground text-xs">仅支持 {{ accept }} 格式</span>
        </div>
        <button
          v-else
          type="button"
          :disabled="disabled"
          class="border-input bg-background hover:bg-muted inline-flex h-8 cursor-pointer items-center gap-1.5 rounded-lg border px-2.5 text-sm font-medium transition-colors disabled:pointer-events-none disabled:opacity-50"
        >
          <svg
            class="size-4"
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          <slot name="button-text">选择文件</slot>
        </button>
      </slot>
    </div>

    <!-- File list -->
    <ul v-if="showFileList && fileList.length" class="flex flex-col gap-1">
      <li
        v-for="(file, index) in fileList"
        :key="file.name + file.size"
        class="border-input bg-background flex items-center gap-2 rounded-lg border px-2.5 py-1.5 text-sm"
      >
        <svg
          v-if="file.status === 'uploading'"
          class="text-muted-foreground size-4 animate-spin"
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M21 12a9 9 0 1 1-6.219-8.56" />
        </svg>
        <svg
          v-else-if="file.status === 'success'"
          class="text-emerald-500 size-4"
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M20 6 9 17l-5-5" />
        </svg>
        <svg
          v-else-if="file.status === 'fail'"
          class="text-destructive size-4"
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="15" x2="9" y1="9" y2="15" />
          <line x1="9" x2="15" y1="9" y2="15" />
        </svg>
        <svg
          v-else
          class="text-muted-foreground size-4"
          xmlns="http://www.w3.org/2000/svg"
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
          <polyline points="14 2 14 8 20 8" />
        </svg>
        <span class="flex-1 truncate">{{ file.name }}</span>
        <span class="text-muted-foreground shrink-0 text-xs">{{ formatSize(file.size) }}</span>
        <button
          type="button"
          class="text-muted-foreground hover:text-foreground ml-auto cursor-pointer"
          @click="removeFile(index)"
        >
          <svg
            class="size-4"
            xmlns="http://www.w3.org/2000/svg"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <line x1="18" x2="6" y1="6" y2="18" />
            <line x1="6" x2="18" y1="6" y2="18" />
          </svg>
        </button>
      </li>
    </ul>

    <!-- Tip slot -->
    <slot name="tip" />
  </div>
</template>
