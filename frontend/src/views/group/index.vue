<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { groupApi } from '@/api/group'
import { userApi } from '@/api/user'
import type { 
  UserGroup, 
  CreateGroupDto, 
  UpdateGroupDto, 
  GroupQueryDto,
  GroupTreeResponse,
  GroupStats,
  User
} from '@/types/group'

const loading = ref(false)
const groups = ref<UserGroup[]>([])
const treeData = ref<GroupTreeResponse[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户组')
const currentGroup = ref<Partial<UserGroup>>({})
const memberDialogVisible = ref(false)
const adminDialogVisible = ref(false)
const currentGroupId = ref('')
const stats = ref<GroupStats | null>(null)
const viewMode = ref<'list' | 'tree'>('list')

const queryForm = reactive<GroupQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined,
  parentId: undefined
})

const groupForm = reactive<CreateGroupDto & UpdateGroupDto>({
  name: '',
  description: '',
  type: 'team',
  parentId: undefined
})

const memberForm = reactive({
  selectedUsers: [] as string[],
  availableUsers: [] as User[],
  currentMembers: [] as User[],
  currentAdmins: [] as User[]
})

const rules = {
  name: [
    { required: true, message: '请输入用户组名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择用户组类型', trigger: 'change' }
  ]
}

const parentOptions = computed(() => {
  const options: Array<{ value: string; label: string; disabled?: boolean }> = []
  
  const addOptions = (items: GroupTreeResponse[], level = 0) => {
    items.forEach(item => {
      const prefix = '　'.repeat(level)
      options.push({
        value: item.id,
        label: prefix + item.name,
        disabled: currentGroup.value.id === item.id
      })
      if (item.children && item.children.length > 0) {
        addOptions(item.children, level + 1)
      }
    })
  }
  
  addOptions(treeData.value)
  return options
})

const loadGroups = async () => {
  loading.value = true
  try {
    const res = await groupApi.getList(queryForm)
    groups.value = res.data.groups
    total.value = res.data.total
  } catch (error) {
    console.error('加载用户组列表失败:', error)
    ElMessage.error('加载用户组列表失败')
  } finally {
    loading.value = false
  }
}

const loadTree = async () => {
  try {
    const res = await groupApi.getTree()
    treeData.value = res.data
  } catch (error) {
    console.error('加载用户组树失败:', error)
  }
}

const loadStats = async () => {
  try {
    const res = await groupApi.getStats()
    stats.value = res.data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadGroups()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.type = undefined
  queryForm.parentId = undefined
  queryForm.page = 1
  loadGroups()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户组'
  Object.assign(groupForm, {
    name: '',
    description: '',
    type: 'team',
    parentId: undefined
  })
  currentGroup.value = {}
  dialogVisible.value = true
}

const handleEdit = (row: UserGroup) => {
  dialogTitle.value = '编辑用户组'
  Object.assign(groupForm, {
    name: row.name,
    description: row.description || '',
    type: row.type,
    parentId: row.parentId || undefined
  })
  currentGroup.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: UserGroup) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户组吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await groupApi.delete(row.id)
    ElMessage.success('删除成功')
    loadGroups()
    loadTree()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户组失败:', error)
      ElMessage.error('删除用户组失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    if (currentGroup.value.id) {
      await groupApi.update(currentGroup.value.id, groupForm)
      ElMessage.success('更新成功')
    } else {
      await groupApi.create(groupForm as CreateGroupDto)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadGroups()
    loadTree()
    loadStats()
  } catch (error: any) {
    console.error('保存用户组失败:', error)
    ElMessage.error(error.response?.data?.message || '保存用户组失败')
  }
}

const handleManageMembers = async (row: UserGroup) => {
  currentGroupId.value = row.id
  memberDialogVisible.value = true
  
  try {
    const [membersRes, usersRes] = await Promise.all([
      groupApi.getMembers(row.id),
      userApi.getList({ pageSize: 1000 })
    ])
    
    memberForm.currentMembers = membersRes.data.members
    memberForm.currentAdmins = membersRes.data.admins
    memberForm.availableUsers = usersRes.data.users
    memberForm.selectedUsers = []
  } catch (error) {
    console.error('加载成员数据失败:', error)
    ElMessage.error('加载成员数据失败')
  }
}

const handleManageAdmins = async (row: UserGroup) => {
  currentGroupId.value = row.id
  adminDialogVisible.value = true
  
  try {
    const [membersRes, usersRes] = await Promise.all([
      groupApi.getMembers(row.id),
      userApi.getList({ pageSize: 1000 })
    ])
    
    memberForm.currentMembers = membersRes.data.members
    memberForm.currentAdmins = membersRes.data.admins
    memberForm.availableUsers = usersRes.data.users
    memberForm.selectedUsers = []
  } catch (error) {
    console.error('加载管理员数据失败:', error)
    ElMessage.error('加载管理员数据失败')
  }
}

const handleAddMembers = async () => {
  if (memberForm.selectedUsers.length === 0) {
    ElMessage.warning('请选择要添加的成员')
    return
  }
  
  try {
    await groupApi.addMembers(currentGroupId.value, { userIds: memberForm.selectedUsers })
    ElMessage.success('添加成员成功')
    handleManageMembers({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('添加成员失败:', error)
    ElMessage.error(error.response?.data?.message || '添加成员失败')
  }
}

const handleRemoveMember = async (userId: string) => {
  try {
    await groupApi.removeMembers(currentGroupId.value, { userIds: [userId] })
    ElMessage.success('移除成员成功')
    handleManageMembers({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('移除成员失败:', error)
    ElMessage.error(error.response?.data?.message || '移除成员失败')
  }
}

const handleAddAdmins = async () => {
  if (memberForm.selectedUsers.length === 0) {
    ElMessage.warning('请选择要添加的管理员')
    return
  }
  
  try {
    await groupApi.addAdmins(currentGroupId.value, { userIds: memberForm.selectedUsers })
    ElMessage.success('添加管理员成功')
    handleManageAdmins({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('添加管理员失败:', error)
    ElMessage.error(error.response?.data?.message || '添加管理员失败')
  }
}

const handleRemoveAdmin = async (userId: string) => {
  try {
    await groupApi.removeAdmins(currentGroupId.value, { userIds: [userId] })
    ElMessage.success('移除管理员成功')
    handleManageAdmins({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('移除管理员失败:', error)
    ElMessage.error(error.response?.data?.message || '移除管理员失败')
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadGroups()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadGroups()
}

const getTypeText = (type: string) => {
  switch (type) {
    case 'team':
      return '团队'
    case 'department':
      return '部门'
    case 'project':
      return '项目'
    case 'organization':
      return '组织'
    default:
      return '未知'
  }
}

const getTypeColor = (type: string) => {
  switch (type) {
    case 'team':
      return 'primary'
    case 'department':
      return 'success'
    case 'project':
      return 'warning'
    case 'organization':
      return 'danger'
    default:
      return 'info'
  }
}

onMounted(() => {
  loadGroups()
  loadTree()
  loadStats()
})
</script>

<template>
  <div class="group-management">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.totalGroups || 0 }}</div>
            <div class="stat-label">用户组总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.teamGroups || 0 }}</div>
            <div class="stat-label">团队</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.departmentGroups || 0 }}</div>
            <div class="stat-label">部门</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.projectGroups || 0 }}</div>
            <div class="stat-label">项目</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.organizationGroups || 0 }}</div>
            <div class="stat-label">组织</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats?.rootGroups || 0 }}</div>
            <div class="stat-label">根级组</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户组管理</span>
          <div>
            <el-radio-group v-model="viewMode" style="margin-right: 10px;">
              <el-radio-button value="list">列表视图</el-radio-button>
              <el-radio-button value="tree">树形视图</el-radio-button>
            </el-radio-group>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增用户组
            </el-button>
          </div>
        </div>
      </template>

      <el-form v-if="viewMode === 'list'" :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="用户组名称">
          <el-input v-model="queryForm.name" placeholder="请输入用户组名称" clearable />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.type" placeholder="请选择类型" clearable style="width: 150px">
            <el-option label="团队" value="team" />
            <el-option label="部门" value="department" />
            <el-option label="项目" value="project" />
            <el-option label="组织" value="organization" />
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

      <el-table 
        v-if="viewMode === 'list'"
        :data="groups" 
        v-loading="loading" 
        style="width: 100%"
      >
        <el-table-column prop="name" label="用户组名称" width="200" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parent" label="父级用户组" width="150">
          <template #default="{ row }">
            {{ row.parent?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="成员数" width="100" align="center">
          <template #default="{ row }">
            {{ row._count?.members || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="管理员数" width="100" align="center">
          <template #default="{ row }">
            {{ row._count?.admins || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="子组数" width="100" align="center">
          <template #default="{ row }">
            {{ row._count?.children || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleManageMembers(row)">
              成员管理
            </el-button>
            <el-button size="small" type="warning" @click="handleManageAdmins(row)">
              管理员
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-tree
        v-else
        :data="treeData"
        :props="{
          children: 'children',
          label: 'name'
        }"
        node-key="id"
        default-expand-all
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span class="node-label">{{ data.name }}</span>
            <el-tag :type="getTypeColor(data.type)" size="small" style="margin-left: 10px;">
              {{ getTypeText(data.type) }}
            </el-tag>
            <span class="node-info">
              成员: {{ data.memberCount }} | 管理员: {{ data.adminCount }}
            </span>
            <span class="node-actions">
              <el-button size="small" text @click.stop="handleEdit({ id: data.id } as UserGroup)">
                编辑
              </el-button>
              <el-button size="small" text type="primary" @click.stop="handleManageMembers({ id: data.id } as UserGroup)">
                成员
              </el-button>
              <el-button size="small" text type="danger" @click.stop="handleDelete({ id: data.id } as UserGroup)">
                删除
              </el-button>
            </span>
          </div>
        </template>
      </el-tree>

      <el-pagination
        v-if="viewMode === 'list'"
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="groupForm" :rules="rules" label-width="100px">
        <el-form-item label="用户组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入用户组名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="groupForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="groupForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="团队" value="team" />
            <el-option label="部门" value="department" />
            <el-option label="项目" value="project" />
            <el-option label="组织" value="organization" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级用户组">
          <el-select
            v-model="groupForm.parentId"
            placeholder="请选择父级用户组"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="option in parentOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
              :disabled="option.disabled"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="memberDialogVisible"
      title="成员管理"
      width="800px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>当前成员 ({{ memberForm.currentMembers.length }})</span>
              </div>
            </template>
            <el-table :data="memberForm.currentMembers" max-height="400">
              <el-table-column prop="name" label="姓名" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="email" label="邮箱" />
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    type="danger"
                    text
                    @click="handleRemoveMember(row.id)"
                  >
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>添加成员</span>
              </div>
            </template>
            <el-select
              v-model="memberForm.selectedUsers"
              multiple
              filterable
              placeholder="请选择要添加的成员"
              style="width: 100%; margin-bottom: 10px;"
            >
              <el-option
                v-for="user in memberForm.availableUsers.filter(
                  u => !memberForm.currentMembers.find(m => m.id === u.id)
                )"
                :key="user.id"
                :label="`${user.name} (${user.username})`"
                :value="user.id"
              />
            </el-select>
            <el-button type="primary" @click="handleAddMembers" style="width: 100%;">
              添加选中成员
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>

    <el-dialog
      v-model="adminDialogVisible"
      title="管理员管理"
      width="800px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>当前管理员 ({{ memberForm.currentAdmins.length }})</span>
              </div>
            </template>
            <el-table :data="memberForm.currentAdmins" max-height="400">
              <el-table-column prop="name" label="姓名" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="email" label="邮箱" />
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    type="danger"
                    text
                    @click="handleRemoveAdmin(row.id)"
                  >
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>添加管理员</span>
              </div>
            </template>
            <el-select
              v-model="memberForm.selectedUsers"
              multiple
              filterable
              placeholder="请选择要添加的管理员"
              style="width: 100%; margin-bottom: 10px;"
            >
              <el-option
                v-for="user in memberForm.availableUsers.filter(
                  u => !memberForm.currentAdmins.find(a => a.id === u.id)
                )"
                :key="user.id"
                :label="`${user.name} (${user.username})`"
                :value="user.id"
              />
            </el-select>
            <el-button type="primary" @click="handleAddAdmins" style="width: 100%;">
              添加选中管理员
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<style scoped>
.group-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 14px;
  padding-right: 8px;
}

.node-label {
  font-weight: 500;
}

.node-info {
  margin-left: 15px;
  font-size: 12px;
  color: #909399;
}

.node-actions {
  margin-left: auto;
}
</style>
