<template>
  <div class="brand-settings">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">品牌设置</h1>
        <p class="page-subtitle">自定义登录页面、管理面板样式和品牌元素</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>
          保存设置
        </el-button>
      </div>
    </div>

    <el-card class="settings-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <el-tab-pane label="登录页面" name="loginPage">
          <div class="tab-content">
            <el-form :model="settings.loginPage" label-width="120px" class="settings-form">
              <div class="form-section">
                <h3 class="section-title">基础信息</h3>
                <el-form-item label="页面标题">
                  <el-input v-model="settings.loginPage.title" placeholder="请输入登录页面标题" />
                </el-form-item>
                
                <el-form-item label="页面副标题">
                  <el-input v-model="settings.loginPage.subtitle" placeholder="请输入登录页面副标题" />
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">品牌元素</h3>
                <el-form-item label="Logo">
                  <div class="upload-container">
                    <el-upload
                      class="logo-uploader"
                      action="#"
                      :show-file-list="false"
                      :before-upload="beforeLogoUpload"
                      :http-request="handleLogoUpload"
                    >
                      <img v-if="settings.loginPage.logo" :src="settings.loginPage.logo" class="logo-preview" />
                      <div v-else class="upload-placeholder">
                        <el-icon class="upload-icon"><Plus /></el-icon>
                        <span class="upload-text">上传Logo</span>
                      </div>
                    </el-upload>
                    <el-button v-if="settings.loginPage.logo" link type="danger" @click="settings.loginPage.logo = ''" class="delete-btn">
                      删除Logo
                    </el-button>
                  </div>
                </el-form-item>
                
                <el-form-item label="背景图片">
                  <div class="upload-container">
                    <el-upload
                      class="bg-uploader"
                      action="#"
                      :show-file-list="false"
                      :before-upload="beforeBgUpload"
                      :http-request="handleBgUpload"
                    >
                      <img v-if="settings.loginPage.backgroundImage" :src="settings.loginPage.backgroundImage" class="bg-preview" />
                      <div v-else class="upload-placeholder bg-placeholder">
                        <el-icon class="upload-icon"><Plus /></el-icon>
                        <span class="upload-text">上传背景图片</span>
                      </div>
                    </el-upload>
                    <el-button v-if="settings.loginPage.backgroundImage" link type="danger" @click="settings.loginPage.backgroundImage = ''" class="delete-btn">
                      删除背景图片
                    </el-button>
                  </div>
                </el-form-item>
                
                <el-form-item label="背景颜色">
                  <div class="color-input">
                    <el-color-picker v-model="settings.loginPage.backgroundColor" />
                    <el-input v-model="settings.loginPage.backgroundColor" placeholder="#f5f7fa" style="width: 200px" />
                  </div>
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">自定义样式</h3>
                <el-form-item label="自定义CSS">
                  <el-input
                    v-model="settings.loginPage.customCSS"
                    type="textarea"
                    :rows="10"
                    placeholder="请输入自定义CSS样式"
                    class="code-input"
                  />
                </el-form-item>
              </div>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="管理面板" name="adminPanel">
          <div class="tab-content">
            <el-form :model="settings.adminPanel" label-width="120px" class="settings-form">
              <div class="form-section">
                <h3 class="section-title">品牌元素</h3>
                <el-form-item label="Logo">
                  <div class="upload-container">
                    <el-upload
                      class="logo-uploader"
                      action="#"
                      :show-file-list="false"
                      :before-upload="beforeLogoUpload"
                      :http-request="handleAdminLogoUpload"
                    >
                      <img v-if="settings.adminPanel.logo" :src="settings.adminPanel.logo" class="logo-preview" />
                      <div v-else class="upload-placeholder">
                        <el-icon class="upload-icon"><Plus /></el-icon>
                        <span class="upload-text">上传Logo</span>
                      </div>
                    </el-upload>
                    <el-button v-if="settings.adminPanel.logo" link type="danger" @click="settings.adminPanel.logo = ''" class="delete-btn">
                      删除Logo
                    </el-button>
                  </div>
                </el-form-item>
                
                <el-form-item label="Favicon">
                  <div class="upload-container">
                    <el-upload
                      class="logo-uploader"
                      action="#"
                      :show-file-list="false"
                      :before-upload="beforeLogoUpload"
                      :http-request="handleFaviconUpload"
                    >
                      <img v-if="settings.adminPanel.favicon" :src="settings.adminPanel.favicon" class="logo-preview" />
                      <div v-else class="upload-placeholder">
                        <el-icon class="upload-icon"><Plus /></el-icon>
                        <span class="upload-text">上传Favicon</span>
                      </div>
                    </el-upload>
                    <el-button v-if="settings.adminPanel.favicon" link type="danger" @click="settings.adminPanel.favicon = ''" class="delete-btn">
                      删除Favicon
                    </el-button>
                  </div>
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">主题颜色</h3>
                <el-form-item label="主题色">
                  <div class="color-input">
                    <el-color-picker v-model="settings.adminPanel.primaryColor" />
                    <el-input v-model="settings.adminPanel.primaryColor" placeholder="#0369A1" style="width: 200px" />
                  </div>
                  <div class="color-presets">
                    <span class="preset-label">预设颜色：</span>
                    <div 
                      v-for="color in colorPresets" 
                      :key="color.value" 
                      class="color-preset"
                      :style="{ backgroundColor: color.value }"
                      @click="settings.adminPanel.primaryColor = color.value"
                    >
                      <el-icon v-if="settings.adminPanel.primaryColor === color.value" class="check-icon"><Check /></el-icon>
                    </div>
                  </div>
                </el-form-item>
                
                <el-form-item label="头部颜色">
                  <div class="color-input">
                    <el-color-picker v-model="settings.adminPanel.headerColor" />
                    <el-input v-model="settings.adminPanel.headerColor" placeholder="#ffffff" style="width: 200px" />
                  </div>
                </el-form-item>
                
                <el-form-item label="侧边栏颜色">
                  <div class="color-input">
                    <el-color-picker v-model="settings.adminPanel.sidebarColor" />
                    <el-input v-model="settings.adminPanel.sidebarColor" placeholder="#0F172A" style="width: 200px" />
                  </div>
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">自定义样式</h3>
                <el-form-item label="自定义CSS">
                  <el-input
                    v-model="settings.adminPanel.customCSS"
                    type="textarea"
                    :rows="10"
                    placeholder="请输入自定义CSS样式"
                    class="code-input"
                  />
                </el-form-item>
              </div>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="法律文档" name="legalDocuments">
          <div class="tab-content">
            <el-form :model="settings.legalDocuments" label-width="120px" class="settings-form">
              <div class="form-section">
                <h3 class="section-title">服务条款</h3>
                <el-form-item label="服务条款">
                  <el-input
                    v-model="settings.legalDocuments.termsOfService"
                    type="textarea"
                    :rows="15"
                    placeholder="请输入服务条款内容"
                    class="code-input"
                  />
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">隐私政策</h3>
                <el-form-item label="隐私政策">
                  <el-input
                    v-model="settings.legalDocuments.privacyPolicy"
                    type="textarea"
                    :rows="15"
                    placeholder="请输入隐私政策内容"
                    class="code-input"
                  />
                </el-form-item>
              </div>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="自定义域名" name="customDomain">
          <div class="tab-content">
            <el-form label-width="120px" class="settings-form">
              <div class="form-section">
                <h3 class="section-title">域名配置</h3>
                <el-form-item label="自定义域名">
                  <el-input v-model="settings.customDomain" placeholder="请输入自定义域名，例如：login.yourcompany.com">
                    <template #prepend>https://</template>
                  </el-input>
                </el-form-item>
              </div>

              <div class="form-section">
                <div class="domain-help-card">
                  <div class="help-header">
                    <el-icon class="help-icon"><InfoFilled /></el-icon>
                    <span class="help-title">域名配置说明</span>
                  </div>
                  <div class="help-content">
                    <div class="help-step">
                      <span class="step-number">1</span>
                      <span class="step-text">在您的DNS服务商处添加CNAME记录指向：easy1auth.com</span>
                    </div>
                    <div class="help-step">
                      <span class="step-number">2</span>
                      <span class="step-text">配置完成后，用户可以通过自定义域名访问登录页面</span>
                    </div>
                    <div class="help-step">
                      <span class="step-number">3</span>
                      <span class="step-text">请确保域名已正确解析，否则无法正常使用</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Plus, InfoFilled } from '@element-plus/icons-vue'
import { brandSettingsApi } from '@/api/brandSettings'
import type { BrandSettings } from '@/types/brandSettings'

const activeTab = ref('loginPage')
const saving = ref(false)

const colorPresets = [
  { name: '默认蓝', value: '#0369A1' },
  { name: '翠绿', value: '#10B981' },
  { name: '紫罗兰', value: '#7C3AED' },
  { name: '橙红', value: '#F97316' },
  { name: '玫红', value: '#EC4899' },
  { name: '青色', value: '#06B6D4' },
]

const settings = reactive<BrandSettings>({
  loginPage: {
    title: 'Easy1Auth',
    subtitle: '企业级身份管理平台',
    backgroundColor: '#f5f7fa',
  },
  adminPanel: {
    primaryColor: '#0369A1',
    headerColor: '#ffffff',
    sidebarColor: '#0F172A',
  },
  legalDocuments: {},
})

const loadSettings = async () => {
  try {
    const response = await brandSettingsApi.get()
    Object.assign(settings, response.brandSettings)
  } catch (error) {
    console.error('加载品牌设置失败:', error)
    ElMessage.error('加载品牌设置失败')
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await brandSettingsApi.update(settings)
    ElMessage.success('保存成功')
    
    applyAdminPanelStyles()
  } catch (error: any) {
    console.error('保存品牌设置失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const beforeLogoUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const beforeBgUpload = (file: any) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleLogoUpload = async (options: any) => {
  try {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.loginPage.logo = e.target?.result as string
    }
    reader.readAsDataURL(options.file)
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const handleBgUpload = async (options: any) => {
  try {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.loginPage.backgroundImage = e.target?.result as string
    }
    reader.readAsDataURL(options.file)
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const handleAdminLogoUpload = async (options: any) => {
  try {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.adminPanel.logo = e.target?.result as string
    }
    reader.readAsDataURL(options.file)
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const handleFaviconUpload = async (options: any) => {
  try {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.adminPanel.favicon = e.target?.result as string
    }
    reader.readAsDataURL(options.file)
  } catch (error) {
    ElMessage.error('上传失败')
  }
}

const applyAdminPanelStyles = () => {
  const root = document.documentElement
  
  if (settings.adminPanel.primaryColor) {
    root.style.setProperty('--primary-color', settings.adminPanel.primaryColor)
    root.style.setProperty('--el-color-primary', settings.adminPanel.primaryColor)
  }
  
  if (settings.adminPanel.headerColor) {
    root.style.setProperty('--header-bg', settings.adminPanel.headerColor)
  }
  
  if (settings.adminPanel.sidebarColor) {
    root.style.setProperty('--sidebar-bg', settings.adminPanel.sidebarColor)
  }
  
  if (settings.adminPanel.customCSS) {
    let styleElement = document.getElementById('custom-admin-styles')
    if (!styleElement) {
      styleElement = document.createElement('style')
      styleElement.id = 'custom-admin-styles'
      document.head.appendChild(styleElement)
    }
    styleElement.textContent = settings.adminPanel.customCSS
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.brand-settings {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.settings-card {
  border-radius: var(--border-radius-lg);
}

.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.settings-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border-color);
}

.settings-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  padding: 0 20px;
  height: 44px;
  line-height: 44px;
}

.settings-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
}

.settings-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 2px;
}

.tab-content {
  padding: 0 8px;
}

.settings-form {
  max-width: 800px;
}

.form-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.form-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 20px 0;
}

.upload-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.logo-uploader {
  width: 120px;
  height: 120px;
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  overflow: hidden;
}

.logo-uploader:hover {
  border-color: var(--primary-color);
}

.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.upload-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 12px;
}

.logo-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.bg-uploader {
  width: 100%;
  max-width: 400px;
  height: 200px;
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  overflow: hidden;
}

.bg-uploader:hover {
  border-color: var(--primary-color);
}

.bg-placeholder {
  width: 100%;
  height: 100%;
}

.bg-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.delete-btn {
  align-self: flex-start;
}

.color-input {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-presets {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.preset-label {
  font-size: 13px;
  color: var(--text-muted);
}

.color-preset {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.color-preset:hover {
  transform: scale(1.1);
}

.color-preset .check-icon {
  color: white;
  font-size: 14px;
}

.code-input :deep(.el-textarea__inner) {
  font-family: 'Fira Code', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  background-color: #F8FAFC;
  border-radius: var(--border-radius);
}

.domain-help-card {
  background: linear-gradient(135deg, #F0F9FF 0%, #E0F2FE 100%);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  border: 1px solid rgba(3, 105, 161, 0.1);
}

.help-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.help-icon {
  font-size: 20px;
  color: var(--primary-color);
}

.help-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.help-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.help-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.step-number {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--primary-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 24px;
}
</style>
