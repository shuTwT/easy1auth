<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Form, FormItem, Modal, Pagination as AntPagination, Table, message } from 'antdv-next'
import { Link, CheckCircle, XCircle, Grid3X3, Plus, Search, RefreshCw } from '@lucide/vue'
import { socialIdentitySourceApi } from '@/api/socialIdentitySource'
import type {
  SocialIdentitySource,
  SocialIdentitySourceStats,
  SocialSourceType,
} from '@/types/socialIdentitySource'
import { SOURCE_CONFIGS as SOURCE_CONFIGS_CONST, SOCIAL_SOURCE_TYPES } from '@/types/socialIdentitySource'

const SOURCE_CONFIGS = SOURCE_CONFIGS_CONST

const loading = ref(false)
const sources = ref<SocialIdentitySource[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const [modal, contextHolder] = Modal.useModal()
const stats = ref<SocialIdentitySourceStats>({
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
  type: SocialSourceType
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

const sourceFormRules = {
  name: [{ required: true, message: '请输入身份源名称' }],
  type: [{ required: true, message: '请选择身份源类型' }],
  clientId: [{ required: true, message: '请输入 Client ID' }],
}

const sourceTypeOptions = computed(() =>
  SOCIAL_SOURCE_TYPES.map((value) => ({
    value,
    label: SOURCE_CONFIGS[value].name,
  })),
)

const dialogTitle = computed(() => (isEdit.value ? '编辑身份源' : '添加身份源'))

const currentTypeConfig = computed(() => SOURCE_CONFIGS[sourceForm.type])

const loadStats = async () => {
  try {
    const data = await socialIdentitySourceApi.getStats()
    stats.value = data
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
    const data = await socialIdentitySourceApi.getList({
      type: filterType.value || undefined,
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

const handleTypeChange = (type: SocialSourceType) => {
  sourceForm.type = type
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(sourceForm, {
    name: '',
    type: 'github',
    mode: null,
    clientId: '',
    clientSecret: '',
  })
  dialogVisible.value = true
}

const handleEdit = (row: SocialIdentitySource) => {
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
    const payload: any = {
      name: sourceForm.name.trim(),
      type: sourceForm.type,
      mode: sourceForm.mode,
      clientId: sourceForm.clientId.trim(),
    }
    if (sourceForm.clientSecret.trim()) {
      payload.clientSecret = sourceForm.clientSecret.trim()
    }
    if (isEdit.value) {
      if (!sourceForm.id) {
        message.error('身份源信息无效')
        return
      }
      await socialIdentitySourceApi.update(sourceForm.id, payload)
      message.success('更新成功')
    } else {
      payload.clientSecret = sourceForm.clientSecret.trim()
      await socialIdentitySourceApi.create(payload)
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadSources()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row: SocialIdentitySource) => {
  try {
    const newStatus = row.status === 'active' ? 'disabled' : 'active'
    await socialIdentitySourceApi.update(row.id, { status: newStatus })
    message.success('状态更新成功')
    loadSources()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '状态更新失败')
  }
}

const handleDelete = async (row: SocialIdentitySource) => {
  const confirmed = await modal.confirm({
    title: '删除身份源',
    content: '确定要删除该身份源吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await socialIdentitySourceApi.delete(row.id)
    message.success('删除成功')
    loadSources()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除失败')
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
        <h1 class="text-2xl font-bold text-foreground mb-2">社会化身份源</h1>
        <p class="text-sm text-muted-foreground">管理社会化登录提供商配置</p>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5 mb-6">
      <Card>
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
      </Card>
      <Card>
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
      </Card>
      <Card>
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
      </Card>
      <Card>
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
      </Card>
    </div>

    <Card>
      <div>
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-3">
            <Input v-model:value="searchQuery" placeholder="搜索身份源名称" class="w-52">
              <template #prefix>
                <Search class="size-4 text-muted-foreground" />
              </template>
            </Input>
            <Select v-model:value="filterType" class="w-36" allow-clear @update:value="loadSources">
              <SelectOption v-for="type in SOCIAL_SOURCE_TYPES" :key="type" :value="type">
                {{ SOURCE_CONFIGS[type].name }}
              </SelectOption>
            </Select>
            <Select v-model:value="filterStatus" class="w-28" allow-clear @update:value="loadSources">
              <SelectOption value="active">已启用</SelectOption>
              <SelectOption value="disabled">已禁用</SelectOption>
            </Select>
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
          <Button @click="handleCreate">
            <Plus class="w-4 h-4 mr-2" />
            添加身份源
          </Button>
        </div>
      </div>
      <div>
        <Table :columns="[
          { title: '名称', dataIndex: 'name', width: 192 },
          { title: '类型', key: 'type', width: 144 },
          { title: 'Client ID', dataIndex: 'clientId', width: 256 },
          { title: '状态', key: 'status', width: 96 },
          { title: '创建时间', key: 'createdAt', width: 160 },
          { title: '操作', key: 'actions', width: 200 }
        ]" :data-source="sources" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1100 }">
          <template #bodyCell="{ column, record: row }">
            <template v-if="column.key === 'type'">
              <div class="flex items-center gap-2">
                <Link :style="{ color: SOURCE_CONFIGS[row.type as SocialSourceType]?.color }" class="size-4" />
                <span>{{ SOURCE_CONFIGS[row.type as SocialSourceType]?.name || row.type }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="row.status === 'active' ? 'green' : 'red'">
                {{ row.status === 'active' ? '已启用' : '已禁用' }}
              </Tag>
            </template>
            <template v-else-if="column.key === 'createdAt'">{{ formatDate(row.createdAt) }}</template>
            <template v-else-if="column.key === 'actions'">
              <div class="flex gap-1 flex-wrap">
                <Button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</Button>
                <Button type="link" size="small" class="h-auto p-0" @click="handleToggleStatus(row)">
                  {{ row.status === 'active' ? '禁用' : '启用' }}
                </Button>
                <Button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDelete(row)">删除</Button>
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
    </Card>

    <Modal v-model:open="dialogVisible" :footer="null">
      <div class="max-w-xl">
        <div>
          <h3>{{ dialogTitle }}</h3>
          <p>配置预定义的社会化登录身份源，端点由系统自动管理。</p>
        </div>
        <Form :model="sourceForm" :rules="sourceFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <FormItem label="身份源类型" name="type">
              <Select
                v-model:value="sourceForm.type"
                :options="sourceTypeOptions"
                :disabled="isEdit"
                @update:value="handleTypeChange($event as SocialSourceType)"
              />
            </FormItem>
            <FormItem label="身份源名称" name="name">
              <Input v-model:value="sourceForm.name" placeholder="请输入身份源名称" />
            </FormItem>
            <FormItem :label="currentTypeConfig.clientIdLabel" name="clientId">
              <Input v-model:value="sourceForm.clientId" :placeholder="`请输入${currentTypeConfig.clientIdLabel}`" />
            </FormItem>
            <FormItem :label="currentTypeConfig.clientSecretLabel" name="clientSecret">
              <Input
                v-model:value="sourceForm.clientSecret"
                type="password"
                :placeholder="isEdit ? '留空表示保持原值' : `请输入${currentTypeConfig.clientSecretLabel}`"
              />
            </FormItem>
          </div>
          <div class="flex justify-end gap-2">
            <Button @click="dialogVisible = false">取消</Button>
            <Button type="primary" html-type="submit" :loading="submitting">确定</Button>
          </div>
        </Form>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>
