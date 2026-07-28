<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { toast } from 'vue-sonner'
import { Plus, Search, Copy, Trash2 } from '@lucide/vue'
import { applicationApi } from '@/api/application'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Checkbox } from '@/components/ui/checkbox'
import { NumberField, NumberFieldContent, NumberFieldDecrement, NumberFieldIncrement, NumberFieldInput } from '@/components/ui/number-field'
import { Minus, Plus as PlusIcon } from '@lucide/vue'
import type { Application, CreateApplicationDto, UpdateApplicationDto, ApplicationQueryDto } from '@/types/application'

type ApiErrorResponse = {
  readonly message?: string
}

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) return fallback
  return error.response?.data?.message ?? fallback
}

const router = useRouter()

const loading = ref(false)
const applications = ref<Application[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增应用')
const currentApp = ref<Partial<Application>>({})
const secretDialogVisible = ref(false)
const newClientSecret = ref('')

const queryForm = reactive<ApplicationQueryDto>({
  page: 1,
  pageSize: 10,
  name: '',
  type: undefined,
  status: undefined
})

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

const isEditing = computed(() => Boolean(currentApp.value.id))

const redirectUriInput = ref('')

const formErrors = reactive<{
  name?: string
  type?: string
  allowedGrantTypes?: string
  accessTokenLifetime?: string
  refreshTokenLifetime?: string
}>({})

const resetFormErrors = () => {
  formErrors.name = undefined
  formErrors.type = undefined
  formErrors.allowedGrantTypes = undefined
  formErrors.accessTokenLifetime = undefined
  formErrors.refreshTokenLifetime = undefined
}

const validateCreateForm = (): boolean => {
  resetFormErrors()
  let valid = true
  if (!appForm.name || !appForm.name.trim()) {
    formErrors.name = '请输入应用名称'
    valid = false
  }
  if (!appForm.type) {
    formErrors.type = '请选择应用类型'
    valid = false
  }
  return valid
}

const validateEditForm = (): boolean => {
  resetFormErrors()
  let valid = true
  if (!appForm.name || !appForm.name.trim()) {
    formErrors.name = '请输入应用名称'
    valid = false
  }
  if (!appForm.type) {
    formErrors.type = '请选择应用类型'
    valid = false
  }
  if (!appForm.allowedGrantTypes || appForm.allowedGrantTypes.length === 0) {
    formErrors.allowedGrantTypes = '请至少选择一种授权类型'
    valid = false
  }
  if (!appForm.accessTokenLifetime || appForm.accessTokenLifetime < 60) {
    formErrors.accessTokenLifetime = '访问令牌有效期不能小于 60 秒'
    valid = false
  }
  if (!appForm.refreshTokenLifetime || appForm.refreshTokenLifetime < 3600) {
    formErrors.refreshTokenLifetime = '刷新令牌有效期不能小于 3600 秒'
    valid = false
  }
  return valid
}

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await applicationApi.getList(queryForm)
    applications.value = res.data.applications
    total.value = res.data.total
  } catch (error) {
    console.error('加载应用列表失败:', error)
    toast.error('加载应用列表失败')
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
  redirectUriInput.value = ''
  currentApp.value = {}
  resetFormErrors()
  dialogVisible.value = true
}

const handleDetail = (row: Application) => {
  router.push(`/application/${row.id}`)
}

const handleEdit = (row: Application) => {
  dialogTitle.value = '编辑应用'
  Object.assign(appForm, {
    name: row.name,
    logo: row.logo || '',
    description: row.description || '',
    type: row.type,
    redirectUris: row.redirectUris || [],
    allowedGrantTypes: row.allowedGrantTypes || [],
    accessTokenLifetime: row.accessTokenLifetime,
    refreshTokenLifetime: row.refreshTokenLifetime
  })
  redirectUriInput.value = ''
  currentApp.value = row
  resetFormErrors()
  dialogVisible.value = true
}

const handleDelete = async (row: Application) => {
  const confirmed = window.confirm('确定要删除该应用吗？删除后无法恢复！')
  if (!confirmed) return
  
  try {
    await applicationApi.delete(row.id)
    toast.success('删除成功')
    loadApplications()
  } catch (error) {
    console.error('删除应用失败:', error)
    toast.error('删除应用失败')
  }
}

const handleStatusChange = async (row: Application, status: string) => {
  try {
    await applicationApi.updateStatus(row.id, status)
    toast.success('状态更新成功')
    loadApplications()
  } catch (error) {
    console.error('更新状态失败:', error)
    toast.error('更新状态失败')
  }
}

const handleAddRedirectUri = () => {
  if (redirectUriInput.value) {
    if (!appForm.redirectUris) {
      appForm.redirectUris = []
    }
    appForm.redirectUris.push(redirectUriInput.value)
    redirectUriInput.value = ''
  }
}

const handleRemoveRedirectUri = (index: number) => {
  appForm.redirectUris?.splice(index, 1)
}

const handleSubmit = async () => {
  const valid = isEditing.value ? validateEditForm() : validateCreateForm()
  if (!valid) {
    toast.error('请检查表单填写是否正确')
    return
  }
  try {
    if (isEditing.value && currentApp.value.id) {
      await applicationApi.update(currentApp.value.id, appForm)
      toast.success('更新成功')
    } else {
      const createPayload = {
        name: appForm.name.trim(),
        type: appForm.type
      }
      const res = await applicationApi.create(createPayload)
      newClientSecret.value = res.data.clientSecret
      secretDialogVisible.value = true
      toast.success('创建成功')
    }
    dialogVisible.value = false
    loadApplications()
  } catch (error: unknown) {
    console.error('保存应用失败:', error)
    toast.error(getApiErrorMessage(error, '保存应用失败'))
  }
}

const handlePageChange = (page: number) => {
  queryForm.page = page
  loadApplications()
}

const handleSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.page = 1
  loadApplications()
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    toast.error('复制失败')
  }
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'active':
      return 'default'
    case 'disabled':
      return 'secondary'
    default:
      return 'outline'
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

const toggleGrantType = (type: string) => {
  const index = appForm.allowedGrantTypes?.indexOf(type) ?? -1
  if (index > -1) {
    appForm.allowedGrantTypes?.splice(index, 1)
  } else {
    if (!appForm.allowedGrantTypes) {
      appForm.allowedGrantTypes = []
    }
    appForm.allowedGrantTypes.push(type)
  }
}

const totalPages = () => Math.ceil(total.value / queryForm.pageSize!)

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="p-5">
    <Card>
      <CardHeader>
        <div class="flex justify-between items-center">
          <CardTitle>应用管理</CardTitle>
          <Button @click="handleAdd">
            <Plus class="w-4 h-4 mr-2" />
            新增应用
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div class="flex flex-wrap gap-4 items-end mb-5">
          <div class="grid gap-2">
            <label class="text-sm font-medium">应用名称</label>
            <Input v-model="queryForm.name" placeholder="请输入应用名称" class="w-48" />
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">应用类型</label>
            <Select v-model="queryForm.type">
              <SelectTrigger class="w-36">
                <SelectValue placeholder="请选择应用类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="web">Web应用</SelectItem>
                <SelectItem value="native">原生应用</SelectItem>
                <SelectItem value="spa">单页应用</SelectItem>
                <SelectItem value="machine">机器对机器</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="grid gap-2">
            <label class="text-sm font-medium">状态</label>
            <Select v-model="queryForm.status">
              <SelectTrigger class="w-28">
                <SelectValue placeholder="请选择状态" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="active">正常</SelectItem>
                <SelectItem value="disabled">禁用</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex gap-2">
            <Button @click="handleSearch">
              <Search class="w-4 h-4 mr-2" />
              搜索
            </Button>
            <Button variant="outline" @click="handleReset">重置</Button>
          </div>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-44">应用名称</TableHead>
              <TableHead class="w-28">应用类型</TableHead>
              <TableHead class="w-72">Client ID</TableHead>
              <TableHead class="w-24">状态</TableHead>
              <TableHead class="w-32">访问令牌有效期</TableHead>
              <TableHead class="w-32">刷新令牌有效期</TableHead>
              <TableHead class="w-40">创建时间</TableHead>
              <TableHead class="w-80">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="8" class="text-center py-8 text-muted-foreground">加载中...</TableCell>
            </TableRow>
            <TableRow v-else-if="applications.length === 0">
              <TableCell colspan="8" class="text-center py-8 text-muted-foreground">暂无数据</TableCell>
            </TableRow>
            <TableRow v-for="row in applications" :key="row.id">
              <TableCell>{{ row.name }}</TableCell>
              <TableCell>{{ getTypeText(row.type) }}</TableCell>
              <TableCell>
                <div class="flex items-center gap-2">
                  <span class="font-mono text-xs">{{ row.clientId }}</span>
                  <Button variant="link" size="sm" class="h-auto p-0" @click="copyToClipboard(row.clientId)">
                    <Copy class="w-4 h-4" />
                  </Button>
                </div>
              </TableCell>
              <TableCell>
                <Badge :variant="getStatusVariant(row.status)">
                  {{ getStatusText(row.status) }}
                </Badge>
              </TableCell>
              <TableCell>{{ formatLifetime(row.accessTokenLifetime) }}</TableCell>
              <TableCell>{{ formatLifetime(row.refreshTokenLifetime) }}</TableCell>
              <TableCell>{{ new Date(row.createdAt).toLocaleString() }}</TableCell>
              <TableCell>
                <div class="flex gap-1 flex-wrap">
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleDetail(row)">详情</Button>
                  <Button variant="link" size="sm" class="h-auto p-0" @click="handleEdit(row)">编辑</Button>
                  <Button 
                    variant="link" 
                    size="sm" 
                    class="h-auto p-0"
                    @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
                  >
                    {{ row.status === 'active' ? '禁用' : '启用' }}
                  </Button>
                  <Button variant="link" size="sm" class="h-auto p-0 text-destructive" @click="handleDelete(row)">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div class="flex items-center justify-between mt-5">
          <span class="text-sm text-muted-foreground">共 {{ total }} 条</span>
          <div class="flex items-center gap-1">
            <Select v-model="queryForm.pageSize!" @update:model-value="handleSizeChange(Number($event))">
              <SelectTrigger class="w-20">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem :value="10">10</SelectItem>
                <SelectItem :value="20">20</SelectItem>
                <SelectItem :value="50">50</SelectItem>
                <SelectItem :value="100">100</SelectItem>
              </SelectContent>
            </Select>
            <span class="text-sm px-2">条/页</span>
            <Button variant="outline" size="sm" :disabled="queryForm.page! <= 1" @click="handlePageChange(queryForm.page! - 1)">上一页</Button>
            <span class="text-sm px-2">{{ queryForm.page! }} / {{ totalPages() }}</span>
            <Button variant="outline" size="sm" :disabled="queryForm.page! >= totalPages()" @click="handlePageChange(queryForm.page! + 1)">下一页</Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="dialogVisible">
      <DialogContent class="max-w-xl">
        <DialogHeader>
          <DialogTitle>{{ dialogTitle }}</DialogTitle>
        </DialogHeader>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">应用名称</label>
              <Input v-model="appForm.name" placeholder="请输入应用名称" :aria-invalid="!!formErrors.name" />
              <p v-if="formErrors.name" class="text-sm text-destructive">{{ formErrors.name }}</p>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">应用类型</label>
              <Select v-model="appForm.type">
                <SelectTrigger :aria-invalid="!!formErrors.type">
                  <SelectValue placeholder="请选择应用类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="web">Web应用</SelectItem>
                  <SelectItem value="native">原生应用</SelectItem>
                  <SelectItem value="spa">单页应用</SelectItem>
                  <SelectItem value="machine">机器对机器</SelectItem>
                </SelectContent>
              </Select>
              <p v-if="formErrors.type" class="text-sm text-destructive">{{ formErrors.type }}</p>
            </div>
            <template v-if="isEditing">
              <div class="grid gap-2">
                <label class="text-sm font-medium">应用描述</label>
                <Textarea v-model="appForm.description" :rows="3" placeholder="请输入应用描述" />
              </div>
              <div class="grid gap-2">
                <label class="text-sm font-medium">应用Logo</label>
                <Input v-model="appForm.logo" placeholder="请输入Logo URL" />
              </div>
              <div class="grid gap-2">
                <label class="text-sm font-medium">重定向URI</label>
                <div class="flex gap-2 mb-2">
                  <Input v-model="redirectUriInput" placeholder="请输入重定向URI" class="flex-1" />
                  <Button type="button" @click="handleAddRedirectUri">添加</Button>
                </div>
                <div v-if="appForm.redirectUris && appForm.redirectUris.length > 0" class="flex flex-wrap gap-2">
                  <Badge
                    v-for="(uri, index) in appForm.redirectUris"
                    :key="index"
                    variant="secondary"
                    class="cursor-pointer"
                    @click="handleRemoveRedirectUri(index)"
                  >
                    {{ uri }}
                    <Trash2 class="w-3 h-3 ml-1" />
                  </Badge>
                </div>
              </div>
              <div class="grid gap-2">
                <label class="text-sm font-medium">授权类型</label>
                <div class="flex flex-col gap-2">
                  <div class="flex items-center gap-2">
                    <Checkbox
                      :checked="appForm.allowedGrantTypes?.includes('authorization_code')"
                      @update:checked="toggleGrantType('authorization_code')"
                    />
                    <span class="text-sm">授权码模式</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Checkbox
                      :checked="appForm.allowedGrantTypes?.includes('client_credentials')"
                      @update:checked="toggleGrantType('client_credentials')"
                    />
                    <span class="text-sm">客户端凭证模式</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Checkbox
                      :checked="appForm.allowedGrantTypes?.includes('refresh_token')"
                      @update:checked="toggleGrantType('refresh_token')"
                    />
                    <span class="text-sm">刷新令牌</span>
                  </div>
                </div>
                <p v-if="formErrors.allowedGrantTypes" class="text-sm text-destructive">{{ formErrors.allowedGrantTypes }}</p>
              </div>
              <div class="grid gap-2">
                <label class="text-sm font-medium">访问令牌有效期</label>
                <div class="flex items-center gap-2">
                  <NumberField
                    v-model="appForm.accessTokenLifetime"
                    :min="60"
                    :max="86400"
                  >
                    <NumberFieldContent>
                      <NumberFieldDecrement>
                        <Minus class="w-4 h-4" />
                      </NumberFieldDecrement>
                      <NumberFieldInput />
                      <NumberFieldIncrement>
                        <PlusIcon class="w-4 h-4" />
                      </NumberFieldIncrement>
                    </NumberFieldContent>
                  </NumberField>
                  <span class="text-sm text-muted-foreground">秒 ({{ formatLifetime(appForm.accessTokenLifetime) }})</span>
                </div>
                <p v-if="formErrors.accessTokenLifetime" class="text-sm text-destructive">{{ formErrors.accessTokenLifetime }}</p>
              </div>
              <div class="grid gap-2">
                <label class="text-sm font-medium">刷新令牌有效期</label>
                <div class="flex items-center gap-2">
                  <NumberField
                    v-model="appForm.refreshTokenLifetime"
                    :min="3600"
                    :max="31536000"
                  >
                    <NumberFieldContent>
                      <NumberFieldDecrement>
                        <Minus class="w-4 h-4" />
                      </NumberFieldDecrement>
                      <NumberFieldInput />
                      <NumberFieldIncrement>
                        <PlusIcon class="w-4 h-4" />
                      </NumberFieldIncrement>
                    </NumberFieldContent>
                  </NumberField>
                  <span class="text-sm text-muted-foreground">秒 ({{ formatLifetime(appForm.refreshTokenLifetime) }})</span>
                </div>
                <p v-if="formErrors.refreshTokenLifetime" class="text-sm text-destructive">{{ formErrors.refreshTokenLifetime }}</p>
              </div>
            </template>
          </div>
        </form>
        <DialogFooter>
          <Button variant="outline" @click="dialogVisible = false">取消</Button>
          <Button @click="handleSubmit">确定</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="secretDialogVisible">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>Client Secret</DialogTitle>
        </DialogHeader>
        <Alert class="mb-5">
          <AlertTitle class="font-semibold">请妥善保管您的客户端密钥</AlertTitle>
          <AlertDescription>
            密钥只会在创建应用或重新生成时显示一次，请立即复制并妥善保管。
          </AlertDescription>
        </Alert>
        <div class="bg-muted p-3 rounded-md font-mono text-sm break-all">
          {{ newClientSecret }}
        </div>
        <DialogFooter>
          <Button @click="copyToClipboard(newClientSecret)">复制密钥</Button>
          <Button variant="outline" @click="secretDialogVisible = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    </div>
</template>
