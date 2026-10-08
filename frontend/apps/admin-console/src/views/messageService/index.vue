<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive, watch } from 'vue'
import { messageTemplateApi, type MessageTemplate, type CreateTemplateDto, type MessageTemplateSearch } from '@/api/messageTemplate'
import { Modal, message, type TabsProps } from 'antdv-next'
import EmailTemplates from './EmailTemplates.vue'
import SmsTemplates from './SmsTemplates.vue'
import { Plus } from '@lucide/vue'
import { useUserStore } from '@/stores/user'

const [modal, contextHolder] = Modal.useModal()

const templateType = ref('email')
const templateTabs: TabsProps['items'] = [
  { key: 'email', label: '邮件模板', forceRender: true },
  { key: 'sms', label: '短信模板', forceRender: true },
]

type TemplateType = MessageTemplate['type']
const userStore = useUserStore()
const templates = reactive<Record<TemplateType, MessageTemplate[]>>({ email: [], sms: [] })
const templatesLoading = reactive<Record<TemplateType, boolean>>({ email: false, sms: false })
const templateQueries = reactive<Record<TemplateType, MessageTemplateSearch>>({ email: {}, sms: {} })
const requestVersions = { email: 0, sms: 0 }
let mounted = true
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

const loadTemplates = async (type?: TemplateType): Promise<void> => {
  if (!type) {
    await Promise.all([loadTemplates('email'), loadTemplates('sms')])
    return
  }
  const version = ++requestVersions[type]
  const userPoolId = userStore.currentUserPool?.id
  const isCurrent = () => mounted && requestVersions[type] === version && userStore.currentUserPool?.id === userPoolId
  templatesLoading[type] = true
  try {
    const response = await messageTemplateApi.list(type, { ...templateQueries[type] })
    if (isCurrent()) templates[type] = response
  } catch (error) {
    if (isCurrent()) {
      console.error('加载模板列表失败:', error)
      message.error('加载消息模板失败')
    }
  } finally {
    if (isCurrent()) templatesLoading[type] = false
  }
}

const handleTemplateSearch = (type: TemplateType, query: MessageTemplateSearch) => {
  templateQueries[type] = query
  loadTemplates(type)
}

watch(() => userStore.currentUserPool?.id, () => {
  templates.email = []
  templates.sms = []
  loadTemplates()
})

onUnmounted(() => {
  mounted = false
})

const handleCreateTemplate = () => {
  editingTemplate.value = null
  Object.assign(templateForm, {
    type: templateType.value === 'sms' ? 'sms' : 'email',
    code: '',
    name: '',
    subject: '',
    content: '',
    isDefault: false,
  })
  showTemplateDialog.value = true
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
    message.warning('请填写完整信息')
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
      message.success('模板更新成功')
    } else {
      await messageTemplateApi.create(templateForm)
      message.success('模板创建成功')
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
    message.error(error.response?.data?.msg || '保存模板失败')
  } finally {
    savingTemplate.value = false
  }
}

const handleDeleteTemplate = async (template: MessageTemplate) => {
  const confirmed = await modal.confirm({
    title: '删除消息模板',
    content: '确定要删除该模板吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await messageTemplateApi.delete(template.id)
    message.success('模板删除成功')
    loadTemplates()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除模板失败')
  }
}

onMounted(() => loadTemplates())
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground mb-2">消息服务</h1>
      <p class="text-sm text-muted-foreground">统一管理邮箱和短信通知模板</p>
    </div>

    <a-card>
      <div class="px-2">
        <div class="flex flex-wrap justify-between items-center gap-3 mb-5">
          <h3 class="text-base font-semibold">模板管理</h3>
          <div class="flex flex-wrap gap-3">
            <a-button type="primary" @click="handleCreateTemplate">
              <Plus class="size-4 mr-2" />
              新建模板
            </a-button>
          </div>
        </div>

        <a-tabs v-model:active-key="templateType" :items="templateTabs">
          <template #contentRender="{ item }">
            <EmailTemplates
              v-if="item.key === 'email'"
              :templates="templates.email"
              :loading="templatesLoading.email"
              @edit="handleEditTemplate"
              @delete="handleDeleteTemplate"
              @search="handleTemplateSearch('email', $event)"
            />
            <SmsTemplates
              v-else-if="item.key === 'sms'"
              :templates="templates.sms"
              :loading="templatesLoading.sms"
              @edit="handleEditTemplate"
              @delete="handleDeleteTemplate"
              @search="handleTemplateSearch('sms', $event)"
            />
          </template>
        </a-tabs>
      </div>
    </a-card>

    <a-modal v-model:open="showTemplateDialog" :footer="null">
      <div class="sm:max-w-[700px]">
        <div>
          <h3>{{ editingTemplate ? '编辑模板' : '新建模板' }}</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板类型</label>
              <a-radio-group v-model:value="templateForm.type" :disabled="!!editingTemplate">
                <div class="flex items-center gap-2">
                  <a-radio value="email" />
                  <label class="text-sm cursor-pointer">邮件</label>
                </div>
                <div class="flex items-center gap-2">
                  <a-radio value="sms" />
                  <label class="text-sm cursor-pointer">短信</label>
                </div>
              </a-radio-group>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板代码</label>
              <a-input v-model:value="templateForm.code" placeholder="例如：verification_code" :disabled="!!editingTemplate" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板名称</label>
              <a-input v-model:value="templateForm.name" placeholder="请输入模板名称" />
            </div>
            <div v-if="templateForm.type === 'email'" class="grid gap-2">
              <label class="text-sm font-medium">邮件主题</label>
              <a-input v-model:value="templateForm.subject" placeholder="请输入邮件主题，支持变量如 {{appName}}" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板内容</label>
              <a-textarea v-model:value="templateForm.content"
                :rows="10"
                placeholder="请输入模板内容，支持变量如 {{code}}, {{username}} 等"
              />
            </div>
            <div class="flex items-center gap-2">
              <a-switch :checked="templateForm.isDefault" @update:checked="templateForm.isDefault = $event === true" />
              <label class="text-sm cursor-pointer">设为默认</label>
            </div>
          </div>
        </form>
        <div>
          <a-button  @click="showTemplateDialog = false">取消</a-button>
            <a-button type="primary" :loading="savingTemplate" :disabled="savingTemplate" @click="handleSaveTemplate">保存</a-button>
        </div>
      </div>
    </a-modal>

    <contextHolder />
  </div>
</template>
