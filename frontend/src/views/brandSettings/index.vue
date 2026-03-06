<template>
  <div class="brand-settings">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>品牌设置</span>
          <el-button type="primary" @click="handleSave" :loading="saving">
            <el-icon><Check /></el-icon>
            保存设置
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录页面" name="loginPage">
          <el-form :model="settings.loginPage" label-width="120px">
            <el-form-item label="页面标题">
              <el-input v-model="settings.loginPage.title" placeholder="请输入登录页面标题" />
            </el-form-item>
            
            <el-form-item label="页面副标题">
              <el-input v-model="settings.loginPage.subtitle" placeholder="请输入登录页面副标题" />
            </el-form-item>
            
            <el-form-item label="Logo">
              <el-upload
                class="logo-uploader"
                action="#"
                :show-file-list="false"
                :before-upload="beforeLogoUpload"
                :http-request="handleLogoUpload"
              >
                <img v-if="settings.loginPage.logo" :src="settings.loginPage.logo" class="logo" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <el-button v-if="settings.loginPage.logo" link type="danger" @click="settings.loginPage.logo = ''">
                删除Logo
              </el-button>
            </el-form-item>
            
            <el-form-item label="背景图片">
              <el-upload
                class="bg-uploader"
                action="#"
                :show-file-list="false"
                :before-upload="beforeBgUpload"
                :http-request="handleBgUpload"
              >
                <img v-if="settings.loginPage.backgroundImage" :src="settings.loginPage.backgroundImage" class="bg-image" />
                <el-icon v-else class="bg-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <el-button v-if="settings.loginPage.backgroundImage" link type="danger" @click="settings.loginPage.backgroundImage = ''">
                删除背景图片
              </el-button>
            </el-form-item>
            
            <el-form-item label="背景颜色">
              <el-color-picker v-model="settings.loginPage.backgroundColor" />
              <el-input v-model="settings.loginPage.backgroundColor" placeholder="#f5f7fa" style="width: 200px; margin-left: 10px" />
            </el-form-item>
            
            <el-form-item label="自定义CSS">
              <el-input
                v-model="settings.loginPage.customCSS"
                type="textarea"
                :rows="10"
                placeholder="请输入自定义CSS样式"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="管理面板" name="adminPanel">
          <el-form :model="settings.adminPanel" label-width="120px">
            <el-form-item label="Logo">
              <el-upload
                class="logo-uploader"
                action="#"
                :show-file-list="false"
                :before-upload="beforeLogoUpload"
                :http-request="handleAdminLogoUpload"
              >
                <img v-if="settings.adminPanel.logo" :src="settings.adminPanel.logo" class="logo" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <el-button v-if="settings.adminPanel.logo" link type="danger" @click="settings.adminPanel.logo = ''">
                删除Logo
              </el-button>
            </el-form-item>
            
            <el-form-item label="Favicon">
              <el-upload
                class="logo-uploader"
                action="#"
                :show-file-list="false"
                :before-upload="beforeLogoUpload"
                :http-request="handleFaviconUpload"
              >
                <img v-if="settings.adminPanel.favicon" :src="settings.adminPanel.favicon" class="logo" />
                <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <el-button v-if="settings.adminPanel.favicon" link type="danger" @click="settings.adminPanel.favicon = ''">
                删除Favicon
              </el-button>
            </el-form-item>
            
            <el-form-item label="主题色">
              <el-color-picker v-model="settings.adminPanel.primaryColor" />
              <el-input v-model="settings.adminPanel.primaryColor" placeholder="#409eff" style="width: 200px; margin-left: 10px" />
            </el-form-item>
            
            <el-form-item label="头部颜色">
              <el-color-picker v-model="settings.adminPanel.headerColor" />
              <el-input v-model="settings.adminPanel.headerColor" placeholder="#ffffff" style="width: 200px; margin-left: 10px" />
            </el-form-item>
            
            <el-form-item label="侧边栏颜色">
              <el-color-picker v-model="settings.adminPanel.sidebarColor" />
              <el-input v-model="settings.adminPanel.sidebarColor" placeholder="#304156" style="width: 200px; margin-left: 10px" />
            </el-form-item>
            
            <el-form-item label="自定义CSS">
              <el-input
                v-model="settings.adminPanel.customCSS"
                type="textarea"
                :rows="10"
                placeholder="请输入自定义CSS样式"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="法律文档" name="legalDocuments">
          <el-form :model="settings.legalDocuments" label-width="120px">
            <el-form-item label="服务条款">
              <el-input
                v-model="settings.legalDocuments.termsOfService"
                type="textarea"
                :rows="15"
                placeholder="请输入服务条款内容"
              />
            </el-form-item>
            
            <el-form-item label="隐私政策">
              <el-input
                v-model="settings.legalDocuments.privacyPolicy"
                type="textarea"
                :rows="15"
                placeholder="请输入隐私政策内容"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="自定义域名" name="customDomain">
          <el-form label-width="120px">
            <el-form-item label="自定义域名">
              <el-input v-model="settings.customDomain" placeholder="请输入自定义域名，例如：login.yourcompany.com" />
              <div class="domain-help">
                <el-alert type="info" :closable="false">
                  <template #title>
                    <strong>域名配置说明</strong>
                  </template>
                  <div style="margin-top: 10px">
                    <p>1. 在您的DNS服务商处添加CNAME记录指向：easy1auth.com</p>
                    <p>2. 配置完成后，用户可以通过自定义域名访问登录页面</p>
                    <p>3. 请确保域名已正确解析，否则无法正常使用</p>
                  </div>
                </el-alert>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Plus } from '@element-plus/icons-vue'
import { brandSettingsApi } from '@/api/brandSettings'
import type { BrandSettings } from '@/types/brandSettings'

const activeTab = ref('loginPage')
const saving = ref(false)

const settings = reactive<BrandSettings>({
  loginPage: {
    title: 'Easy1Auth',
    subtitle: '企业级身份管理平台',
    backgroundColor: '#f5f7fa',
  },
  adminPanel: {
    primaryColor: '#409eff',
    headerColor: '#ffffff',
    sidebarColor: '#304156',
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
    
    // 应用管理面板样式
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
    root.style.setProperty('--el-color-primary', settings.adminPanel.primaryColor)
  }
  
  if (settings.adminPanel.headerColor) {
    root.style.setProperty('--header-bg-color', settings.adminPanel.headerColor)
  }
  
  if (settings.adminPanel.sidebarColor) {
    root.style.setProperty('--sidebar-bg-color', settings.adminPanel.sidebarColor)
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
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
}

.logo-uploader:hover {
  border-color: #409eff;
}

.logo-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.logo {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: contain;
}

.bg-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 100%;
  max-width: 600px;
  height: 300px;
}

.bg-uploader:hover {
  border-color: #409eff;
}

.bg-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100%;
  height: 300px;
  line-height: 300px;
  text-align: center;
}

.bg-image {
  width: 100%;
  height: 300px;
  display: block;
  object-fit: cover;
}

.domain-help {
  margin-top: 20px;
}

.domain-help p {
  margin: 8px 0;
  line-height: 1.6;
}
</style>
