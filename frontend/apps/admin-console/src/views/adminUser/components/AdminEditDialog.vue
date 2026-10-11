<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'antdv-next'
import { adminUserApi } from '@/api/adminUser'
import type { AdminUser, UpdateAdminDto } from '@/types/adminUser'
import { getApiErrorMessage } from '../utils/hooks'

const emit = defineEmits<{
  success: []
}>()

const editDialogVisible = ref(false)
const editTarget = ref<AdminUser | null>(null)
const editForm = reactive<UpdateAdminDto>({ username: '', email: '', phone: '' })
const editSubmitting = ref(false)

const open = (row: AdminUser) => {
  editTarget.value = row
  editForm.username = row.username
  editForm.email = row.email
  editForm.phone = row.phone || ''
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editTarget.value) {
    return
  }
  editSubmitting.value = true
  try {
    await adminUserApi.update(editTarget.value.id, {
      username: editForm.username,
      email: editForm.email,
      phone: editForm.phone || undefined
    })
    message.success('管理员信息更新成功')
    editDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '更新管理员失败'))
  } finally {
    editSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="editDialogVisible" :footer="null">
    <div class="max-w-lg">
      <div>
        <h3>编辑管理员</h3>
      </div>
      <a-form :model="editForm" layout="vertical" class="py-4" @finish="handleEditSubmit">
        <div class="grid gap-4">
          <a-form-item label="用户名" name="username">
            <a-input v-model:value="editForm.username" placeholder="请输入用户名" />
          </a-form-item>
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="editForm.email" placeholder="请输入邮箱" />
          </a-form-item>
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="editForm.phone" placeholder="请输入手机号" />
          </a-form-item>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <a-button html-type="button" @click="editDialogVisible = false">取消</a-button>
          <a-button type="primary" html-type="submit" :loading="editSubmitting" :disabled="editSubmitting">
            {{ editSubmitting ? '提交中...' : '确定' }}
          </a-button>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>
