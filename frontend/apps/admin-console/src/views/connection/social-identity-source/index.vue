<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { CheckCircle, Grid3X3, Link, Link2, Plus, RefreshCw, Search, XCircle } from '@lucide/vue'
import { socialIdentitySourceApi } from '@/api/socialIdentitySource'
import type { SocialIdentitySource, SocialIdentitySourceStats, SocialSourceType } from '@/types/socialIdentitySource'
import SourceEditModal from './components/SourceEditModal.vue'
import { getApiErrorMessage, useSocialSourceDict } from './utils/hooks'
import type { SourceEditModalExpose, SourceStatus } from './utils/types'

const rows = ref<SocialIdentitySource[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchQuery = ref('')
const filterType = ref<SocialSourceType>()
const filterStatus = ref<SourceStatus>()

const stats = ref<SocialIdentitySourceStats>({
  totalSources: 0,
  activeSources: 0,
  inactiveSources: 0,
  byType: {}
})

const sourceEditModalRef = ref<SourceEditModalExpose>()

const [modal, contextHolder] = Modal.useModal()

const { typeOptions, statusOptions, statusText, statusColor, typeName, typeColor, formatDate } = useSocialSourceDict()

const columns: TableProps<SocialIdentitySource>['columns'] = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 192 },
  { title: '类型', key: 'type', width: 144 },
  { title: 'Client ID', dataIndex: 'clientId', key: 'clientId', width: 256 },
  { title: '状态', key: 'status', width: 96 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'actions', width: 200 }
]

const loadStats = async () => {
  try {
    stats.value = await socialIdentitySourceApi.getStats()
  } catch (error: unknown) {
    console.error('加载社会化身份源统计失败:', error)
    message.error('加载社会化身份源统计失败')
  }
}

const loadSources = async () => {
  loading.value = true
  try {
    const data = await socialIdentitySourceApi.getList({
      type: filterType.value,
      status: filterStatus.value,
      search: searchQuery.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    rows.value = data.items
    total.value = data.total
  } catch (error: unknown) {
    console.error('加载社会化身份源失败:', error)
    message.error(getApiErrorMessage(error, '加载社会化身份源失败'))
  } finally {
    loading.value = false
  }
}

const reload = () => {
  loadSources()
  loadStats()
}

const handleSearch = () => {
  page.value = 1
  loadSources()
}

const handleReset = () => {
  filterType.value = undefined
  filterStatus.value = undefined
  searchQuery.value = ''
  page.value = 1
  loadSources()
}

const handlePageChange = (newPage: number) => {
  page.value = newPage
  loadSources()
}

const handleAdd = () => {
  sourceEditModalRef.value?.open()
}

const handleEdit = (row: SocialIdentitySource) => {
  sourceEditModalRef.value?.open(row)
}

const handleToggleStatus = async (row: SocialIdentitySource) => {
  try {
    const status: SourceStatus = row.status === 'active' ? 'disabled' : 'active'
    await socialIdentitySourceApi.update(row.id, { status })
    message.success(status === 'active' ? '身份源已启用' : '身份源已禁用')
    reload()
  } catch (error: unknown) {
    console.error('更新社会化身份源状态失败:', error)
    message.error(getApiErrorMessage(error, '更新身份源状态失败'))
  }
}

const handleDelete = async (row: SocialIdentitySource) => {
  const confirmed = await modal.confirm({
    title: '删除社会化身份源',
    content: `确定要删除“${row.name}”吗？`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed) {
    return
  }
  try {
    await socialIdentitySourceApi.delete(row.id)
    message.success('社会化身份源已删除')
    reload()
  } catch (error: unknown) {
    console.error('删除社会化身份源失败:', error)
    message.error(getApiErrorMessage(error, '删除社会化身份源失败'))
  }
}

onMounted(reload)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex gap-3">
        <Link2 class="mt-1 text-[#0369A1] size-6" />
        <div>
          <h1 class="text-2xl font-bold text-[#102A43] mb-2">社会化身份源</h1>
          <p class="text-sm text-[#52677D]">管理微信、GitHub 等社会化登录提供商配置。</p>
        </div>
      </div>
      <a-button type="primary" @click="handleAdd">
        <Plus class="size-4 mr-2" />
        添加社会化身份源
      </a-button>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <a-card>
        <div class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-indigo-500 to-purple-600">
              <Link class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-[#102A43]">{{ stats.totalSources }}</div>
              <div class="text-sm text-[#52677D]">总身份源</div>
            </div>
          </div>
        </div>
      </a-card>
      <a-card>
        <div class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-emerald-400 to-teal-400">
              <CheckCircle class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-[#102A43]">{{ stats.activeSources }}</div>
              <div class="text-sm text-[#52677D]">已启用</div>
            </div>
          </div>
        </div>
      </a-card>
      <a-card>
        <div class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-pink-400 to-rose-500">
              <XCircle class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-[#102A43]">{{ stats.inactiveSources }}</div>
              <div class="text-sm text-[#52677D]">已禁用</div>
            </div>
          </div>
        </div>
      </a-card>
      <a-card>
        <div class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-blue-400 to-cyan-400">
              <Grid3X3 class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-[#102A43]">{{ Object.keys(stats.byType).length }}</div>
              <div class="text-sm text-[#52677D]">类型数量</div>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div class="flex justify-between items-center mb-3">
        <div class="flex items-center gap-3">
          <a-input v-model:value="searchQuery" placeholder="搜索身份源名称" class="w-52" @press-enter="handleSearch">
            <template #prefix>
              <Search class="size-4 text-[#52677D]" />
            </template>
          </a-input>
          <a-select
              v-model:value="filterType"
              class="w-42"
              placeholder="请选择类型"
              allow-clear
              :options="typeOptions"
              @change="handleSearch"
              @clear="handleSearch"
          />
          <a-select
              v-model:value="filterStatus"
              class="w-40"
              placeholder="请选择状态"
              allow-clear
              :options="statusOptions"
              @change="handleSearch"
              @clear="handleSearch"
          />
          <a-button @click="handleSearch">
            <Search class="w-4 h-4 mr-2" />
            搜索
          </a-button>
          <a-button @click="handleReset">
            <RefreshCw class="w-4 h-4 mr-2" />
            重置
          </a-button>
        </div>
      </div>

      <a-table
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          row-key="id"
          :pagination="false"
          :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <div class="flex items-center gap-2">
              <Link :style="{ color: typeColor(record.type) }" class="size-4" />
              <span>{{ typeName(record.type) }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex gap-1 flex-wrap">
              <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" class="h-auto p-0" @click="handleToggleStatus(record)">
                {{ record.status === 'active' ? '禁用' : '启用' }}
              </a-button>
              <a-button type="link" size="small" class="h-auto p-0 text-[#B91C1C]" @click="handleDelete(record)">删除</a-button>
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
    </a-card>

    <SourceEditModal ref="sourceEditModalRef" @success="reload" />
    <contextHolder />
  </div>
</template>
