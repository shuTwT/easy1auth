<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive, watch } from 'vue'
import { messageTemplateApi, type MessageTemplate, type MessageTemplateSearch } from '@/api/messageTemplate'
import { Modal, message, type TabsProps } from 'antdv-next'
import EmailTemplates from './components/EmailTemplates.vue'
import SmsTemplates from './components/SmsTemplates.vue'
import TemplateEditDialog from './components/TemplateEditDialog.vue'
import { Plus } from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import { getApiErrorMessage } from './utils/hooks'
import type { TemplateEditDialogExpose, TemplateType } from './utils/types'

const [modal, contextHolder] = Modal.useModal()

const templateType = ref<TemplateType>('email')
const templateTabs: TabsProps['items'] = [
  { key: 'email', label: '邮件模板', forceRender: true },
  { key: 'sms', label: '短信模板', forceRender: true },
]

const userStore = useUserStore()
const templates = reactive<Record<TemplateType, MessageTemplate[]>>({ email: [], sms: [] })
const templatesLoading = reactive<Record<TemplateType, boolean>>({ email: false, sms: false })
const templateQueries = reactive<Record<TemplateType, MessageTemplateSearch>>({ email: {}, sms: {} })
const requestVersions = { email: 0, sms: 0 }
let mounted = true

const templateEditDialogRef = ref<TemplateEditDialogExpose>()

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
    if (isCurrent()) {
      templates[type] = response
    }
  } catch (error: unknown) {
    if (isCurrent()) {
      console.error('加载模板列表失败:', error)
      message.error('加载消息模板失败')
    }
  } finally {
    if (isCurrent()) {
      templatesLoading[type] = false
    }
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
  templateEditDialogRef.value?.open(templateType.value)
}

const handleEditTemplate = (template: MessageTemplate) => {
  templateEditDialogRef.value?.open(template.type, template)
}

const handleDeleteTemplate = async (template: MessageTemplate) => {
  const confirmed = await modal.confirm({
    title: '删除消息模板',
    content: '确定要删除该模板吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) {
    return
  }

  try {
    await messageTemplateApi.delete(template.id)
    message.success('模板删除成功')
    loadTemplates()
  } catch (error: unknown) {
    console.error('删除模板失败:', error)
    message.error(getApiErrorMessage(error, '删除模板失败'))
  }
}

onMounted(() => loadTemplates())
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#102A43] mb-2">消息服务</h1>
      <p class="text-sm text-[#52677D]">统一管理邮箱和短信通知模板</p>
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

    <TemplateEditDialog ref="templateEditDialogRef" @success="loadTemplates()" />

    <contextHolder />
  </div>
</template>
