<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@lucide/vue'
import { userGroupApi } from '@/api/user-group'
import type { UserGroup, GroupQueryDto, GroupStats } from '@/types/user-group'
import GroupEditDialog from './components/GroupEditDialog.vue'
import { getApiErrorMessage, useGroupDict } from './utils/hooks'
import type { GroupEditDialogExpose } from './utils/types'

const router = useRouter()

const loading = ref(false)
const groups = ref<UserGroup[]>([])
const total = ref(0)
const stats = ref<GroupStats | null>(null)
const groupEditDialogRef = ref<GroupEditDialogExpose>()

const [modal, contextHolder] = Modal.useModal()

const { groupTypeOptions, getTypeText, getTypeVariant, formatDate } = useGroupDict()

const queryForm = reactive<GroupQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined
})

const columns: TableProps<UserGroup>['columns'] = [
  { title: '用户组名称', key: 'name', width: 200 },
  { title: '描述', key: 'description' },
  { title: '类型', key: 'type', width: 100 },
  { title: '成员数', key: 'members', width: 100, align: 'center' },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 220 }
]

const loadGroups = async () => {
  loading.value = true
  try {
    const res = await userGroupApi.getList(queryForm)
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
    stats.value = await userGroupApi.getStats()
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载用户组统计数据失败')
  }
}

const refresh = () => {
  loadGroups()
  loadStats()
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
  groupEditDialogRef.value?.open()
}

const handleEdit = (row: UserGroup) => {
  groupEditDialogRef.value?.open(row)
}

const handleDelete = async (row: UserGroup) => {
  const confirmed = await modal.confirm({
    title: '删除用户组',
    content: '确定要删除该用户组吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed) {
    return
  }

  try {
    await userGroupApi.delete(row.id)
    message.success('删除成功')
    refresh()
  } catch (error: unknown) {
    console.error('删除用户组失败:', error)
    message.error(getApiErrorMessage(error, '删除用户组失败'))
  }
}

const openDetail = (id: string) => {
  router.push({ name: 'UserGroupDetail', params: { id } })
}

const handlePageChange = (page: number, pageSize?: number) => {
  queryForm.page = page
  if (pageSize !== undefined) {
    queryForm.pageSize = pageSize
  }
  loadGroups()
}

onMounted(refresh)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">用户组管理</h1>
        <p class="text-sm text-[#52677D]">管理用户组，包括创建、编辑、删除和成员管理</p>
      </div>
      <div class="flex items-center gap-3">
        <a-button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增用户组
        </a-button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-5 mb-6">
      <a-card
        v-for="stat in [
          { label: '用户组总数', value: stats?.totalGroups || 0 },
          { label: '团队', value: stats?.teamGroups || 0 },
          { label: '部门', value: stats?.departmentGroups || 0 },
          { label: '项目', value: stats?.projectGroups || 0 },
          { label: '组织', value: stats?.organizationGroups || 0 }
        ]"
        :key="stat.label"
      >
        <div class="pt-4">
          <div class="text-center">
            <div class="text-2xl font-bold text-[#102A43] mb-1">{{ stat.value }}</div>
            <div class="text-sm text-[#52677D]">{{ stat.label }}</div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div class="flex flex-wrap items-end gap-4">
        <a-form-item name="name" label="用户组名称">
          <a-input v-model:value="queryForm.name" placeholder="请输入用户组名称" class="w-48" />
        </a-form-item>
        <a-form-item name="type" label="类型">
          <a-select v-model:value="queryForm.type" placeholder="请选择类型" class="w-40" allow-clear :options="groupTypeOptions" />
        </a-form-item>
        <a-form-item>
          <div class="flex gap-2">
            <a-button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </a-form-item>
      </div>

      <a-table :columns="columns" :data-source="groups" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 910 }">
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.key === 'name'">
            <a-button type="link" class="h-auto p-0! font-medium" @click="openDetail(row.id)">{{ row.name }}</a-button>
          </template>
          <template v-else-if="column.key === 'description'">
            <span class="text-[#52677D]">{{ row.description || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'type'">
            <a-tag :color="getTypeVariant(row.type)" class="text-xs">
              {{ getTypeText(row.type) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'members'">{{ row._count?.members || 0 }}</template>
          <template v-else-if="column.key === 'createdAt'">
            <span class="text-[#52677D]">{{ formatDate(row.createdAt) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex gap-1">
              <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</a-button>
              <a-button type="link" size="small" class="h-auto p-0" @click="openDetail(row.id)">详情</a-button>
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
          :show-size-changer="false"
          size="small"
          @change="handlePageChange"
        />
      </div>
    </a-card>

    <GroupEditDialog ref="groupEditDialogRef" @success="refresh" />
    <contextHolder />
  </div>
</template>
