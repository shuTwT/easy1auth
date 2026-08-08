<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Form, FormItem, Modal, Pagination as AntPagination, Table, message, Upload as AUpload } from 'antdv-next'
import { Plus, Upload, Download, Search, RefreshCw } from '@lucide/vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import request from '@/utils/request'
import type { User, CreateUserDto, UpdateUserDto, UserQueryDto } from '@/types/user'
import type { Role } from '@/types/role'

const loading = ref(false)
const users = ref<User[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const currentUser = ref<Partial<User>>({})
const resetPasswordDialogVisible = ref(false)
const resetPasswordUserId = ref('')
const assignRoleDialogVisible = ref(false)
const assignRoleUserId = ref('')
const userRoles = ref<Role[]>([])
const allRoles = ref<Role[]>([])
const selectedRoleIds = ref<string[]>([])
const assignRoleLoading = ref(false)
const importDialogVisible = ref(false)
const importLoading = ref(false)
const importResult = ref<any>(null)

const [modal, contextHolder] = Modal.useModal()

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

const userForm = reactive<CreateUserDto & UpdateUserDto>({
  username: '',
  email: '',
  password: '',
  phone: '',
  name: '',
  avatar: '',
  department: '',
  position: ''
})

const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const userFormRules = {
  username: [{ required: true, message: '请输入用户名' }],
  email: [{ required: true, message: '请输入邮箱' }],
  name: [{ required: true, message: '请输入姓名' }],
}

const resetPasswordFormRules = {
  newPassword: [{ required: true, message: '请输入新密码' }],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    {
      validator: (_rule: unknown, value: string) => {
        if (value !== resetPasswordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
    },
  ],
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getList(queryForm)
    users.value = res.items
    total.value = res.total
  } catch (error) {
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
  dialogTitle.value = '新增用户'
  Object.assign(userForm, {
    username: '',
    email: '',
    password: '',
    phone: '',
    name: '',
    avatar: '',
    department: '',
    position: ''
  })
  currentUser.value = {}
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    username: row.username,
    email: row.email || '',
    phone: row.phone || '',
    name: row.name,
    avatar: row.avatar || '',
    department: row.department || '',
    position: row.position || ''
  })
  currentUser.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: User) => {
  const confirmed = await modal.confirm({
    title: '删除用户',
    content: '确定要删除该用户吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await userApi.delete(row.id)
    message.success('删除成功')
    loadUsers()
  } catch (error) {
    console.error('删除用户失败:', error)
    message.error('删除用户失败')
  }
}

const handleStatusChange = async (row: User, status: string) => {
  try {
    await userApi.updateStatus(row.id, status)
    message.success('状态更新成功')
    loadUsers()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('更新状态失败')
  }
}

const handleResetPassword = (row: User) => {
  resetPasswordUserId.value = row.id
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordDialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (currentUser.value.id) {
      await userApi.update(currentUser.value.id, userForm)
      message.success('更新成功')
    } else {
      await userApi.create(userForm as CreateUserDto)
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    console.error('保存用户失败:', error)
    message.error(error.response?.data?.msg || '保存用户失败')
  }
}

const handleResetPasswordSubmit = async () => {
  try {
    await userApi.resetPassword(resetPasswordUserId.value, resetPasswordForm.newPassword)
    message.success('密码重置成功')
    resetPasswordDialogVisible.value = false
  } catch (error: any) {
    console.error('重置密码失败:', error)
    message.error(error.response?.data?.msg || '重置密码失败')
  }
}

const handleAssignRole = async (row: User) => {
  assignRoleUserId.value = row.id
  assignRoleLoading.value = true
  try {
    const [userRolesData, allRolesData] = await Promise.all([
      roleApi.getUserRoles(row.id),
      roleApi.getList(),
    ])
    userRoles.value = userRolesData
    allRoles.value = allRolesData.items
    selectedRoleIds.value = userRolesData.map((role: Role) => role.id)
    assignRoleDialogVisible.value = true
  } catch (error) {
    console.error('加载角色数据失败:', error)
    message.error('加载角色数据失败')
  } finally {
    assignRoleLoading.value = false
  }
}

const handleAssignRoleSubmit = async () => {
  assignRoleLoading.value = true
  try {
    await roleApi.assignRolesToUser(assignRoleUserId.value, selectedRoleIds.value)
    message.success('分配角色成功')
    assignRoleDialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    console.error('分配角色失败:', error)
    message.error(error.response?.data?.msg || '分配角色失败')
  } finally {
    assignRoleLoading.value = false
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

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'active':
      return 'green'
    case 'disabled':
      return 'orange'
    case 'locked':
      return 'red'
    default:
      return 'default'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'active':
      return '正常'
    case 'disabled':
      return '禁用'
    case 'locked':
      return '锁定'
    default:
      return '未知'
  }
}

onMounted(() => {
  loadUsers()
})

const handleDownloadTemplate = () => {
  const link = document.createElement('a')
  link.href = '/api/users/import/template'
  link.download = 'user_import_template.xlsx'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handleImport = () => {
  importDialogVisible.value = true
  importResult.value = null
}

const handleFileChange = async (file: any) => {
  importLoading.value = true

  try {
    const formData = new FormData()
    formData.append('file', file.raw)

    importResult.value = await request.post('/users/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    message.success('导入完成')
    loadUsers()
  } catch (error) {
    console.error('导入用户失败:', error)
    message.error('导入用户失败')
  } finally {
    importLoading.value = false
  }
}

const toggleRole = (roleId: string) => {
  const index = selectedRoleIds.value.indexOf(roleId)
  if (index > -1) {
    selectedRoleIds.value.splice(index, 1)
  } else {
    selectedRoleIds.value.push(roleId)
  }
}
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">用户管理</h1>
        <p class="text-sm text-muted-foreground">管理系统用户，包括添加、编辑、删除和分配角色</p>
      </div>
      <div class="flex gap-3">
        <Button  @click="handleImport">
          <Upload class="size-4 mr-2" />
          导入用户
        </Button>
        <Button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增用户
        </Button>
      </div>
    </div>

    <Card class="mb-4">
      <div class="pt-6">
        <div class="flex flex-wrap gap-4 items-end">
          <div class="grid gap-2">
            <label class="text-sm font-medium">用户名</label>
            <Input v-model:value="queryForm.username" placeholder="请输入用户名" class="w-40" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">邮箱</label>
            <Input v-model:value="queryForm.email" placeholder="请输入邮箱" class="w-40" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">姓名</label>
            <Input v-model:value="queryForm.name" placeholder="请输入姓名" class="w-40" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">状态</label>
            <Select v-model:value="queryForm.status" class="w-32" allow-clear>
                <SelectOption value="active">正常</SelectOption>
                <SelectOption value="disabled">禁用</SelectOption>
                <SelectOption value="locked">锁定</SelectOption>

            </Select>
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </Button>
            <Button  @click="handleReset">
              <RefreshCw class="size-4 mr-2" />
              重置
            </Button>
          </div>
        </div>
      </div>
    </Card>

    <Card>
      <div class="pt-6">
        <Table :columns="[
          { title: '用户名', key: 'username', width: 160 }, { title: '邮箱', dataIndex: 'email', width: 192 },
          { title: '手机号', key: 'phone', width: 128 }, { title: '部门', key: 'department', width: 112 },
          { title: '状态', key: 'status', width: 96 }, { title: '最后登录', key: 'lastLoginAt', width: 160 },
          { title: '创建时间', key: 'createdAt', width: 160 }, { title: '操作', key: 'actions', width: 256 }
        ]" :data-source="users" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1264 }">
          <template #bodyCell="{ column, record: row }">
            <template v-if="column.key === 'username'">
                <div class="flex items-center gap-3">
                  <Avatar class="size-8 bg-gradient-to-br from-blue-500 to-purple-600">
                    <span class="bg-transparent text-white text-sm font-semibold">
                      {{ row.name?.charAt(0) || row.username.charAt(0).toUpperCase() }}
                    </span>
                  </Avatar>
                  <div class="flex flex-col">
                    <span class="font-medium">{{ row.username }}</span>
                    <span class="text-xs text-muted-foreground">{{ row.name }}</span>
                  </div>
                </div>
            </template>
            <template v-else-if="column.key === 'phone'">{{ row.phone || '-' }}</template>
            <template v-else-if="column.key === 'department'">{{ row.department || '-' }}</template>
            <template v-else-if="column.key === 'status'">
                <Tag :color="getStatusVariant(row.status)">
                  {{ getStatusText(row.status) }}
                </Tag>
            </template>
            <template v-else-if="column.key === 'lastLoginAt'">{{ row.lastLoginAt ? new Date(row.lastLoginAt).toLocaleString() : '-' }}</template>
            <template v-else-if="column.key === 'createdAt'">{{ new Date(row.createdAt).toLocaleString() }}</template>
            <template v-else-if="column.key === 'actions'">
                <div class="flex gap-1 flex-wrap">
                  <Button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</Button>
                  <Button type="link" size="small" class="h-auto p-0" @click="handleAssignRole(row)">分配角色</Button>
                  <Button type="link" size="small" class="h-auto p-0" @click="handleResetPassword(row)">重置密码</Button>
                  <Button
                    type="link"
                    size="small"
                    class="h-auto p-0"
                    @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </Button>
                  <Button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDelete(row)">删除</Button>
                </div>
            </template>
          </template>
        </Table>

        <div class="flex items-center justify-between mt-4 pt-4 border-t">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <AntPagination
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
      </div>
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <Form :model="userForm" :rules="userFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <FormItem label="用户名" name="username">
              <Input v-model:value="userForm.username" placeholder="请输入用户名" :disabled="!!currentUser.id" />
            </FormItem>
            <FormItem label="邮箱" name="email">
              <Input v-model:value="userForm.email" placeholder="请输入邮箱" />
            </FormItem>
            <FormItem v-if="!currentUser.id" label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
              <Input v-model:value="userForm.password" type="password" placeholder="请输入密码" />
            </FormItem>
            <FormItem label="姓名" name="name">
              <Input v-model:value="userForm.name" placeholder="请输入姓名" />
            </FormItem>
            <FormItem label="手机号" name="phone">
              <Input v-model:value="userForm.phone" placeholder="请输入手机号" />
            </FormItem>
            <FormItem label="部门" name="department">
              <Input v-model:value="userForm.department" placeholder="请输入部门" />
            </FormItem>
            <FormItem label="岗位" name="position">
              <Input v-model:value="userForm.position" placeholder="请输入岗位" />
            </FormItem>
            <FormItem label="头像" name="avatar">
              <Input v-model:value="userForm.avatar" placeholder="请输入头像URL" />
            </FormItem>
          </div>
          <div class="flex justify-end gap-2">
            <Button @click="dialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit">确定</Button>
          </div>
        </Form>
      </div>
    </Modal>

    <Modal v-model:open="resetPasswordDialogVisible" :footer="null">
      <div class="max-w-md">
        <div>
          <h3>重置密码</h3>
        </div>
        <Form :model="resetPasswordForm" :rules="resetPasswordFormRules" layout="vertical" class="py-4" @finish="handleResetPasswordSubmit">
          <div class="grid gap-4">
            <FormItem label="新密码" name="newPassword">
              <Input v-model:value="resetPasswordForm.newPassword" type="password" placeholder="请输入新密码" />
            </FormItem>
            <FormItem label="确认密码" name="confirmPassword">
              <Input v-model:value="resetPasswordForm.confirmPassword" type="password" placeholder="请确认新密码" />
            </FormItem>
          </div>
          <div class="flex justify-end gap-2">
            <Button @click="resetPasswordDialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit">确定</Button>
          </div>
        </Form>
      </div>
    </Modal>

    <Modal v-model:open="assignRoleDialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>分配角色</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">当前角色</label>
              <div v-if="userRoles.length > 0" class="flex flex-wrap gap-2">
                <Tag
                  v-for="role in userRoles"
                  :key="role.id"
                  :color="role.type === 'system' ? 'purple' : 'blue'"
                >
                  {{ role.name }}
                </Tag>
              </div>
              <div v-else class="text-muted-foreground text-sm">暂未分配角色</div>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">选择角色</label>
              <div class="flex flex-col gap-3">
                <div
                  v-for="role in allRoles"
                  :key="role.id"
                  class="flex items-center gap-2"
                >
                  <Checkbox
                    :checked="selectedRoleIds.includes(role.id)"
                    @update:checked="toggleRole(role.id)"
                  />
                  <Tag
                    :color="role.type === 'system' ? 'purple' : 'blue'"
                    class="text-xs"
                  >
                    {{ role.type === 'system' ? '系统' : '自定义' }}
                  </Tag>
                  <span class="font-medium">{{ role.name }}</span>
                  <span class="text-xs text-muted-foreground">({{ role.code }})</span>
                </div>
              </div>
            </div>
          </div>
        </form>
        <div>
          <Button  @click="assignRoleDialogVisible = false">取消</Button>
          <Button @click="handleAssignRoleSubmit" :disabled="assignRoleLoading">确定</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="importDialogVisible" :footer="null">
      <div class="max-w-2xl">
        <div>
          <h3>导入用户</h3>
        </div>
        <div class="py-4">
          <Alert class="mb-6" type="info" show-icon title="导入说明">
            <template #description>
              <div class="space-y-1">
                <div>1. 请先下载导入模板，按照模板格式填写用户信息</div>
                <div>2. 必填字段：用户名、邮箱、姓名</div>
                <div>3. 如果不填写密码，系统将使用默认密码：Password123</div>
                <div>4. 文件格式：.xlsx 或 .xls</div>
              </div>
            </template>
          </Alert>

          <div class="text-center mb-6">
            <Button @click="handleDownloadTemplate">
              <Download class="size-4 mr-2" />
              下载导入模板
            </Button>
          </div>

          <Divider class="my-4" />

          <AUpload
            drag
            accept=".xlsx,.xls"
            :auto-upload="false"
            :show-file-list="false"
            @change="handleFileChange"
          >
            <template #default>
              <p class="text-sm font-medium">将文件拖到此处，或<em class="text-primary not-italic">点击上传</em></p>
              <p class="text-xs text-muted-foreground mt-1">仅支持 xlsx/xls 文件</p>
            </template>
          </AUpload>

          <div v-if="importResult" class="mt-6">
            <Divider class="my-4" />
            <h4 class="font-semibold mb-4">导入结果</h4>
            <div class="grid grid-cols-3 gap-4 mb-4">
              <div class="border rounded-lg p-3">
                <div class="text-sm text-muted-foreground">总数</div>
                <div class="text-xl font-bold">{{ importResult.total }}</div>
              </div>
              <div class="border rounded-lg p-3">
                <div class="text-sm text-muted-foreground">成功</div>
                <div class="text-xl font-bold text-emerald-600">{{ importResult.success }}</div>
              </div>
              <div class="border rounded-lg p-3">
                <div class="text-sm text-muted-foreground">失败</div>
                <div class="text-xl font-bold text-destructive">{{ importResult.failed }}</div>
              </div>
            </div>

            <div v-if="importResult.errors && importResult.errors.length > 0" class="mt-4">
              <h4 class="font-semibold mb-2">错误详情</h4>
              <Table :columns="[{ title: '行号', dataIndex: 'row', width: 80 }, { title: '用户名', dataIndex: 'username', width: 144 }, { title: '错误信息', dataIndex: 'error' }]" :data-source="importResult.errors" :pagination="false" row-key="row" size="small" />
            </div>

            <div v-if="importResult.importedUsers && importResult.importedUsers.length > 0" class="mt-4">
              <h4 class="font-semibold mb-2">成功导入的用户</h4>
              <Table :columns="[{ title: '用户名', dataIndex: 'username', width: 144 }, { title: '邮箱', dataIndex: 'email', width: 192 }, { title: '姓名', dataIndex: 'name' }]" :data-source="importResult.importedUsers" :pagination="false" row-key="username" size="small" />
            </div>
          </div>
        </div>
        <div>
          <Button  @click="importDialogVisible = false">关闭</Button>
        </div>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>
