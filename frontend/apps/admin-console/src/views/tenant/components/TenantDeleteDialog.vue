<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { tenantApi } from '@/api/tenant'
import { useUserStore } from '@/stores/user'
import type { Tenant } from '@/types/tenant'
import { errorMessage } from '../utils/hooks'

const emit = defineEmits<{
  success: []
}>()

const userStore = useUserStore()

const deleting = ref(false)
const deleteTarget = ref<Tenant | null>(null)

const open = (row: Tenant) => {
  deleteTarget.value = row
}

const close = () => {
  deleteTarget.value = null
}

const remove = async () => {
  const target = deleteTarget.value
  if (!target || deleting.value) {
    return
  }
  deleting.value = true
  try {
    await tenantApi.delete(target.id)
    if (target.userPoolId !== userStore.currentUserPool?.id) {
      return
    }
    deleteTarget.value = null
    message.success('租户已删除')
    emit('success')
  } catch (error) {
    message.error(errorMessage(error, '删除租户失败'))
  } finally {
    deleting.value = false
  }
}

defineExpose({ open, close })
</script>

<template>
  <a-modal :open="!!deleteTarget" title="删除租户" ok-text="删除" cancel-text="取消" :ok-button-props="{ danger: true }" :confirm-loading="deleting" @ok="remove" @cancel="deleteTarget = null">
    <p>确定删除租户「{{ deleteTarget?.name }}」的资料吗？</p>
  </a-modal>
</template>
