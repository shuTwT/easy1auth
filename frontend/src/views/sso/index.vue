<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { applicationApi } from '@/api/application'
import type { Application } from '@/types/application'

const loading = ref(false)
const applications = ref<Application[]>([])
const baseUrl = ref(window.location.origin)
const stats = ref({
  totalApps: 0,
  activeApps: 0,
  webApps: 0,
  spaApps: 0,
  nativeApps: 0,
  machineApps: 0
})

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await applicationApi.getList({ pageSize: 1000 })
    applications.value = res.data.applications
    
    stats.value = {
      totalApps: res.data.total,
      activeApps: applications.value.filter(app => app.status === 'active').length,
      webApps: applications.value.filter(app => app.type === 'web').length,
      spaApps: applications.value.filter(app => app.type === 'spa').length,
      nativeApps: applications.value.filter(app => app.type === 'native').length,
      machineApps: applications.value.filter(app => app.type === 'machine').length
    }
  } catch (error) {
    console.error('加载应用列表失败:', error)
    ElMessage.error('加载应用列表失败')
  } finally {
    loading.value = false
  }
}

const getTypeText = (type: string) => {
  switch (type) {
    case 'web':
      return 'Web应用'
    case 'spa':
      return '单页应用'
    case 'native':
      return '原生应用'
    case 'machine':
      return '机器对机器'
    default:
      return '未知'
  }
}

const getTypeColor = (type: string) => {
  switch (type) {
    case 'web':
      return 'primary'
    case 'spa':
      return 'success'
    case 'native':
      return 'warning'
    case 'machine':
      return 'info'
    default:
      return ''
  }
}

const getStatusText = (status: string) => {
  return status === 'active' ? '启用' : '禁用'
}

const getStatusColor = (status: string) => {
  return status === 'active' ? 'success' : 'danger'
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="sso-management">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.totalApps }}</div>
            <div class="stat-label">应用总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.activeApps }}</div>
            <div class="stat-label">启用应用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.webApps }}</div>
            <div class="stat-label">Web应用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.spaApps }}</div>
            <div class="stat-label">单页应用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.nativeApps }}</div>
            <div class="stat-label">原生应用</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value">{{ stats.machineApps }}</div>
            <div class="stat-label">机器应用</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>单点登录配置</span>
        </div>
      </template>

      <el-alert
        title="单点登录说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <p>单点登录（SSO）允许用户使用一个账号登录多个应用。系统支持 OAuth 2.0 和 OpenID Connect 协议。</p>
        <p style="margin-top: 10px;">
          <strong>OAuth 2.0 授权端点：</strong>
          <el-link type="primary" @click="copyToClipboard(`${baseUrl}/oauth2/authorize`)">
            {{ baseUrl }}/oauth2/authorize
          </el-link>
        </p>
        <p style="margin-top: 5px;">
          <strong>Token 端点：</strong>
          <el-link type="primary" @click="copyToClipboard(`${baseUrl}/oauth2/token`)">
            {{ baseUrl }}/oauth2/token
          </el-link>
        </p>
        <p style="margin-top: 5px;">
          <strong>用户信息端点：</strong>
          <el-link type="primary" @click="copyToClipboard(`${baseUrl}/oauth2/userinfo`)">
            {{ baseUrl }}/oauth2/userinfo
          </el-link>
        </p>
      </el-alert>

      <el-table :data="applications" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="应用名称" width="200" />
        <el-table-column prop="type" label="应用类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeColor(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientId" label="Client ID" width="300">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-family: monospace; font-size: 12px;">{{ row.clientId }}</span>
              <el-button size="small" text @click="copyToClipboard(row.clientId)">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="redirectUris" label="回调地址" min-width="200">
          <template #default="{ row }">
            <div v-if="Array.isArray(row.redirectUris)">
              <div v-for="(uri, index) in row.redirectUris" :key="index" style="font-size: 12px; margin-bottom: 4px;">
                {{ uri }}
              </div>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="accessTokenLifetime" label="Access Token 有效期" width="150" align="center">
          <template #default="{ row }">
            {{ row.accessTokenLifetime }} 秒
          </template>
        </el-table-column>
        <el-table-column prop="refreshTokenLifetime" label="Refresh Token 有效期" width="150" align="center">
          <template #default="{ row }">
            {{ row.refreshTokenLifetime }} 秒
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>集成指南</span>
      </template>
      
      <el-tabs>
        <el-tab-pane label="OAuth 2.0 授权码流程">
          <div class="integration-guide">
            <h4>1. 获取授权码</h4>
            <p>将用户重定向到授权端点：</p>
            <pre>{{ baseUrl }}/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=openid profile email&state=RANDOM_STATE</pre>
            
            <h4>2. 使用授权码换取 Token</h4>
            <p>向 Token 端点发送 POST 请求：</p>
            <pre>POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET</pre>
            
            <h4>3. 获取用户信息</h4>
            <p>使用 Access Token 获取用户信息：</p>
            <pre>GET {{ baseUrl }}/oauth2/userinfo
Authorization: Bearer ACCESS_TOKEN</pre>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="PKCE 安全增强">
          <div class="integration-guide">
            <h4>1. 生成 Code Verifier 和 Code Challenge</h4>
            <p>Code Verifier: 随机字符串（43-128字符）</p>
            <p>Code Challenge: BASE64URL(SHA256(code_verifier))</p>
            
            <h4>2. 授权请求</h4>
            <pre>{{ baseUrl }}/oauth2/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&response_type=code&scope=openid profile email&state=RANDOM_STATE&code_challenge=CODE_CHALLENGE&code_challenge_method=S256</pre>
            
            <h4>3. Token 请求</h4>
            <pre>POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=YOUR_REDIRECT_URI&client_id=YOUR_CLIENT_ID&code_verifier=CODE_VERIFIER</pre>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="客户端凭证流程">
          <div class="integration-guide">
            <h4>适用于机器对机器通信</h4>
            <pre>POST {{ baseUrl }}/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&scope=read write</pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.sso-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 10px 0;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.integration-guide {
  padding: 20px;
}

.integration-guide h4 {
  margin-top: 20px;
  margin-bottom: 10px;
  color: #303133;
}

.integration-guide h4:first-child {
  margin-top: 0;
}

.integration-guide p {
  margin: 8px 0;
  color: #606266;
}

.integration-guide pre {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
}
</style>
