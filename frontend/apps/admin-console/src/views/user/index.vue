<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Plus, Upload, Search, RefreshCw } from '@lucide/vue'
import { userApi } from '@/api/user'
import type { User, UserQueryDto } from '@/types/user'
import UserEditDialog from './components/UserEditDialog.vue'
import ResetPasswordDialog from './components/ResetPasswordDialog.vue'
import AssignRoleDialog from './components/AssignRoleDialog.vue'
import ImportDialog from './components/ImportDialog.vue'
import { getApiErrorMessage, useUserDict } from './utils/hooks'
import type {
  AssignRoleDialogExpose,
  ImportDialogExpose,
  ResetPasswordDialogExpose,
  UserEditDialogExpose
} from './utils/types'

const loading = ref(false)
const users = ref<User[]>([])
const total = ref(0)

const userEditDialogRef = ref<UserEditDialogExpose>()
const resetPasswordDialogRef = ref<ResetPasswordDialogExpose>()
const assignRoleDialogRef = ref<AssignRoleDialogExpose>()
const importDialogRef = ref<ImportDialogExpose>()

const [modal, contextHolder] = Modal.useModal()

const { userStatusOptions, getStatusText, getStatusVariant, formatDate } = useUserDict()

const queryForm = reactive<UserQueryDto>({
  page: 1,
  pageSize: 10,
  username: '',
  email: '',
  phone: '',
  name: '',
  status: undefined,
  department: ''
})

const columns: TableProps<User>['columns'] = [
  { title: '用户名', key: 'username', width: 160 },
  { title: '邮箱', dataIndex: 'email', width: 192 },
  { title: '手机号', key: 'phone', width: 128 },
  { title: '部门', key: 'department', width: 112 },
  { title: '状态', key: 'status', width: 96 },
  { title: '最后登录', key: 'lastLoginAt', width: 160 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'actions', width: 256 }
]

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getList(queryForm)
    users.value = res.items
    total.value = res.total
  } catch (error: unknown) {
    console.error('加载用户列表失败:', error)
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadUsers()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.email = ''
  queryForm.phone = ''
  queryForm.name = ''
  queryForm.status = undefined
  queryForm.department = ''
  queryForm.page = 1
  loadUsers()
}

const handleAdd = () => {
  userEditDialogRef.value?.open()
}

const handleEdit = (row: User) => {
  userEditDialogRef.value?.open(row)
}

const handleImport = () => {
  importDialogRef.value?.open()
}

const handleResetPassword = (row: User) => {
  resetPasswordDialogRef.value?.open(row.id)
}

const handleAssignRole = (row: User) => {
  assignRoleDialogRef.value?.open(row)
}

const handleDelete = async (row: User) => {
  const confirmed = await modal.confirm({
    title: '删除用户',
    content: '确定要删除该用户吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed) return

  try {
    await userApi.delete(row.id)
    message.success('删除成功')
    loadUsers()
  } catch (error: unknown) {
    console.error('删除用户失败:', error)
    message.error(getApiErrorMessage(error, '删除用户失败'))
  }
}

const handleStatusChange = async (row: User, status: string) => {
  try {
    await userApi.updateStatus(row.id, status)
    message.success('状态更新成功')
    loadUsers()
  } catch (error: unknown) {
    console.error('更新状态失败:', error)
    message.error(getApiErrorMessage(error, '更新状态失败'))
  }
}

const handlePageChange = (page: number, pageSize?: number) => {
  queryForm.page = page
  if (pageSize !== undefined) queryForm.pageSize = pageSize
  loadUsers()
}

const handleSizeChange = (_current: number, size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadUsers()
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">用户管理</h1>
        <p class="text-sm text-[#52677D]">管理系统用户，包括添加、编辑、删除和分配角色</p>
      </div>
      <div class="flex gap-3">
        <a-button @click="handleImport">
          <Upload class="size-4 mr-2" />
          导入用户
        </a-button>
        <a-button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增用户
        </a-button>
      </div>
    </div>

    <a-card class="mb-4!">
      <a-form class="flex flex-wrap gap-4">
        <a-form-item name="username" label="用户名">
          <a-input v-model:value="queryForm.username" placeholder="请输入用户名" class="w-40" />
        </a-form-item>
        <a-form-item name="email" label="邮箱">
          <a-input v-model:value="queryForm.email" placeholder="请输入邮箱" class="w-40" />
        </a-form-item>
        <a-form-item name="name" label="姓名">
          <a-input v-model:value="queryForm.name" placeholder="请输入姓名" class="w-40" />
        </a-form-item>
        <a-form-item name="status" label="状态">
          <a-select
              v-model:value="queryForm.status"
              placeholder="请选择状态"
              class="w-42"
              allow-clear
              :options="userStatusOptions"
          />
        </a-form-item>
        <a-form-item>
          <div class="flex gap-2">
            <a-button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </a-button>
            <a-button @click="handleReset">
              <RefreshCw class="size-4 mr-2" />
              重置
            </a-button>
          </div>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card>
      <a-table :columns="columns" :data-source="users" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1264 }">
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.key === 'username'">
            <div class="flex items-center gap-3">
              <a-avatar class="size-8 bg-gradient-to-br from-blue-500 to-purple-600">
                <span class="bg-transparent text-white text-sm font-semibold">
                  {{ row.name?.charAt(0) || row.username.charAt(0).toUpperCase() }}
                </span>
              </a-avatar>
              <div class="flex flex-col">
                <span class="font-medium">{{ row.username }}</span>
                <span class="text-xs text-[#52677D]">{{ row.name }}</span>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'phone'">{{ row.phone || '-' }}</template>
          <template v-else-if="column.key === 'department'">{{ row.department || '-' }}</template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusVariant(row.status)">
              {{ getStatusText(row.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'lastLoginAt'">{{ formatDate(row.lastLoginAt) }}</template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDate(row.createdAt) }}</template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex gap-1 flex-wrap">
              <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</a-button>
              <a-button type="link" size="small" class="h-auto p-0" @click="handleAssignRole(row)">分配角色</a-button>
              <a-button type="link" size="small" class="h-auto p-0" @click="handleResetPassword(row)">重置密码</a-button>
              <a-button
                  type="link"
                  size="small"
                  class="h-auto p-0"
                  @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </a-button>
              <a-button type="link" size="small" danger class="h-auto p-0" @click="handleDelete(row)">删除</a-button>
            </div>
          </template>
        </template>
      </a-table>

      <div class="flex items-center justify-between mt-4 pt-4">
        <span class="text-sm text-[#52677D]">共 {{ total }} 条</span>
        <a-pagination
            :current="queryForm.page"
            :page-size="queryForm.pageSize"
            :total="total"
            :show-size-changer="true"
            :page-size-options="['10', '20', '50', '100']"
            size="small"
            @change="handlePageChange"
            @show-size-change="handleSizeChange"
        />
      </div>
    </a-card>

    <UserEditDialog ref="userEditDialogRef" @success="loadUsers" />
    <ResetPasswordDialog ref="resetPasswordDialogRef" />
    <AssignRoleDialog ref="assignRoleDialogRef" @success="loadUsers" />
    <ImportDialog ref="importDialogRef" @success="loadUsers" />

    <contextHolder />
  </div>
</template>
