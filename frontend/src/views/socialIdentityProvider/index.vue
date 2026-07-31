<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'antdv-next'
import { Link, CheckCircle, XCircle, Grid3X3, Plus, Search, RefreshCw } from '@lucide/vue'
import { socialIdentityProviderApi } from '@/api/socialIdentityProvider'
import { Button } from '@/components/antd-compat'
import { Input } from '@/components/antd-compat'
import { Textarea } from '@/components/antd-compat'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/antd-compat'
import { Card, CardContent, CardHeader } from '@/components/antd-compat'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/antd-compat'
import { Badge } from '@/components/antd-compat'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/antd-compat'
import { Alert, AlertTitle } from '@/components/antd-compat'
import { Separator } from '@/components/antd-compat'
import { Label } from '@/components/antd-compat'
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

const attributeMappingStr = computed({
  get: () => JSON.stringify(providerForm.attributeMapping || {}, null, 2),
  set: (value: string) => {
    try {
      providerForm.attributeMapping = JSON.parse(value)
    } catch (error) {
      // Invalid JSON, keep the old value
    }
  },
})

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
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (isEdit.value) {
      if (!providerForm.id) return
      await socialIdentityProviderApi.update(providerForm.id, {
        name: providerForm.name,
        issuer: providerForm.issuer,
        clientId: providerForm.clientId,
        clientSecret: providerForm.clientSecret,
        scope: providerForm.scope,
        attributeMapping: providerForm.attributeMapping,
      })
      message.success('更新成功')
    } else {
      await socialIdentityProviderApi.create({
        name: providerForm.name,
        type: providerForm.type,
        issuer: providerForm.issuer,
        clientId: providerForm.clientId,
        clientSecret: providerForm.clientSecret,
        scope: providerForm.scope,
        attributeMapping: providerForm.attributeMapping,
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
  const confirmed = window.confirm('确定要删除该身份源吗？')
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

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const handlePageChange = (newPage: number) => {
  page.value = newPage
  loadProviders()
}

onMounted(() => {
  loadStats()
  loadProviders()
})
</script>

<template>
  <div class="p-5">
    <div class="grid grid-cols-4 gap-5 mb-5">
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-indigo-500 to-purple-600">
              <Link class="w-7 h-7" />
            </div>
            <div>
              <div class="text-2xl font-bold">{{ stats.totalProviders }}</div>
              <div class="text-sm text-muted-foreground">总身份源</div>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-emerald-400 to-teal-400">
              <CheckCircle class="w-7 h-7" />
            </div>
            <div>
              <div class="text-2xl font-bold">{{ stats.activeProviders }}</div>
              <div class="text-sm text-muted-foreground">已启用</div>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-pink-400 to-rose-500">
              <XCircle class="w-7 h-7" />
            </div>
            <div>
              <div class="text-2xl font-bold">{{ stats.inactiveProviders }}</div>
              <div class="text-sm text-muted-foreground">已禁用</div>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent class="pt-6">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 rounded-lg flex items-center justify-center text-white text-xl bg-gradient-to-br from-blue-400 to-cyan-400">
              <Grid3X3 class="w-7 h-7" />
            </div>
            <div>
              <div class="text-2xl font-bold">{{ Object.keys(stats.byType).length }}</div>
              <div class="text-sm text-muted-foreground">类型数量</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <Card>
      <CardHeader>
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-3">
            <Input
              v-model="searchQuery"
              placeholder="搜索身份源名称"
              class="w-52"
            >
              <template #prefix>
                <Search class="w-4 h-4 text-muted-foreground" />
              </template>
            </Input>
            <Select v-model="filterType" @update:model-value="loadProviders">
              <SelectTrigger class="w-36">
                <SelectValue placeholder="身份源类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="(config, key) in PROVIDER_CONFIGS" :key="key" :value="key">
                  {{ config.name }}
                </SelectItem>
              </SelectContent>
            </Select>
            <Select v-model="filterStatus" @update:model-value="loadProviders">
              <SelectTrigger class="w-28">
                <SelectValue placeholder="状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="active">已启用</SelectItem>
                <SelectItem value="inactive">已禁用</SelectItem>
              </SelectContent>
            </Select>
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button variant="outline" @click="handleReset">
              <RefreshCw class="w-4 h-4 mr-2" />
              重置
            </Button>
          </div>
          <Button @click="handleCreate">
            <Plus class="w-4 h-4 mr-2" />
            添加身份源
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-48">名称</TableHead>
              <TableHead class="w-36">类型</TableHead>
              <TableHead class="w-64">Client ID</TableHead>
              <TableHead class="w-24">状态</TableHead>
              <TableHead class="w-48">Scope</TableHead>
              <TableHead class="w-40">创建时间</TableHead>
              <TableHead class="w-60">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="7" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
            </TableRow>
            <TableRow v-else-if="providers.length === 0">
              <TableCell colspan="7" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
            </TableRow>
            <TableRow v-for="row in providers" :key="row.id">
              <TableCell>{{ row.name }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-2">
                  <Link :style="{ color: PROVIDER_CONFIGS[row.type as SocialProviderType]?.color }" class="w-4 h-4" />
                  <span>{{ PROVIDER_CONFIGS[row.type as SocialProviderType]?.name || row.type }}</span>
                </div>
              </TableCell>
              <TableCell>{{ row.clientId }}</TableCell>
              <TableCell>
                <Badge :variant="row.status === 'active' ? 'default' : 'destructive'">
                  {{ row.status === 'active' ? '已启用' : '已禁用' }}
                </Badge>
              </TableCell>
              <TableCell>
                <div class="flex flex-wrap gap-1">
                  <Badge v-for="scope in row.scope.slice(0, 2)" :key="scope" variant="secondary" class="text-xs">
                    {{ scope }}
                  </Badge>
                  <Badge v-if="row.scope.length > 2" variant="outline" class="text-xs">
                    +{{ row.scope.length - 2 }}
                  </Badge>
                </div>
              </TableCell>
              <TableCell>{{ formatDate(row.createdAt) }}</TableCell>
              <TableCell>
                <div class="flex gap-1 flex-wrap">
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleEdit(row)">编辑</Button>
                  <Button 
                    variant="link" 
                    size="sm" 
                    class="h-auto p-0"
                    @click="handleToggleStatus(row)"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </Button>
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleViewGuide(row)">配置指南</Button>
                  <Button variant="link" size="sm" class="h-auto p-0 text-destructive" @click="handleDelete(row)">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-1">
            <Button variant="outline" size="sm" :disabled="page <= 1" @click="handlePageChange(page - 1)">
              上一页
            </Button>
            <span class="text-sm px-2">{{ page }} / {{ totalPages || 1 }}</span>
            <Button variant="outline" size="sm" :disabled="page >= totalPages" @click="handlePageChange(page + 1)">
              下一页
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="max-w-xl">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
          <DialogDescription>配置支持 Discovery 的标准 OpenID Connect 身份源。</DialogDescription>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <Label for="provider-issuer">Issuer</Label>
              <Input id="provider-issuer" v-model="providerForm.issuer" placeholder="https://idp.example.com" />
            </div>
            <div class="grid gap-2">
              <Label for="provider-name">身份源名称</Label>
              <Input id="provider-name" v-model="providerForm.name" placeholder="请输入身份源名称" />
            </div>
            <div class="grid gap-2">
              <Label for="provider-type">身份源类型</Label>
              <Select v-model="providerForm.type" :disabled="isEdit" @update:model-value="handleTypeChange($event as SocialProviderType)">
                <SelectTrigger id="provider-type">
                  <SelectValue placeholder="请选择身份源类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="(config, key) in PROVIDER_CONFIGS" :key="key" :value="key">
                    <div class="flex items-center gap-2">
                      <Link :style="{ color: config.color }" class="w-4 h-4" />
                      <span>{{ config.name }}</span>
                    </div>
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="grid gap-2">
              <Label for="provider-client-id">Client ID</Label>
              <Input id="provider-client-id" v-model="providerForm.clientId" placeholder="请输入Client ID" />
            </div>
            <div class="grid gap-2">
              <Label for="provider-client-secret">Client Secret</Label>
              <Input id="provider-client-secret" v-model="providerForm.clientSecret" type="password" placeholder="请输入Client Secret" />
            </div>
            <div class="grid gap-2">
              <Label id="provider-scope-label">Scope</Label>
              <div class="flex flex-wrap gap-2" role="group" aria-labelledby="provider-scope-label">
                <button
                  v-for="scope in getAvailableScopes()"
                  :key="scope"
                  type="button"
                  :aria-pressed="providerForm.scope.includes(scope)"
                  class="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  @click="toggleScope(scope)"
                >
                  <Badge :variant="providerForm.scope.includes(scope) ? 'default' : 'outline'">
                    {{ scope }}
                  </Badge>
                </button>
              </div>
            </div>
            <div class="grid gap-2">
              <Label for="provider-attribute-mapping">属性映射</Label>
              <Textarea
                id="provider-attribute-mapping"
                v-model="attributeMappingStr"
                :rows="4"
                placeholder='JSON格式的属性映射配置，例如：{"username": "login", "email": "email"}'
              />
            </div>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit" :disabled="submitting">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="guideDialogVisible">
      <DialogContent class="max-w-2xl">
        <DialogHeader>
          <DialogTitle>配置指南</DialogTitle>
          <DialogDescription>按照第三方开放平台要求完成应用和回调地址配置。</DialogDescription>
        </DialogHeader>
        <div class="py-5" v-if="currentProvider">
          <Alert class="mb-5">
            <AlertTitle class="font-semibold">
              {{ PROVIDER_CONFIGS[currentProvider.type]?.name }} 身份源配置指南
            </AlertTitle>
          </Alert>

          <div class="flex flex-col gap-4">
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Badge variant="default">步骤 1</Badge>
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
                <Badge variant="default">步骤 2</Badge>
                <span class="font-semibold">配置回调地址</span>
              </div>
              <div class="text-sm space-y-1">
                <p>在应用配置中添加以下回调地址：</p>
                <div class="flex gap-2 mt-2">
                  <Input
                    :model-value="`${baseUrl}/auth/callback/${currentProvider.type}`"
                    readonly
                    class="flex-1"
                  />
                  <Button @click="copyCallbackUrl">复制</Button>
                </div>
              </div>
            </div>
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Badge variant="default">步骤 3</Badge>
                <span class="font-semibold">填写配置信息</span>
              </div>
              <div class="text-sm space-y-1">
                <p>将获取到的 Client ID 和 Client Secret 填写到上方表单中</p>
              </div>
            </div>
            <div class="border rounded-lg p-4">
              <div class="flex items-center gap-2 mb-2">
                <Badge variant="default">步骤 4</Badge>
                <span class="font-semibold">测试连接</span>
              </div>
              <div class="text-sm space-y-1">
                <p>保存配置后，点击"测试连接"按钮验证配置是否正确</p>
              </div>
            </div>
          </div>

          <Separator class="my-4" />

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
      </DialogContent>
    </Dialog>
  </div>
</template>
