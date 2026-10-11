<script setup lang="ts">
import { ref } from 'vue'
import type { AuditLog } from '@/types/audit'
import { useAuditDict } from '../utils/hooks'

const detailDialogVisible = ref(false)
const currentLog = ref<AuditLog | null>(null)

const {
  getTypeText,
  getTypeVariant,
  getActionText,
  getStatusText,
  getStatusVariant,
  formatDate
} = useAuditDict()

const open = (row: AuditLog) => {
  currentLog.value = row
  detailDialogVisible.value = true
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="detailDialogVisible" :footer="null">
    <div class="sm:max-w-[700px]">
      <div>
        <h3>审计日志详情</h3>
      </div>
      <div v-if="currentLog" class="grid grid-cols-2 gap-4 py-4">
        <div class="space-y-1">
          <label class="text-[#52677D]">日志ID</label>
          <div class="text-sm font-mono">{{ currentLog.id }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">时间</label>
          <div class="text-sm">{{ formatDate(currentLog.createdAt) }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">用户ID</label>
          <div class="text-sm">{{ currentLog.userId || '-' }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">用户名</label>
          <div class="text-sm">{{ currentLog.username || '-' }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">类型</label>
          <div class="text-sm">
            <a-tag :color="getTypeVariant(currentLog.type)">
              {{ getTypeText(currentLog.type) }}
            </a-tag>
          </div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">操作</label>
          <div class="text-sm">{{ getActionText(currentLog.action) }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">资源</label>
          <div class="text-sm">{{ currentLog.resource }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">资源ID</label>
          <div class="text-sm">{{ currentLog.resourceId || '-' }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">请求方法</label>
          <div class="text-sm">{{ currentLog.method || '-' }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">IP地址</label>
          <div class="text-sm">{{ currentLog.ip }}</div>
        </div>
        <div class="col-span-2 space-y-1">
          <label class="text-[#52677D]">User Agent</label>
          <div class="text-sm text-[#52677D]">{{ currentLog.userAgent || '-' }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">位置</label>
          <div class="text-sm">{{ currentLog.location || '-' }}</div>
        </div>
        <div class="space-y-1">
          <label class="text-[#52677D]">状态</label>
          <div class="text-sm">
            <a-tag :color="getStatusVariant(currentLog.status)">
              {{ getStatusText(currentLog.status) }}
            </a-tag>
          </div>
        </div>
        <div v-if="currentLog.errorMessage" class="col-span-2 space-y-1">
          <label class="text-[#52677D]">错误信息</label>
          <div class="text-sm text-[#B91C1C]">{{ currentLog.errorMessage }}</div>
        </div>
        <div v-if="currentLog.changes" class="col-span-2 space-y-1">
          <label class="text-[#52677D]">变更内容</label>
          <pre class="text-sm bg-[#F4F8FB] p-3 rounded-md overflow-auto max-h-75">{{ JSON.stringify(currentLog.changes, null, 2) }}</pre>
        </div>
      </div>
      <div>
        <a-button @click="detailDialogVisible = false">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>
