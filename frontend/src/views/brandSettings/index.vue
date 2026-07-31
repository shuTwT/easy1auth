<template>
  <div class="brand-settings p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-foreground mb-2">品牌设置</h1>
        <p class="text-sm text-muted-foreground">自定义登录页面、管理面板样式和品牌元素</p>
      </div>
      <Button @click="handleSave" :disabled="saving">
        <Check class="size-4 mr-2" />
        保存设置
      </Button>
    </div>

    <Card>
      <CardContent class="pt-6">
        <Tabs v-model="activeTab">
          <TabsList class="mb-6">
            <TabsTrigger value="loginPage">登录页面</TabsTrigger>
            <TabsTrigger value="adminPanel">管理面板</TabsTrigger>
            <TabsTrigger value="legalDocuments">法律文档</TabsTrigger>
            <TabsTrigger value="customDomain">自定义域名</TabsTrigger>
          </TabsList>

          <TabsContent value="loginPage" class="px-2">
            <form class="max-w-[800px]">
              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">基础信息</h3>
                <div class="grid gap-4">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">页面标题</label>
                    <Input v-model="settings.loginPage.title" placeholder="请输入登录页面标题" />
                  </div>
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">页面副标题</label>
                    <Input v-model="settings.loginPage.subtitle" placeholder="请输入登录页面副标题" />
                  </div>
                </div>
              </div>

              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">品牌元素</h3>
                <div class="grid gap-4">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">Logo</label>
                    <div class="flex flex-col gap-3">
                      <Upload
                        :http-request="handleLogoUpload"
                        :before-upload="beforeLogoUpload"
                        accept="image/*"
                        :show-file-list="false"
                      >
                        <img v-if="settings.loginPage.logo" :src="settings.loginPage.logo" class="w-[120px] h-[120px] object-contain rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer" />
                        <div v-else class="w-[120px] h-[120px] flex flex-col items-center justify-center border-2 border-dashed border-border rounded-lg cursor-pointer hover:border-primary transition-colors">
                          <Plus class="size-8 text-muted-foreground mb-2" />
                          <span class="text-xs text-muted-foreground">上传Logo</span>
                        </div>
                      </Upload>
                      <Button v-if="settings.loginPage.logo" variant="link" class="text-destructive justify-start p-0 h-auto" @click="settings.loginPage.logo = ''">
                        删除Logo
                      </Button>
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">背景图片</label>
                    <div class="flex flex-col gap-3">
                      <Upload
                        :http-request="handleBgUpload"
                        :before-upload="beforeBgUpload"
                        accept="image/*"
                        :show-file-list="false"
                      >
                        <img v-if="settings.loginPage.backgroundImage" :src="settings.loginPage.backgroundImage" class="w-full max-w-[400px] h-[200px] object-cover rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer" />
                        <div v-else class="w-full max-w-[400px] h-[200px] flex flex-col items-center justify-center border-2 border-dashed border-border rounded-lg cursor-pointer hover:border-primary transition-colors">
                          <Plus class="size-8 text-muted-foreground mb-2" />
                          <span class="text-xs text-muted-foreground">上传背景图片</span>
                        </div>
                      </Upload>
                      <Button v-if="settings.loginPage.backgroundImage" variant="link" class="text-destructive justify-start p-0 h-auto" @click="settings.loginPage.backgroundImage = ''">
                        删除背景图片
                      </Button>
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">背景颜色</label>
                    <div class="flex items-center gap-3">
                      <ColorPicker v-model="settings.loginPage.backgroundColor" :presets="colorPresets.map(c => c.value)" />
                      <Input v-model="settings.loginPage.backgroundColor" placeholder="#f5f7fa" class="w-[200px]" />
                    </div>
                  </div>
                </div>
              </div>

              <div class="mb-8">
                <h3 class="text-base font-semibold mb-5">自定义样式</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">自定义CSS</label>
                  <Textarea
                    v-model="settings.loginPage.customCSS"
                    :rows="10"
                    placeholder="请输入自定义CSS样式"
                    class="font-mono text-[13px] leading-relaxed bg-muted/50"
                  />
                </div>
              </div>
            </form>
          </TabsContent>

          <TabsContent value="adminPanel" class="px-2">
            <form class="max-w-[800px]">
              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">品牌元素</h3>
                <div class="grid gap-4">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">Logo</label>
                    <div class="flex flex-col gap-3">
                      <Upload
                        :http-request="handleAdminLogoUpload"
                        :before-upload="beforeLogoUpload"
                        accept="image/*"
                        :show-file-list="false"
                      >
                        <img v-if="settings.adminPanel.logo" :src="settings.adminPanel.logo" class="w-[120px] h-[120px] object-contain rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer" />
                        <div v-else class="w-[120px] h-[120px] flex flex-col items-center justify-center border-2 border-dashed border-border rounded-lg cursor-pointer hover:border-primary transition-colors">
                          <Plus class="size-8 text-muted-foreground mb-2" />
                          <span class="text-xs text-muted-foreground">上传Logo</span>
                        </div>
                      </Upload>
                      <Button v-if="settings.adminPanel.logo" variant="link" class="text-destructive justify-start p-0 h-auto" @click="settings.adminPanel.logo = ''">
                        删除Logo
                      </Button>
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">Favicon</label>
                    <div class="flex flex-col gap-3">
                      <Upload
                        :http-request="handleFaviconUpload"
                        :before-upload="beforeLogoUpload"
                        accept="image/*"
                        :show-file-list="false"
                      >
                        <img v-if="settings.adminPanel.favicon" :src="settings.adminPanel.favicon" class="w-[120px] h-[120px] object-contain rounded-lg border-2 border-dashed border-border hover:border-primary transition-colors cursor-pointer" />
                        <div v-else class="w-[120px] h-[120px] flex flex-col items-center justify-center border-2 border-dashed border-border rounded-lg cursor-pointer hover:border-primary transition-colors">
                          <Plus class="size-8 text-muted-foreground mb-2" />
                          <span class="text-xs text-muted-foreground">上传Favicon</span>
                        </div>
                      </Upload>
                      <Button v-if="settings.adminPanel.favicon" variant="link" class="text-destructive justify-start p-0 h-auto" @click="settings.adminPanel.favicon = ''">
                        删除Favicon
                      </Button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">主题颜色</h3>
                <div class="grid gap-4">
                  <div class="grid gap-2">
                    <label class="text-sm font-medium">主题色</label>
                    <div class="flex items-center gap-3">
                      <ColorPicker v-model="settings.adminPanel.primaryColor" :presets="colorPresets.map(c => c.value)" />
                      <Input v-model="settings.adminPanel.primaryColor" placeholder="#0369A1" class="w-[200px]" />
                    </div>
                    <div class="flex items-center gap-2 mt-3">
                      <span class="text-sm text-muted-foreground">预设颜色：</span>
                      <div 
                        v-for="color in colorPresets" 
                        :key="color.value" 
                        class="w-7 h-7 rounded flex items-center justify-center cursor-pointer transition-transform hover:scale-110 border-2 border-transparent"
                        :style="{ backgroundColor: color.value }"
                        @click="settings.adminPanel.primaryColor = color.value"
                      >
                        <Check v-if="settings.adminPanel.primaryColor === color.value" class="size-3.5 text-white" />
                      </div>
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">头部颜色</label>
                    <div class="flex items-center gap-3">
                      <ColorPicker v-model="settings.adminPanel.headerColor" :presets="colorPresets.map(c => c.value)" />
                      <Input v-model="settings.adminPanel.headerColor" placeholder="#ffffff" class="w-[200px]" />
                    </div>
                  </div>

                  <div class="grid gap-2">
                    <label class="text-sm font-medium">侧边栏颜色</label>
                    <div class="flex items-center gap-3">
                      <ColorPicker v-model="settings.adminPanel.sidebarColor" :presets="colorPresets.map(c => c.value)" />
                      <Input v-model="settings.adminPanel.sidebarColor" placeholder="#0F172A" class="w-[200px]" />
                    </div>
                  </div>
                </div>
              </div>

              <div class="mb-8">
                <h3 class="text-base font-semibold mb-5">自定义样式</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">自定义CSS</label>
                  <Textarea
                    v-model="settings.adminPanel.customCSS"
                    :rows="10"
                    placeholder="请输入自定义CSS样式"
                    class="font-mono text-[13px] leading-relaxed bg-muted/50"
                  />
                </div>
              </div>
            </form>
          </TabsContent>

          <TabsContent value="legalDocuments" class="px-2">
            <form class="max-w-[800px]">
              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">服务条款</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">服务条款</label>
                  <Textarea
                    v-model="settings.legalDocuments.termsOfService"
                    :rows="15"
                    placeholder="请输入服务条款内容"
                    class="font-mono text-[13px] leading-relaxed bg-muted/50"
                  />
                </div>
              </div>

              <div class="mb-8">
                <h3 class="text-base font-semibold mb-5">隐私政策</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">隐私政策</label>
                  <Textarea
                    v-model="settings.legalDocuments.privacyPolicy"
                    :rows="15"
                    placeholder="请输入隐私政策内容"
                    class="font-mono text-[13px] leading-relaxed bg-muted/50"
                  />
                </div>
              </div>
            </form>
          </TabsContent>

          <TabsContent value="customDomain" class="px-2">
            <form class="max-w-[800px]">
              <div class="mb-8 pb-6 border-b">
                <h3 class="text-base font-semibold mb-5">域名配置</h3>
                <div class="grid gap-2">
                  <label class="text-sm font-medium">自定义域名</label>
                  <div class="flex">
                    <div class="flex items-center px-3 bg-muted border border-r-0 rounded-l-md text-sm text-muted-foreground">https://</div>
                    <Input v-model="settings.customDomain" placeholder="请输入自定义域名，例如：login.yourcompany.com" class="rounded-l-none" />
                  </div>
                </div>
              </div>

              <div class="bg-muted rounded-lg p-6 border">
                <div class="flex items-center gap-2 mb-4">
                  <Info class="size-5 text-primary" />
                  <span class="text-base font-semibold">域名配置说明</span>
                </div>
                <div class="flex flex-col gap-3">
                  <div class="flex items-start gap-3">
                    <div class="w-6 h-6 rounded-full bg-primary text-white flex items-center justify-center text-xs font-semibold shrink-0">1</div>
                    <span class="text-sm text-muted-foreground leading-6">在您的DNS服务商处添加CNAME记录指向：easy1auth.com</span>
                  </div>
                  <div class="flex items-start gap-3">
                    <div class="w-6 h-6 rounded-full bg-primary text-white flex items-center justify-center text-xs font-semibold shrink-0">2</div>
                    <span class="text-sm text-muted-foreground leading-6">配置完成后，用户可以通过自定义域名访问登录页面</span>
                  </div>
                  <div class="flex items-start gap-3">
                    <div class="w-6 h-6 rounded-full bg-primary text-white flex items-center justify-center text-xs font-semibold shrink-0">3</div>
                    <span class="text-sm text-muted-foreground leading-6">请确保域名已正确解析，否则无法正常使用</span>
                  </div>
                </div>
              </div>
            </form>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message } from 'antdv-next'
import { Check, Plus, Info } from '@lucide/vue'
import { Button } from '@/components/antd-compat'
import { Input } from '@/components/antd-compat'
import { Textarea } from '@/components/antd-compat'
import { Card, CardContent } from '@/components/antd-compat'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/antd-compat'
import { ColorPicker } from '@/components/antd-compat'
import { Upload, type UploadFile } from '@/components/antd-compat'
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
    message.error('加载品牌设置失败')
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await brandSettingsApi.update(settings)
    message.success('保存成功')
    
    applyAdminPanelStyles()
  } catch (error: any) {
    console.error('保存品牌设置失败:', error)
    message.error(error.response?.data?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

const beforeLogoUpload = (file: UploadFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const beforeBgUpload = (file: UploadFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleLogoUpload = async (options: { file: { raw: File } }) => {
  return new Promise<void>((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.loginPage.logo = e.target?.result as string
      resolve()
    }
    reader.readAsDataURL(options.file.raw)
  })
}

const handleBgUpload = async (options: { file: { raw: File } }) => {
  return new Promise<void>((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.loginPage.backgroundImage = e.target?.result as string
      resolve()
    }
    reader.readAsDataURL(options.file.raw)
  })
}

const handleAdminLogoUpload = async (options: { file: { raw: File } }) => {
  return new Promise<void>((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.adminPanel.logo = e.target?.result as string
      resolve()
    }
    reader.readAsDataURL(options.file.raw)
  })
}

const handleFaviconUpload = async (options: { file: { raw: File } }) => {
  return new Promise<void>((resolve) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      settings.adminPanel.favicon = e.target?.result as string
      resolve()
    }
    reader.readAsDataURL(options.file.raw)
  })
}

const applyAdminPanelStyles = () => {
  const root = document.documentElement
  
  if (settings.adminPanel.primaryColor) {
        root.style.setProperty('--primary-color', settings.adminPanel.primaryColor)
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
