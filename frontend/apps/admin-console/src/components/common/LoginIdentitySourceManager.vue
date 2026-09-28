<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Modal, message } from 'antdv-next'
import { Link, CheckCircle, XCircle, Grid3X3, Plus, Search, RefreshCw } from '@lucide/vue'
import { socialIdentitySourceApi } from '@/api/socialIdentitySource'
import { enterpriseIdentitySourceApi } from '@/api/enterpriseIdentitySource'
import type {
  LoginIdentitySource,
  LoginIdentitySourceStats,
  LoginSourceType,
  CreateLoginIdentitySourceDto,
} from '@/types/loginIdentitySource'
import { SOURCE_CONFIGS as SOURCE_CONFIGS_CONST, SOCIAL_SOURCE_TYPES, ENTERPRISE_SOURCE_TYPES } from '@/types/loginIdentitySource'

const SOURCE_CONFIGS = SOURCE_CONFIGS_CONST
const props = defineProps<{ category: 'social' | 'enterprise' }>()
const isEnterpriseSource = computed(() => props.category === 'enterprise')
const sourceApi = computed(() => isEnterpriseSource.value ? enterpriseIdentitySourceApi : socialIdentitySourceApi)
const visibleSourceTypes = computed(() => isEnterpriseSource.value ? ENTERPRISE_SOURCE_TYPES : SOCIAL_SOURCE_TYPES)

const loading = ref(false)
const sources = ref<LoginIdentitySource[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const [modal, contextHolder] = Modal.useModal()
const stats = ref<LoginIdentitySourceStats>({
  totalSources: 0,
  activeSources: 0,
  inactiveSources: 0,
  byType: {},
})
const filterType = ref('')
const filterStatus = ref('')
const searchQuery = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
type SourceForm = {
  id?: string
  name: string
  type: LoginSourceType
  mode: string | null
  clientId: string
  clientSecret: string
  status?: 'active' | 'disabled'
}

const sourceForm = reactive<SourceForm>({
  name: '',
  type: 'github',
  mode: null,
  clientId: '',
  clientSecret: '',
})

const sourceFormRules = computed(() => ({
  name: [{ required: true, message: '请输入身份源名称' }],
  type: [{ required: true, message: '请选择身份源类型' }],
  clientId: [{ required: true, message: `请输入${SOURCE_CONFIGS[sourceForm.type].clientIdLabel}` }],
}))

const sourceTypeOptions = computed(() =>
  visibleSourceTypes.value.map((value) => ({
    value,
    label: SOURCE_CONFIGS[value].name,
  })),
)
const filterTypeOptions = computed(() => visibleSourceTypes.value.map((value) => ({ value, label: SOURCE_CONFIGS[value].name })))
const filterStatusOptions = [{ value: 'active', label: '已启用' }, { value: 'disabled', label: '已禁用' }]

const sourceLabel = computed(() => isEnterpriseSource.value ? '企业身份源' : '社会化身份源')
const dialogTitle = computed(() => `${isEdit.value ? '编辑' : '添加'}${sourceLabel.value}`)

const currentTypeConfig = computed(() => SOURCE_CONFIGS[sourceForm.type])

const loadStats = async () => {
  try {
    stats.value = await sourceApi.value.getStats()
  } catch (error) {
    console.error('加载统计信息失败:', error)
    message.error('加载身份源统计信息失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadSources()
}

const handleReset = () => {
  filterType.value = ''
  filterStatus.value = ''
  searchQuery.value = ''
  page.value = 1
  loadSources()
}

const loadSources = async () => {
  loading.value = true
  try {
    const data = await sourceApi.value.getList({
      type: filterType.value as LoginSourceType || undefined,
      status: filterStatus.value || undefined,
      search: searchQuery.value || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    sources.value = data.items
    total.value = data.total
  } catch (error) {
    message.error('加载身份源列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (type: LoginSourceType) => {
  sourceForm.type = type
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(sourceForm, {
    id: undefined,
    name: '',
    type: isEnterpriseSource.value ? 'feishu_web' : 'github',
    mode: null,
    clientId: '',
    clientSecret: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row: LoginIdentitySource) => {
  isEdit.value = true
  Object.assign(sourceForm, {
    id: row.id,
    name: row.name,
    type: row.type,
    mode: row.mode,
    clientId: row.clientId,
    clientSecret: '',
    status: row.status,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload: Omit<CreateLoginIdentitySourceDto, 'clientSecret'> & { clientSecret?: string } = {
      name: sourceForm.name.trim(),
      type: sourceForm.type,
      mode: sourceForm.mode,
      clientId: sourceForm.clientId.trim(),
    }
    if (!visibleSourceTypes.value.some(type => type === sourceForm.type)) {
      message.error('身份源类型与当前分类不一致')
      return
    }
    if (sourceForm.clientSecret.trim()) {
      payload.clientSecret = sourceForm.clientSecret.trim()
    }
    if (isEdit.value) {
      if (!sourceForm.id) {
        message.error('身份源信息无效')
        return
      }
      await sourceApi.value.update(sourceForm.id, payload)
      message.success('更新成功')
    } else {
      payload.clientSecret = sourceForm.clientSecret.trim()
      await sourceApi.value.create({ ...payload, clientSecret: sourceForm.clientSecret.trim() })
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadSources()
    loadStats()
  } catch (error: any) {
    message.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row: LoginIdentitySource) => {
  try {
    const newStatus = row.status === 'active' ? 'disabled' : 'active'
    await sourceApi.value.update(row.id, { status: newStatus })
    message.success('状态更新成功')
    loadSources()
    loadStats()
  } catch (error: any) {
    message.error(error instanceof Error ? error.message : '状态更新失败')
  }
}

const handleDelete = async (row: LoginIdentitySource) => {
  const confirmed = await modal.confirm({
    title: `删除${sourceLabel.value}`,
    content: '确定要删除该身份源吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await sourceApi.value.delete(row.id)
    message.success('删除成功')
    loadSources()
    loadStats()
  } catch (error: any) {
    message.error(error instanceof Error ? error.message : '删除失败')
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const handlePageChange = (newPage: number, newPageSize?: number) => {
  page.value = newPage
  if (newPageSize !== undefined) pageSize.value = newPageSize
  loadSources()
}

onMounted(() => {
  loadStats()
  loadSources()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">{{ isEnterpriseSource ? '企业身份源' : '社会化身份源' }}</h1>
        <p class="text-sm text-muted-foreground">{{ isEnterpriseSource ? '配置企业级登录授权，目前支持飞书网页授权。' : '管理微信、GitHub 等社会化登录提供商配置。' }}</p>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <a-card>
        <div class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-indigo-500 to-purple-600">
              <Link class="size-5" />
            </div>
            <div>
              <div class="text-2xl font-bold text-foreground">{{ stats.totalSources }}</div>
              <div class="text-sm text-muted-foreground">总身份源</div>
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
              <div class="text-2xl font-bold text-foreground">{{ stats.activeSources }}</div>
              <div class="text-sm text-muted-foreground">已启用</div>
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
              <div class="text-2xl font-bold text-foreground">{{ stats.inactiveSources }}</div>
              <div class="text-sm text-muted-foreground">已禁用</div>
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
              <div class="text-2xl font-bold text-foreground">{{ Object.keys(stats.byType).length }}</div>
              <div class="text-sm text-muted-foreground">类型数量</div>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div>
        <div class="flex justify-between items-center mb-3">
          <div class="flex items-center gap-3">
            <a-input v-model:value="searchQuery" placeholder="搜索身份源名称" class="w-52">
              <template #prefix>
                <Search class="size-4 text-muted-foreground" />
              </template>
            </a-input>
            <a-select v-model:value="filterType" class="w-42" placeholder="请选择类型" allow-clear :options="filterTypeOptions" @update:value="handleSearch" />
            <a-select v-model:value="filterStatus" class="w-40" placeholder="请选择状态" allow-clear :options="filterStatusOptions" @update:value="handleSearch" />
            <a-button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </a-button>
            <a-button @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </a-button>
          </div>
          <a-button @click="handleCreate">
            <Plus class="w-4 h-4 mr-2" />
            添加{{ sourceLabel }}
          </a-button>
        </div>
      </div>
      <div>
        <a-table :columns="[
          { title: '名称', dataIndex: 'name', width: 192 },
          { title: '类型', key: 'type', width: 144 },
          { title: isEnterpriseSource ? 'App ID' : 'Client ID', dataIndex: 'clientId', width: 256 },
          { title: '状态', key: 'status', width: 96 },
          { title: '创建时间', key: 'createdAt', width: 160 },
          { title: '操作', key: 'actions', width: 200 }
        ]" :data-source="sources" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1100 }">
          <template #bodyCell="{ column, record: row }">
            <template v-if="column.key === 'type'">
              <div class="flex items-center gap-2">
                <Link :style="{ color: SOURCE_CONFIGS[row.type as LoginSourceType]?.color }" class="size-4" />
                <span>{{ SOURCE_CONFIGS[row.type as LoginSourceType]?.name || row.type }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="row.status === 'active' ? 'green' : 'red'">
                {{ row.status === 'active' ? '已启用' : '已禁用' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'createdAt'">{{ formatDate(row.createdAt) }}</template>
            <template v-else-if="column.key === 'actions'">
              <div class="flex gap-1 flex-wrap">
                <a-button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</a-button>
                <a-button type="link" size="small" class="h-auto p-0" @click="handleToggleStatus(row)">
                  {{ row.status === 'active' ? '禁用' : '启用' }}
                </a-button>
                <a-button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDelete(row)">删除</a-button>
              </div>
            </template>
          </template>
        </a-table>

        <div class="flex items-center justify-between mt-4 pt-4 border-t">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
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
    </a-card>

    <a-modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-xl">
        <div>
          <h3>{{ dialogTitle }}</h3>
          <p>{{ isEnterpriseSource ? '配置企业应用的登录授权凭证。' : '配置微信、GitHub、Gitee 等社会化登录凭证。' }}</p>
        </div>
        <a-form :model="sourceForm" :rules="sourceFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <a-form-item label="身份源类型" name="type">
              <a-select
                v-model:value="sourceForm.type"
                :options="sourceTypeOptions"
                :disabled="isEdit"
                @update:value="handleTypeChange($event as LoginSourceType)"
              />
            </a-form-item>
            <a-form-item label="身份源名称" name="name">
              <a-input v-model:value="sourceForm.name" placeholder="请输入身份源名称" />
            </a-form-item>
            <a-form-item :label="currentTypeConfig.clientIdLabel" name="clientId">
              <a-input v-model:value="sourceForm.clientId" :placeholder="`请输入${currentTypeConfig.clientIdLabel}`" />
            </a-form-item>
            <a-form-item :label="currentTypeConfig.clientSecretLabel" name="clientSecret" :rules="isEdit ? [] : [{ required: true, whitespace: true, message: `请输入${currentTypeConfig.clientSecretLabel}` }]">
              <a-input
                v-model:value="sourceForm.clientSecret"
                type="password"
                :placeholder="isEdit ? '留空表示保持原值' : `请输入${currentTypeConfig.clientSecretLabel}`"
              />
            </a-form-item>
          </div>
          <div class="flex justify-end gap-2">
            <a-button @click="dialogVisible = false">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="submitting">确定</a-button>
          </div>
        </a-form>
      </div>
    </a-modal>
    <contextHolder />
  </div>
</template>
