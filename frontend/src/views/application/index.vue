<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applicationApi } from '@/api/application'
import type { Application, CreateApplicationDto, UpdateApplicationDto, ApplicationQueryDto } from '@/types/application'

const loading = ref(false)
const applications = ref<Application[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增应用')
const currentApp = ref<Partial<Application>>({})
const secretDialogVisible = ref(false)
const newClientSecret = ref('')
const showSecretDialogVisible = ref(false)
const currentAppSecret = ref('')

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

const redirectUriInput = ref('')

const rules = {
  name: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择应用类型', trigger: 'change' }
  ],
  accessTokenLifetime: [
    { required: true, message: '请输入访问令牌有效期', trigger: 'blur' },
    { type: 'number', min: 60, max: 86400, message: '有效期范围为 60 到 86400 秒', trigger: 'blur' }
  ],
  refreshTokenLifetime: [
    { required: true, message: '请输入刷新令牌有效期', trigger: 'blur' },
    { type: 'number', min: 3600, max: 31536000, message: '有效期范围为 3600 到 31536000 秒', trigger: 'blur' }
  ]
}

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await applicationApi.getList(queryForm)
    applications.value = res.data.applications
    total.value = res.data.total
  } catch (error) {
    console.error('加载应用列表失败:', error)
    ElMessage.error('加载应用列表失败')
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
  dialogVisible.value = true
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
  dialogVisible.value = true
}

const handleDelete = async (row: Application) => {
  try {
    await ElMessageBox.confirm('确定要删除该应用吗？删除后无法恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await applicationApi.delete(row.id)
    ElMessage.success('删除成功')
    loadApplications()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除应用失败:', error)
      ElMessage.error('删除应用失败')
    }
  }
}

const handleStatusChange = async (row: Application, status: string) => {
  try {
    await applicationApi.updateStatus(row.id, status)
    ElMessage.success('状态更新成功')
    loadApplications()
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新状态失败')
  }
}

const handleRegenerateSecret = async (row: Application) => {
  try {
    await ElMessageBox.confirm('重新生成密钥后，旧密钥将立即失效。确定要重新生成吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await applicationApi.regenerateSecret(row.id)
    newClientSecret.value = res.data.clientSecret
    secretDialogVisible.value = true
    ElMessage.success('密钥重新生成成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新生成密钥失败:', error)
      ElMessage.error('重新生成密钥失败')
    }
  }
}

const handleShowSecret = (row: Application) => {
  currentAppSecret.value = row.clientSecret
  showSecretDialogVisible.value = true
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
  try {
    if (currentApp.value.id) {
      await applicationApi.update(currentApp.value.id, appForm)
      ElMessage.success('更新成功')
    } else {
      const res = await applicationApi.create(appForm as CreateApplicationDto)
      newClientSecret.value = res.data.clientSecret
      secretDialogVisible.value = true
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadApplications()
  } catch (error: any) {
    console.error('保存应用失败:', error)
    ElMessage.error(error.response?.data?.message || '保存应用失败')
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
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'active':
      return 'success'
    case 'disabled':
      return 'warning'
    default:
      return 'info'
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

const formatLifetime = (seconds: number) => {
  if (seconds < 3600) {
    return `${Math.floor(seconds / 60)} 分钟`
  } else if (seconds < 86400) {
    return `${Math.floor(seconds / 3600)} 小时`
  } else {
    return `${Math.floor(seconds / 86400)} 天`
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="application-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>应用管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增应用
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="应用名称">
          <el-input v-model="queryForm.name" placeholder="请输入应用名称" clearable />
        </el-form-item>
        <el-form-item label="应用类型">
          <el-select v-model="queryForm.type" placeholder="请选择应用类型" clearable>
            <el-option label="Web应用" value="web" />
            <el-option label="原生应用" value="native" />
            <el-option label="单页应用" value="spa" />
            <el-option label="机器对机器" value="machine" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="active" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="applications" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="应用名称" width="180" />
        <el-table-column prop="type" label="应用类型" width="120">
          <template #default="{ row }">
            {{ getTypeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="clientId" label="Client ID" width="280">
          <template #default="{ row }">
            <div style="display: flex; align-items: center;">
              <span style="font-family: monospace; font-size: 12px;">{{ row.clientId }}</span>
              <el-button link type="primary" @click="copyToClipboard(row.clientId)" style="margin-left: 8px;">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="accessTokenLifetime" label="访问令牌有效期" width="140">
          <template #default="{ row }">
            {{ formatLifetime(row.accessTokenLifetime) }}
          </template>
        </el-table-column>
        <el-table-column prop="refreshTokenLifetime" label="刷新令牌有效期" width="140">
          <template #default="{ row }">
            {{ formatLifetime(row.refreshTokenLifetime) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ new Date(row.createdAt).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleShowSecret(row)">查看密钥</el-button>
            <el-button link type="warning" @click="handleRegenerateSecret(row)">重新生成密钥</el-button>
            <el-button 
              link 
              :type="row.status === 'active' ? 'warning' : 'success'" 
              @click="handleStatusChange(row, row.status === 'active' ? 'disabled' : 'active')"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: flex-end"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="appForm" :rules="rules" label-width="140px">
        <el-form-item label="应用名称" prop="name">
          <el-input v-model="appForm.name" placeholder="请输入应用名称" />
        </el-form-item>
        <el-form-item label="应用类型" prop="type">
          <el-select v-model="appForm.type" placeholder="请选择应用类型">
            <el-option label="Web应用" value="web" />
            <el-option label="原生应用" value="native" />
            <el-option label="单页应用" value="spa" />
            <el-option label="机器对机器" value="machine" />
          </el-select>
        </el-form-item>
        <el-form-item label="应用描述" prop="description">
          <el-input v-model="appForm.description" type="textarea" :rows="3" placeholder="请输入应用描述" />
        </el-form-item>
        <el-form-item label="应用Logo" prop="logo">
          <el-input v-model="appForm.logo" placeholder="请输入Logo URL" />
        </el-form-item>
        <el-form-item label="重定向URI" prop="redirectUris">
          <div style="width: 100%;">
            <div style="display: flex; margin-bottom: 8px;">
              <el-input v-model="redirectUriInput" placeholder="请输入重定向URI" style="flex: 1; margin-right: 8px;" />
              <el-button type="primary" @click="handleAddRedirectUri">添加</el-button>
            </div>
            <div v-if="appForm.redirectUris && appForm.redirectUris.length > 0">
              <el-tag
                v-for="(uri, index) in appForm.redirectUris"
                :key="index"
                closable
                @close="handleRemoveRedirectUri(index)"
                style="margin: 4px;"
              >
                {{ uri }}
              </el-tag>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="授权类型" prop="allowedGrantTypes">
          <el-checkbox-group v-model="appForm.allowedGrantTypes">
            <el-checkbox label="authorization_code">授权码模式</el-checkbox>
            <el-checkbox label="client_credentials">客户端凭证模式</el-checkbox>
            <el-checkbox label="refresh_token">刷新令牌</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="访问令牌有效期" prop="accessTokenLifetime">
          <el-input-number v-model="appForm.accessTokenLifetime" :min="60" :max="86400" :step="60" />
          <span style="margin-left: 8px; color: #999;">秒 ({{ formatLifetime(appForm.accessTokenLifetime) }})</span>
        </el-form-item>
        <el-form-item label="刷新令牌有效期" prop="refreshTokenLifetime">
          <el-input-number v-model="appForm.refreshTokenLifetime" :min="3600" :max="31536000" :step="3600" />
          <span style="margin-left: 8px; color: #999;">秒 ({{ formatLifetime(appForm.refreshTokenLifetime) }})</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="secretDialogVisible" title="Client Secret" width="500px">
      <el-alert
        title="请妥善保管您的客户端密钥"
        type="warning"
        description="密钥只会在创建应用或重新生成时显示一次，请立即复制并妥善保管。"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      <div style="background: #f5f5f5; padding: 12px; border-radius: 4px; font-family: monospace; word-break: break-all;">
        {{ newClientSecret }}
      </div>
      <template #footer>
        <el-button type="primary" @click="copyToClipboard(newClientSecret)">复制密钥</el-button>
        <el-button @click="secretDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSecretDialogVisible" title="Client Secret" width="500px">
      <el-alert
        title="客户端密钥"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      <div style="background: #f5f5f5; padding: 12px; border-radius: 4px; font-family: monospace; word-break: break-all;">
        {{ currentAppSecret }}
      </div>
      <template #footer>
        <el-button type="primary" @click="copyToClipboard(currentAppSecret)">复制密钥</el-button>
        <el-button @click="showSecretDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.application-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
