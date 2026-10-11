<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'antdv-next'
import { adminUserApi } from '@/api/adminUser'
import type { AdminMembership, AdminUser } from '@/types/adminUser'
import type { AdminRole } from '@/types/adminRole'
import { getApiErrorMessage, useAdminDict, useAdminMembership } from '../utils/hooks'

defineProps<{
  roles: AdminRole[]
}>()

const emit = defineEmits<{
  success: []
}>()

const { roleTypeVariant, permissionsSummary } = useAdminDict()
const { currentMembership } = useAdminMembership()

const assignRolesDialogVisible = ref(false)
const assignRolesTarget = ref<AdminUser | null>(null)
const assignRolesSelected = ref<string[]>([])
const assignRolesSubmitting = ref(false)
const assignRolesMembership = computed<AdminMembership | undefined>(() => {
  const target = assignRolesTarget.value
  return target ? currentMembership(target) : undefined
})

const open = (row: AdminUser) => {
  const membership = currentMembership(row)
  if (!membership) {
    return
  }
  assignRolesTarget.value = row
  assignRolesSelected.value = membership.roles.map((r) => {
    return r.id
  })
  assignRolesDialogVisible.value = true
}

const toggleRoleSelection = (roleId: string) => {
  const idx = assignRolesSelected.value.indexOf(roleId)
  if (idx > -1) {
    assignRolesSelected.value.splice(idx, 1)
  } else {
    assignRolesSelected.value.push(roleId)
  }
}

const handleAssignRolesSubmit = async () => {
  const target = assignRolesTarget.value
  const membership = target ? currentMembership(target) : undefined
  if (!target || !membership) {
    return
  }
  assignRolesSubmitting.value = true
  try {
    await adminUserApi.assignRoles(target.id, {
      userPoolId: membership.userPoolId,
      roleIds: assignRolesSelected.value
    })
    message.success('角色分配成功')
    assignRolesDialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    message.error(getApiErrorMessage(error, '分配角色失败'))
  } finally {
    assignRolesSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="assignRolesDialogVisible" :footer="null">
    <div class="max-w-lg">
      <div>
        <h3>分配管理员角色</h3>
      </div>
      <div class="py-4">
        <div class="grid gap-2 mb-4">
          <label>当前角色</label>
          <div v-if="assignRolesMembership && assignRolesMembership.roles.length > 0" class="flex flex-wrap gap-2">
            <a-tag
              v-for="role in assignRolesMembership.roles"
              :key="role.id"
              color="blue"
              class="text-xs"
            >
              {{ role.name }}
            </a-tag>
          </div>
          <div v-else class="text-[#52677D] text-sm">暂未分配角色</div>
        </div>
        <a-divider class="my-2" />
        <div class="grid gap-2">
          <label>选择角色</label>
          <div class="border rounded-lg p-3 max-h-72 overflow-y-auto">
            <div v-if="roles.length === 0" class="text-[#52677D] text-sm text-center py-4">
              暂无可分配角色
            </div>
            <div
              v-for="role in roles"
              :key="role.id"
              class="flex items-center gap-2 py-1.5"
            >
              <a-checkbox
                :checked="assignRolesSelected.includes(role.id)"
                @update:checked="toggleRoleSelection(role.id)"
              />
              <a-tag :color="roleTypeVariant(role.isSystem)" class="text-xs">
                {{ role.isSystem ? '系统' : '自定义' }}
              </a-tag>
              <span class="font-medium text-sm">{{ role.name }}</span>
              <span class="text-xs text-[#52677D]">{{ permissionsSummary(role.permissions) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div>
        <a-button  @click="assignRolesDialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="assignRolesSubmitting" :disabled="assignRolesSubmitting" @click="handleAssignRolesSubmit">
          {{ assignRolesSubmitting ? '提交中...' : '确定' }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>
