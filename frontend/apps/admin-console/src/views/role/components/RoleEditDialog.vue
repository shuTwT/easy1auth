<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'antdv-next'
import { roleApi } from '@/api/role'
import type { Role } from '@/types/role'
import { getApiErrorMessage, usePermissionTree, useRoleDict } from '../utils/hooks'
import { roleFormRules } from '../utils/rules'
import type { RoleEditForm } from '../utils/types'

const props = defineProps<{
  roles: Role[]
}>()

const emit = defineEmits<{
  success: []
}>()

const { roleTypeOptions, dataScopeOptions } = useRoleDict()
const { permissionTree, loadPermissionTree, getPermissionCodesFromJson, buildPermissionsFromCodes } = usePermissionTree()

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const currentId = ref<string | undefined>(undefined)
const checkedPermissionCodes = ref<string[]>([])

const dialogTitle = computed(() => (isEdit.value ? '编辑角色' : '创建角色'))

const availableParentRoles = computed(() => {
  return props.roles
    .filter((role) => role.id !== currentId.value)
    .map((role) => ({ value: role.id, label: role.name }))
})

const createDefaultForm = (): RoleEditForm => {
  return {
    name: '',
    code: '',
    description: '',
    type: 'custom',
    dataScope: 'self',
    parentId: undefined
  }
}

const roleForm = ref<RoleEditForm>(createDefaultForm())

const open = async (row?: Role) => {
  if (permissionTree.value.length === 0) {
    await loadPermissionTree()
  }
  if (row) {
    isEdit.value = true
    currentId.value = row.id
    checkedPermissionCodes.value = getPermissionCodesFromJson(row.permissions || {})
    roleForm.value = {
      name: row.name,
      code: row.code,
      description: row.description || '',
      type: row.type,
      dataScope: row.dataScope,
      parentId: row.parentId || undefined
    }
  } else {
    isEdit.value = false
    currentId.value = undefined
    checkedPermissionCodes.value = []
    roleForm.value = createDefaultForm()
  }
  dialogVisible.value = true
}

const togglePermission = (code: string) => {
  if (checkedPermissionCodes.value.includes(code)) {
    checkedPermissionCodes.value = checkedPermissionCodes.value.filter((item) => item !== code)
  } else {
    checkedPermissionCodes.value = [...checkedPermissionCodes.value, code]
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const permissions = buildPermissionsFromCodes(checkedPermissionCodes.value)
    if (isEdit.value && currentId.value) {
      await roleApi.update(currentId.value, {
        name: roleForm.value.name,
        description: roleForm.value.description,
        permissions,
        dataScope: roleForm.value.dataScope,
        parentId: roleForm.value.parentId
      })
      message.success('更新成功')
    } else {
      await roleApi.create({
        ...roleForm.value,
        name: roleForm.value.name.trim(),
        code: roleForm.value.code.trim(),
        permissions
      })
      message.success('创建成功')
    }
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存角色失败:', error)
    message.error(getApiErrorMessage(error, '操作失败'))
  } finally {
    submitting.value = false
  }
}

onMounted(loadPermissionTree)

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" :title="dialogTitle" :width="800" :footer="null">
    <a-form :model="roleForm" :rules="roleFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid grid-cols-1 gap-x-6 md:grid-cols-2">
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="roleForm.name" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" name="code">
          <a-input v-model:value="roleForm.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="角色类型" name="type">
          <a-select v-model:value="roleForm.type" :disabled="isEdit" :options="roleTypeOptions" />
        </a-form-item>
        <a-form-item label="数据范围" name="dataScope">
          <a-select v-model:value="roleForm.dataScope" :options="dataScopeOptions" />
        </a-form-item>
        <a-form-item label="父级角色" name="parentId">
          <a-select
            v-model:value="roleForm.parentId"
            allow-clear
            :options="availableParentRoles"
            placeholder="请选择父级角色"
          />
        </a-form-item>
        <a-form-item label="权限配置" class="md:col-span-2">
          <div class="max-h-[360px] overflow-y-auto border rounded-lg p-2">
            <a-tree
              :tree-data="permissionTree"
              :field-names="{ key: 'code', title: 'name', children: 'children' }"
              :default-expand-all="true"
              :selectable="false"
            >
              <template #titleRender="data">
                <div class="flex items-center gap-2">
                  <input
                    type="checkbox"
                    class="rounded border-[#D7E2EC]"
                    :checked="checkedPermissionCodes.includes(data.code)"
                    @change="togglePermission(data.code)"
                  />
                  <span>{{ data.name }}</span>
                </div>
              </template>
            </a-tree>
          </div>
        </a-form-item>
        <a-form-item label="描述" name="description" class="md:col-span-2">
          <a-textarea v-model:value="roleForm.description" :rows="3" placeholder="请输入描述" />
        </a-form-item>
      </div>
      <div class="flex justify-end gap-2 pt-2">
        <a-button html-type="button" @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">
          {{ submitting ? '提交中...' : '确定' }}
        </a-button>
      </div>
    </a-form>
  </a-modal>
</template>
