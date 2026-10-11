<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'antdv-next'
import { userPoolApi } from '@/api/userPool'
import type { AdminUser } from '@/types/adminUser'
import type { UserPool } from '@/types/userPool'

const props = defineProps<{
  administrators: AdminUser[]
}>()

const emit = defineEmits<{
  success: []
}>()

const transferDialogVisible = ref(false)
const transferSubmitting = ref(false)
const transferTarget = ref<UserPool | null>(null)
const transferAdministratorId = ref<string>('')

const administratorOptions = computed(() => {
  return props.administrators.map((item) => {
    return {
      value: item.id,
      label: `${item.username} · ${item.email}`,
    }
  })
})

const open = (userPool: UserPool) => {
  transferTarget.value = userPool
  transferAdministratorId.value = ''
  transferDialogVisible.value = true
}

const submitTransfer = async () => {
  if (!transferTarget.value || !transferAdministratorId.value) {
    message.error('请选择目标管理员')
    return
  }
  transferSubmitting.value = true
  try {
    await userPoolApi.transferAdministrator(transferTarget.value.id, transferAdministratorId.value)
    message.success('用户池管理员转移成功')
    transferDialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('转移用户池管理员失败:', error)
    message.error('转移用户池管理员失败')
  } finally {
    transferSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="transferDialogVisible" :footer="null">
    <div class="w-full max-w-lg">
      <div><h3>转移用户池管理员</h3><p>目标管理员将成为“{{ transferTarget?.name }}”的用户池管理员，原管理员将被移出该用户池。</p></div>
      <div class="mt-6 grid gap-2"><label for="transfer-administrator">目标管理员</label><a-select id="transfer-administrator" v-model:value="transferAdministratorId" class="w-full" placeholder="请选择目标管理员" :options="administratorOptions" /></div>
      <div class="mt-6 flex justify-end gap-2"><a-button @click="transferDialogVisible = false">取消</a-button><a-button :disabled="transferSubmitting" @click="submitTransfer">{{ transferSubmitting ? '转移中...' : '确认转移' }}</a-button></div>
    </div>
  </a-modal>
</template>
