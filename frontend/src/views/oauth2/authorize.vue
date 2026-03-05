<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const authorizing = ref(false)
const application = ref<any>(null)
const scopes = ref<string[]>([])
const error = ref('')

const client_id = route.query.client_id as string
const redirect_uri = route.query.redirect_uri as string
const response_type = route.query.response_type as string
const scope = route.query.scope as string
const state = route.query.state as string
const code_challenge = route.query.code_challenge as string
const code_challenge_method = route.query.code_challenge_method as string

const loadApplicationInfo = async () => {
  try {
    if (!client_id || !redirect_uri || !response_type) {
      error.value = '缺少必要的授权参数'
      loading.value = false
      return
    }

    const res = await request.get('/applications', {
      params: {
        page: 1,
        pageSize: 1
      }
    })

    const apps = res.data.applications || []
    const app = apps.find((a: any) => a.clientId === client_id)
    
    if (!app) {
      error.value = '应用不存在或未授权'
      loading.value = false
      return
    }

    application.value = app
    
    if (scope) {
      scopes.value = scope.split(' ')
    }

    loading.value = false
  } catch (err: any) {
    console.error('加载应用信息失败:', err)
    error.value = err.response?.data?.message || '加载应用信息失败'
    loading.value = false
  }
}

const handleAuthorize = async () => {
  try {
    authorizing.value = true

    const params = new URLSearchParams({
      client_id,
      redirect_uri,
      response_type,
      ...(scope && { scope }),
      ...(state && { state }),
      ...(code_challenge && { code_challenge }),
      ...(code_challenge_method && { code_challenge_method })
    })

    window.location.href = `/oauth2/authorize?${params.toString()}`
  } catch (err: any) {
    console.error('授权失败:', err)
    ElMessage.error(err.response?.data?.error || '授权失败')
    authorizing.value = false
  }
}

const handleDeny = () => {
  const redirectUrl = new URL(redirect_uri)
  redirectUrl.searchParams.set('error', 'access_denied')
  if (state) {
    redirectUrl.searchParams.set('state', state)
  }
  window.location.href = redirectUrl.toString()
}

onMounted(() => {
  loadApplicationInfo()
})
</script>

<template>
  <div class="oauth-authorize">
    <el-card v-loading="loading" class="authorize-card">
      <template v-if="error">
        <el-result
          icon="error"
          title="授权失败"
          :sub-title="error"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/dashboard')">返回控制台</el-button>
          </template>
        </el-result>
      </template>

      <template v-else-if="application">
        <div class="authorize-header">
          <h2>授权请求</h2>
          <p>以下应用正在请求访问您的账户</p>
        </div>

        <div class="application-info">
          <div class="app-logo">
            <el-avatar :size="64" :src="application.logo">
              {{ application.name.charAt(0).toUpperCase() }}
            </el-avatar>
          </div>
          <h3>{{ application.name }}</h3>
          <p v-if="application.description" class="app-description">
            {{ application.description }}
          </p>
        </div>

        <el-divider />

        <div class="permissions">
          <h4>该应用将获得以下权限：</h4>
          <ul class="scope-list">
            <li v-if="scopes.includes('openid')">
              <el-icon><User /></el-icon>
              <span>获取您的基本信息</span>
            </li>
            <li v-if="scopes.includes('profile')">
              <el-icon><UserFilled /></el-icon>
              <span>访问您的个人资料</span>
            </li>
            <li v-if="scopes.includes('email')">
              <el-icon><Message /></el-icon>
              <span>访问您的邮箱地址</span>
            </li>
            <li v-if="scopes.includes('phone')">
              <el-icon><Phone /></el-icon>
              <span>访问您的手机号码</span>
            </li>
            <li v-if="!scopes.length">
              <el-icon><InfoFilled /></el-icon>
              <span>基本访问权限</span>
            </li>
          </ul>
        </div>

        <el-divider />

        <div class="authorize-actions">
          <el-button @click="handleDeny" :disabled="authorizing">拒绝</el-button>
          <el-button type="primary" @click="handleAuthorize" :loading="authorizing">
            授权
          </el-button>
        </div>

        <div class="authorize-notice">
          <el-icon><WarningFilled /></el-icon>
          <span>授权后，该应用将可以访问上述信息</span>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.oauth-authorize {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.authorize-card {
  width: 100%;
  max-width: 480px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.authorize-header {
  text-align: center;
  margin-bottom: 24px;
}

.authorize-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.authorize-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.application-info {
  text-align: center;
  margin: 24px 0;
}

.app-logo {
  margin-bottom: 16px;
}

.application-info h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #303133;
}

.app-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.permissions {
  margin: 24px 0;
}

.permissions h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.scope-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.scope-list li {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 8px;
  color: #606266;
  font-size: 14px;
}

.scope-list li .el-icon {
  margin-right: 12px;
  font-size: 18px;
  color: #409eff;
}

.authorize-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 24px 0;
}

.authorize-actions .el-button {
  min-width: 120px;
}

.authorize-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 8px;
  color: #e6a23c;
  font-size: 13px;
}

.authorize-notice .el-icon {
  font-size: 16px;
}
</style>
