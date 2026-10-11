<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { userGroupApi } from '@/api/user-group'
import type { UserGroup } from '@/types/user-group'
import { getApiErrorMessage, useGroupDict } from '../utils/hooks'
import { groupFormRules } from '../utils/rules'
import type { GroupEditForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const { groupTypeOptions } = useGroupDict()

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户组')
const submitting = ref(false)
const currentId = ref<string | undefined>(undefined)

const createDefaultForm = (): GroupEditForm => {
  return {
    name: '',
    description: '',
    type: 'team'
  }
}

const groupForm = ref<GroupEditForm>(createDefaultForm())

const open = (row?: UserGroup) => {
  if (row) {
    dialogTitle.value = '编辑用户组'
    currentId.value = row.id
    groupForm.value = {
      name: row.name,
      description: row.description || '',
      type: row.type
    }
  } else {
    dialogTitle.value = '新增用户组'
    currentId.value = undefined
    groupForm.value = createDefaultForm()
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (currentId.value) {
      await userGroupApi.update(currentId.value, groupForm.value)
      message.success('更新成功')
    } else {
      await userGroupApi.create(groupForm.value)
      message.success('创建成功')
    }
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存用户组失败:', error)
    message.error(getApiErrorMessage(error, '保存用户组失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" :title="dialogTitle" :footer="null">
    <a-form :model="groupForm" :rules="groupFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid gap-4">
        <a-form-item label="用户组名称" name="name">
          <a-input v-model:value="groupForm.name" placeholder="请输入用户组名称" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="groupForm.description" :rows="3" placeholder="请输入描述" />
        </a-form-item>
        <a-form-item label="类型" name="type">
          <a-select v-model:value="groupForm.type" :options="groupTypeOptions" />
        </a-form-item>
      </div>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">确定</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
