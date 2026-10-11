<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Plus, Search, RefreshCw } from '@lucide/vue'
import { positionApi } from '@/api/position'
import type { Position, PositionQueryDto, PositionStats } from '@/types/position'
import PositionEditDialog from './components/PositionEditDialog.vue'
import { usePositionDict } from './utils/hooks'
import type { PositionEditDialogExpose } from './utils/types'

const loading = ref(false)
const positions = ref<Position[]>([])
const total = ref(0)
const stats = ref<PositionStats | null>(null)
const positionEditDialogRef = ref<PositionEditDialogExpose>()

const [modal, contextHolder] = Modal.useModal()

const { levelOptions, getLevelColor, getLevelText, formatDate } = usePositionDict()

const queryForm = reactive<PositionQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  code: '',
  departmentId: undefined,
  level: undefined
})

const columns: TableProps<Position>['columns'] = [
  { title: '岗位名称', dataIndex: 'name', width: 176 },
  { title: '岗位编码', key: 'code', width: 144 },
  { title: '描述', key: 'description' },
  { title: '岗位级别', key: 'level', width: 112, align: 'center' },
  { title: '在职人数', key: 'userCount', width: 96, align: 'center' },
  { title: '创建时间', key: 'createdAt', width: 176 },
  { title: '操作', key: 'actions', width: 144, fixed: 'right' }
]

const loadPositions = async () => {
  loading.value = true
  try {
    const res = await positionApi.getList(queryForm)
    positions.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载岗位列表失败:', error)
    message.error('加载岗位列表失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    stats.value = await positionApi.getStats()
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载岗位统计数据失败')
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadPositions()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.code = ''
  queryForm.departmentId = undefined
  queryForm.level = undefined
  queryForm.page = 1
  loadPositions()
}

const handleAdd = () => {
  positionEditDialogRef.value?.open()
}

const handleEdit = (row: Position) => {
  positionEditDialogRef.value?.open(row)
}

const handleDelete = async (row: Position) => {
  const confirmed = await modal.confirm({
    title: '删除岗位',
    content: '确定要删除该岗位吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed) return

  try {
    await positionApi.delete(row.id)
    message.success('删除成功')
    loadPositions()
    loadStats()
  } catch (error: any) {
    console.error('删除岗位失败:', error)
    message.error(error.response?.data?.msg || '删除岗位失败')
  }
}

const handlePageChange = (page: number, pageSize?: number) => {
  queryForm.page = page
  if (pageSize !== undefined) queryForm.pageSize = pageSize
  loadPositions()
}

const handleEditSuccess = () => {
  loadPositions()
  loadStats()
}

onMounted(() => {
  loadPositions()
  loadStats()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">岗位管理</h1>
        <p class="text-sm text-[#52677D]">管理系统岗位，包括添加、编辑和删除</p>
      </div>
      <div class="flex gap-3">
        <a-button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增岗位
        </a-button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats?.totalPositions || 0 }}</div>
          <div class="text-sm text-[#52677D]">岗位总数</div>
        </div>
      </a-card>
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats?.filledPositions || 0 }}</div>
          <div class="text-sm text-[#52677D]">已分配岗位</div>
        </div>
      </a-card>
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats?.vacantPositions || 0 }}</div>
          <div class="text-sm text-[#52677D]">空缺岗位</div>
        </div>
      </a-card>
      <a-card>
        <div class="text-center">
          <div class="text-2xl font-bold text-[#102A43]">{{ stats?.averageLevel || 0 }}</div>
          <div class="text-sm text-[#52677D]">平均级别</div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div class="flex flex-wrap gap-4 items-end">
        <a-form-item name="name" label="岗位名称">
          <a-input v-model:value="queryForm.name" placeholder="请输入岗位名称" class="w-48" />
        </a-form-item>
        <a-form-item name="code" label="岗位编码">
          <a-input v-model:value="queryForm.code" placeholder="请输入岗位编码" class="w-48" />
        </a-form-item>
        <a-form-item name="level" label="岗位级别">
          <a-select v-model:value="queryForm.level" class="w-40" allow-clear placeholder="请选择级别" :options="levelOptions" />
        </a-form-item>
        <a-form-item>
          <div class="flex gap-2">
            <a-button @click="handleSearch">
              <Search class="size-4 mr-2" />
              搜索
            </a-button>
            <a-button @click="handleReset">
              <RefreshCw class="size-4 mr-2" />
              重置
            </a-button>
          </div>
        </a-form-item>
      </div>

      <a-table :columns="columns" :data-source="positions" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1050 }">
        <template #bodyCell="{ column, record: position }">
          <template v-if="column.key === 'code'">
            <span class="font-mono">{{ position.code }}</span>
          </template>
          <template v-else-if="column.key === 'description'">{{ position.description || '-' }}</template>
          <template v-else-if="column.key === 'level'">
            <a-tag :color="getLevelColor(position.level)">
              {{ position.level }} - {{ getLevelText(position.level) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'userCount'">
            {{ position.userCount }}{{ position.maxCount ? ` / ${position.maxCount}` : '' }}
          </template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDate(position.createdAt) }}</template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex justify-end gap-1">
              <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(position)">编辑</a-button>
              <a-button type="link" size="small" danger class="h-auto p-0" @click="handleDelete(position)">删除</a-button>
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

    <PositionEditDialog ref="positionEditDialogRef" @success="handleEditSuccess" />
    <contextHolder />
  </div>
</template>
