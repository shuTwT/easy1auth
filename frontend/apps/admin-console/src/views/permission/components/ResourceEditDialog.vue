<script setup lang="ts">
import { toRef, ref } from 'vue'
import { message } from 'antdv-next'
import type { FormInstance } from 'antdv-next'
import { permissionApi } from '@/api/permission'
import type { PermissionSpace, PermissionTree } from '@/types/permission'
import { useOperationOptions, useSpaceOptions } from '../utils/hooks'
import { resourceFormRules } from '../utils/rules'
import type { ResourceEditForm } from '../utils/types'

const props = defineProps<{
  resources: PermissionTree[]
  spaces: PermissionSpace[]
  spacesLoading: boolean
}>()

const emit = defineEmits<{
  success: []
  createSpace: []
  refreshSpaces: []
}>()

const dialogVisible = ref(false)
const submitting = ref(false)
const currentId = ref<string | undefined>(undefined)
const formRef = ref<FormInstance>()

const createDefaultForm = (): ResourceEditForm => {
  return {
    name: '',
    code: '',
    description: '',
    type: 'data',
    parentId: undefined,
    resource: '',
    action: '',
    spaceId: undefined,
    operations: []
  }
}

const resourceForm = ref<ResourceEditForm>(createDefaultForm())

const { spaceOptions } = useSpaceOptions(toRef(props, 'spaces'))
const { operationOptions } = useOperationOptions(toRef(props, 'resources'), () => resourceForm.value.operations)

const open = (resource?: PermissionTree, defaultSpaceId?: string) => {
  currentId.value = resource?.id
  resourceForm.value = {
    name: resource?.name || '',
    code: resource?.code || '',
    description: resource?.description || '',
    type: 'data',
    parentId: undefined,
    resource: resource?.resource || '',
    action: resource?.action || '',
    spaceId: resource?.spaceId || defaultSpaceId,
    operations: [...(resource?.operations || [])]
  }
  dialogVisible.value = true
}

/**
 * 新建权限空间后，若弹窗仍打开则自动选中该空间。
 */
const selectSpace = (spaceId: string) => {
  if (dialogVisible.value) {
    resourceForm.value.spaceId = spaceId
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (currentId.value) {
      await permissionApi.update(currentId.value, resourceForm.value)
    } else {
      await permissionApi.create(resourceForm.value)
    }
    message.success(currentId.value ? '数据资源已更新' : '数据资源已创建')
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存数据资源失败:', error)
    message.error('保存数据资源失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open, selectSpace })
</script>

<template>
  <a-modal
    v-model:open="dialogVisible"
    :title="currentId ? '编辑数据资源' : '新建数据资源'"
    :confirm-loading="submitting"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="resourceForm" :rules="resourceFormRules" layout="vertical" class="py-4">
      <a-form-item name="name" label="资源名称">
        <a-input v-model:value="resourceForm.name" placeholder="例如：用户" />
      </a-form-item>
      <a-form-item name="code" label="资源编码">
        <a-input v-model:value="resourceForm.code" :disabled="!!currentId" placeholder="例如：user" />
      </a-form-item>
      <a-form-item name="spaceId" label="权限空间">
        <a-select
          v-model:value="resourceForm.spaceId"
          allow-clear
          show-search
          option-filter-prop="label"
          class="w-full"
          :options="spaceOptions"
          :loading="spacesLoading"
          placeholder="请选择权限空间"
        >
          <template #notFoundContent>
            <a-spin v-if="spacesLoading" size="small" />
            <span v-else>{{ spaces.length ? '暂无匹配的权限空间' : '暂无可选权限空间，请先新建' }}</span>
          </template>
        </a-select>
        <div class="mt-2 flex gap-3">
          <a-button type="link" size="small" class="p-0!" @click="emit('createSpace')">新建权限空间</a-button>
          <a-button type="link" size="small" class="p-0!" :disabled="spacesLoading" @click="emit('refreshSpaces')">刷新选项</a-button>
        </div>
      </a-form-item>
      <a-form-item name="parentId" label="上级资源权限">
        <a-tree-select
          v-model:value="resourceForm.parentId"
          class="w-full"
          :tree-data="resources"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          allow-clear
          tree-default-expand-all
          placeholder="请选择上级资源"
        />
      </a-form-item>
      <a-form-item name="operations" label="资源操作">
        <a-select
          v-model:value="resourceForm.operations"
          mode="tags"
          class="w-full"
          :options="operationOptions"
          option-filter-prop="label"
          placeholder="请选择操作，也可输入自定义操作后回车"
        />
      </a-form-item>
      <a-form-item name="description" label="资源描述">
        <a-textarea v-model:value="resourceForm.description" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
