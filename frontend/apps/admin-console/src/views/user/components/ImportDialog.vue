<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps, UploadProps } from 'antdv-next'
import { Download } from '@lucide/vue'
import request from '@/utils/request'
import type { ImportErrorItem, ImportedUserItem, ImportResult } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const importLoading = ref(false)
const importResult = ref<ImportResult | null>(null)

const errorColumns: TableProps<ImportErrorItem>['columns'] = [
  { title: '行号', dataIndex: 'row', width: 80 },
  { title: '用户名', dataIndex: 'username', width: 144 },
  { title: '错误信息', dataIndex: 'error' }
]

const importedColumns: TableProps<ImportedUserItem>['columns'] = [
  { title: '用户名', dataIndex: 'username', width: 144 },
  { title: '邮箱', dataIndex: 'email', width: 192 },
  { title: '姓名', dataIndex: 'name' }
]

const open = () => {
  importResult.value = null
  dialogVisible.value = true
}

const handleDownloadTemplate = () => {
  const link = document.createElement('a')
  link.href = '/api/users/import/template'
  link.download = 'user_import_template.xlsx'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const uploadFile = async (file: File) => {
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    importResult.value = await request.post('/users/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }) as unknown as ImportResult
    message.success('导入完成')
    emit('success')
  } catch (error: unknown) {
    console.error('导入用户失败:', error)
    message.error('导入用户失败')
  } finally {
    importLoading.value = false
  }
}

// 手动上传：拦截默认请求，直接用原始 File 调用导入接口
const handleBeforeUpload: UploadProps['beforeUpload'] = (file) => {
  uploadFile(file as unknown as File)
  return false
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" title="导入用户" :width="640" :footer="null">
    <div class="py-4">
      <a-alert class="mb-6!" type="info" show-icon title="导入说明">
        <template #description>
          <div class="space-y-1">
            <div>1. 请先下载导入模板，按照模板格式填写用户信息</div>
            <div>2. 必填字段：用户名、邮箱、姓名</div>
            <div>3. 如果不填写密码，系统将使用默认密码：Password123</div>
            <div>4. 文件格式：.xlsx 或 .xls</div>
          </div>
        </template>
      </a-alert>

      <div class="mb-6 text-center">
        <a-button @click="handleDownloadTemplate">
          <Download class="mr-2 size-4" />
          下载导入模板
        </a-button>
      </div>

      <a-upload-dragger
          accept=".xlsx,.xls"
          :multiple="false"
          :show-upload-list="false"
          :disabled="importLoading"
          :before-upload="handleBeforeUpload"
      >
        <p class="text-sm font-medium">
          将文件拖到此处，或<em class="not-italic text-[#0369A1]">点击上传</em>
        </p>
        <p class="mt-1 text-xs text-[#52677D]">仅支持 xlsx/xls 文件</p>
      </a-upload-dragger>

      <a-spin :spinning="importLoading">
        <div v-if="importResult" class="mt-6">
          <h4 class="mb-4 font-semibold">导入结果</h4>
          <div class="mb-4 grid grid-cols-3 gap-4">
            <div class="rounded-lg border p-3">
              <div class="text-sm text-[#52677D]">总数</div>
              <div class="text-xl font-bold">{{ importResult.total }}</div>
            </div>
            <div class="rounded-lg border p-3">
              <div class="text-sm text-[#52677D]">成功</div>
              <div class="text-xl font-bold text-emerald-600">{{ importResult.success }}</div>
            </div>
            <div class="rounded-lg border p-3">
              <div class="text-sm text-[#52677D]">失败</div>
              <div class="text-xl font-bold text-[#B91C1C]">{{ importResult.failed }}</div>
            </div>
          </div>

          <div v-if="importResult.errors && importResult.errors.length > 0" class="mt-4">
            <h4 class="mb-2 font-semibold">错误详情</h4>
            <a-table
                :columns="errorColumns"
                :data-source="importResult.errors"
                :pagination="false"
                row-key="row"
                size="small"
            />
          </div>

          <div v-if="importResult.importedUsers && importResult.importedUsers.length > 0" class="mt-4">
            <h4 class="mb-2 font-semibold">成功导入的用户</h4>
            <a-table
                :columns="importedColumns"
                :data-source="importResult.importedUsers"
                :pagination="false"
                row-key="username"
                size="small"
            />
          </div>
        </div>
      </a-spin>
    </div>
    <div class="flex justify-end">
      <a-button @click="dialogVisible = false">关闭</a-button>
    </div>
  </a-modal>
</template>
