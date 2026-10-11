<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { userPoolPackageApi } from '@/api/userPoolPackage'
import type { UserPoolPackage } from '@/types/userPoolPackage'

const emit = defineEmits<{
  success: []
}>()

const deleteDialogVisible = ref(false)
const deletingPackage = ref<UserPoolPackage | null>(null)

const open = (item: UserPoolPackage) => {
  deletingPackage.value = item
  deleteDialogVisible.value = true
}

const confirmDelete = async () => {
  if (!deletingPackage.value) {
    return
  }
  try {
    await userPoolPackageApi.remove(deletingPackage.value.id)
    message.success('套餐已删除')
    deleteDialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('删除用户池套餐失败:', error)
    message.error('删除用户池套餐失败')
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="deleteDialogVisible" :footer="null">
    <div>
      <div><h3>删除用户池套餐</h3><p>确定删除“{{ deletingPackage?.name }}”吗？已分配给用户池的套餐不能删除。</p></div>
      <div><a-button @click="deleteDialogVisible = false">取消</a-button><a-button danger @click="confirmDelete">确认删除</a-button></div>
    </div>
  </a-modal>
</template>
