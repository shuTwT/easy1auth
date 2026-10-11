<script setup lang="ts">

import {ref} from "vue";
import {message} from "antdv-next";

const secretDialogVisible = ref(false)
const newClientSecret = ref('')

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    message.error('复制失败')
  }
}

const open=(secret:string)=>{
  newClientSecret.value=secret
}

defineExpose({open})
</script>

<template>
  <a-modal v-model:open="secretDialogVisible">
    <div class="max-w-md">
      <div>
        <h3>Client Secret</h3>
      </div>
      <a-alert
          class="mb-5!"
          type="warning"
          show-icon
          title="请妥善保管您的客户端密钥"
          description="密钥只会在创建应用或重新生成时显示一次，请立即复制并妥善保管。"
      />
      <div class="bg-[#F4F8FB] p-3 rounded-md font-mono text-sm break-all">
        {{ newClientSecret }}
      </div>
      <div>
        <a-button @click="copyToClipboard(newClientSecret)">复制密钥</a-button>
        <a-button  @click="secretDialogVisible = false">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>

</style>