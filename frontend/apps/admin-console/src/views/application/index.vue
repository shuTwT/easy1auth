<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Modal, message } from 'antdv-next'
import { Plus, Search, Copy } from '@lucide/vue'
import { applicationApi } from '@/api/application'
import type { Application, CreateApplicationDto, UpdateApplicationDto, ApplicationQueryDto } from '@/types/application'
import ApplicationEditDialog from "@/views/application/components/ApplicationEditDialog.vue";
import SecretDialog from "@/views/application/components/SecretDialog.vue";
import type {ColumnType} from "antdv-next/dist/table";

const router = useRouter()

const loading = ref(false)
const applications = ref<Application[]>([])
const total = ref(0)
const dialogTitle = ref('新增应用')

const ApplicationEditDialogRef = ref()
const SecretDialogRef = ref()

const [modal, contextHolder] = Modal.useModal()

const queryForm = reactive<ApplicationQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined,
  status: undefined
})

const applicationTypeOptions = [
  { value: 'web', label: 'Web应用' },
  { value: 'native', label: '原生应用' },
  { value: 'spa', label: '单页应用' },
  { value: 'machine', label: '机器对机器' },
]
const applicationStatusOptions = [
  { value: 'active', label: '正常' },
  { value: 'disabled', label: '禁用' },
]

const columns = [
  { title: '应用名称', dataIndex: 'name', width: 176 },
  { title: '应用类型', key: 'type', width: 112 },
  { title: 'Client ID', key: 'clientId', width: 330 },
  { title: '状态', key: 'status', width: 96 },
  { title: '访问令牌有效期', key: 'accessTokenLifetime', width: 128 },
  { title: '刷新令牌有效期', key: 'refreshTokenLifetime', width: 128 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'actions',fixed:'right', width: 320 }
]

const appForm = reactive<CreateApplicationDto & UpdateApplicationDto>({
  name: '',
  logo: '',
  description: '',
  type: 'web',
  redirectUris: [],
  allowedGrantTypes: ['authorization_code'],
  accessTokenLifetime: 3600,
  refreshTokenLifetime: 2592000
})


const loadApplications = async () => {
  loading.value = true
  try {
    const res = await applicationApi.getList(queryForm)
    applications.value = res.items
    total.value = res.total
  } catch (error) {
    console.error('加载应用列表失败:', error)
    message.error('加载应用列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  loadApplications()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.type = undefined
  queryForm.status = undefined
  queryForm.page = 1
  loadApplications()
}

const handleAdd = () => {
  dialogTitle.value = '新增应用'
  Object.assign(appForm, {
    name: '',
    logo: '',
    description: '',
    type: 'web',
    redirectUris: [],
    allowedGrantTypes: ['authorization_code'],
    accessTokenLifetime: 3600,
    refreshTokenLifetime: 2592000
  })
  ApplicationEditDialogRef.value.open('create')
}

const handleDetail = (row: Application) => {
  router.push(`/application/${row.id}`)
}

const handleEdit = (row: Application) => {
  ApplicationEditDialogRef.value.open('edit',row)
}

const handleDelete = async (row: Application) => {
  const confirmed = await modal.confirm({
    title: '删除应用',
    content: '确定要删除该应用吗？删除后无法恢复！',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await applicationApi.delete(row.id)
    message.success('删除成功')
    loadApplications()
  } catch (error) {
    console.error('删除应用失败:', error)
    message.error('删除应用失败')
  }
}

const handleStatusChange = async (row: Application, status: string) => {
  try {
    await applicationApi.updateStatus(row.id, status)
    message.success('状态更新成功')
    loadApplications()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('更新应用状态失败')
  }
}

const handlePageChange = (page: number, pageSize?: number) => {
  queryForm.page = page
  if (pageSize !== undefined) queryForm.pageSize = pageSize
  loadApplications()
}

const handleSizeChange = (_current: number, size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadApplications()
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    message.error('复制失败')
  }
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'active':
      return 'green'
    case 'disabled':
      return 'orange'
    default:
      return 'red'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'active':
      return '正常'
    case 'disabled':
      return '禁用'
    default:
      return '未知'
  }
}

const getTypeText = (type: string) => {
  switch (type) {
    case 'web':
      return 'Web应用'
    case 'native':
      return '原生应用'
    case 'spa':
      return '单页应用'
    case 'machine':
      return '机器对机器'
    default:
      return '未知'
  }
}

const formatLifetime = (seconds?: number) => {
  if (!seconds) {
    return 'N/A'
  }
  if (seconds < 3600) {
    return `${Math.floor(seconds / 60)} 分钟`
  } else if (seconds < 86400) {
    return `${Math.floor(seconds / 3600)} 小时`
  } else {
    return `${Math.floor(seconds / 86400)} 天`
  }
}

const handleSubmitApplicationEditDialog=(type:string,secret:string)=>{
  if(type=='create'){
    SecretDialogRef.value.open(secret)
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">应用管理</h1>
        <p class="text-sm text-[#52677D]">管理接入应用，包括 OAuth 2.0 客户端配置</p>
      </div>
      <div class="flex gap-3">
        <a-button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          新增应用
        </a-button>
      </div>
    </div>

    <a-card>
      <div>
        <div class="flex flex-wrap gap-4 items-end">
          <a-form-item name="name" label="应用名称">
            <a-input v-model:value="queryForm.name" placeholder="请输入应用名称" class="w-48" />
          </a-form-item>
          <a-form-item name="type" label="应用类型">
            <a-select v-model:value="queryForm.type" placeholder="选择应用类型" class="w-36" allow-clear :options="applicationTypeOptions" />
          </a-form-item>
          <a-form-item name="type" label="状态">
            <a-select v-model:value="queryForm.status" placeholder="选择状态" class="w-28" allow-clear :options="applicationStatusOptions" />
          </a-form-item>
          <a-form-item>
            <div class="flex gap-2">
              <a-button @click="handleSearch">
                <Search class="size-4 mr-2" />
                搜索
              </a-button>
              <a-button  @click="handleReset">重置</a-button>
            </div>
          </a-form-item>
        </div>

        <a-table :columns="columns" :data-source="applications" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1408 }">
          <template #bodyCell="{ column, record: row }">
            <template v-if="column.key === 'type'">{{ getTypeText(row.type) }}</template>
            <template v-else-if="column.key === 'clientId'">
                <div class="flex items-center gap-2">
                  <span class="font-mono text-xs">{{ row.clientId }}</span>
                  <a-button type="link" size="small" class="h-auto p-0" @click="copyToClipboard(row.clientId)">
                    <Copy class="size-4" />
                  </a-button>
                </div>
            </template>
            <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusVariant(row.status)">
                  {{ getStatusText(row.status) }}
                </a-tag>
            </template>
            <template v-else-if="column.key === 'accessTokenLifetime'">{{ formatLifetime(row.accessTokenLifetime) }}</template>
            <template v-else-if="column.key === 'refreshTokenLifetime'">{{ formatLifetime(row.refreshTokenLifetime) }}</template>
            <template v-else-if="column.key === 'createdAt'">{{ new Date(row.createdAt).toLocaleString() }}</template>
            <template v-else-if="column.key === 'actions'">
                <div class="flex gap-1 flex-wrap">
                  <a-button type="link" size="small" class="h-auto p-0" @click="handleDetail(row)">详情</a-button>
                  <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="h-auto p-0"
                    @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </a-button>
                  <a-button type="link" size="small" danger @click="handleDelete(row)">删除</a-button>
                </div>
            </template>
          </template>
        </a-table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-[#52677D]">共 {{ total }} 条</span>
          <a-pagination
            :current="queryForm.page"
            :page-size="queryForm.pageSize"
            :total="total"
            :show-size-changer="true"
            :page-size-options="['10', '20', '50', '100']"
            size="small"
            @change="handlePageChange"
            @show-size-change="handleSizeChange"
          />
        </div>
      </div>
    </a-card>

    <ApplicationEditDialog ref="ApplicationEditDialogRef" @submit="handleSubmitApplicationEditDialog"/>

    <SecretDialog ref="SecretDialogRef"/>

    <contextHolder />
    </div>
</template>
