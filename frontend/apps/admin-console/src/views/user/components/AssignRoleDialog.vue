<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { roleApi } from '@/api/role'
import type { Role } from '@/types/role'
import type { User } from '@/types/user'
import { getApiErrorMessage } from '../utils/hooks'

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const loading = ref(false)
const currentUserId = ref('')
const userRoles = ref<Role[]>([])
const allRoles = ref<Role[]>([])
const selectedRoleIds = ref<string[]>([])

const getRoleColor = (type: string) => {
  return type === 'system' ? 'purple' : 'blue'
}

const toggleRole = (roleId: string) => {
  const index = selectedRoleIds.value.indexOf(roleId)
  if (index > -1) {
    selectedRoleIds.value.splice(index, 1)
  } else {
    selectedRoleIds.value.push(roleId)
  }
}

const open = async (row: User) => {
  loading.value = true
  try {
    const [userRolesData, allRolesData] = await Promise.all([
      roleApi.getUserRoles(row.id),
      roleApi.getList()
    ])
    currentUserId.value = row.id
    userRoles.value = userRolesData
    allRoles.value = allRolesData.items
    selectedRoleIds.value = userRolesData.map((role: Role) => role.id)
    dialogVisible.value = true
  } catch (error: unknown) {
    console.error('加载角色数据失败:', error)
    message.error('加载角色数据失败')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  loading.value = true
  try {
    await roleApi.assignRolesToUser(currentUserId.value, selectedRoleIds.value)
    message.success('分配角色成功')
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('分配角色失败:', error)
    message.error(getApiErrorMessage(error, '分配角色失败'))
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal
      v-model:open="dialogVisible"
      title="分配角色"
      ok-text="确定"
      cancel-text="取消"
      :confirm-loading="loading"
      @ok="handleSubmit"
  >
    <div class="grid gap-4 py-4">
      <div class="grid gap-2">
        <label class="text-sm font-medium">当前角色</label>
        <div v-if="userRoles.length > 0" class="flex flex-wrap gap-2">
          <a-tag v-for="role in userRoles" :key="role.id" :color="getRoleColor(role.type)">
            {{ role.name }}
          </a-tag>
        </div>
        <div v-else class="text-sm text-[#52677D]">暂未分配角色</div>
      </div>
      <div class="grid gap-2">
        <label class="text-sm font-medium">选择角色</label>
        <div class="flex flex-col gap-3">
          <div v-for="role in allRoles" :key="role.id" class="flex items-center gap-2">
            <a-checkbox :checked="selectedRoleIds.includes(role.id)" @update:checked="toggleRole(role.id)" />
            <a-tag :color="getRoleColor(role.type)" class="text-xs">
              {{ role.type === 'system' ? '系统' : '自定义' }}
            </a-tag>
            <span class="font-medium">{{ role.name }}</span>
            <span class="text-xs text-[#52677D]">({{ role.code }})</span>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>
