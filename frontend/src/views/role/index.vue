<template>
  <div class="role-management">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">角色管理</h1>
        <p class="page-subtitle">管理系统角色，包括创建、编辑、删除和分配用户</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          创建角色
        </el-button>
      </div>
    </div>

    <div class="stats-cards">
      <el-card v-for="(stat, index) in statsData" :key="index" class="stat-card" shadow="hover">
        <div class="stat-content">
          <div class="stat-icon" :class="stat.class">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-input
              v-model="searchQuery"
              placeholder="搜索角色名称、编码"
              style="width: 300px"
              clearable
              @clear="loadRoles"
              @keyup.enter="loadRoles"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-select v-model="filterType" placeholder="角色类型" clearable style="width: 150px; margin-left: 12px" @change="loadRoles">
              <el-option label="系统角色" value="system" />
              <el-option label="自定义角色" value="custom" />
            </el-select>
          </div>
          <div class="header-right">
            <el-button-group style="margin-right: 12px">
              <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">
                <el-icon><List /></el-icon>
                列表
              </el-button>
              <el-button :type="viewMode === 'tree' ? 'primary' : ''" @click="viewMode = 'tree'">
                <el-icon><Share /></el-icon>
                树形
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <div v-if="viewMode === 'list'" class="role-list">
        <el-table :data="roles" style="width: 100%" v-loading="loading">
          <el-table-column prop="name" label="角色名称" width="200">
            <template #default="{ row }">
              <div class="role-name-cell">
                <el-tag :type="row.type === 'system' ? 'danger' : 'success'" size="small">
                  {{ row.type === 'system' ? '系统' : '自定义' }}
                </el-tag>
                <span class="role-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="角色编码" width="180">
            <template #default="{ row }">
              <code class="role-code">{{ row.code }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="数据范围" width="140">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ getDataScopeLabel(row.dataScope) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="用户数" width="100" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleViewUsers(row)" class="user-count-btn">
                {{ row.userCount || 0 }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleViewUsers(row)">
                查看用户
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="handleEdit(row)"
                :disabled="row.type === 'system'"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                @click="handleDelete(row)"
                :disabled="row.type === 'system'"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else class="role-tree">
        <el-tree
          :data="roleTree"
          :props="{ children: 'children', label: 'name' }"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="node-label">
                <el-tag :type="data.type === 'system' ? 'danger' : 'success'" size="small" style="margin-right: 8px">
                  {{ data.type === 'system' ? '系统' : '自定义' }}
                </el-tag>
                {{ data.name }}
                <span class="node-code">({{ data.code }})</span>
              </span>
              <span class="node-users">
                <el-button link type="primary" size="small" @click.stop="handleViewUsers(data)">
                  {{ data.userCount || 0 }} 用户
                </el-button>
              </span>
            </div>
          </template>
        </el-tree>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" class="custom-dialog">
      <el-form :model="roleForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleForm.code" placeholder="请输入角色编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色类型" prop="type">
          <el-select v-model="roleForm.type" placeholder="请选择角色类型" :disabled="isEdit" style="width: 100%">
            <el-option label="自定义角色" value="custom" />
            <el-option label="系统角色" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="roleForm.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option label="仅本人数据" value="self" />
            <el-option label="本部门数据" value="department" />
            <el-option label="本部门及下级部门数据" value="department_and_sub" />
            <el-option label="全部数据" value="all" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级角色" prop="parentId">
          <el-select v-model="roleForm.parentId" placeholder="请选择父级角色" clearable style="width: 100%">
            <el-option
              v-for="role in availableParentRoles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="usersDialogVisible" title="角色用户" width="800px" class="custom-dialog">
      <div class="users-dialog-content">
        <div class="users-toolbar">
          <el-input
            v-model="userSearchQuery"
            placeholder="搜索用户名、邮箱、姓名"
            style="width: 300px"
            clearable
            @clear="loadRoleUsers"
            @keyup.enter="loadRoleUsers"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleAssignUsers">
            <el-icon><Plus /></el-icon>
            分配用户
          </el-button>
        </div>
        <el-table :data="roleUsers" style="width: 100%" v-loading="usersLoading">
          <el-table-column prop="username" label="用户名" width="150">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="28" class="user-avatar">
                  {{ row.name?.charAt(0) || row.username.charAt(0).toUpperCase() }}
                </el-avatar>
                <span>{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="email" label="邮箱" width="200" />
          <el-table-column prop="department" label="部门" width="150" />
          <el-table-column prop="position" label="岗位" width="150" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
                {{ row.status === 'active' ? '正常' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="handleRemoveUser(row)">
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="assignUsersDialogVisible" title="分配用户" width="800px" class="custom-dialog">
      <div class="assign-users-content">
        <el-transfer
          v-model="selectedUserIds"
          :data="allUsers"
          :titles="['可选用户', '已选用户']"
          :props="{ key: 'id', label: 'name' }"
          filterable
          filter-placeholder="搜索用户"
        />
      </div>
      <template #footer>
        <el-button @click="assignUsersDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitAssignUsers" :loading="assigning">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  List,
  Share,
} from '@element-plus/icons-vue'
import { roleApi } from '../../api/role'
import { userApi } from '../../api/user'
import type { Role, RoleTree, RoleStats, CreateRoleDto, RoleUser } from '../../types/role'

const loading = ref(false)
const roles = ref<Role[]>([])
const roleTree = ref<RoleTree[]>([])
const stats = ref<RoleStats>({
  totalRoles: 0,
  systemRoles: 0,
  customRoles: 0,
  totalUsers: 0,
})
const searchQuery = ref('')
const filterType = ref('')
const viewMode = ref<'list' | 'tree'>('list')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const roleForm = reactive<CreateRoleDto & { id?: string }>({
  name: '',
  code: '',
  description: '',
  type: 'custom',
  permissions: {},
  dataScope: 'self',
  parentId: undefined,
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '编码只能包含大写字母和下划线', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择角色类型', trigger: 'change' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }],
}

const statsData = computed(() => [
  { label: '总角色数', value: stats.value.totalRoles, icon: 'Collection', class: 'primary' },
  { label: '系统角色', value: stats.value.systemRoles, icon: 'Lock', class: 'danger' },
  { label: '自定义角色', value: stats.value.customRoles, icon: 'UserFilled', class: 'success' },
  { label: '已分配用户', value: stats.value.totalUsers, icon: 'User', class: 'info' },
])

const availableParentRoles = computed(() => {
  return roles.value.filter((role) => role.id !== roleForm.id)
})

const dialogTitle = computed(() => (isEdit.value ? '编辑角色' : '创建角色'))

const usersDialogVisible = ref(false)
const usersLoading = ref(false)
const roleUsers = ref<RoleUser[]>([])
const currentRole = ref<Role | null>(null)
const userSearchQuery = ref('')

const assignUsersDialogVisible = ref(false)
const selectedUserIds = ref<string[]>([])
const allUsers = ref<any[]>([])
const assigning = ref(false)

const loadStats = async () => {
  try {
    const data = await roleApi.getStats()
    stats.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const loadRoles = async () => {
  loading.value = true
  try {
    const data = await roleApi.getList({
      search: searchQuery.value,
      type: filterType.value,
    })
    roles.value = data
  } catch (error) {
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

const loadRoleTree = async () => {
  try {
    const data = await roleApi.getTree()
    roleTree.value = data
  } catch (error) {
    console.error('加载角色树失败:', error)
  }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(roleForm, {
    name: '',
    code: '',
    description: '',
    type: 'custom',
    permissions: {},
    dataScope: 'self',
    parentId: undefined,
  })
  dialogVisible.value = true
}

const handleEdit = (row: Role) => {
  isEdit.value = true
  Object.assign(roleForm, {
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description || '',
    type: row.type,
    permissions: row.permissions,
    dataScope: row.dataScope,
    parentId: row.parentId || undefined,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await roleApi.update(roleForm.id!, {
          name: roleForm.name,
          description: roleForm.description,
          permissions: roleForm.permissions,
          dataScope: roleForm.dataScope,
          parentId: roleForm.parentId,
        })
        ElMessage.success('更新成功')
      } else {
        await roleApi.create(roleForm)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadRoles()
      loadRoleTree()
      loadStats()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.error || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row: Role) => {
  try {
    await ElMessageBox.confirm('确定要删除该角色吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await roleApi.delete(row.id)
    ElMessage.success('删除成功')
    loadRoles()
    loadRoleTree()
    loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || '删除失败')
    }
  }
}

const handleViewUsers = async (row: Role | RoleTree) => {
  currentRole.value = row as Role
  usersDialogVisible.value = true
  await loadRoleUsers()
}

const loadRoleUsers = async () => {
  if (!currentRole.value) return

  usersLoading.value = true
  try {
    const data = await roleApi.getRoleUsers(currentRole.value.id, {
      search: userSearchQuery.value,
    })
    roleUsers.value = data.users
  } catch (error) {
    ElMessage.error('加载角色用户失败')
  } finally {
    usersLoading.value = false
  }
}

const handleAssignUsers = async () => {
  try {
    const response = await userApi.getList()
    allUsers.value = response.data.users.map((user: any) => ({
      id: user.id,
      name: `${user.name} (${user.username})`,
      disabled: roleUsers.value.some((ru) => ru.id === user.id),
    }))
    selectedUserIds.value = []
    assignUsersDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  }
}

const handleSubmitAssignUsers = async () => {
  if (selectedUserIds.value.length === 0) {
    ElMessage.warning('请选择要分配的用户')
    return
  }

  if (!currentRole.value) return

  assigning.value = true
  try {
    await roleApi.assignUsers(currentRole.value.id, {
      userIds: selectedUserIds.value,
    })
    ElMessage.success('分配用户成功')
    assignUsersDialogVisible.value = false
    loadRoleUsers()
    loadStats()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.error || '分配用户失败')
  } finally {
    assigning.value = false
  }
}

const handleRemoveUser = async (user: RoleUser) => {
  try {
    await ElMessageBox.confirm(`确定要从该角色移除用户 ${user.name} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    if (!currentRole.value) return

    await roleApi.removeUsers(currentRole.value.id, {
      userIds: [user.id],
    })
    ElMessage.success('移除成功')
    loadRoleUsers()
    loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || '移除失败')
    }
  }
}

const getDataScopeLabel = (scope: string) => {
  const map: Record<string, string> = {
    self: '仅本人',
    department: '本部门',
    department_and_sub: '本部门及下级',
    all: '全部',
  }
  return map[scope] || scope
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadStats()
  loadRoles()
  loadRoleTree()
})
</script>

<style scoped>
.role-management {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  cursor: pointer;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--card-hover-shadow);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--border-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: var(--primary-gradient);
}

.stat-icon.danger {
  background: linear-gradient(135deg, #EF4444 0%, #F87171 100%);
}

.stat-icon.success {
  background: linear-gradient(135deg, #10B981 0%, #34D399 100%);
}

.stat-icon.info {
  background: linear-gradient(135deg, #0EA5E9 0%, #38BDF8 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.main-card {
  border-radius: var(--border-radius-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-name {
  font-weight: 500;
  color: var(--text-primary);
}

.role-code {
  background: #F1F5F9;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  font-family: 'Fira Code', monospace;
}

.user-count-btn {
  font-weight: 600;
  font-size: 14px;
}

.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 20px;
}

.node-label {
  display: flex;
  align-items: center;
}

.node-code {
  color: var(--text-muted);
  font-size: 12px;
  margin-left: 8px;
}

.node-users {
  margin-left: 20px;
}

.custom-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid var(--border-color);
  padding: 16px 20px;
  margin-right: 0;
}

.custom-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.custom-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid var(--border-color);
  padding: 16px 20px;
}

.users-dialog-content {
  min-height: 400px;
}

.users-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.assign-users-content {
  min-height: 400px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  background: var(--primary-gradient);
  color: white;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
