<template>
  <div class="personalization-settings">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">个性化设置</h1>
        <p class="page-subtitle">自定义域名、登录页面样式和消息模板</p>
      </div>
    </div>

    <el-card class="settings-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <el-tab-pane label="自定义域名" name="domains">
          <div class="tab-content">
            <div class="section-header">
              <h3 class="section-title">域名管理</h3>
              <el-button type="primary" @click="showDomainDialog = true">
                <el-icon><Plus /></el-icon>
                添加域名
              </el-button>
            </div>

            <el-table :data="domains" v-loading="domainsLoading" class="domain-table">
              <el-table-column prop="domain" label="域名" min-width="200">
                <template #default="{ row }">
                  <div class="domain-cell">
                    <span class="domain-name">{{ row.domain }}</span>
                    <el-tag v-if="row.status === 'verified'" type="success" size="small">已验证</el-tag>
                    <el-tag v-else-if="row.status === 'pending'" type="warning" size="small">待验证</el-tag>
                    <el-tag v-else-if="row.status === 'verifying'" type="info" size="small">验证中</el-tag>
                    <el-tag v-else type="danger" size="small">验证失败</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="sslStatus" label="SSL状态" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.sslStatus === 'active'" type="success" size="small">已配置</el-tag>
                  <el-tag v-else type="info" size="small">未配置</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="verificationMethod" label="验证方式" width="100">
                <template #default="{ row }">
                  {{ row.verificationMethod === 'dns' ? 'DNS记录' : '文件验证' }}
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    v-if="row.status !== 'verified'" 
                    link 
                    type="primary" 
                    @click="handleVerifyDomain(row)"
                  >
                    验证
                  </el-button>
                  <el-button 
                    v-if="row.status === 'verified'" 
                    link 
                    type="primary" 
                    @click="handleShowSSLDialog(row)"
                  >
                    配置SSL
                  </el-button>
                  <el-button link type="danger" @click="handleDeleteDomain(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="help-section">
              <el-alert type="info" :closable="false">
                <template #title>
                  <div class="alert-title">
                    <el-icon><InfoFilled /></el-icon>
                    域名验证说明
                  </div>
                </template>
                <div class="help-steps">
                  <p><strong>DNS验证：</strong>在DNS服务商添加TXT记录，主机记录为 @ 或空，记录值为验证令牌</p>
                  <p><strong>文件验证：</strong>在网站根目录创建 .well-known/easy1auth-verification.txt 文件，内容为验证令牌</p>
                </div>
              </el-alert>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="登录页面" name="loginStyle">
          <div class="tab-content">
            <el-form :model="loginStyle" label-width="120px" class="settings-form">
              <div class="form-section">
                <h3 class="section-title">基础信息</h3>
                <el-form-item label="页面标题">
                  <el-input v-model="loginStyle.title" placeholder="请输入登录页面标题" />
                </el-form-item>
                <el-form-item label="页面副标题">
                  <el-input v-model="loginStyle.subtitle" placeholder="请输入登录页面副标题" />
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
                      :http-request="(options: any) => handleLogoUpload(options, 'logo')"
                    >
                      <img v-if="loginStyle.logo" :src="loginStyle.logo" class="logo-preview" />
                      <div v-else class="upload-placeholder">
                        <el-icon class="upload-icon"><Plus /></el-icon>
                        <span class="upload-text">上传Logo</span>
                      </div>
                    </el-upload>
                    <el-button v-if="loginStyle.logo" link type="danger" @click="loginStyle.logo = undefined">
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
                      :http-request="(options: any) => handleLogoUpload(options, 'backgroundImage')"
                    >
                      <img v-if="loginStyle.backgroundImage" :src="loginStyle.backgroundImage" class="bg-preview" />
                      <div v-else class="upload-placeholder bg-placeholder">
                        <el-icon class="upload-icon"><Plus /></el-icon>
                        <span class="upload-text">上传背景图片</span>
                      </div>
                    </el-upload>
                    <el-button v-if="loginStyle.backgroundImage" link type="danger" @click="loginStyle.backgroundImage = undefined">
                      删除背景图片
                    </el-button>
                  </div>
                </el-form-item>

                <el-form-item label="背景颜色">
                  <div class="color-input">
                    <el-color-picker v-model="loginStyle.backgroundColor" />
                    <el-input v-model="loginStyle.backgroundColor" placeholder="#f5f7fa" style="width: 200px" />
                  </div>
                </el-form-item>

                <el-form-item label="主题色">
                  <div class="color-input">
                    <el-color-picker v-model="loginStyle.primaryColor" />
                    <el-input v-model="loginStyle.primaryColor" placeholder="#0369A1" style="width: 200px" />
                  </div>
                  <div class="color-presets">
                    <span class="preset-label">预设颜色：</span>
                    <div 
                      v-for="color in colorPresets" 
                      :key="color.value" 
                      class="color-preset"
                      :style="{ backgroundColor: color.value }"
                      @click="loginStyle.primaryColor = color.value"
                    >
                      <el-icon v-if="loginStyle.primaryColor === color.value" class="check-icon"><Check /></el-icon>
                    </div>
                  </div>
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">登录方式</h3>
                <el-form-item label="启用方式">
                  <el-checkbox-group v-model="loginStyle.loginMethods">
                    <el-checkbox label="password">账号密码</el-checkbox>
                    <el-checkbox label="email">邮箱验证码</el-checkbox>
                    <el-checkbox label="passkey">Passkey</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
              </div>

              <div class="form-section">
                <h3 class="section-title">自定义样式</h3>
                <el-form-item label="自定义CSS">
                  <el-input
                    v-model="loginStyle.customCSS"
                    type="textarea"
                    :rows="8"
                    placeholder="请输入自定义CSS样式"
                    class="code-input"
                  />
                </el-form-item>
              </div>

              <el-form-item>
                <el-button type="primary" @click="handleSaveLoginStyle" :loading="savingLoginStyle">
                  保存设置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="消息模板" name="templates">
          <div class="tab-content">
            <div class="section-header">
              <h3 class="section-title">模板管理</h3>
              <div class="header-actions">
                <el-button @click="handleInitTemplates" :loading="initingTemplates">
                  初始化默认模板
                </el-button>
                <el-button type="primary" @click="showTemplateDialog = true">
                  <el-icon><Plus /></el-icon>
                  新建模板
                </el-button>
              </div>
            </div>

            <el-tabs v-model="templateType" class="template-type-tabs">
              <el-tab-pane label="邮件模板" name="email">
                <el-table :data="emailTemplates" v-loading="templatesLoading">
                  <el-table-column prop="name" label="模板名称" width="150" />
                  <el-table-column prop="code" label="模板代码" width="150" />
                  <el-table-column prop="subject" label="邮件主题" min-width="200" />
                  <el-table-column prop="isDefault" label="默认" width="80">
                    <template #default="{ row }">
                      <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
                      <el-tag v-else type="info" size="small">否</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="80">
                    <template #default="{ row }">
                      <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
                      <el-tag v-else type="info" size="small">禁用</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="150" fixed="right">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="handleEditTemplate(row)">编辑</el-button>
                      <el-button link type="danger" @click="handleDeleteTemplate(row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="短信模板" name="sms">
                <el-table :data="smsTemplates" v-loading="templatesLoading">
                  <el-table-column prop="name" label="模板名称" width="150" />
                  <el-table-column prop="code" label="模板代码" width="150" />
                  <el-table-column prop="content" label="模板内容" min-width="300" show-overflow-tooltip />
                  <el-table-column prop="isDefault" label="默认" width="80">
                    <template #default="{ row }">
                      <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
                      <el-tag v-else type="info" size="small">否</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="80">
                    <template #default="{ row }">
                      <el-tag v-if="row.status === 'active'" type="success" size="small">启用</el-tag>
                      <el-tag v-else type="info" size="small">禁用</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="150" fixed="right">
                    <template #default="{ row }">
                      <el-button link type="primary" @click="handleEditTemplate(row)">编辑</el-button>
                      <el-button link type="danger" @click="handleDeleteTemplate(row)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showDomainDialog" title="添加域名" width="500px">
      <el-form :model="domainForm" label-width="100px">
        <el-form-item label="域名">
          <el-input v-model="domainForm.domain" placeholder="例如：login.yourcompany.com">
            <template #prepend>https://</template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证方式">
          <el-radio-group v-model="domainForm.verificationMethod">
            <el-radio label="dns">DNS记录验证</el-radio>
            <el-radio label="file">文件验证</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDomainDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateDomain" :loading="creatingDomain">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSSLDialog" title="配置SSL证书" width="600px">
      <el-form :model="sslForm" label-width="120px">
        <el-form-item label="SSL证书">
          <el-input
            v-model="sslForm.sslCertificate"
            type="textarea"
            :rows="8"
            placeholder="请粘贴SSL证书内容（PEM格式）"
          />
        </el-form-item>
        <el-form-item label="私钥">
          <el-input
            v-model="sslForm.sslPrivateKey"
            type="textarea"
            :rows="8"
            placeholder="请粘贴SSL私钥内容（PEM格式）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSSLDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSSL" :loading="savingSSL">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTemplateDialog" :title="editingTemplate ? '编辑模板' : '新建模板'" width="700px">
      <el-form :model="templateForm" label-width="100px">
        <el-form-item label="模板类型">
          <el-radio-group v-model="templateForm.type" :disabled="!!editingTemplate">
            <el-radio label="email">邮件</el-radio>
            <el-radio label="sms">短信</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模板代码">
          <el-input v-model="templateForm.code" placeholder="例如：verification_code" :disabled="!!editingTemplate" />
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item v-if="templateForm.type === 'email'" label="邮件主题">
          <el-input v-model="templateForm.subject" placeholder="请输入邮件主题，支持变量如 {{appName}}" />
        </el-form-item>
        <el-form-item label="模板内容">
          <el-input
            v-model="templateForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入模板内容，支持变量如 {{code}}, {{username}} 等"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="templateForm.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTemplateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveTemplate" :loading="savingTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showVerifyDialog" title="域名验证" width="600px">
      <div class="verify-content">
        <el-alert type="info" :closable="false" class="verify-alert">
          <template #title>
            <span>请按照以下步骤完成验证</span>
          </template>
        </el-alert>
        <div v-if="verifyingDomain?.verificationMethod === 'dns'" class="verify-steps">
          <h4>DNS记录验证</h4>
          <p>在您的DNS服务商处添加以下TXT记录：</p>
          <div class="code-block">
            <p><strong>记录类型：</strong>TXT</p>
            <p><strong>主机记录：</strong>@ 或 留空</p>
            <p><strong>记录值：</strong></p>
            <pre>{{ verifyingDomain?.verificationToken }}</pre>
          </div>
        </div>
        <div v-else class="verify-steps">
          <h4>文件验证</h4>
          <p>在您的网站根目录创建以下文件：</p>
          <div class="code-block">
            <p><strong>文件路径：</strong>/.well-known/easy1auth-verification.txt</p>
            <p><strong>文件内容：</strong></p>
            <pre>{{ verifyingDomain?.verificationToken?.split('=')[1] }}</pre>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showVerifyDialog = false">取消</el-button>
        <el-button type="primary" @click="handleVerifyDomainConfirm" :loading="verifying">验证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Check, InfoFilled } from '@element-plus/icons-vue'
import { customDomainApi, type CustomDomain, type CreateDomainDto, type UpdateSSLDto } from '@/api/customDomain'
import { messageTemplateApi, type MessageTemplate, type CreateTemplateDto, type UpdateTemplateDto } from '@/api/messageTemplate'
import { loginStyleApi, type LoginStyle, type UpdateLoginStyleDto } from '@/api/loginStyle'

const activeTab = ref('domains')
const templateType = ref('email')

const domains = ref<CustomDomain[]>([])
const domainsLoading = ref(false)
const showDomainDialog = ref(false)
const creatingDomain = ref(false)
const domainForm = reactive<CreateDomainDto>({
  domain: '',
  verificationMethod: 'dns',
})

const showSSLDialog = ref(false)
const savingSSL = ref(false)
const sslForm = reactive<UpdateSSLDto>({
  sslCertificate: '',
  sslPrivateKey: '',
})
const currentDomainId = ref('')

const showVerifyDialog = ref(false)
const verifying = ref(false)
const verifyingDomain = ref<CustomDomain | null>(null)

const loginStyle = reactive<UpdateLoginStyleDto>({
  title: 'Easy1Auth',
  subtitle: '企业级身份管理平台',
  backgroundColor: '#f5f7fa',
  primaryColor: '#0369A1',
  loginMethods: ['password', 'email', 'passkey'],
})
const savingLoginStyle = ref(false)

const templates = ref<MessageTemplate[]>([])
const templatesLoading = ref(false)
const showTemplateDialog = ref(false)
const savingTemplate = ref(false)
const editingTemplate = ref<MessageTemplate | null>(null)
const templateForm = reactive<CreateTemplateDto>({
  type: 'email',
  code: '',
  name: '',
  subject: '',
  content: '',
  isDefault: false,
})
const initingTemplates = ref(false)

const emailTemplates = computed(() => templates.value.filter(t => t.type === 'email'))
const smsTemplates = computed(() => templates.value.filter(t => t.type === 'sms'))

const colorPresets = [
  { name: '默认蓝', value: '#0369A1' },
  { name: '翠绿', value: '#10B981' },
  { name: '紫罗兰', value: '#7C3AED' },
  { name: '橙红', value: '#F97316' },
  { name: '玫红', value: '#EC4899' },
  { name: '青色', value: '#06B6D4' },
]

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const loadDomains = async () => {
  domainsLoading.value = true
  try {
    const response = await customDomainApi.list()
    domains.value = response.data.data
  } catch (error) {
    console.error('加载域名列表失败:', error)
    ElMessage.error('加载域名列表失败')
  } finally {
    domainsLoading.value = false
  }
}

const handleCreateDomain = async () => {
  if (!domainForm.domain) {
    ElMessage.warning('请输入域名')
    return
  }

  creatingDomain.value = true
  try {
    await customDomainApi.create(domainForm)
    ElMessage.success('域名添加成功，请完成验证')
    showDomainDialog.value = false
    domainForm.domain = ''
    domainForm.verificationMethod = 'dns'
    loadDomains()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '添加域名失败')
  } finally {
    creatingDomain.value = false
  }
}

const handleVerifyDomain = (domain: CustomDomain) => {
  verifyingDomain.value = domain
  showVerifyDialog.value = true
}

const handleVerifyDomainConfirm = async () => {
  if (!verifyingDomain.value) return

  verifying.value = true
  try {
    await customDomainApi.verify(verifyingDomain.value.id)
    ElMessage.success('域名验证成功')
    showVerifyDialog.value = false
    verifyingDomain.value = null
    loadDomains()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '域名验证失败')
  } finally {
    verifying.value = false
  }
}

const handleShowSSLDialog = (domain: CustomDomain) => {
  currentDomainId.value = domain.id
  sslForm.sslCertificate = ''
  sslForm.sslPrivateKey = ''
  showSSLDialog.value = true
}

const handleSaveSSL = async () => {
  if (!sslForm.sslCertificate || !sslForm.sslPrivateKey) {
    ElMessage.warning('请填写SSL证书和私钥')
    return
  }

  savingSSL.value = true
  try {
    await customDomainApi.updateSSL(currentDomainId.value, sslForm)
    ElMessage.success('SSL证书配置成功')
    showSSLDialog.value = false
    loadDomains()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '配置SSL证书失败')
  } finally {
    savingSSL.value = false
  }
}

const handleDeleteDomain = async (domain: CustomDomain) => {
  try {
    await ElMessageBox.confirm('确定要删除该域名吗？', '提示', {
      type: 'warning',
    })
    await customDomainApi.delete(domain.id)
    ElMessage.success('域名删除成功')
    loadDomains()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除域名失败')
    }
  }
}

const loadLoginStyle = async () => {
  try {
    const response = await loginStyleApi.get()
    Object.assign(loginStyle, response.data.data)
  } catch (error) {
    console.error('加载登录样式失败:', error)
  }
}

const handleSaveLoginStyle = async () => {
  savingLoginStyle.value = true
  try {
    await loginStyleApi.update(loginStyle)
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    savingLoginStyle.value = false
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

const handleLogoUpload = (options: any, field: 'logo' | 'logoDark' | 'backgroundImage') => {
  const reader = new FileReader()
  reader.onload = (e) => {
    (loginStyle as any)[field] = e.target?.result as string
  }
  reader.readAsDataURL(options.file)
}

const loadTemplates = async () => {
  templatesLoading.value = true
  try {
    const response = await messageTemplateApi.list()
    templates.value = response.data.data
  } catch (error) {
    console.error('加载模板列表失败:', error)
    ElMessage.error('加载模板列表失败')
  } finally {
    templatesLoading.value = false
  }
}

const handleInitTemplates = async () => {
  initingTemplates.value = true
  try {
    await messageTemplateApi.initDefaults()
    ElMessage.success('默认模板初始化成功')
    loadTemplates()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '初始化模板失败')
  } finally {
    initingTemplates.value = false
  }
}

const handleEditTemplate = (template: MessageTemplate) => {
  editingTemplate.value = template
  templateForm.type = template.type
  templateForm.code = template.code
  templateForm.name = template.name
  templateForm.subject = template.subject || ''
  templateForm.content = template.content
  templateForm.isDefault = template.isDefault
  showTemplateDialog.value = true
}

const handleSaveTemplate = async () => {
  if (!templateForm.code || !templateForm.name || !templateForm.content) {
    ElMessage.warning('请填写完整信息')
    return
  }

  savingTemplate.value = true
  try {
    if (editingTemplate.value) {
      await messageTemplateApi.update(editingTemplate.value.id, {
        name: templateForm.name,
        subject: templateForm.subject,
        content: templateForm.content,
        isDefault: templateForm.isDefault,
      })
      ElMessage.success('模板更新成功')
    } else {
      await messageTemplateApi.create(templateForm)
      ElMessage.success('模板创建成功')
    }
    showTemplateDialog.value = false
    editingTemplate.value = null
    templateForm.type = 'email'
    templateForm.code = ''
    templateForm.name = ''
    templateForm.subject = ''
    templateForm.content = ''
    templateForm.isDefault = false
    loadTemplates()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存模板失败')
  } finally {
    savingTemplate.value = false
  }
}

const handleDeleteTemplate = async (template: MessageTemplate) => {
  try {
    await ElMessageBox.confirm('确定要删除该模板吗？', '提示', {
      type: 'warning',
    })
    await messageTemplateApi.delete(template.id)
    ElMessage.success('模板删除成功')
    loadTemplates()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除模板失败')
    }
  }
}

onMounted(() => {
  loadDomains()
  loadLoginStyle()
  loadTemplates()
})
</script>

<style scoped>
.personalization-settings {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.page-header {
  margin-bottom: 24px;
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

.tab-content {
  padding: 0 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.domain-table {
  margin-bottom: 24px;
}

.domain-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.domain-name {
  font-weight: 500;
}

.help-section {
  margin-top: 16px;
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.help-steps {
  margin-top: 12px;
}

.help-steps p {
  margin: 8px 0;
  font-size: 13px;
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

.template-type-tabs {
  margin-top: 16px;
}

.verify-content {
  padding: 16px 0;
}

.verify-alert {
  margin-bottom: 20px;
}

.verify-steps h4 {
  margin: 0 0 12px;
  color: var(--text-primary);
}

.verify-steps p {
  margin: 8px 0;
  color: var(--text-secondary);
}

.code-block {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  margin-top: 12px;
}

.code-block p {
  margin: 4px 0;
}

.code-block pre {
  margin: 8px 0 0;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
  word-break: break-all;
}
</style>
