<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
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
import type { Role, RoleTree, RoleStats } from '@/types/role'
import RoleEditDialog from './components/RoleEditDialog.vue'
import RoleUsersDialog from './components/RoleUsersDialog.vue'
import { getApiErrorMessage, useRoleDict } from './utils/hooks'
import type { RoleEditDialogExpose, RoleUsersDialogExpose } from './utils/types'

const loading = ref(false)
const roles = ref<Role[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
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

const roleEditDialogRef = ref<RoleEditDialogExpose>()
const roleUsersDialogRef = ref<RoleUsersDialogExpose>()

const [modal, contextHolder] = Modal.useModal()

const {
  roleTypeOptions,
  getDataScopeLabel,
  getTypeText,
  getTypeVariant,
  formatDate
} = useRoleDict()

const columns: TableProps<Role>['columns'] = [
  { title: '角色名称', key: 'name', width: 200 },
  { title: '角色编码', key: 'code', width: 180 },
  { title: '描述', key: 'description', width: 200 },
  { title: '数据范围', key: 'dataScope', width: 140 },
  { title: '用户数', key: 'userCount', width: 100, align: 'center' },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 180 }
]

const statsData = computed(() => [
  { label: '总角色数', value: stats.value.totalRoles, icon: Settings2, class: 'primary' },
  { label: '内置角色', value: stats.value.systemRoles, icon: Lock, class: 'danger' },
  { label: '自定义角色', value: stats.value.customRoles, icon: User, class: 'success' },
  { label: '已分配用户', value: stats.value.totalUsers, icon: Users, class: 'info' },
])

const loadStats = async () => {
  try {
    stats.value = await roleApi.getStats()
  } catch (error) {
    console.error('加载统计信息失败:', error)
    message.error('加载角色统计信息失败')
  }
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
    console.error('加载角色列表失败:', error)
    message.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

const loadRoleTree = async () => {
  try {
    roleTree.value = await roleApi.getTree()
  } catch (error) {
    console.error('加载角色树失败:', error)
    message.error('加载角色树失败')
  }
}

const refreshAll = () => {
  loadRoles()
  loadRoleTree()
  loadStats()
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

const handlePageChange = (newPage: number, newPageSize?: number) => {
  page.value = newPage
  if (newPageSize !== undefined) {
    pageSize.value = newPageSize
  }
  loadRoles()
}

const handleCreate = () => {
  roleEditDialogRef.value?.open()
}

const handleEdit = (row: Role) => {
  roleEditDialogRef.value?.open(row)
}

const handleViewUsers = (row: Role | RoleTree) => {
  roleUsersDialogRef.value?.open(row)
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
    refreshAll()
  } catch (error: unknown) {
    console.error('删除角色失败:', error)
    message.error(getApiErrorMessage(error, '删除失败'))
  }
}

onMounted(() => {
  loadStats()
  loadRoles()
  loadRoleTree()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">角色管理</h1>
        <p class="text-sm text-[#52677D]">管理用户池角色，包括创建、编辑、删除和分配用户池用户</p>
      </div>
      <div>
        <a-button @click="handleCreate">
          <Plus class="size-4 mr-2" />
          创建角色
        </a-button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <a-card v-for="(stat, index) in statsData" :key="index" class="transition-all hover:shadow-md">
        <div class="pt-4">
          <div class="flex items-center gap-3">
            <div
              class="size-11 rounded-lg flex items-center justify-center text-white shrink-0"
              :class="{
                'bg-gradient-to-br from-[#0369A1] to-[#0369A1]/60': stat.class === 'primary',
                'bg-gradient-to-br from-red-500 to-red-400': stat.class === 'danger',
                'bg-gradient-to-br from-emerald-500 to-emerald-400': stat.class === 'success',
                'bg-gradient-to-br from-sky-500 to-sky-400': stat.class === 'info',
              }"
            >
              <component :is="stat.icon" class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-[#102A43] leading-tight">{{ stat.value }}</div>
              <div class="text-xs text-[#52677D] mt-0.5">{{ stat.label }}</div>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div class="flex justify-between items-center">
        <div class="flex items-center gap-3">
          <div class="relative">
            <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 size-4 text-[#52677D]" />
            <a-input
              v-model:value="searchQuery"
              placeholder="搜索角色名称、编码"
              class="w-72 pl-8"
              @keyup.enter="loadRoles"
            />
          </div>
          <a-select v-model:value="filterType" class="w-36" allow-clear :options="roleTypeOptions" @update:value="loadRoles" />
          <a-button @click="handleSearch">
            <Search class="size-4 mr-2" />
            搜索
          </a-button>
          <a-button @click="handleReset">
            <RefreshCw class="size-4 mr-2" />
            重置
          </a-button>
        </div>
        <div class="flex gap-0">
          <a-button
            :type="viewMode === 'list' ? 'primary' : 'default'"
            class="rounded-r-none"
            @click="viewMode = 'list'"
          >
            <List class="size-4 mr-2" />
            列表
          </a-button>
          <a-button
            :type="viewMode === 'tree' ? 'primary' : 'default'"
            class="rounded-l-none"
            @click="viewMode = 'tree'"
          >
            <Share2 class="size-4 mr-2" />
            树形
          </a-button>
        </div>
      </div>

      <div class="pt-4">
        <div v-if="viewMode === 'list'">
          <a-table :columns="columns" :data-source="roles" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1180 }">
            <template #bodyCell="{ column, record: row }">
              <template v-if="column.key === 'name'">
                <div class="flex items-center gap-2">
                  <a-tag :color="getTypeVariant(row.type)" class="text-xs">
                    {{ getTypeText(row.type) }}
                  </a-tag>
                  <span class="font-medium text-[#102A43]">{{ row.name }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'code'">
                <code class="bg-[#F4F8FB] px-2 py-0.5 rounded text-xs text-[#52677D] font-mono">
                  {{ row.code }}
                </code>
              </template>
              <template v-else-if="column.key === 'description'">
                <span class="text-[#52677D]">{{ row.description || '-' }}</span>
              </template>
              <template v-else-if="column.key === 'dataScope'">
                <a-tag class="text-xs">
                  {{ getDataScopeLabel(row.dataScope) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'userCount'">
                <a-button type="link" size="small" class="p-0 h-auto font-semibold" @click="handleViewUsers(row)">
                  {{ row.userCount || 0 }}
                </a-button>
              </template>
              <template v-else-if="column.key === 'createdAt'">
                <span class="text-[#52677D]">{{ formatDate(row.createdAt) }}</span>
              </template>
              <template v-else-if="column.key === 'actions'">
                <div class="flex gap-2">
                  <a-button type="link" size="small" class="p-0 h-auto" @click="handleViewUsers(row)">
                    查看用户
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="p-0 h-auto"
                    :disabled="row.type === 'system'"
                    @click="handleEdit(row)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="p-0 h-auto text-[#B91C1C]"
                    :disabled="row.type === 'system'"
                    @click="handleDelete(row)"
                  >
                    删除
                  </a-button>
                </div>
              </template>
            </template>
          </a-table>

          <div class="flex items-center justify-between mt-4 pt-4 border-t">
            <span class="text-sm text-[#52677D]">共 {{ total }} 条</span>
            <a-pagination
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
          <a-tree
            :tree-data="roleTree"
            :field-names="{ key: 'id', title: 'name', children: 'children' }"
            :default-expand-all="true"
            :selectable="false"
          >
            <template #titleRender="data">
              <div class="flex items-center justify-between w-full pr-5">
                <div class="flex items-center gap-2">
                  <a-tag :color="getTypeVariant(data.type)" class="text-xs">
                    {{ getTypeText(data.type) }}
                  </a-tag>
                  <span>{{ data.name }}</span>
                  <span class="text-xs text-[#52677D] ml-1">({{ data.code }})</span>
                </div>
                <a-button type="link" size="small" class="p-0 h-auto" @click.stop="handleViewUsers(data as unknown as RoleTree)">
                  {{ data.userCount || 0 }} 用户
                </a-button>
              </div>
            </template>
          </a-tree>
        </div>
      </div>
    </a-card>

    <RoleEditDialog ref="roleEditDialogRef" :roles="roles" @success="refreshAll" />
    <RoleUsersDialog ref="roleUsersDialogRef" @change="refreshAll" />

    <contextHolder />
  </div>
</template>
