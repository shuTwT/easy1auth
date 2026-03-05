<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { roleApi } from '@/api/role'
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

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { min: 6, message: '密码长度至少为 6 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ]
}

const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== resetPasswordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getList(queryForm)
    users.value = res.data.users
    total.value = res.data.total
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
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
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await userApi.delete(row.id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败')
    }
  }
}

const handleStatusChange = async (row: User, status: string) => {
  try {
    await userApi.updateStatus(row.id, status)
    ElMessage.success('状态更新成功')
    loadUsers()
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新状态失败')
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
      ElMessage.success('更新成功')
    } else {
      if (!userForm.password) {
        ElMessage.warning('创建用户时必须设置密码')
        return
      }
      await userApi.create(userForm as CreateUserDto)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    console.error('保存用户失败:', error)
    ElMessage.error(error.response?.data?.message || '保存用户失败')
  }
}

const handleResetPasswordSubmit = async () => {
  try {
    if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    
    await userApi.resetPassword(resetPasswordUserId.value, resetPasswordForm.newPassword)
    ElMessage.success('密码重置成功')
    resetPasswordDialogVisible.value = false
  } catch (error: any) {
    console.error('重置密码失败:', error)
    ElMessage.error(error.response?.data?.message || '重置密码失败')
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
    ElMessage.error('加载角色数据失败')
  } finally {
    assignRoleLoading.value = false
  }
}

const handleAssignRoleSubmit = async () => {
  assignRoleLoading.value = true
  try {
    await roleApi.assignRolesToUser(assignRoleUserId.value, selectedRoleIds.value)
    ElMessage.success('分配角色成功')
    assignRoleDialogVisible.value = false
    loadUsers()
  } catch (error: any) {
    console.error('分配角色失败:', error)
    ElMessage.error(error.response?.data?.error || '分配角色失败')
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

const getStatusType = (status: string) => {
  switch (status) {
    case 'active':
      return 'success'
    case 'disabled':
      return 'warning'
    case 'locked':
      return 'danger'
    default:
      return 'info'
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

const handleFileChange = async (options: any) => {
  const { file } = options
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
      ElMessage.success(result.message)
      loadUsers()
    } else {
      ElMessage.error(result.message || '导入失败')
    }
  } catch (error) {
    console.error('导入用户失败:', error)
    ElMessage.error('导入用户失败')
  } finally {
    importLoading.value = false
  }
}

</script>

<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div>
            <el-button @click="handleImport">
              <el-icon><Upload /></el-icon>
              导入用户
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增用户
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="queryForm.email" placeholder="请输入邮箱" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="disabled" />
            <el-option label="锁定" value="locked" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" width="180">
          <template #default="{ row }">
            {{ row.lastLoginAt ? new Date(row.lastLoginAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleAssignRole(row)">分配角色</el-button>
            <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button 
              link 
              :type="row.status === 'active' ? 'warning' : 'success'" 
              @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="userForm" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="!!currentUser.id" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!currentUser.id">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="userForm.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="岗位" prop="position">
          <el-input v-model="userForm.position" placeholder="请输入岗位" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="userForm.avatar" placeholder="请输入头像URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPasswordDialogVisible" title="重置密码" width="400px">
      <el-form :model="resetPasswordForm" :rules="resetPasswordRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPasswordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetPasswordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPasswordSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignRoleDialogVisible" title="分配角色" width="600px">
      <el-form label-width="100px">
        <el-form-item label="当前角色">
          <div v-if="userRoles.length > 0">
            <el-tag
              v-for="role in userRoles"
              :key="role.id"
              :type="role.type === 'system' ? 'danger' : 'success'"
              style="margin-right: 8px; margin-bottom: 8px"
            >
              {{ role.name }}
            </el-tag>
          </div>
          <div v-else style="color: #909399">暂未分配角色</div>
        </el-form-item>
        <el-form-item label="选择角色">
          <el-checkbox-group v-model="selectedRoleIds">
            <el-checkbox
              v-for="role in allRoles"
              :key="role.id"
              :label="role.id"
              style="display: block; margin-bottom: 10px"
            >
              <el-tag
                :type="role.type === 'system' ? 'danger' : 'success'"
                size="small"
                style="margin-right: 8px"
              >
                {{ role.type === 'system' ? '系统' : '自定义' }}
              </el-tag>
              {{ role.name }}
              <span style="color: #909399; font-size: 12px; margin-left: 8px">({{ role.code }})</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignRoleSubmit" :loading="assignRoleLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="导入用户" width="700px">
      <div class="import-content">
        <el-alert type="info" :closable="false" style="margin-bottom: 20px">
          <template #title>
            <strong>导入说明</strong>
          </template>
          <div style="margin-top: 10px">
            <p>1. 请先下载导入模板，按照模板格式填写用户信息</p>
            <p>2. 必填字段：用户名、邮箱、姓名</p>
            <p>3. 如果不填写密码，系统将使用默认密码：Password123</p>
            <p>4. 文件格式：.xlsx 或 .xls</p>
          </div>
        </el-alert>

        <div class="import-actions">
          <el-button type="primary" @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon>
            下载导入模板
          </el-button>
        </div>

        <el-divider />

        <el-upload
          class="upload-area"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              只能上传 xlsx/xls 文件，且文件大小不超过 5MB
            </div>
          </template>
        </el-upload>

        <div v-if="importResult" class="import-result">
          <el-divider />
          <h4>导入结果</h4>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="总数">{{ importResult.total }}</el-descriptions-item>
            <el-descriptions-item label="成功">
              <el-tag type="success">{{ importResult.success }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="失败">
              <el-tag type="danger">{{ importResult.failed }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top: 20px">
            <h4>错误详情</h4>
            <el-table :data="importResult.errors" style="width: 100%" max-height="300">
              <el-table-column prop="row" label="行号" width="80" />
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="error" label="错误信息" />
            </el-table>
          </div>

          <div v-if="importResult.importedUsers && importResult.importedUsers.length > 0" style="margin-top: 20px">
            <h4>成功导入的用户</h4>
            <el-table :data="importResult.importedUsers" style="width: 100%" max-height="300">
              <el-table-column prop="username" label="用户名" width="150" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="name" label="姓名" />
            </el-table>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.import-content {
  padding: 10px 0;
}

.import-actions {
  text-align: center;
  margin: 20px 0;
}

.upload-area {
  width: 100%;
}

.import-result {
  margin-top: 20px;
}

.import-result h4 {
  margin-bottom: 15px;
  color: #303133;
}
</style>
