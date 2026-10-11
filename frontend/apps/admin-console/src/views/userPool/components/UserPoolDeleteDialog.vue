<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { userPoolApi } from '@/api/userPool'
import type { UserPool } from '@/types/userPool'

const emit = defineEmits<{
  success: []
}>()

const deleteDialogVisible = ref(false)
const deleteTarget = ref<UserPool | null>(null)

const open = (userPool: UserPool) => {
  deleteTarget.value = userPool
  deleteDialogVisible.value = true
}

const confirmDelete = async () => {
  if (!deleteTarget.value) {
    return
  }
  try {
    await userPoolApi.delete(deleteTarget.value.id)
    message.success('用户池已删除')
    deleteDialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('删除用户池失败:', error)
    message.error('删除用户池失败')
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="deleteDialogVisible" :footer="null">
    <div><div><h3>删除用户池</h3><p>确定删除“{{ deleteTarget?.name }}”吗？删除后将停用该用户池及其所有管理员成员关系，但保留业务与审计数据。</p></div><div><a-button @click="deleteDialogVisible = false">取消</a-button><a-button danger @click="confirmDelete">确认删除</a-button></div></div>
  </a-modal>
</template>
