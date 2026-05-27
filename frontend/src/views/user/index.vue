<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { toast } from 'vue-sonner'
import { Plus, Upload, Download, Search, RefreshCw } from '@lucide/vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Checkbox } from '@/components/ui/checkbox'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Upload as UploadComponent } from '@/components/ui/upload'
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

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getList(queryForm)
    users.value = res.data.users
    total.value = res.data.total
  } catch (error) {
    console.error('加载用户列表失败:', error)
    toast.error('加载用户列表失败')
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
    email: row.email,
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
  const confirmed = window.confirm('确定要删除该用户吗？删除后无法恢复！')
  if (!confirmed) return
  
  try {
    await userApi.delete(row.id)
    toast.success('删除成功')
    loadUsers()
  } catch (error) {
    console.error('删除用户失败:', error)
    toast.error('删除用户失败')
  }
}

const handleStatusChange = async (row: User, status: string) => {
  try {
    await userApi.updateStatus(row.id, status)
    toast.success('状态更新成功')
    loadUsers()
  } catch (error) {
    console.error('更新状态失败:', error)
    toast.error('更新状态失败')
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
      toast.success('更新成功')
    } else {
      if (!userForm.password) {
        toast.warning('创建用户时必须设置密码')
        return
      }
      await userApi.create(userForm as CreateUserDto)
      toast.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    console.error('保存用户失败:', error)
    toast.error(error.response?.data?.message || '保存用户失败')
  }
}

const handleResetPasswordSubmit = async () => {
  try {
    if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) {
      toast.error('两次输入的密码不一致')
      return
    }
    
    await userApi.resetPassword(resetPasswordUserId.value, resetPasswordForm.newPassword)
    toast.success('密码重置成功')
    resetPasswordDialogVisible.value = false
  } catch (error: any) {
    console.error('重置密码失败:', error)
    toast.error(error.response?.data?.message || '重置密码失败')
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
    allRoles.value = allRolesData
    selectedRoleIds.value = userRolesData.map((role: Role) => role.id)
    assignRoleDialogVisible.value = true
  } catch (error) {
    console.error('加载角色数据失败:', error)
    toast.error('加载角色数据失败')
  } finally {
    assignRoleLoading.value = false
  }
}

const handleAssignRoleSubmit = async () => {
  assignRoleLoading.value = true
  try {
    await roleApi.assignRolesToUser(assignRoleUserId.value, selectedRoleIds.value)
    toast.success('分配角色成功')
    assignRoleDialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    console.error('分配角色失败:', error)
    toast.error(error.response?.data?.error || '分配角色失败')
  } finally {
    assignRoleLoading.value = false
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadUsers()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadUsers()
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'active':
      return 'default'
    case 'disabled':
      return 'secondary'
    case 'locked':
      return 'destructive'
    default:
      return 'outline'
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
    
    const response = await fetch('/api/users/import', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: formData
    })
    
    const result = await response.json()
    
    if (result.status === 'success') {
      importResult.value = result.data
      toast.success(result.message)
      loadUsers()
    } else {
      toast.error(result.message || '导入失败')
    }
  } catch (error) {
    console.error('导入用户失败:', error)
    toast.error('导入用户失败')
  } finally {
    importLoading.value = false
  }
}

const totalPages = () => Math.ceil(total.value / queryForm.pageSize!)

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
        <Button variant="outline" @click="handleImport">
          <Upload class="w-4 h-4 mr-2" />
          导入用户
        </Button>
        <Button @click="handleAdd">
          <Plus class="w-4 h-4 mr-2" />
          新增用户
        </Button>
      </div>
    </div>

    <Card class="mb-4">
      <CardContent class="pt-6">
        <div class="flex flex-wrap gap-4 items-end">
          <div class="grid gap-2">
            <label class="text-sm font-medium">用户名</label>
            <Input v-model="queryForm.username" placeholder="请输入用户名" class="w-40" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">邮箱</label>
            <Input v-model="queryForm.email" placeholder="请输入邮箱" class="w-40" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">姓名</label>
            <Input v-model="queryForm.name" placeholder="请输入姓名" class="w-40" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">状态</label>
            <Select v-model="queryForm.status">
              <SelectTrigger class="w-32">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="active">正常</SelectItem>
                <SelectItem value="disabled">禁用</SelectItem>
                <SelectItem value="locked">锁定</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button variant="outline" @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <Card>
      <CardContent class="pt-6">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-40">用户名</TableHead>
              <TableHead class="w-48">邮箱</TableHead>
              <TableHead class="w-32">手机号</TableHead>
              <TableHead class="w-28">部门</TableHead>
              <TableHead class="w-24">状态</TableHead>
              <TableHead class="w-40">最后登录</TableHead>
              <TableHead class="w-40">创建时间</TableHead>
              <TableHead class="w-64">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="row in users" :key="row.id">
              <TableCell>
                <div class="flex items-center gap-3">
                  <Avatar class="size-8 bg-gradient-to-br from-blue-500 to-purple-600">
                    <AvatarFallback class="bg-transparent text-white text-sm font-semibold">
                      {{ row.name?.charAt(0) || row.username.charAt(0).toUpperCase() }}
                    </AvatarFallback>
                  </Avatar>
                  <div class="flex flex-col">
                    <span class="font-medium">{{ row.username }}</span>
                    <span class="text-xs text-muted-foreground">{{ row.name }}</span>
                  </div>
                </div>
              </TableCell>
              <TableCell>{{ row.email }}</TableCell>
              <TableCell>{{ row.phone || '-' }}</TableCell>
              <TableCell>{{ row.department || '-' }}</TableCell>
              <TableCell>
                <Badge :variant="getStatusVariant(row.status)">
                  {{ getStatusText(row.status) }}
                </Badge>
              </TableCell>
              <TableCell>{{ row.lastLoginAt ? new Date(row.lastLoginAt).toLocaleString() : '-' }}</TableCell>
              <TableCell>{{ new Date(row.createdAt).toLocaleString() }}</TableCell>
              <TableCell>
                <div class="flex gap-1 flex-wrap">
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleEdit(row)">编辑</Button>
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleAssignRole(row)">分配角色</Button>
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleResetPassword(row)">重置密码</Button>
                  <Button 
                    variant="link" 
                    size="sm" 
                    class="h-auto p-0"
                    @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </Button>
                  <Button variant="link" size="sm" class="h-auto p-0 text-destructive" @click="handleDelete(row)">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div class="flex items-center justify-between mt-4 pt-4 border-t">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-1">
            <Select v-model="queryForm.pageSize!" @update:model-value="handleSizeChange(Number($event))">
              <SelectTrigger class="w-20">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem :value="10">10</SelectItem>
                <SelectItem :value="20">20</SelectItem>
                <SelectItem :value="50">50</SelectItem>
                <SelectItem :value="100">100</SelectItem>
              </SelectContent>
            </Select>
            <span class="text-sm px-2">条/页</span>
            <Button variant="outline" size="sm" :disabled="queryForm.page! <= 1" @click="handlePageChange(queryForm.page! - 1)">上一页</Button>
            <span class="text-sm px-2">{{ queryForm.page! }} / {{ totalPages() }}</span>
            <Button variant="outline" size="sm" :disabled="queryForm.page! >= totalPages()" @click="handlePageChange(queryForm.page! + 1)">下一页</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">用户名</label>
              <Input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!currentUser.id" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">邮箱</label>
              <Input v-model="userForm.email" placeholder="请输入邮箱" />
            </div>
            <div v-if="!currentUser.id" class="grid gap-2">
              <label class="text-sm font-medium">密码</label>
              <Input v-model="userForm.password" type="password" placeholder="请输入密码" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">姓名</label>
              <Input v-model="userForm.name" placeholder="请输入姓名" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">手机号</label>
              <Input v-model="userForm.phone" placeholder="请输入手机号" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">部门</label>
              <Input v-model="userForm.department" placeholder="请输入部门" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">岗位</label>
              <Input v-model="userForm.position" placeholder="请输入岗位" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">头像</label>
              <Input v-model="userForm.avatar" placeholder="请输入头像URL" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="resetPasswordDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>重置密码</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">新密码</label>
              <Input v-model="resetPasswordForm.newPassword" type="password" placeholder="请输入新密码" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">确认密码</label>
              <Input v-model="resetPasswordForm.confirmPassword" type="password" placeholder="请确认新密码" />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="resetPasswordDialogVisible = false">取消</Button>
          <Button @click="handleResetPasswordSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="assignRoleDialogVisible">
      <DialogContent class="max-w-lg">
        <DialogHeader>
          <DialogTitle>分配角色</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">当前角色</label>
              <div v-if="userRoles.length > 0" class="flex flex-wrap gap-2">
                <Badge
                  v-for="role in userRoles"
                  :key="role.id"
                  :variant="role.type === 'system' ? 'destructive' : 'default'"
                >
                  {{ role.name }}
                </Badge>
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
                  <Badge
                    :variant="role.type === 'system' ? 'destructive' : 'default'"
                    class="text-xs"
                  >
                    {{ role.type === 'system' ? '系统' : '自定义' }}
                  </Badge>
                  <span class="font-medium">{{ role.name }}</span>
                  <span class="text-xs text-muted-foreground">({{ role.code }})</span>
                </div>
              </div>
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="assignRoleDialogVisible = false">取消</Button>
          <Button @click="handleAssignRoleSubmit" :disabled="assignRoleLoading">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="importDialogVisible">
      <DialogContent class="max-w-2xl">
        <DialogHeader>
          <DialogTitle>导入用户</DialogTitle>
        </DialogHeader>
        <div class="py-4">
          <Alert class="mb-6">
            <AlertTitle class="font-semibold">导入说明</AlertTitle>
            <AlertDescription class="mt-2">
              <p>1. 请先下载导入模板，按照模板格式填写用户信息</p>
              <p>2. 必填字段：用户名、邮箱、姓名</p>
              <p>3. 如果不填写密码，系统将使用默认密码：Password123</p>
              <p>4. 文件格式：.xlsx 或 .xls</p>
            </AlertDescription>
          </Alert>

          <div class="text-center mb-6">
            <Button @click="handleDownloadTemplate">
              <Download class="w-4 h-4 mr-2" />
              下载导入模板
            </Button>
          </div>

          <Separator class="my-4" />

          <UploadComponent
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
          </UploadComponent>

          <div v-if="importResult" class="mt-6">
            <Separator class="my-4" />
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
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead class="w-20">行号</TableHead>
                    <TableHead class="w-36">用户名</TableHead>
                    <TableHead>错误信息</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="(err, idx) in importResult.errors" :key="idx">
                    <TableCell>{{ err.row }}</TableCell>
                    <TableCell>{{ err.username }}</TableCell>
                    <TableCell>{{ err.error }}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>

            <div v-if="importResult.importedUsers && importResult.importedUsers.length > 0" class="mt-4">
              <h4 class="font-semibold mb-2">成功导入的用户</h4>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead class="w-36">用户名</TableHead>
                    <TableHead class="w-48">邮箱</TableHead>
                    <TableHead>姓名</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow v-for="(user, idx) in importResult.importedUsers" :key="idx">
                    <TableCell>{{ user.username }}</TableCell>
                    <TableCell>{{ user.email }}</TableCell>
                    <TableCell>{{ user.name }}</TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="importDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
