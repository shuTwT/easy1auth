<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'antdv-next'
import { adminRoleApi } from '@/api/adminRole'
import type { AdminRole, CreateAdminRoleDto, UpdateAdminRoleDto } from '@/types/adminRole'
import { getApiErrorMessage } from '../utils/hooks'
import { ROLE_CODE_PATTERN, roleFormRules } from '../utils/rules'
import type { RoleDialogMode, RoleForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const roleDialogVisible = ref(false)
const roleDialogIsEdit = ref(false)
const roleDialogSubmitting = ref(false)
const roleForm = reactive<RoleForm>({
  name: '',
  code: '',
  description: ''
})
const roleDialogTitle = computed(() => {
  return roleDialogIsEdit.value ? '编辑管理员角色' : '创建管理员角色'
})

const open = (mode: RoleDialogMode, row?: AdminRole) => {
  if (mode === 'edit' && row) {
    roleDialogIsEdit.value = true
    roleForm.id = row.id
    roleForm.name = row.name
    roleForm.code = row.code
    roleForm.description = row.description || ''
  } else {
    roleDialogIsEdit.value = false
    roleForm.id = undefined
    roleForm.name = ''
    roleForm.code = ''
    roleForm.description = ''
  }
  roleDialogVisible.value = true
}

const handleRoleSubmit = async () => {
  if (!roleForm.name.trim()) {
    message.warning('请填写角色名称')
    return
  }
  if (!ROLE_CODE_PATTERN.test(roleForm.code.trim())) {
    message.warning('请填写有效的角色编码')
    return
  }
  roleDialogSubmitting.value = true
  try {
    if (roleDialogIsEdit.value && roleForm.id) {
      const payload: UpdateAdminRoleDto = {
        name: roleForm.name,
        description: roleForm.description || undefined
      }
      await adminRoleApi.update(roleForm.id, payload)
      message.success('角色更新成功')
    } else {
      const payload: CreateAdminRoleDto = {
        name: roleForm.name,
        code: roleForm.code.trim(),
        description: roleForm.description || undefined,
        permissions: []
      }
      await adminRoleApi.create(payload)
      message.success('角色创建成功，请在角色列表中配置权限')
    }
    roleDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '操作失败'))
  } finally {
    roleDialogSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="roleDialogVisible" :footer="null">
    <div class="max-w-lg">
      <div>
        <h3>{{ roleDialogTitle }}</h3>
      </div>
      <a-form :model="roleForm" :rules="roleFormRules" layout="vertical" class="py-4" @finish="handleRoleSubmit">
        <div class="grid gap-4">
          <a-form-item label="角色名称" name="name">
            <a-input v-model:value="roleForm.name" placeholder="请输入角色名称" />
          </a-form-item>
          <a-form-item label="角色编码" name="code" extra="创建后不可修改">
            <a-input
              v-model:value="roleForm.code"
              placeholder="例如：operations_admin"
              :maxlength="100"
              :disabled="roleDialogIsEdit"
            />
          </a-form-item>
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="roleForm.description" :rows="2" placeholder="请输入角色描述" />
          </a-form-item>
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <a-button html-type="button" @click="roleDialogVisible = false">取消</a-button>
          <a-button type="primary" html-type="submit" :loading="roleDialogSubmitting" :disabled="roleDialogSubmitting">
            {{ roleDialogSubmitting ? '提交中...' : '确定' }}
          </a-button>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>
