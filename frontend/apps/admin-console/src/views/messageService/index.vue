<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { messageTemplateApi, type MessageTemplate, type CreateTemplateDto } from '@/api/messageTemplate'
import { Modal, message } from 'antdv-next'
import { Plus } from '@lucide/vue'

const [modal, contextHolder] = Modal.useModal()

const templateType = ref('email')

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

const loadTemplates = async () => {
  templatesLoading.value = true
  try {
    const response = await messageTemplateApi.list()
    templates.value = response
  } catch (error) {
    console.error('加载模板列表失败:', error)
    message.error('加载消息模板失败')
  } finally {
    templatesLoading.value = false
  }
}

const handleInitTemplates = async () => {
  initingTemplates.value = true
  try {
    await messageTemplateApi.initDefaults()
    message.success('默认模板初始化成功')
    loadTemplates()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '初始化模板失败')
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

onMounted(loadTemplates)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground mb-2">消息服务</h1>
      <p class="text-sm text-muted-foreground">统一管理邮箱和短信通知模板</p>
    </div>

    <a-card>
      <div class="px-2">
        <div class="flex justify-between items-center mb-5">
          <h3 class="text-base font-semibold">模板管理</h3>
          <div class="flex gap-3">
          <a-button type="primary" :loading="initingTemplates" :disabled="initingTemplates" @click="handleInitTemplates">
              初始化默认模板
            </a-button>
            <a-button @click="showTemplateDialog = true">
              <Plus class="size-4 mr-2" />
              新建模板
            </a-button>
          </div>
        </div>

        <div>
          <div class="mb-4">
            <a-button :type="templateType === 'email' ? 'primary' : 'default'" @click="templateType = 'email'">邮件模板</a-button>
            <a-button :type="templateType === 'sms' ? 'primary' : 'default'" @click="templateType = 'sms'">短信模板</a-button>
          </div>

          <div v-show="templateType === 'email'">
            <div class="rounded-md border">
              <a-table :columns="[
                { title: '模板名称', dataIndex: 'name', width: 150 }, { title: '模板代码', dataIndex: 'code', width: 150 },
                { title: '邮件主题', dataIndex: 'subject', width: 200 }, { title: '默认', key: 'isDefault', width: 80 },
                { title: '状态', key: 'status', width: 80 }, { title: '操作', key: 'actions', width: 150, align: 'right' }
              ]" :data-source="emailTemplates" :loading="templatesLoading" row-key="id" :pagination="false" :scroll="{ x: 810 }">
                <template #bodyCell="{ column, record: template }">
                  <template v-if="column.key === 'isDefault'">
                      <a-tag v-if="template.isDefault" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">是</a-tag>
                      <a-tag v-else >否</a-tag>
                  </template>
                  <template v-else-if="column.key === 'status'">
                      <a-tag v-if="template.status === 'active'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">启用</a-tag>
                      <a-tag v-else >禁用</a-tag>
                  </template>
                  <template v-else-if="column.key === 'actions'">
                      <div class="flex justify-end gap-2">
                        <a-button type="link" size="small" class="h-auto p-0" @click="handleEditTemplate(template)">编辑</a-button>
                        <a-button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDeleteTemplate(template)">删除</a-button>
                      </div>
                  </template>
                </template>
              </a-table>
            </div>
          </div>

          <div v-show="templateType === 'sms'">
            <div class="rounded-md border">
              <a-table :columns="[
                { title: '模板名称', dataIndex: 'name', width: 150 }, { title: '模板代码', dataIndex: 'code', width: 150 },
                { title: '模板内容', key: 'content', width: 300 }, { title: '默认', key: 'isDefault', width: 80 },
                { title: '状态', key: 'status', width: 80 }, { title: '操作', key: 'actions', width: 150, align: 'right' }
              ]" :data-source="smsTemplates" :loading="templatesLoading" row-key="id" :pagination="false" :scroll="{ x: 910 }">
                <template #bodyCell="{ column, record: template }">
                  <template v-if="column.key === 'content'"><span class="block truncate">{{ template.content }}</span></template>
                  <template v-else-if="column.key === 'isDefault'">
                      <a-tag v-if="template.isDefault" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">是</a-tag>
                      <a-tag v-else >否</a-tag>
                  </template>
                  <template v-else-if="column.key === 'status'">
                      <a-tag v-if="template.status === 'active'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">启用</a-tag>
                      <a-tag v-else >禁用</a-tag>
                  </template>
                  <template v-else-if="column.key === 'actions'">
                      <div class="flex justify-end gap-2">
                        <a-button type="link" size="small" class="h-auto p-0" @click="handleEditTemplate(template)">编辑</a-button>
                        <a-button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDeleteTemplate(template)">删除</a-button>
                      </div>
                  </template>
                </template>
              </a-table>
            </div>
          </div>
        </div>
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
