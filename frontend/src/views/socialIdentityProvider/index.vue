<template>
  <div class="social-identity-provider-management">
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon total">
                <el-icon><Connection /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.totalProviders }}</div>
                <div class="stat-label">总身份源</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon active">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.activeProviders }}</div>
                <div class="stat-label">已启用</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon inactive">
                <el-icon><CircleClose /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ stats.inactiveProviders }}</div>
                <div class="stat-label">已禁用</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-icon types">
                <el-icon><Grid /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ Object.keys(stats.byType).length }}</div>
                <div class="stat-label">类型数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-select v-model="filterType" placeholder="身份源类型" clearable style="width: 150px" @change="loadProviders">
              <el-option v-for="(config, key) in PROVIDER_CONFIGS" :key="key" :label="config.name" :value="key" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px; margin-left: 10px" @change="loadProviders">
              <el-option label="已启用" value="active" />
              <el-option label="已禁用" value="inactive" />
            </el-select>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              添加身份源
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="providers" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="名称" width="200" />
        <el-table-column label="类型" width="150">
          <template #default="{ row }">
            <div class="provider-type">
              <el-icon :style="{ color: PROVIDER_CONFIGS[row.type as SocialProviderType]?.color }">
                <component :is="PROVIDER_CONFIGS[row.type as SocialProviderType]?.icon || 'Connection'" />
              </el-icon>
              <span>{{ PROVIDER_CONFIGS[row.type as SocialProviderType]?.name || row.type }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="clientId" label="Client ID" width="250" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Scope" width="200">
          <template #default="{ row }">
            <el-tag v-for="scope in row.scope.slice(0, 2)" :key="scope" size="small" style="margin-right: 4px">
              {{ scope }}
            </el-tag>
            <el-tag v-if="row.scope.length > 2" size="small">+{{ row.scope.length - 2 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'active' ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'active' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="primary" size="small" @click="handleViewGuide(row)">配置指南</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="providerForm" :rules="rules" ref="formRef" label-width="140px">
        <el-form-item label="身份源名称" prop="name">
          <el-input v-model="providerForm.name" placeholder="请输入身份源名称" />
        </el-form-item>
        <el-form-item label="身份源类型" prop="type">
          <el-select v-model="providerForm.type" placeholder="请选择身份源类型" :disabled="isEdit" style="width: 100%" @change="handleTypeChange">
            <el-option v-for="(config, key) in PROVIDER_CONFIGS" :key="key" :label="config.name" :value="key">
              <div class="provider-option">
                <el-icon :style="{ color: config.color }">
                  <component :is="config.icon" />
                </el-icon>
                <span>{{ config.name }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Client ID" prop="clientId">
          <el-input v-model="providerForm.clientId" placeholder="请输入Client ID" />
        </el-form-item>
        <el-form-item label="Client Secret" prop="clientSecret">
          <el-input v-model="providerForm.clientSecret" type="password" placeholder="请输入Client Secret" show-password />
        </el-form-item>
        <el-form-item label="授权端点" prop="authorizationEndpoint">
          <el-input v-model="providerForm.authorizationEndpoint" placeholder="OAuth授权端点URL" />
        </el-form-item>
        <el-form-item label="Token端点" prop="tokenEndpoint">
          <el-input v-model="providerForm.tokenEndpoint" placeholder="OAuth Token端点URL" />
        </el-form-item>
        <el-form-item label="用户信息端点" prop="userInfoEndpoint">
          <el-input v-model="providerForm.userInfoEndpoint" placeholder="用户信息端点URL" />
        </el-form-item>
        <el-form-item label="Scope" prop="scope">
          <el-select v-model="providerForm.scope" multiple placeholder="请选择Scope" style="width: 100%">
            <el-option v-for="scope in getAvailableScopes()" :key="scope" :label="scope" :value="scope" />
          </el-select>
        </el-form-item>
        <el-form-item label="属性映射" prop="attributeMapping">
          <el-input
            v-model="attributeMappingStr"
            type="textarea"
            :rows="4"
            placeholder="JSON格式的属性映射配置，例如：{&quot;username&quot;: &quot;login&quot;, &quot;email&quot;: &quot;email&quot;}"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="guideDialogVisible" title="配置指南" width="800px">
      <div class="guide-content" v-if="currentProvider">
        <el-alert type="info" :closable="false" style="margin-bottom: 20px">
          <template #title>
            <strong>{{ PROVIDER_CONFIGS[currentProvider.type]?.name }} 身份源配置指南</strong>
          </template>
        </el-alert>

        <el-steps :active="1" direction="vertical">
          <el-step title="创建应用">
            <template #description>
              <div class="step-content">
                <p>1. 访问 {{ PROVIDER_CONFIGS[currentProvider.type]?.name }} 开放平台</p>
                <p>2. 创建一个网站应用或移动应用</p>
                <p>3. 获取应用的 Client ID 和 Client Secret</p>
              </div>
            </template>
          </el-step>
          <el-step title="配置回调地址">
            <template #description>
              <div class="step-content">
                <p>在应用配置中添加以下回调地址：</p>
                <el-input
                  :model-value="`${baseUrl}/auth/callback/${currentProvider.type}`"
                  readonly
                  style="margin-top: 10px"
                >
                  <template #append>
                    <el-button @click="copyCallbackUrl">复制</el-button>
                  </template>
                </el-input>
              </div>
            </template>
          </el-step>
          <el-step title="填写配置信息">
            <template #description>
              <div class="step-content">
                <p>将获取到的 Client ID 和 Client Secret 填写到上方表单中</p>
              </div>
            </template>
          </el-step>
          <el-step title="测试连接">
            <template #description>
              <div class="step-content">
                <p>保存配置后，点击"测试连接"按钮验证配置是否正确</p>
              </div>
            </template>
          </el-step>
        </el-steps>

        <el-divider />

        <div class="config-info">
          <h4>当前配置信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="Client ID">{{ currentProvider.clientId }}</el-descriptions-item>
            <el-descriptions-item label="授权端点">{{ currentProvider.authorizationEndpoint }}</el-descriptions-item>
            <el-descriptions-item label="Token端点">{{ currentProvider.tokenEndpoint }}</el-descriptions-item>
            <el-descriptions-item label="用户信息端点">{{ currentProvider.userInfoEndpoint }}</el-descriptions-item>
            <el-descriptions-item label="Scope">{{ currentProvider.scope.join(', ') }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Connection,
  CircleCheck,
  CircleClose,
  Grid,
  Plus,
} from '@element-plus/icons-vue'
import { socialIdentityProviderApi } from '../../api/socialIdentityProvider'
import type {
  SocialIdentityProvider,
  CreateSocialIdentityProviderDto,
  UpdateSocialIdentityProviderDto,
  SocialIdentityProviderStats,
  SocialProviderType,
} from '../../types/socialIdentityProvider'
import { PROVIDER_CONFIGS as PROVIDER_CONFIGS_CONST } from '../../types/socialIdentityProvider'

const PROVIDER_CONFIGS = PROVIDER_CONFIGS_CONST

const loading = ref(false)
const providers = ref<SocialIdentityProvider[]>([])
const stats = ref<SocialIdentityProviderStats>({
  totalProviders: 0,
  activeProviders: 0,
  inactiveProviders: 0,
  byType: {},
})
const filterType = ref('')
const filterStatus = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()
const providerForm = reactive<CreateSocialIdentityProviderDto & UpdateSocialIdentityProviderDto & { id?: string }>({
  name: '',
  type: 'github' as SocialProviderType,
  clientId: '',
  clientSecret: '',
  authorizationEndpoint: '',
  tokenEndpoint: '',
  userInfoEndpoint: '',
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

const rules = {
  name: [{ required: true, message: '请输入身份源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择身份源类型', trigger: 'change' }],
  clientId: [{ required: true, message: '请输入Client ID', trigger: 'blur' }],
  clientSecret: [{ required: true, message: '请输入Client Secret', trigger: 'blur' }],
  authorizationEndpoint: [{ required: true, message: '请输入授权端点', trigger: 'blur' }],
  tokenEndpoint: [{ required: true, message: '请输入Token端点', trigger: 'blur' }],
  userInfoEndpoint: [{ required: true, message: '请输入用户信息端点', trigger: 'blur' }],
}

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

const loadProviders = async () => {
  loading.value = true
  try {
    const data = await socialIdentityProviderApi.getList({
      type: filterType.value,
      status: filterStatus.value,
    })
    providers.value = data
  } catch (error) {
    ElMessage.error('加载身份源列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (type: SocialProviderType) => {
  const configs: Record<SocialProviderType, any> = {
    wechat: {
      authorizationEndpoint: 'https://open.weixin.qq.com/connect/qrconnect',
      tokenEndpoint: 'https://api.weixin.qq.com/sns/oauth2/access_token',
      userInfoEndpoint: 'https://api.weixin.qq.com/sns/userinfo',
      scope: ['snsapi_login'],
    },
    qq: {
      authorizationEndpoint: 'https://graph.qq.com/oauth2.0/authorize',
      tokenEndpoint: 'https://graph.qq.com/oauth2.0/token',
      userInfoEndpoint: 'https://graph.qq.com/user/get_user_info',
      scope: ['get_user_info'],
    },
    feishu: {
      authorizationEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/authorize',
      tokenEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/oidc/access_token',
      userInfoEndpoint: 'https://open.feishu.cn/open-apis/authen/v1/user_info',
      scope: ['contact:user.base:readonly'],
    },
    github: {
      authorizationEndpoint: 'https://github.com/login/oauth/authorize',
      tokenEndpoint: 'https://github.com/login/oauth/access_token',
      userInfoEndpoint: 'https://api.github.com/user',
      scope: ['user:email'],
    },
    gitee: {
      authorizationEndpoint: 'https://gitee.com/oauth/authorize',
      tokenEndpoint: 'https://gitee.com/oauth/token',
      userInfoEndpoint: 'https://gitee.com/api/v5/user',
      scope: ['user_info', 'emails'],
    },
    dingtalk: {
      authorizationEndpoint: 'https://login.dingtalk.com/oauth2/auth',
      tokenEndpoint: 'https://api.dingtalk.com/v1.0/oauth2/userAccessToken',
      userInfoEndpoint: 'https://api.dingtalk.com/v1.0/contact/users/me',
      scope: ['openid'],
    },
    wechat_work: {
      authorizationEndpoint: 'https://open.work.weixin.qq.com/wwopen/sso/qrConnect',
      tokenEndpoint: 'https://qyapi.weixin.qq.com/cgi-bin/miniprogram/jscode2session',
      userInfoEndpoint: 'https://qyapi.weixin.qq.com/cgi-bin/user/get',
      scope: ['snsapi_base'],
    },
    custom: {
      authorizationEndpoint: '',
      tokenEndpoint: '',
      userInfoEndpoint: '',
      scope: [],
    },
  }

  const config = configs[type]
  if (config && !isEdit.value) {
    providerForm.authorizationEndpoint = config.authorizationEndpoint
    providerForm.tokenEndpoint = config.tokenEndpoint
    providerForm.userInfoEndpoint = config.userInfoEndpoint
    providerForm.scope = config.scope
  }
}

const getAvailableScopes = () => {
  const scopesByType: Record<SocialProviderType, string[]> = {
    wechat: ['snsapi_login', 'snsapi_userinfo'],
    qq: ['get_user_info', 'get_simple_userinfo'],
    feishu: ['contact:user.base:readonly', 'contact:user.email:readonly'],
    github: ['user', 'user:email', 'repo', 'read:org'],
    gitee: ['user_info', 'projects', 'pull_requests', 'issues'],
    dingtalk: ['openid', 'corpid', 'userid'],
    wechat_work: ['snsapi_base', 'snsapi_userinfo'],
    custom: [],
  }
  return scopesByType[providerForm.type] || []
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(providerForm, {
    name: '',
    type: 'github',
    clientId: '',
    clientSecret: '',
    authorizationEndpoint: '',
    tokenEndpoint: '',
    userInfoEndpoint: '',
    scope: [],
    attributeMapping: {},
  })
  handleTypeChange('github')
  dialogVisible.value = true
}

const handleEdit = (row: SocialIdentityProvider) => {
  isEdit.value = true
  Object.assign(providerForm, {
    id: row.id,
    name: row.name,
    type: row.type,
    clientId: row.clientId,
    clientSecret: row.clientSecret,
    authorizationEndpoint: row.authorizationEndpoint,
    tokenEndpoint: row.tokenEndpoint,
    userInfoEndpoint: row.userInfoEndpoint,
    scope: row.scope,
    attributeMapping: row.attributeMapping || {},
    status: row.status,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await socialIdentityProviderApi.update(providerForm.id!, {
          name: providerForm.name,
          clientId: providerForm.clientId,
          clientSecret: providerForm.clientSecret,
          authorizationEndpoint: providerForm.authorizationEndpoint,
          tokenEndpoint: providerForm.tokenEndpoint,
          userInfoEndpoint: providerForm.userInfoEndpoint,
          scope: providerForm.scope,
          attributeMapping: providerForm.attributeMapping,
        })
        ElMessage.success('更新成功')
      } else {
        await socialIdentityProviderApi.create({
          name: providerForm.name,
          type: providerForm.type,
          clientId: providerForm.clientId,
          clientSecret: providerForm.clientSecret,
          authorizationEndpoint: providerForm.authorizationEndpoint,
          tokenEndpoint: providerForm.tokenEndpoint,
          userInfoEndpoint: providerForm.userInfoEndpoint,
          scope: providerForm.scope,
          attributeMapping: providerForm.attributeMapping,
        })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadProviders()
      loadStats()
    } catch (error: any) {
      ElMessage.error(error.response?.data?.error || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleToggleStatus = async (row: SocialIdentityProvider) => {
  try {
    const newStatus = row.status === 'active' ? 'inactive' : 'active'
    await socialIdentityProviderApi.update(row.id, { status: newStatus })
    ElMessage.success('状态更新成功')
    loadProviders()
    loadStats()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.error || '状态更新失败')
  }
}

const handleDelete = async (row: SocialIdentityProvider) => {
  try {
    await ElMessageBox.confirm('确定要删除该身份源吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await socialIdentityProviderApi.delete(row.id)
    ElMessage.success('删除成功')
    loadProviders()
    loadStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.error || '删除失败')
    }
  }
}

const handleViewGuide = (row: SocialIdentityProvider) => {
  currentProvider.value = row
  guideDialogVisible.value = true
}

const copyCallbackUrl = () => {
  if (currentProvider.value) {
    const url = `${baseUrl.value}/auth/callback/${currentProvider.value.type}`
    navigator.clipboard.writeText(url)
    ElMessage.success('回调地址已复制到剪贴板')
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadStats()
  loadProviders()
})
</script>

<style scoped>
.social-identity-provider-management {
  padding: 20px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  margin-right: 15px;
}

.stat-icon.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.active {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-icon.inactive {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.types {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.main-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.provider-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.provider-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.guide-content {
  padding: 20px 0;
}

.step-content {
  padding: 10px 0;
}

.step-content p {
  margin: 8px 0;
  line-height: 1.6;
}

.config-info {
  margin-top: 20px;
}

.config-info h4 {
  margin-bottom: 15px;
  color: #303133;
}
</style>
