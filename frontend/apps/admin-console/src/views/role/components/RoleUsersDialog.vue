<script setup lang="ts">
import { ref } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Plus, Search } from '@lucide/vue'
import { roleApi } from '@/api/role'
import type { Role, RoleTree, RoleUser } from '@/types/role'
import AssignUsersDialog from './AssignUsersDialog.vue'
import { getApiErrorMessage } from '../utils/hooks'
import type { AssignUsersDialogExpose } from '../utils/types'

const emit = defineEmits<{
  change: []
}>()

const [modal, contextHolder] = Modal.useModal()

const dialogVisible = ref(false)
const loading = ref(false)
const roleUsers = ref<RoleUser[]>([])
const currentRole = ref<Role | null>(null)
const userSearchQuery = ref('')
const assignUsersDialogRef = ref<AssignUsersDialogExpose>()

const columns: TableProps<RoleUser>['columns'] = [
  { title: '用户名', key: 'username', width: 150 },
  { title: '姓名', dataIndex: 'name', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 200 },
  { title: '部门', dataIndex: 'department', width: 150 },
  { title: '岗位', dataIndex: 'position', width: 150 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 100 }
]

const loadRoleUsers = async () => {
  if (!currentRole.value) {
    return
  }
  loading.value = true
  try {
    const data = await roleApi.getRoleUsers(currentRole.value.id, { search: userSearchQuery.value })
    roleUsers.value = data.users
  } catch (error: unknown) {
    console.error('加载角色用户失败:', error)
    message.error('加载角色用户失败')
  } finally {
    loading.value = false
  }
}

const open = async (row: Role | RoleTree) => {
  currentRole.value = row as Role
  userSearchQuery.value = ''
  roleUsers.value = []
  dialogVisible.value = true
  await loadRoleUsers()
}

const handleAssignUsers = () => {
  if (!currentRole.value) {
    return
  }
  assignUsersDialogRef.value?.open(currentRole.value.id, roleUsers.value.map((user) => user.id))
}

const handleAssignSuccess = () => {
  loadRoleUsers()
  emit('change')
}

const handleRemoveUser = async (user: RoleUser) => {
  const confirmed = await modal.confirm({
    title: '移除角色成员',
    content: `确定要从该角色移除用户“${user.name}”吗？`,
    okText: '移除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed || !currentRole.value) {
    return
  }
  try {
    await roleApi.removeUsers(currentRole.value.id, { userIds: [user.id] })
    message.success('移除成功')
    loadRoleUsers()
    emit('change')
  } catch (error: unknown) {
    console.error('移除用户失败:', error)
    message.error(getApiErrorMessage(error, '移除失败'))
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" title="角色用户" :width="800" :footer="null">
    <div class="min-h-[400px]">
      <div class="flex justify-between mb-5">
        <div class="relative">
          <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-[#52677D]" />
          <a-input
            v-model:value="userSearchQuery"
            placeholder="搜索用户名、邮箱、姓名"
            class="w-72 pl-8"
            @keyup.enter="loadRoleUsers"
          />
        </div>
        <a-button @click="handleAssignUsers">
          <Plus class="size-4 mr-2" />
          分配用户
        </a-button>
      </div>
      <a-table
        :columns="columns"
        :data-source="roleUsers"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 970 }"
      >
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.key === 'username'">
            <div class="flex items-center gap-2">
              <a-avatar class="size-7">
                <span class="bg-gradient-to-br from-[#0369A1] to-[#0369A1]/60 text-white text-xs font-semibold">
                  {{ row.name?.charAt(0) || row.username.charAt(0).toUpperCase() }}
                </span>
              </a-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="row.status === 'active' ? 'green' : 'red'" class="text-xs">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-button type="link" size="small" danger class="p-0 h-auto" @click="handleRemoveUser(row)">移除</a-button>
          </template>
        </template>
      </a-table>
    </div>
    <AssignUsersDialog ref="assignUsersDialogRef" @success="handleAssignSuccess" />
    <contextHolder />
  </a-modal>
</template>
