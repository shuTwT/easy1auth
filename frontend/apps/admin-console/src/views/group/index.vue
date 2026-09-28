<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { Modal, message } from 'antdv-next'
import { Plus, Search } from '@lucide/vue'
import { groupApi } from '@/api/group'
import { userApi } from '@/api/user'
import type {
  UserGroup,
  CreateGroupDto,
  UpdateGroupDto,
  GroupQueryDto,
  GroupStats,
  User
} from '@/types/group'
const loading = ref(false)
const groups = ref<UserGroup[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户组')
const currentGroup = ref<Partial<UserGroup>>({})
const memberDialogVisible = ref(false)
const currentGroupId = ref('')
const stats = ref<GroupStats | null>(null)

const [modal, contextHolder] = Modal.useModal()

const queryForm = reactive<GroupQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined
})

const groupForm = reactive<CreateGroupDto & UpdateGroupDto>({
  name: '',
  description: '',
  type: 'team'
})

const groupFormRules = {
  name: [{ required: true, message: '请输入用户组名称' }],
}
const groupTypeOptions = [
  { value: 'team', label: '团队' },
  { value: 'department', label: '部门' },
  { value: 'project', label: '项目' },
  { value: 'organization', label: '组织' },
]

const memberForm = reactive({
  selectedUsers: [] as string[],
  availableUsers: [] as User[],
  currentMembers: [] as User[]
})

const memberUserOptions = computed(() => memberForm.availableUsers.filter((user) => !memberForm.currentMembers.some((member) => member.id === user.id)).map((user) => ({ value: user.id, label: `${user.name} (${user.username})` })))

const loadGroups = async () => {
  loading.value = true
  try {
    const res = await groupApi.getList(queryForm)
    groups.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载用户组列表失败:', error)
    message.error('加载用户组列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await groupApi.getStats()
    stats.value = res
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载用户组统计数据失败')
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadGroups()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.type = undefined
  queryForm.page = 1
  loadGroups()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户组'
  Object.assign(groupForm, {
    name: '',
    description: '',
    type: 'team'
  })
  currentGroup.value = {}
  dialogVisible.value = true
}

const handleEdit = (row: UserGroup) => {
  dialogTitle.value = '编辑用户组'
  Object.assign(groupForm, {
    name: row.name,
    description: row.description || '',
    type: row.type
  })
  currentGroup.value = row
  dialogVisible.value = true
}

const handleDelete = async (row: UserGroup) => {
  const confirmed = await modal.confirm({
    title: '删除用户组',
    content: '确定要删除该用户组吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) {
    return
  }

  try {
    await groupApi.delete(row.id)
    message.success('删除成功')
    loadGroups()
    loadStats()
  } catch (error) {
    console.error('删除用户组失败:', error)
    message.error('删除用户组失败')
  }
}

const handleSubmit = async () => {
  try {
    if (currentGroup.value.id) {
      await groupApi.update(currentGroup.value.id, groupForm)
      message.success('更新成功')
    } else {
      await groupApi.create(groupForm as CreateGroupDto)
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadGroups()
    loadStats()
  } catch (error: any) {
    console.error('保存用户组失败:', error)
    message.error(error.response?.data?.msg || '保存用户组失败')
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

    memberForm.currentMembers = membersRes.members
    memberForm.availableUsers = usersRes.items
    memberForm.selectedUsers = []
  } catch (error) {
    console.error('加载成员数据失败:', error)
    message.error('加载成员数据失败')
  }
}

const handleAddMembers = async () => {
  if (memberForm.selectedUsers.length === 0) {
    message.warning('请选择要添加的成员')
    return
  }

  try {
    await groupApi.addMembers(currentGroupId.value, { userIds: memberForm.selectedUsers })
    message.success('添加成员成功')
    handleManageMembers({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('添加成员失败:', error)
    message.error(error.response?.data?.msg || '添加成员失败')
  }
}

const handleRemoveMember = async (userId: string) => {
  try {
    await groupApi.removeMembers(currentGroupId.value, { userIds: [userId] })
    message.success('移除成员成功')
    handleManageMembers({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('移除成员失败:', error)
    message.error(error.response?.data?.msg || '移除成员失败')
  }
}

const handlePageChange = (p: number, ps?: number) => {
  queryForm.page = p
  if (ps !== undefined) queryForm.pageSize = ps
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

const getTypeVariant = (type: string): string => {
  switch (type) {
    case 'team':
      return 'blue'
    case 'department':
      return 'green'
    case 'project':
      return 'purple'
    case 'organization':
      return 'orange'
    default:
      return 'default'
  }
}

onMounted(() => {
  loadGroups()
  loadStats()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">用户组管理</h1>
        <p class="text-sm text-muted-foreground">管理用户组，包括创建、编辑、删除和成员管理</p>
      </div>
      <div class="flex items-center gap-3">
        <a-button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增用户组
        </a-button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-5 mb-6">
      <a-card v-for="(stat, key) in [
        { label: '用户组总数', value: stats?.totalGroups || 0 },
        { label: '团队', value: stats?.teamGroups || 0 },
        { label: '部门', value: stats?.departmentGroups || 0 },
        { label: '项目', value: stats?.projectGroups || 0 },
        { label: '组织', value: stats?.organizationGroups || 0 }
      ]" :key="key">
        <div class="pt-4">
          <div class="text-center">
            <div class="text-2xl font-bold text-foreground mb-1">{{ stat.value }}</div>
            <div class="text-sm text-muted-foreground">{{ stat.label }}</div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card>

      <div class="pt-4">
        <div class="mb-4">
          <div class="flex flex-wrap items-end gap-3">
            <div class="grid gap-1.5">
              <label class="text-sm font-medium">用户组名称</label>
              <a-input v-model:value="queryForm.name" placeholder="请输入用户组名称" class="w-48" />
            </div>
            <div class="grid gap-1.5">
              <label class="text-sm font-medium">类型</label>
              <a-select v-model:value="queryForm.type" placeholder="请选择类型" class="w-40" allow-clear :options="groupTypeOptions" />
            </div>
            <div class="flex gap-2">
              <a-button @click="handleSearch">
                <Search class="size-4 mr-2" />
                搜索
              </a-button>
              <a-button  @click="handleReset">重置</a-button>
            </div>
          </div>
        </div>

        <div>
          <a-table :columns="[
            { title: '用户组名称', key: 'name', width: 200 }, { title: '描述', key: 'description' },
            { title: '类型', key: 'type', width: 100 },
            { title: '成员数', key: 'members', width: 100, align: 'center' },
            { title: '创建时间', key: 'createdAt', width: 180 },
            { title: '操作', key: 'actions', width: 220 }
          ]" :data-source="groups" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 910 }">
            <template #bodyCell="{ column, record: row }">
              <template v-if="column.key === 'name'"><span class="font-medium">{{ row.name }}</span></template>
              <template v-else-if="column.key === 'description'"><span class="text-muted-foreground">{{ row.description || '-' }}</span></template>
              <template v-else-if="column.key === 'type'">
                  <a-tag :color="getTypeVariant(row.type)" class="text-xs">
                    {{ getTypeText(row.type) }}
                  </a-tag>
              </template>
              <template v-else-if="column.key === 'members'">{{ row._count?.members || 0 }}</template>
              <template v-else-if="column.key === 'createdAt'"><span class="text-muted-foreground">{{ new Date(row.createdAt).toLocaleString() }}</span></template>
              <template v-else-if="column.key === 'actions'">
                  <div class="flex gap-1">
                    <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">
                      编辑
                    </a-button>
                    <a-button type="link" size="small" class="h-auto p-0" @click="handleManageMembers(row)">
                      成员
                    </a-button>
                    <a-button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDelete(row)">
                      删除
                    </a-button>
                  </div>
              </template>
            </template>
          </a-table>

          <div v-if="total > (queryForm.pageSize || 10)" class="flex items-center justify-between mt-4">
            <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
            <a-pagination
              :current="queryForm.page"
              :page-size="queryForm.pageSize"
              :total="total"
              :show-size-changer="false"
              size="small"
              @change="handlePageChange"
            />
          </div>
        </div>


      </div>
    </a-card>

    <a-modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <a-form :model="groupForm" :rules="groupFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <a-form-item label="用户组名称" name="name">
              <a-input v-model:value="groupForm.name" placeholder="请输入用户组名称" />
            </a-form-item>
            <a-form-item label="描述" name="description">
              <a-textarea v-model:value="groupForm.description" :rows="3" placeholder="请输入描述" />
            </a-form-item>
            <a-form-item label="类型" name="type">
              <a-select v-model:value="groupForm.type" :options="groupTypeOptions" />
            </a-form-item>
          </div>
          <div class="flex justify-end gap-2">
            <a-button @click="dialogVisible = false">取消</a-button>
            <a-button type="primary" html-type="submit">确定</a-button>
          </div>
        </a-form>
      </div>
    </a-modal>

    <a-modal v-model:open="memberDialogVisible" :footer="null">
      <div class="max-w-3xl">
        <div>
          <h3>成员管理</h3>
        </div>
        <div class="min-h-[400px]">
          <div class="grid grid-cols-2 gap-5">
            <a-card>
              <div class="border-b py-3">
                <span class="font-medium">当前成员 ({{ memberForm.currentMembers.length }})</span>
              </div>
              <div class="pt-4">
                <div class="max-h-[350px] overflow-y-auto">
                  <a-table :columns="[{ title: '姓名', dataIndex: 'name' }, { title: '用户名', dataIndex: 'username' }, { title: '邮箱', dataIndex: 'email' }, { title: '操作', key: 'actions', width: 80 }]" :data-source="memberForm.currentMembers" :pagination="false" row-key="id" size="small"><template #bodyCell="{ column, record: member }"><a-button v-if="column.key === 'actions'" type="link" size="small" class="p-0 h-auto text-destructive" @click="handleRemoveMember(member.id)">移除</a-button></template></a-table>
                </div>
              </div>
            </a-card>
            <a-card>
              <div class="border-b py-3">
                <span class="font-medium">添加成员</span>
              </div>
              <div class="pt-4">
                <a-select v-model:value="memberForm.selectedUsers" multiple :options="memberUserOptions" />
                <a-button class="w-full" @click="handleAddMembers">
                  添加选中成员
                </a-button>
              </div>
            </a-card>
          </div>
        </div>
      </div>
    </a-modal>

    <contextHolder />
  </div>
</template>

<style scoped>
</style>
