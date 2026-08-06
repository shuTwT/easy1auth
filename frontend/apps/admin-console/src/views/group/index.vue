<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { Table, message } from 'antdv-next'
import { Plus, Search, Users, UserCog, Pencil, Trash2 } from '@lucide/vue'
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

const parentOptions = computed(() => {
  const options: Array<{ value: string; label: string; disabled?: boolean }> = []

  const addOptions = (items: GroupTreeResponse[], level = 0) => {
    items.forEach(item => {
      const prefix = '\u3000'.repeat(level)
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
    groups.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载用户组列表失败:', error)
    message.error('加载用户组列表失败')
  } finally {
    loading.value = false
  }
}

const loadTree = async () => {
  try {
    const res = await groupApi.getTree()
    treeData.value = res
  } catch (error) {
    console.error('加载用户组树失败:', error)
  }
}

const loadStats = async () => {
  try {
    const res = await groupApi.getStats()
    stats.value = res
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
  if (!window.confirm('确定要删除该用户组吗？删除后无法恢复！')) {
    return
  }

  try {
    await groupApi.delete(row.id)
    message.success('删除成功')
    loadGroups()
    loadTree()
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
    loadTree()
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
    memberForm.currentAdmins = membersRes.admins
    memberForm.availableUsers = usersRes.items
    memberForm.selectedUsers = []
  } catch (error) {
    console.error('加载成员数据失败:', error)
    message.error('加载成员数据失败')
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

    memberForm.currentMembers = membersRes.members
    memberForm.currentAdmins = membersRes.admins
    memberForm.availableUsers = usersRes.items
    memberForm.selectedUsers = []
  } catch (error) {
    console.error('加载管理员数据失败:', error)
    message.error('加载管理员数据失败')
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

const handleAddAdmins = async () => {
  if (memberForm.selectedUsers.length === 0) {
    message.warning('请选择要添加的管理员')
    return
  }

  try {
    await groupApi.addAdmins(currentGroupId.value, { userIds: memberForm.selectedUsers })
    message.success('添加管理员成功')
    handleManageAdmins({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('添加管理员失败:', error)
    message.error(error.response?.data?.msg || '添加管理员失败')
  }
}

const handleRemoveAdmin = async (userId: string) => {
  try {
    await groupApi.removeAdmins(currentGroupId.value, { userIds: [userId] })
    message.success('移除管理员成功')
    handleManageAdmins({ id: currentGroupId.value } as UserGroup)
  } catch (error: any) {
    console.error('移除管理员失败:', error)
    message.error(error.response?.data?.msg || '移除管理员失败')
  }
}

const handlePageChange = (p: number) => {
  queryForm.page = p
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

const getTypeVariant = (type: string): 'default' | 'secondary' | 'destructive' | 'outline' | 'ghost' | 'link' => {
  switch (type) {
    case 'team':
      return 'default'
    case 'department':
      return 'secondary'
    case 'project':
      return 'outline'
    case 'organization':
      return 'destructive'
    default:
      return 'ghost'
  }
}

onMounted(() => {
  loadGroups()
  loadTree()
  loadStats()
})
</script>

<template>
  <div class="p-5">
    <div class="grid grid-cols-6 gap-5 mb-5">
      <Card v-for="(stat, key) in [
        { label: '用户组总数', value: stats?.totalGroups || 0 },
        { label: '团队', value: stats?.teamGroups || 0 },
        { label: '部门', value: stats?.departmentGroups || 0 },
        { label: '项目', value: stats?.projectGroups || 0 },
        { label: '组织', value: stats?.organizationGroups || 0 },
        { label: '根级组', value: stats?.rootGroups || 0 }
      ]" :key="key">
        <div class="pt-4">
          <div class="text-center">
            <div class="text-3xl font-bold text-primary mb-1">{{ stat.value }}</div>
            <div class="text-sm text-muted-foreground">{{ stat.label }}</div>
          </div>
        </div>
      </Card>
    </div>

    <Card>
      <div class="border-b">
        <div class="flex justify-between items-center">
          <span class="font-semibold">用户组管理</span>
          <div class="flex items-center gap-3">
            <div class="flex gap-0">
              <Button
                :color="viewMode === 'list' ? 'default' : 'outline'"
                class="rounded-r-none"
                @click="viewMode = 'list'"
              >
                列表视图
              </Button>
              <Button
                :color="viewMode === 'tree' ? 'default' : 'outline'"
                class="rounded-l-none"
                @click="viewMode = 'tree'"
              >
                树形视图
              </Button>
            </div>
            <Button @click="handleAdd">
              <Plus class="w-4 h-4 mr-1" />
              新增用户组
            </Button>
          </div>
        </div>
      </div>

      <div class="pt-4">
        <div v-if="viewMode === 'list'" class="mb-4">
          <div class="flex flex-wrap items-end gap-3">
            <div class="grid gap-1.5">
              <label class="text-sm font-medium">用户组名称</label>
              <Input v-model:value="queryForm.name" placeholder="请输入用户组名称" class="w-48" />
            </div>
            <div class="grid gap-1.5">
              <label class="text-sm font-medium">类型</label>
              <Select v-model:value="queryForm.type">
                <div class="w-40">

                </div>

                  <SelectOption value="team">团队</SelectOption>
                  <SelectOption value="department">部门</SelectOption>
                  <SelectOption value="project">项目</SelectOption>
                  <SelectOption value="organization">组织</SelectOption>

              </Select>
            </div>
            <div class="flex gap-2">
              <Button @click="handleSearch">
                <Search class="w-4 h-4 mr-1" />
                搜索
              </Button>
              <Button  @click="handleReset">重置</Button>
            </div>
          </div>
        </div>

        <div v-if="viewMode === 'list'">
          <Table :columns="[
            { title: '用户组名称', key: 'name', width: 200 }, { title: '描述', key: 'description' },
            { title: '类型', key: 'type', width: 100 }, { title: '父级用户组', key: 'parent', width: 150 },
            { title: '成员数', key: 'members', width: 100, align: 'center' }, { title: '管理员数', key: 'admins', width: 100, align: 'center' },
            { title: '子组数', key: 'children', width: 100, align: 'center' }, { title: '创建时间', key: 'createdAt', width: 180 },
            { title: '操作', key: 'actions', width: 280 }
          ]" :data-source="groups" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1320 }">
            <template #bodyCell="{ column, record: row }">
              <template v-if="column.key === 'name'"><span class="font-medium">{{ row.name }}</span></template>
              <template v-else-if="column.key === 'description'"><span class="text-muted-foreground">{{ row.description || '-' }}</span></template>
              <template v-else-if="column.key === 'type'">
                  <Tag :color="getTypeVariant(row.type)" class="text-xs">
                    {{ getTypeText(row.type) }}
                  </Tag>
              </template>
              <template v-else-if="column.key === 'parent'"><span class="text-muted-foreground">{{ row.parent?.name || '-' }}</span></template>
              <template v-else-if="column.key === 'members'">{{ row._count?.members || 0 }}</template>
              <template v-else-if="column.key === 'admins'">{{ row._count?.admins || 0 }}</template>
              <template v-else-if="column.key === 'children'">{{ row._count?.children || 0 }}</template>
              <template v-else-if="column.key === 'createdAt'"><span class="text-muted-foreground">{{ new Date(row.createdAt).toLocaleString() }}</span></template>
              <template v-else-if="column.key === 'actions'">
                  <div class="flex gap-2">
                    <Button size="small"  @click="handleEdit(row)">
                      <Pencil class="w-3 h-3 mr-1" />
                      编辑
                    </Button>
                    <Button size="small"  @click="handleManageMembers(row)">
                      <Users class="w-3 h-3 mr-1" />
                      成员
                    </Button>
                    <Button size="small"  @click="handleManageAdmins(row)">
                      <UserCog class="w-3 h-3 mr-1" />
                      管理员
                    </Button>
                    <Button size="small" color="error" @click="handleDelete(row)">
                      <Trash2 class="w-3 h-3 mr-1" />
                      删除
                    </Button>
                  </div>
              </template>
            </template>
          </Table>

          <div v-if="total > queryForm.pageSize!" class="flex items-center justify-between mt-4">
            <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
            <div class="flex items-center gap-1">
              <Button

                size="sm"
                :disabled="queryForm.page! <= 1"
                @click="handlePageChange(queryForm.page! - 1)"
              >
                上一页
              </Button>
              <span class="text-sm px-2">{{ queryForm.page! }} / {{ Math.ceil(total / queryForm.pageSize!) }}</span>
              <Button

                size="sm"
                :disabled="queryForm.page! >= Math.ceil(total / queryForm.pageSize!)"
                @click="handlePageChange(queryForm.page! + 1)"
              >
                下一页
              </Button>
            </div>
          </div>
        </div>

        <div v-else>
          <Tree
            :data="treeData"
            :props="{ children: 'children', label: 'name' }"
            node-key="id"
            :default-expand-all="true"
          >
            <template #default="{ data }">
              <div class="flex items-center justify-between w-full pr-3">
                <div class="flex items-center gap-2">
                  <span class="font-medium">{{ data.name }}</span>
                  <Tag :color="getTypeVariant(data.type)" class="text-xs">
                    {{ getTypeText(data.type) }}
                  </Tag>
                  <span class="text-xs text-muted-foreground ml-2">
                    成员: {{ data.memberCount }} | 管理员: {{ data.adminCount }}
                  </span>
                </div>
                <div class="flex gap-2">
                  <Button type="link" size="small" class="p-0 h-auto" @click.stop="handleEdit({ id: data.id } as UserGroup)">
                    编辑
                  </Button>
                  <Button type="link" size="small" class="p-0 h-auto" @click.stop="handleManageMembers({ id: data.id } as UserGroup)">
                    成员
                  </Button>
                  <Button type="link" size="small" class="p-0 h-auto text-destructive" @click.stop="handleDelete({ id: data.id } as UserGroup)">
                    删除
                  </Button>
                </div>
              </div>
            </template>
          </Tree>
        </div>
      </div>
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-lg">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <form @submit.prevent="handleSubmit">
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">用户组名称 <span class="text-destructive">*</span></label>
              <Input v-model:value="groupForm.name" placeholder="请输入用户组名称" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">描述</label>
              <InputTextArea v-model:value="groupForm.description" :rows="3" placeholder="请输入描述" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">类型 <span class="text-destructive">*</span></label>
              <Select v-model:value="groupForm.type">
                <div>

                </div>

                  <SelectOption value="team">团队</SelectOption>
                  <SelectOption value="department">部门</SelectOption>
                  <SelectOption value="project">项目</SelectOption>
                  <SelectOption value="organization">组织</SelectOption>

              </Select>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">父级用户组</label>
              <Select v-model:value="groupForm.parentId">
                <div>

                </div>

                  <SelectOption
                    v-for="option in parentOptions"
                    :key="option.value"
                    :value="option.value"
                    :disabled="option.disabled"
                  >
                    {{ option.label }}
                  </SelectOption>

              </Select>
            </div>
          </div>
        </form>
        <div>
          <Button  @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="memberDialogVisible" :footer="null">
      <div class="max-w-3xl">
        <div>
          <h3>成员管理</h3>
        </div>
        <div class="min-h-[400px]">
          <div class="grid grid-cols-2 gap-5">
            <Card>
              <div class="border-b py-3">
                <span class="font-medium">当前成员 ({{ memberForm.currentMembers.length }})</span>
              </div>
              <div class="pt-4">
                <div class="max-h-[350px] overflow-y-auto">
                  <Table :columns="[{ title: '姓名', dataIndex: 'name' }, { title: '用户名', dataIndex: 'username' }, { title: '邮箱', dataIndex: 'email' }, { title: '操作', key: 'actions', width: 80 }]" :data-source="memberForm.currentMembers" :pagination="false" row-key="id" size="small"><template #bodyCell="{ column, record: member }"><Button v-if="column.key === 'actions'" type="link" size="small" class="p-0 h-auto text-destructive" @click="handleRemoveMember(member.id)">移除</Button></template></Table>
                </div>
              </div>
            </Card>
            <Card>
              <div class="border-b py-3">
                <span class="font-medium">添加成员</span>
              </div>
              <div class="pt-4">
                <Select v-model:value="memberForm.selectedUsers" multiple>
                  <div class="mb-3">

                  </div>

                    <SelectOption
                      v-for="user in memberForm.availableUsers.filter(
                        u => !memberForm.currentMembers.find(m => m.id === u.id)
                      )"
                      :key="user.id"
                      :value="user.id"
                    >
                      {{ user.name }} ({{ user.username }})
                    </SelectOption>

                </Select>
                <Button class="w-full" @click="handleAddMembers">
                  添加选中成员
                </Button>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="adminDialogVisible" :footer="null">
      <div class="max-w-3xl">
        <div>
          <h3>管理员管理</h3>
        </div>
        <div class="min-h-[400px]">
          <div class="grid grid-cols-2 gap-5">
            <Card>
              <div class="border-b py-3">
                <span class="font-medium">当前管理员 ({{ memberForm.currentAdmins.length }})</span>
              </div>
              <div class="pt-4">
                <div class="max-h-[350px] overflow-y-auto">
                  <Table :columns="[{ title: '姓名', dataIndex: 'name' }, { title: '用户名', dataIndex: 'username' }, { title: '邮箱', dataIndex: 'email' }, { title: '操作', key: 'actions', width: 80 }]" :data-source="memberForm.currentAdmins" :pagination="false" row-key="id" size="small"><template #bodyCell="{ column, record: admin }"><Button v-if="column.key === 'actions'" type="link" size="small" class="p-0 h-auto text-destructive" @click="handleRemoveAdmin(admin.id)">移除</Button></template></Table>
                </div>
              </div>
            </Card>
            <Card>
              <div class="border-b py-3">
                <span class="font-medium">添加管理员</span>
              </div>
              <div class="pt-4">
                <Select v-model:value="memberForm.selectedUsers" multiple>
                  <div class="mb-3">

                  </div>

                    <SelectOption
                      v-for="user in memberForm.availableUsers.filter(
                        u => !memberForm.currentAdmins.find(a => a.id === u.id)
                      )"
                      :key="user.id"
                      :value="user.id"
                    >
                      {{ user.name }} ({{ user.username }})
                    </SelectOption>

                </Select>
                <Button class="w-full" @click="handleAddAdmins">
                  添加选中管理员
                </Button>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
@media (max-width: 1200px) {
  .grid-cols-6 {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .grid-cols-6 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
