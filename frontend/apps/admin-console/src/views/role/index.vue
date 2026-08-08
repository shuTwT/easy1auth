<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Form, FormItem, Modal, Pagination as AntPagination, Table, message } from 'antdv-next'
import {
  Search,
  Plus,
  List,
  Share2,
  RefreshCw,
  Lock,
  Users,
  Settings2,
  User,
} from '@lucide/vue'
import { roleApi } from '@/api/role'
import { userApi } from '@/api/user'
import { permissionApi } from '@/api/permission'
import type { PermissionTree } from '@/types/permission'
import type { Role, RoleTree, RoleStats, CreateRoleDto, RoleUser } from '@/types/role'
const loading = ref(false)
const roles = ref<Role[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const [modal, contextHolder] = Modal.useModal()
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
const roleForm = reactive<CreateRoleDto & { id?: string }>({
  name: '',
  code: '',
  description: '',
  type: 'custom',
  permissions: {},
  dataScope: 'self',
  parentId: undefined,
})

const roleFormRules = {
  name: [{ required: true, message: '请输入角色名称' }],
  code: [{ required: true, message: '请输入角色编码' }],
  type: [{ required: true, message: '请选择角色类型' }],
  dataScope: [{ required: true, message: '请选择数据范围' }],
}

const statsData = computed(() => [
  { label: '总角色数', value: stats.value.totalRoles, icon: Settings2, class: 'primary' },
  { label: '内置角色', value: stats.value.systemRoles, icon: Lock, class: 'danger' },
  { label: '自定义角色', value: stats.value.customRoles, icon: User, class: 'success' },
  { label: '已分配用户', value: stats.value.totalUsers, icon: Users, class: 'info' },
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
const allUsers = ref<{ id: string; name: string; disabled?: boolean }[]>([])
const assigning = ref(false)

const permissionTree = ref<PermissionTree[]>([])
const checkedPermissionCodes = ref<string[]>([])

const handlePermCheck = (_checkedNode: PermissionTree, checkedInfo: { checkedKeys: string[] }) => {
  checkedPermissionCodes.value = checkedInfo.checkedKeys
}

const loadPermissionTree = async () => {
  try {
    const res = await permissionApi.getTree()
    permissionTree.value = res
  } catch (error) {
    console.error('加载权限树失败:', error)
    message.error('加载权限树失败')
  }
}

function flattenPermissionTree(nodes: PermissionTree[]): PermissionTree[] {
  return nodes.reduce((acc: PermissionTree[], node: PermissionTree) => {
    acc.push(node)
    if (node.children?.length) acc.push(...flattenPermissionTree(node.children))
    return acc
  }, [])
}

function getPermissionCodesFromJson(permissions: Record<string, boolean>): string[] {
  const allCodes = flattenPermissionTree(permissionTree.value).map((p) => p.code)
  return allCodes.filter((code) => permissions[code] === true)
}

function buildPermissionsFromCodes(codes: string[]): Record<string, boolean> {
  const result: Record<string, boolean> = {}
  codes.forEach((code) => { result[code] = true })
  return result
}

const loadStats = async () => {
  try {
    const data = await roleApi.getStats()
    stats.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
    message.error('加载角色统计信息失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadRoles()
}

const handleReset = () => {
  searchQuery.value = ''
  filterType.value = ''
  page.value = 1
  loadRoles()
}

const loadRoles = async () => {
  loading.value = true
  try {
    const data = await roleApi.getList({
      search: searchQuery.value,
      type: filterType.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    roles.value = data.items
    total.value = data.total
  } catch (error) {
    message.error('加载角色列表失败')
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
    message.error('加载角色树失败')
  }
}

const handleCreate = () => {
  isEdit.value = false
  checkedPermissionCodes.value = []
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
  checkedPermissionCodes.value = getPermissionCodesFromJson(row.permissions || {})
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
  submitting.value = true
  try {
    const permissionsPayload = buildPermissionsFromCodes(checkedPermissionCodes.value)
    if (isEdit.value) {
      await roleApi.update(roleForm.id!, {
        name: roleForm.name,
        description: roleForm.description,
        permissions: permissionsPayload,
        dataScope: roleForm.dataScope,
        parentId: roleForm.parentId,
      })
      message.success('更新成功')
    } else {
      await roleApi.create({
        ...roleForm,
        name: roleForm.name.trim(),
        code: roleForm.code.trim(),
        permissions: permissionsPayload,
      })
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadRoles()
    loadRoleTree()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: Role) => {
  const confirmed = await modal.confirm({
    title: '删除角色',
    content: '确定要删除该角色吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) {
    return
  }

  try {
    await roleApi.delete(row.id)
    message.success('删除成功')
    loadRoles()
    loadRoleTree()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除失败')
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
    message.error('加载角色用户失败')
  } finally {
    usersLoading.value = false
  }
}

const handleAssignUsers = async () => {
  try {
    const response = await userApi.getList()
    allUsers.value = response.items.map((user: any) => ({
      id: user.id,
      name: `${user.name} (${user.username})`,
      disabled: roleUsers.value.some((ru) => ru.id === user.id),
    }))
    selectedUserIds.value = []
    assignUsersDialogVisible.value = true
  } catch (error) {
    message.error('加载用户列表失败')
  }
}

const handleSubmitAssignUsers = async () => {
  if (selectedUserIds.value.length === 0) {
    message.warning('请选择要分配的用户')
    return
  }

  if (!currentRole.value) return

  assigning.value = true
  try {
    await roleApi.assignUsers(currentRole.value.id, {
      userIds: selectedUserIds.value,
    })
    message.success('分配用户成功')
    assignUsersDialogVisible.value = false
    loadRoleUsers()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '分配用户失败')
  } finally {
    assigning.value = false
  }
}

const handleRemoveUser = async (user: RoleUser) => {
  const confirmed = await modal.confirm({
    title: '移除角色成员',
    content: `确定要从该角色移除用户”${user.name}”吗？`,
    okText: '移除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) {
    return
  }

  if (!currentRole.value) return

  try {
    await roleApi.removeUsers(currentRole.value.id, {
      userIds: [user.id],
    })
    message.success('移除成功')
    loadRoleUsers()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '移除失败')
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

function getTypeVariant(type: string): string {
  return type === 'system' ? 'purple' : 'blue'
}

const handlePageChange = (newPage: number, newPageSize?: number) => {
  page.value = newPage
  if (newPageSize !== undefined) pageSize.value = newPageSize
  loadRoles()
}

onMounted(() => {
  loadStats()
  loadRoles()
  loadRoleTree()
  loadPermissionTree()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">角色管理</h1>
        <p class="text-sm text-muted-foreground">管理用户池角色，包括创建、编辑、删除和分配用户池用户</p>
      </div>
      <div>
        <Button @click="handleCreate">
          <Plus class="size-4 mr-2" />
          创建角色
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <Card v-for="(stat, index) in statsData" :key="index" class="transition-all hover:shadow-md">
        <div class="pt-4">
          <div class="flex items-center gap-3">
            <div
              class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
              :class="{
                'bg-gradient-to-br from-primary to-primary/60': stat.class === 'primary',
                'bg-gradient-to-br from-red-500 to-red-400': stat.class === 'danger',
                'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.class === 'success',
                'bg-gradient-to-br from-sky-500 to-sky-400': stat.class === 'info',
              }"
            >
              <component :is="stat.icon" class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-foreground leading-tight">{{ stat.value }}</div>
              <div class="text-xs text-muted-foreground mt-0.5">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <Card>
      <div class="border-b">
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-3">
            <div class="relative">
              <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
              <Input v-model:value="searchQuery"
                placeholder="搜索角色名称、编码"
                class="w-72 pl-8"
                @keyup.enter="loadRoles"
              />
            </div>
            <Select v-model:value="filterType" class="w-36" allow-clear @update:value="loadRoles">
                <SelectOption value="system">内置角色</SelectOption>
                <SelectOption value="custom">自定义角色</SelectOption>

            </Select>
            <Button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </Button>
            <Button  @click="handleReset">
              <RefreshCw class="size-4 mr-2" />
              重置
            </Button>
          </div>
          <div class="flex gap-0">
            <Button
              :type="viewMode === 'list' ? 'primary' : 'default'"
              class="rounded-r-none"
              @click="viewMode = 'list'"
            >
              <List class="size-4 mr-2" />
              列表
            </Button>
            <Button
              :type="viewMode === 'tree' ? 'primary' : 'default'"
              class="rounded-l-none"
              @click="viewMode = 'tree'"
            >
              <Share2 class="size-4 mr-2" />
              树形
            </Button>
          </div>
        </div>
      </div>

      <div class="pt-4">
        <div v-if="viewMode === 'list'">
          <Table :columns="[
            { title: '角色名称', key: 'name', width: 200 }, { title: '角色编码', key: 'code', width: 180 },
            { title: '描述', key: 'description', width: 200 }, { title: '数据范围', key: 'dataScope', width: 140 },
            { title: '用户数', key: 'userCount', width: 100, align: 'center' }, { title: '创建时间', key: 'createdAt', width: 180 },
            { title: '操作', key: 'actions', width: 180 }
          ]" :data-source="roles" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1180 }">
            <template #bodyCell="{ column, record: row }">
              <template v-if="column.key === 'name'">
                  <div class="flex items-center gap-2">
                    <Tag :color="getTypeVariant(row.type)" class="text-xs">
                      {{ row.type === 'system' ? '系统' : '自定义' }}
                    </Tag>
                    <span class="font-medium text-foreground">{{ row.name }}</span>
                  </div>
              </template>
              <template v-else-if="column.key === 'code'">
                  <code class="bg-muted px-2 py-0.5 rounded text-xs text-muted-foreground font-mono">
                    {{ row.code }}
                  </code>
              </template>
              <template v-else-if="column.key === 'description'"><span class="text-muted-foreground">{{ row.description || '-' }}</span></template>
              <template v-else-if="column.key === 'dataScope'">
                  <Tag  class="text-xs">
                    {{ getDataScopeLabel(row.dataScope) }}
                  </Tag>
              </template>
              <template v-else-if="column.key === 'userCount'">
                  <Button type="link" size="small" class="p-0 h-auto font-semibold" @click="handleViewUsers(row)">
                    {{ row.userCount || 0 }}
                  </Button>
              </template>
              <template v-else-if="column.key === 'createdAt'"><span class="text-muted-foreground">{{ formatDate(row.createdAt) }}</span></template>
              <template v-else-if="column.key === 'actions'">
                  <div class="flex gap-2">
                    <Button type="link" size="small" class="p-0 h-auto" @click="handleViewUsers(row)">
                      查看用户
                    </Button>
                    <Button
                      type="link"
                      size="small"
                      class="p-0 h-auto"
                      :disabled="row.type === 'system'"
                      @click="handleEdit(row)"
                    >
                      编辑
                    </Button>
                    <Button
                      type="link"
                      size="small"
                      class="p-0 h-auto text-destructive"
                      :disabled="row.type === 'system'"
                      @click="handleDelete(row)"
                    >
                      删除
                    </Button>
                  </div>
              </template>
            </template>
          </Table>

          <div class="flex items-center justify-between mt-4 pt-4 border-t">
            <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
            <AntPagination
              :current="page"
              :page-size="pageSize"
              :total="total"
              :show-size-changer="false"
              size="small"
              @change="handlePageChange"
            />
          </div>
        </div>

        <div v-else>
          <Tree
            :tree-data="roleTree"
            :field-names="{ key: 'id', title: 'name', children: 'children' }"
            :default-expand-all="true"
            :selectable="false"
          >
            <template #titleRender="data">
              <div class="flex items-center justify-between w-full pr-5">
                <div class="flex items-center gap-2">
                  <Tag :color="getTypeVariant(data.type)" class="text-xs">
                    {{ data.type === 'system' ? '系统' : '自定义' }}
                  </Tag>
                  <span>{{ data.name }}</span>
                  <span class="text-xs text-muted-foreground ml-1">({{ data.code }})</span>
                </div>
                <Button type="link" size="small" class="p-0 h-auto" @click.stop="handleViewUsers(data)">
                  {{ data.userCount || 0 }} 用户
                </Button>
              </div>
            </template>
          </Tree>
        </div>
      </div>
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-xl">
        <div>
          <h3>{{ dialogTitle }}</h3>
        </div>
        <Form :model="roleForm" :rules="roleFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <FormItem label="角色名称" name="name">
              <Input v-model:value="roleForm.name" placeholder="请输入角色名称" />
            </FormItem>
            <FormItem label="角色编码" name="code">
              <Input v-model:value="roleForm.code" placeholder="请输入角色编码" :disabled="isEdit" />
            </FormItem>
            <FormItem label="角色类型" name="type">
              <Select
                v-model:value="roleForm.type"
                :disabled="isEdit"
                :options="[
                  { value: 'custom', label: '自定义角色' },
                  { value: 'system', label: '内置角色' },
                ]"
              />
            </FormItem>
            <FormItem label="数据范围" name="dataScope">
              <Select
                v-model:value="roleForm.dataScope"
                :options="[
                  { value: 'self', label: '仅本人数据' },
                  { value: 'department', label: '本部门数据' },
                  { value: 'department_and_sub', label: '本部门及下级部门数据' },
                  { value: 'all', label: '全部数据' },
                ]"
              />
            </FormItem>
            <FormItem label="父级角色" name="parentId">
              <Select
                v-model:value="roleForm.parentId"
                allow-clear
                :options="availableParentRoles.map(role => ({ value: role.id, label: role.name }))"
                placeholder="请选择父级角色"
              />
            </FormItem>
            <FormItem label="权限配置">
              <div class="max-h-[360px] overflow-y-auto border rounded-lg p-2">
                <Tree
                  :tree-data="permissionTree"
                  :field-names="{ key: 'code', title: 'name', children: 'children' }"
                  :default-expand-all="true"
                  :selectable="false"
                >
                  <template #titleRender="data">
                    <div class="flex items-center gap-2">
                      <input
                        type="checkbox"
                        :checked="checkedPermissionCodes.includes(data.code)"
                        @change="handlePermCheck(data, { checkedKeys: checkedPermissionCodes.includes(data.code) ? checkedPermissionCodes.filter(c => c !== data.code) : [...checkedPermissionCodes, data.code] })"
                        class="rounded border-input"
                      />
                      <span>{{ data.name }}</span>
                    </div>
                  </template>
                </Tree>
              </div>
            </FormItem>
            <FormItem label="描述" name="description">
              <InputTextArea v-model:value="roleForm.description" :rows="3" placeholder="请输入描述" />
            </FormItem>
          </div>
          <div class="flex justify-end gap-2 pt-2">
            <Button html-type="button" @click="dialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit" :loading="submitting">
              {{ submitting ? '提交中...' : '确定' }}
            </Button>
          </div>
        </Form>
      </div>
    </Modal>

    <Modal v-model:open="usersDialogVisible" :footer="null">
      <div class="max-w-3xl">
        <div>
          <h3>角色用户</h3>
        </div>
        <div class="min-h-[400px]">
          <div class="flex justify-between mb-5">
            <div class="relative">
              <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
              <Input v-model:value="userSearchQuery"
                placeholder="搜索用户名、邮箱、姓名"
                class="w-72 pl-8"
                @keyup.enter="loadRoleUsers"
              />
            </div>
            <Button @click="handleAssignUsers">
              <Plus class="size-4 mr-2" />
              分配用户
            </Button>
          </div>
          <Table :columns="[
            { title: '用户名', key: 'username', width: 150 }, { title: '姓名', dataIndex: 'name', width: 120 },
            { title: '邮箱', dataIndex: 'email', width: 200 }, { title: '部门', dataIndex: 'department', width: 150 },
            { title: '岗位', dataIndex: 'position', width: 150 }, { title: '状态', key: 'status', width: 100 },
            { title: '操作', key: 'actions', width: 100 }
          ]" :data-source="roleUsers" :loading="usersLoading" row-key="id" :pagination="false" :scroll="{ x: 970 }">
            <template #bodyCell="{ column, record: row }">
              <template v-if="column.key === 'username'">
                  <div class="flex items-center gap-2">
                    <Avatar class="size-7">
                      <span class="bg-gradient-to-br from-primary to-primary/60 text-white text-xs font-semibold">
                        {{ row.name?.charAt(0) || row.username.charAt(0).toUpperCase() }}
                      </span>
                    </Avatar>
                    <span>{{ row.username }}</span>
                  </div>
              </template>
              <template v-else-if="column.key === 'status'">
                  <Tag :color="row.status === 'active' ? 'green' : 'red'" class="text-xs">
                    {{ row.status === 'active' ? '正常' : '禁用' }}
                  </Tag>
              </template>
              <template v-else-if="column.key === 'actions'">
                  <Button type="link" size="small" class="p-0 h-auto text-destructive" @click="handleRemoveUser(row)">
                    移除
                  </Button>
              </template>
            </template>
          </Table>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="assignUsersDialogVisible" :footer="null">
      <div class="max-w-3xl">
        <div>
          <h3>分配用户</h3>
        </div>
        <div class="min-h-[400px]">
          <div class="flex gap-4">
            <div class="flex-1">
              <h3 class="font-medium mb-3">可选用户</h3>
              <div class="border rounded-lg p-3 max-h-[350px] overflow-y-auto">
                <div v-for="user in allUsers.filter(u => !selectedUserIds.includes(u.id))" :key="user.id" class="py-1">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      :disabled="user.disabled"
                      @change="selectedUserIds.push(user.id)"
                      class="rounded border-input"
                    />
                    <span :class="{ 'text-muted-foreground': user.disabled }">{{ user.name }}</span>
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
                      @change="selectedUserIds = selectedUserIds.filter(id => id !== userId)"
                      class="rounded border-input"
                    />
                    <span>{{ allUsers.find(u => u.id === userId)?.name }}</span>
                  </label>
                </div>
                <div v-if="selectedUserIds.length === 0" class="text-muted-foreground text-center py-4">
                  暂无选择
                </div>
              </div>
            </div>
          </div>
        </div>
        <div>
          <Button  @click="assignUsersDialogVisible = false">取消</Button>
          <Button :disabled="assigning" @click="handleSubmitAssignUsers">
            {{ assigning ? '提交中...' : '确定' }}
          </Button>
        </div>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>
