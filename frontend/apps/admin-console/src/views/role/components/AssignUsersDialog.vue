<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'antdv-next'
import { roleApi } from '@/api/role'
import { userApi } from '@/api/user'
import { getApiErrorMessage } from '../utils/hooks'
import type { AssignableUser } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const assigning = ref(false)
const currentRoleId = ref('')
const selectedUserIds = ref<string[]>([])
const allUsers = ref<AssignableUser[]>([])

const availableUsers = computed(() => {
  return allUsers.value.filter((user) => !selectedUserIds.value.includes(user.id))
})

const getUserName = (userId: string) => {
  return allUsers.value.find((user) => user.id === userId)?.name
}

const open = async (roleId: string, existingUserIds: string[]) => {
  try {
    const response = await userApi.getList()
    allUsers.value = response.items.map((user) => ({
      id: user.id,
      name: `${user.name} (${user.username})`,
      disabled: existingUserIds.includes(user.id)
    }))
    currentRoleId.value = roleId
    selectedUserIds.value = []
    dialogVisible.value = true
  } catch (error: unknown) {
    console.error('加载用户列表失败:', error)
    message.error('加载用户列表失败')
  }
}

const handleSelect = (userId: string) => {
  selectedUserIds.value.push(userId)
}

const handleUnselect = (userId: string) => {
  selectedUserIds.value = selectedUserIds.value.filter((id) => id !== userId)
}

const handleSubmit = async () => {
  if (selectedUserIds.value.length === 0) {
    message.warning('请选择要分配的用户')
    return
  }
  assigning.value = true
  try {
    await roleApi.assignUsers(currentRoleId.value, { userIds: selectedUserIds.value })
    message.success('分配用户成功')
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('分配用户失败:', error)
    message.error(getApiErrorMessage(error, '分配用户失败'))
  } finally {
    assigning.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" title="分配用户" :footer="null">
    <div class="min-h-[400px]">
      <div class="flex gap-4">
        <div class="flex-1">
          <h3 class="font-medium mb-3">可选用户</h3>
          <div class="border rounded-lg p-3 max-h-[350px] overflow-y-auto">
            <div v-for="user in availableUsers" :key="user.id" class="py-1">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  class="rounded border-[#D7E2EC]"
                  :disabled="user.disabled"
                  @change="handleSelect(user.id)"
                />
                <span :class="{ 'text-[#52677D]': user.disabled }">{{ user.name }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="flex-1">
          <h3 class="font-medium mb-3">已选用户</h3>
          <div class="border rounded-lg p-3 max-h-[350px] overflow-y-auto">
            <div v-for="userId in selectedUserIds" :key="userId" class="py-1">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked
                  class="rounded border-[#D7E2EC]"
                  @change="handleUnselect(userId)"
                />
                <span>{{ getUserName(userId) }}</span>
              </label>
            </div>
            <div v-if="selectedUserIds.length === 0" class="text-[#52677D] text-center py-4">
              暂无选择
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="flex justify-end gap-2 mt-4">
      <a-button @click="dialogVisible = false">取消</a-button>
      <a-button type="primary" :loading="assigning" @click="handleSubmit">确定</a-button>
    </div>
  </a-modal>
</template>
