<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Form, FormItem, Modal, Pagination as AntPagination, Table, message } from 'antdv-next'
import { Link, CheckCircle, XCircle, Grid3X3, Plus, Search, RefreshCw } from '@lucide/vue'
import { socialIdentityProviderApi } from '@/api/socialIdentityProvider'
import type {
  SocialIdentityProvider,
  SocialIdentityProviderStats,
  SocialProviderType,
} from '@/types/socialIdentityProvider'
import { PROVIDER_CONFIGS as PROVIDER_CONFIGS_CONST } from '@/types/socialIdentityProvider'

const PROVIDER_CONFIGS = PROVIDER_CONFIGS_CONST

const loading = ref(false)
const providers = ref<SocialIdentityProvider[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const [modal, contextHolder] = Modal.useModal()
const stats = ref<SocialIdentityProviderStats>({
  totalProviders: 0,
  activeProviders: 0,
  inactiveProviders: 0,
  byType: {},
})
const filterType = ref('')
const filterStatus = ref('')
const searchQuery = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
type ProviderForm = {
  id?: string
  name: string
  type: SocialProviderType
  issuer: string
  clientId: string
  clientSecret: string
  scope: string[]
  attributeMapping: Record<string, string>
  status?: 'active' | 'disabled'
}

const providerForm = reactive<ProviderForm>({
  name: '',
  type: 'oidc' as SocialProviderType,
  issuer: '',
  clientId: '',
  clientSecret: '',
  scope: [],
  attributeMapping: {},
})

const providerFormRules = {
  name: [{ required: true, message: '请输入身份源名称' }],
  issuer: [{ required: true, message: '请输入 Issuer' }],
  clientId: [{ required: true, message: '请输入 Client ID' }],
}

const attributeMappingInvalid = ref(false)
const attributeMappingStr = computed({
  get: () => JSON.stringify(providerForm.attributeMapping || {}, null, 2),
  set: (value: string) => {
    if (!value.trim()) {
      providerForm.attributeMapping = {}
      attributeMappingInvalid.value = false
      return
    }
    try {
      const parsed = JSON.parse(value)
      if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
        throw new Error('Attribute mapping must be an object')
      }
      providerForm.attributeMapping = parsed as Record<string, string>
      attributeMappingInvalid.value = false
    } catch {
      attributeMappingInvalid.value = true
    }
  },
})

const providerTypeOptions = computed(() =>
  Object.entries(PROVIDER_CONFIGS).map(([value, config]) => ({
    value,
    label: config.name,
  })),
)

const dialogTitle = computed(() => (isEdit.value ? '编辑身份源' : '添加身份源'))

const guideDialogVisible = ref(false)
const currentProvider = ref<SocialIdentityProvider | null>(null)
const baseUrl = ref(window.location.origin)

const loadStats = async () => {
  try {
    const data = await socialIdentityProviderApi.getStats()
    stats.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
    message.error('加载身份源统计信息失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadProviders()
}

const handleReset = () => {
  filterType.value = ''
  filterStatus.value = ''
  searchQuery.value = ''
  page.value = 1
  loadProviders()
}

const loadProviders = async () => {
  loading.value = true
  try {
    const data = await socialIdentityProviderApi.getList({
      type: filterType.value || undefined,
      status: filterStatus.value || undefined,
      search: searchQuery.value || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    providers.value = data.items
    total.value = data.total
  } catch (error) {
    message.error('加载身份源列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (type: SocialProviderType) => {
  if (!isEdit.value) {
    providerForm.scope = [...PROVIDER_CONFIGS[type].defaultScopes]
  }
}

const getAvailableScopes = () => {
  return PROVIDER_CONFIGS[providerForm.type].availableScopes
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(providerForm, {
    name: '',
    type: 'oidc',
    issuer: '',
    clientId: '',
    clientSecret: '',
    scope: [],
    attributeMapping: {},
  })
  attributeMappingInvalid.value = false
  handleTypeChange('oidc')
  dialogVisible.value = true
}

const handleEdit = (row: SocialIdentityProvider) => {
  isEdit.value = true
  Object.assign(providerForm, {
    id: row.id,
    name: row.name,
    type: row.type,
    issuer: row.issuer,
    clientId: row.clientId,
    clientSecret: row.clientSecret,
    scope: row.scope,
    attributeMapping: row.attributeMapping || {},
    status: row.status,
  })
  attributeMappingInvalid.value = false
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (attributeMappingInvalid.value) {
    message.error('属性映射必须是有效的 JSON 对象')
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: providerForm.name.trim(),
      issuer: providerForm.issuer.trim(),
      clientId: providerForm.clientId.trim(),
      clientSecret: providerForm.clientSecret.trim(),
      scope: providerForm.scope,
      attributeMapping: providerForm.attributeMapping,
    }
    if (isEdit.value) {
      if (!providerForm.id) {
        message.error('身份源信息无效')
        return
      }
      await socialIdentityProviderApi.update(providerForm.id, payload)
      message.success('更新成功')
    } else {
      await socialIdentityProviderApi.create({
        ...payload,
        type: providerForm.type,
      })
      message.success('创建成功')
    }
    dialogVisible.value = false
    loadProviders()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (row: SocialIdentityProvider) => {
  try {
    const newStatus = row.status === 'active' ? 'disabled' : 'active'
    await socialIdentityProviderApi.update(row.id, { status: newStatus })
    message.success('状态更新成功')
    loadProviders()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '状态更新失败')
  }
}

const handleDelete = async (row: SocialIdentityProvider) => {
  const confirmed = await modal.confirm({
    title: '删除身份源',
    content: '确定要删除该身份源吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await socialIdentityProviderApi.delete(row.id)
    message.success('删除成功')
    loadProviders()
    loadStats()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除失败')
  }
}

const handleViewGuide = (row: SocialIdentityProvider) => {
  currentProvider.value = row
  guideDialogVisible.value = true
}

const copyCallbackUrl = () => {
  if (currentProvider.value) {
    const url = `${baseUrl.value}/t/{tenantId}/federation/${currentProvider.value.id}/callback`
    navigator.clipboard.writeText(url)
    message.success('回调地址已复制到剪贴板')
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const toggleScope = (scope: string) => {
  const arr = providerForm.scope
  const index = arr.indexOf(scope)
  if (index > -1) {
    arr.splice(index, 1)
  } else {
    arr.push(scope)
  }
}

const handlePageChange = (newPage: number, newPageSize?: number) => {
  page.value = newPage
  if (newPageSize !== undefined) pageSize.value = newPageSize
  loadProviders()
}

onMounted(() => {
  loadStats()
  loadProviders()
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
              <div class="text-2xl font-bold text-foreground">{{ stats.totalProviders }}</div>
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
              <div class="text-2xl font-bold text-foreground">{{ stats.activeProviders }}</div>
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
              <div class="text-2xl font-bold text-foreground">{{ stats.inactiveProviders }}</div>
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
            <Input v-model:value="searchQuery"
              placeholder="搜索身份源名称"
              class="w-52"
            >
              <template #prefix>
                <Search class="size-4 text-muted-foreground" />
              </template>
            </Input>
            <Select v-model:value="filterType" class="w-36" allow-clear @update:value="loadProviders">
                <SelectOption v-for="(config, key) in PROVIDER_CONFIGS" :key="key" :value="key">
                  {{ config.name }}
                </SelectOption>

            </Select>
            <Select v-model:value="filterStatus" class="w-28" allow-clear @update:value="loadProviders">
                <SelectOption value="active">已启用</SelectOption>
                <SelectOption value="inactive">已禁用</SelectOption>

            </Select>
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button  @click="handleReset">
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
          { title: '名称', dataIndex: 'name', width: 192 }, { title: '类型', key: 'type', width: 144 },
          { title: 'Client ID', dataIndex: 'clientId', width: 256 }, { title: '状态', key: 'status', width: 96 },
          { title: 'Scope', key: 'scope', width: 192 }, { title: '创建时间', key: 'createdAt', width: 160 },
          { title: '操作', key: 'actions', width: 240 }
        ]" :data-source="providers" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1280 }">
          <template #bodyCell="{ column, record: row }">
            <template v-if="column.key === 'type'">
                <div class="flex items-center gap-2">
                  <Link :style="{ color: PROVIDER_CONFIGS[row.type as SocialProviderType]?.color }" class="size-4" />
                  <span>{{ PROVIDER_CONFIGS[row.type as SocialProviderType]?.name || row.type }}</span>
                </div>
            </template>
            <template v-else-if="column.key === 'status'">
                <Tag :color="row.status === 'active' ? 'green' : 'red'">
                  {{ row.status === 'active' ? '已启用' : '已禁用' }}
                </Tag>
            </template>
            <template v-else-if="column.key === 'scope'">
                <div class="flex flex-wrap gap-1">
                  <Tag v-for="scope in row.scope.slice(0, 2)" :key="scope" color="blue" class="text-xs">
                    {{ scope }}
                  </Tag>
                  <Tag v-if="row.scope.length > 2"  class="text-xs">
                    +{{ row.scope.length - 2 }}
                  </Tag>
                </div>
            </template>
            <template v-else-if="column.key === 'createdAt'">{{ formatDate(row.createdAt) }}</template>
            <template v-else-if="column.key === 'actions'">
                <div class="flex gap-1 flex-wrap">
                  <Button type="link" size="small" class="h-auto p-0" @click="handleEdit(row)">编辑</Button>
                  <Button
                    type="link"
                    size="small"
                    class="h-auto p-0"
                    @click="handleToggleStatus(row)"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </Button>
                  <Button type="link" size="small" class="h-auto p-0" @click="handleViewGuide(row)">配置指南</Button>
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
          <p>配置支持 Discovery 的标准 OpenID Connect 身份源。</p>
        </div>
        <Form :model="providerForm" :rules="providerFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
          <div class="grid gap-4">
            <FormItem label="Issuer" name="issuer">
              <Input v-model:value="providerForm.issuer" placeholder="https://idp.example.com" />
            </FormItem>
            <FormItem label="身份源名称" name="name">
              <Input v-model:value="providerForm.name" placeholder="请输入身份源名称" />
            </FormItem>
            <FormItem label="身份源类型" name="type">
              <Select
                v-model:value="providerForm.type"
                :options="providerTypeOptions"
                :disabled="isEdit"
                @update:value="handleTypeChange($event as SocialProviderType)"
              />
            </FormItem>
            <FormItem label="Client ID" name="clientId">
              <Input v-model:value="providerForm.clientId" placeholder="请输入Client ID" />
            </FormItem>
            <FormItem label="Client Secret" name="clientSecret">
              <Input
                v-model:value="providerForm.clientSecret"
                type="password"
                :placeholder="isEdit ? '留空表示保持原值' : '请输入Client Secret'"
              />
            </FormItem>
            <FormItem label="Scope">
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="scope in getAvailableScopes()"
                  :key="scope"
                  type="button"
                  :aria-pressed="providerForm.scope.includes(scope)"
                  class="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  @click="toggleScope(scope)"
                >
                  <Tag :color="providerForm.scope.includes(scope) ? 'blue' : 'default'">
                    {{ scope }}
                  </Tag>
                </button>
              </div>
            </FormItem>
            <FormItem label="属性映射">
              <InputTextArea
                v-model:value="attributeMappingStr"
                :rows="4"
                placeholder='JSON格式的属性映射配置，例如：{"username": "login", "email": "email"}'
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

    <Modal v-model:open="guideDialogVisible" :footer="null">
      <div class="max-w-2xl">
        <div>
          <h3>配置指南</h3>
          <p>按照第三方开放平台要求完成应用和回调地址配置。</p>
        </div>
        <div class="py-5" v-if="currentProvider">
          <Alert
            class="mb-5"
            type="info"
            show-icon
            :title="`${PROVIDER_CONFIGS[currentProvider.type]?.name || '身份源'} 身份源配置指南`"
          />

          <div class="flex flex-col gap-4">
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Tag color="processing">步骤 1</Tag>
                <span class="font-semibold">创建应用</span>
              </div>
              <div class="text-sm space-y-1">
                <p>1. 访问 {{ PROVIDER_CONFIGS[currentProvider.type]?.name }} 开放平台</p>
                <p>2. 创建一个网站应用或移动应用</p>
                <p>3. 获取应用的 Client ID 和 Client Secret</p>
              </div>
            </div>
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Tag color="processing">步骤 2</Tag>
                <span class="font-semibold">配置回调地址</span>
              </div>
              <div class="text-sm space-y-1">
                <p>在应用配置中添加以下回调地址：</p>
                <div class="flex gap-2 mt-2">
                  <Input
                    :value="`${baseUrl}/auth/callback/${currentProvider.type}`"
                    readonly
                    class="flex-1"
                  />
                  <Button @click="copyCallbackUrl">复制</Button>
                </div>
              </div>
            </div>
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Tag color="processing">步骤 3</Tag>
                <span class="font-semibold">填写配置信息</span>
              </div>
              <div class="text-sm space-y-1">
                <p>将获取到的 Client ID 和 Client Secret 填写到上方表单中</p>
              </div>
            </div>
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Tag color="processing">步骤 4</Tag>
                <span class="font-semibold">测试连接</span>
              </div>
              <div class="text-sm space-y-1">
                <p>保存配置后，点击"测试连接"按钮验证配置是否正确</p>
              </div>
            </div>
          </div>

          <Divider class="my-4" />

          <div class="mt-4">
            <h4 class="font-semibold mb-3">当前配置信息</h4>
            <div class="grid gap-2 text-sm">
              <div class="flex gap-4">
                <span class="text-muted-foreground w-28">Client ID:</span>
                <span>{{ currentProvider.clientId }}</span>
              </div>
              <div class="flex gap-4">
                <span class="text-muted-foreground w-28">Issuer:</span>
                <span>{{ currentProvider.issuer }}</span>
              </div>
              <div class="flex gap-4">
                <span class="text-muted-foreground w-28">Scope:</span>
                <span>{{ currentProvider.scope.join(', ') }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>
