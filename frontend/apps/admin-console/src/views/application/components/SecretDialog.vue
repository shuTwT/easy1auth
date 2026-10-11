<script setup lang="ts">
import { ref } from 'vue'
import { Copy } from '@lucide/vue'
import { useClipboard } from '../utils/hooks'

const secretDialogVisible = ref(false)
const newClientSecret = ref('')

const { copyToClipboard } = useClipboard()

const open = (secret: string) => {
  if (!secret) {
    return
  }
  newClientSecret.value = secret
  secretDialogVisible.value = true
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="secretDialogVisible" title="客户端密钥（仅显示一次）" :footer="null">
    <a-alert
        class="mb-5!"
        type="warning"
        show-icon
        title="请妥善保管您的客户端密钥"
        description="密钥只会在创建应用或重新生成时显示一次，请立即复制并妥善保管。"
    />
    <div class="flex items-center gap-2">
      <a-input :value="newClientSecret" readonly class="min-w-0 flex-1 font-mono text-sm" />
      <a-button html-type="button" @click="copyToClipboard(newClientSecret)">
        <Copy data-icon="inline-start" />
        复制
      </a-button>
    </div>
    <div class="flex justify-end mt-5">
      <a-button @click="secretDialogVisible = false">关闭</a-button>
    </div>
  </a-modal>
</template>
