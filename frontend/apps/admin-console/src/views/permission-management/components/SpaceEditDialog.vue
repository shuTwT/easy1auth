<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import type { FormInstance } from 'antdv-next'
import { permissionApi } from '@/api/permission'
import type { PermissionSpace } from '@/types/permission'
import { spaceFormRules } from '../utils/rules'
import type { SpaceEditForm } from '../utils/types'

const emit = defineEmits<{
  success: [space: PermissionSpace, created: boolean]
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const currentId = ref<string | undefined>(undefined)
const formRef = ref<FormInstance>()

const createDefaultForm = (): SpaceEditForm => {
  return { name: '', code: '', description: '' }
}

const spaceForm = ref<SpaceEditForm>(createDefaultForm())

const open = (space?: PermissionSpace) => {
  currentId.value = space?.id
  spaceForm.value = {
    name: space?.name || '',
    code: space?.code || '',
    description: space?.description || ''
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (submitting.value) {
    return
  }
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }
  submitting.value = true
  try {
    const editingId = currentId.value
    const input = {
      ...spaceForm.value,
      name: spaceForm.value.name.trim(),
      code: spaceForm.value.code.trim()
    }
    const saved = editingId
      ? await permissionApi.updateSpace(editingId, input)
      : await permissionApi.createSpace(input)
    message.success(editingId ? '权限空间已更新' : '权限空间已创建')
    dialogVisible.value = false
    emit('success', saved, !editingId)
  } catch (error: unknown) {
    message.error(error instanceof Error && error.message ? error.message : '保存权限空间失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <!-- 可从资源弹窗内打开，层级需高于资源弹窗 -->
  <a-modal
    v-model:open="dialogVisible"
    :title="currentId ? '编辑权限空间' : '新建权限空间'"
    :confirm-loading="submitting"
    :z-index="1100"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="spaceForm" :rules="spaceFormRules" layout="vertical" class="py-4" :disabled="submitting">
      <a-form-item name="name" label="空间名称">
        <a-input v-model:value="spaceForm.name" placeholder="例如：组织架构" />
      </a-form-item>
      <a-form-item name="code" label="空间编码">
        <a-input v-model:value="spaceForm.code" :disabled="!!currentId" placeholder="例如：organization" />
      </a-form-item>
      <a-form-item name="description" label="空间描述">
        <a-textarea v-model:value="spaceForm.description" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
