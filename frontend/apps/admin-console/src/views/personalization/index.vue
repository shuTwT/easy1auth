<template>
  <div class="personalization-settings p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-foreground mb-2">{{ pageTitle }}</h1>
      <p class="text-sm text-muted-foreground">{{ pageDescription }}</p>
    </div>

    <Card>
      <div class="pt-6">
        <div>
          <div v-if="section === 'domains'" class="px-2">
            <div class="flex justify-between items-center mb-5">
              <h3 class="text-base font-semibold">域名管理</h3>
              <Button @click="showDomainDialog = true">
                <Plus class="size-4 mr-2" />
                添加域名
              </Button>
            </div>

            <div class="rounded-md border mb-6">
              <Table :columns="[
                { title: '域名', key: 'domain', width: 200 }, { title: 'SSL状态', key: 'sslStatus', width: 120 },
                { title: '验证方式', key: 'verificationMethod', width: 100 }, { title: '创建时间', key: 'createdAt', width: 180 },
                { title: '操作', key: 'actions', width: 200, align: 'right' }
              ]" :data-source="domains" :loading="domainsLoading" row-key="id" :pagination="false" :scroll="{ x: 800 }">
                <template #bodyCell="{ column, record: domain }">
                  <template v-if="column.key === 'domain'">
                      <div class="flex items-center gap-2">
                        <span class="font-medium">{{ domain.domain }}</span>
                        <Tag v-if="domain.status === 'verified'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">已验证</Tag>
                        <Tag v-else-if="domain.status === 'pending'" color="blue" class="bg-yellow-500/10 text-yellow-600 hover:bg-yellow-500/20">待验证</Tag>
                        <Tag v-else-if="domain.status === 'verifying'" >验证中</Tag>
                        <Tag v-else color="red">验证失败</Tag>
                      </div>
                  </template>
                  <template v-else-if="column.key === 'sslStatus'">
                      <Tag v-if="domain.sslStatus === 'active'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">已配置</Tag>
                      <Tag v-else >未配置</Tag>
                  </template>
                  <template v-else-if="column.key === 'verificationMethod'">{{ domain.verificationMethod === 'dns' ? 'DNS记录' : '文件验证' }}</template>
                  <template v-else-if="column.key === 'createdAt'">{{ formatDate(domain.createdAt) }}</template>
                  <template v-else-if="column.key === 'actions'">
                      <div class="flex justify-end gap-2">
                        <Button v-if="domain.status !== 'verified'" type="link" size="small" class="h-auto p-0" disabled title="阶段 6 仅登记域名">验证未启用</Button>
                        <Button v-if="domain.status === 'verified'" type="link" size="small" class="h-auto p-0" disabled title="TLS 由外部网关管理">TLS由网关管理</Button>
                        <Button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDeleteDomain(domain)">删除</Button>
                      </div>
                  </template>
                </template>
              </Table>
            </div>

            <Alert class="bg-muted border" type="info" show-icon title="阶段 6 域名管理说明">
              <template #description>
                <div class="space-y-1">
                  <div>当前仅登记域名和验证令牌，不执行自动所有权验证。</div>
                  <div>TLS 证书由部署网关或证书控制面管理，Easy1Auth 不接收证书私钥。</div>
                </div>
              </template>
            </Alert>
          </div>

          <div v-else-if="section === 'templates'" class="px-2">
            <div class="flex justify-between items-center mb-5">
              <h3 class="text-base font-semibold">模板管理</h3>
              <div class="flex gap-3">
              <Button type="primary" :loading="initingTemplates" :disabled="initingTemplates" @click="handleInitTemplates">
                  初始化默认模板
                </Button>
                <Button @click="showTemplateDialog = true">
                  <Plus class="size-4 mr-2" />
                  新建模板
                </Button>
              </div>
            </div>

            <div>
              <div class="mb-4">
                <Button :type="templateType === 'email' ? 'primary' : 'default'" @click="templateType = 'email'">邮件模板</Button>
                <Button :type="templateType === 'sms' ? 'primary' : 'default'" @click="templateType = 'sms'">短信模板</Button>
              </div>

              <div v-show="templateType === 'email'">
                <div class="rounded-md border">
                  <Table :columns="[
                    { title: '模板名称', dataIndex: 'name', width: 150 }, { title: '模板代码', dataIndex: 'code', width: 150 },
                    { title: '邮件主题', dataIndex: 'subject', width: 200 }, { title: '默认', key: 'isDefault', width: 80 },
                    { title: '状态', key: 'status', width: 80 }, { title: '操作', key: 'actions', width: 150, align: 'right' }
                  ]" :data-source="emailTemplates" :loading="templatesLoading" row-key="id" :pagination="false" :scroll="{ x: 810 }">
                    <template #bodyCell="{ column, record: template }">
                      <template v-if="column.key === 'isDefault'">
                          <Tag v-if="template.isDefault" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">是</Tag>
                          <Tag v-else >否</Tag>
                      </template>
                      <template v-else-if="column.key === 'status'">
                          <Tag v-if="template.status === 'active'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">启用</Tag>
                          <Tag v-else >禁用</Tag>
                      </template>
                      <template v-else-if="column.key === 'actions'">
                          <div class="flex justify-end gap-2">
                            <Button type="link" size="small" class="h-auto p-0" @click="handleEditTemplate(template)">编辑</Button>
                            <Button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDeleteTemplate(template)">删除</Button>
                          </div>
                      </template>
                    </template>
                  </Table>
                </div>
              </div>

              <div v-show="templateType === 'sms'">
                <div class="rounded-md border">
                  <Table :columns="[
                    { title: '模板名称', dataIndex: 'name', width: 150 }, { title: '模板代码', dataIndex: 'code', width: 150 },
                    { title: '模板内容', key: 'content', width: 300 }, { title: '默认', key: 'isDefault', width: 80 },
                    { title: '状态', key: 'status', width: 80 }, { title: '操作', key: 'actions', width: 150, align: 'right' }
                  ]" :data-source="smsTemplates" :loading="templatesLoading" row-key="id" :pagination="false" :scroll="{ x: 910 }">
                    <template #bodyCell="{ column, record: template }">
                      <template v-if="column.key === 'content'"><span class="block truncate">{{ template.content }}</span></template>
                      <template v-else-if="column.key === 'isDefault'">
                          <Tag v-if="template.isDefault" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">是</Tag>
                          <Tag v-else >否</Tag>
                      </template>
                      <template v-else-if="column.key === 'status'">
                          <Tag v-if="template.status === 'active'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">启用</Tag>
                          <Tag v-else >禁用</Tag>
                      </template>
                      <template v-else-if="column.key === 'actions'">
                          <div class="flex justify-end gap-2">
                            <Button type="link" size="small" class="h-auto p-0" @click="handleEditTemplate(template)">编辑</Button>
                            <Button type="link" size="small" class="h-auto p-0 text-destructive" @click="handleDeleteTemplate(template)">删除</Button>
                          </div>
                      </template>
                    </template>
                  </Table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <Modal v-model:open="showDomainDialog" :footer="null">
      <div class="sm:max-w-[500px]">
        <div>
          <h3>添加域名</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">域名</label>
              <div class="flex">
                <div class="flex items-center px-3 bg-muted border border-r-0 rounded-l-md text-sm text-muted-foreground">https://</div>
                <Input v-model:value="domainForm.domain" placeholder="例如：login.yourcompany.com" class="rounded-l-none" />
              </div>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">验证方式</label>
              <RadioGroup v-model:value="domainForm.verificationMethod">
                <div class="flex items-center gap-2">
                  <Radio value="dns" />
                  <label class="text-sm cursor-pointer">DNS记录验证</label>
                </div>
                <div class="flex items-center gap-2">
                  <Radio value="file" />
                  <label class="text-sm cursor-pointer">文件验证</label>
                </div>
              </RadioGroup>
            </div>
          </div>
        </form>
        <div>
          <Button  @click="showDomainDialog = false">取消</Button>
          <Button type="primary" :loading="creatingDomain" :disabled="creatingDomain" @click="handleCreateDomain">添加</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="showSSLDialog" :footer="null">
      <div class="sm:max-w-[600px]">
        <div>
          <h3>配置SSL证书</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">SSL证书</label>
              <InputTextArea v-model:value="sslForm.sslCertificate"
                :rows="8"
                placeholder="请粘贴SSL证书内容（PEM格式）"
              />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">私钥</label>
              <InputTextArea v-model:value="sslForm.sslPrivateKey"
                :rows="8"
                placeholder="请粘贴SSL私钥内容（PEM格式）"
              />
            </div>
          </div>
        </form>
        <div>
          <Button  @click="showSSLDialog = false">取消</Button>
          <Button type="primary" :loading="savingSSL" :disabled="savingSSL" @click="handleSaveSSL">保存</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="showTemplateDialog" :footer="null">
      <div class="sm:max-w-[700px]">
        <div>
          <h3>{{ editingTemplate ? '编辑模板' : '新建模板' }}</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板类型</label>
              <RadioGroup v-model:value="templateForm.type" :disabled="!!editingTemplate">
                <div class="flex items-center gap-2">
                  <Radio value="email" />
                  <label class="text-sm cursor-pointer">邮件</label>
                </div>
                <div class="flex items-center gap-2">
                  <Radio value="sms" />
                  <label class="text-sm cursor-pointer">短信</label>
                </div>
              </RadioGroup>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板代码</label>
              <Input v-model:value="templateForm.code" placeholder="例如：verification_code" :disabled="!!editingTemplate" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板名称</label>
              <Input v-model:value="templateForm.name" placeholder="请输入模板名称" />
            </div>
            <div v-if="templateForm.type === 'email'" class="grid gap-2">
              <label class="text-sm font-medium">邮件主题</label>
              <Input v-model:value="templateForm.subject" placeholder="请输入邮件主题，支持变量如 {{appName}}" />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">模板内容</label>
              <InputTextArea v-model:value="templateForm.content"
                :rows="10"
                placeholder="请输入模板内容，支持变量如 {{code}}, {{username}} 等"
              />
            </div>
            <div class="flex items-center gap-2">
              <Switch :checked="templateForm.isDefault" @update:checked="templateForm.isDefault = $event" />
              <label class="text-sm cursor-pointer">设为默认</label>
            </div>
          </div>
        </form>
        <div>
          <Button  @click="showTemplateDialog = false">取消</Button>
          <Button type="primary" :loading="savingTemplate" :disabled="savingTemplate" @click="handleSaveTemplate">保存</Button>
        </div>
      </div>
    </Modal>

    <Modal v-model:open="showVerifyDialog" :footer="null">
      <div class="sm:max-w-[600px]">
        <div>
          <h3>域名验证</h3>
        </div>
        <div class="py-4">
          <Alert class="mb-5" type="info" show-icon title="请按照以下步骤完成验证" />
          <div v-if="verifyingDomain?.verificationMethod === 'dns'" class="space-y-3">
            <h4 class="font-semibold">DNS记录验证</h4>
            <p class="text-sm text-muted-foreground">在您的DNS服务商处添加以下TXT记录：</p>
            <div class="bg-muted p-4 rounded-lg space-y-2">
              <p class="text-sm"><strong>记录类型：</strong>TXT</p>
              <p class="text-sm"><strong>主机记录：</strong>@ 或 留空</p>
              <p class="text-sm"><strong>记录值：</strong></p>
              <pre class="bg-background p-3 rounded text-sm font-mono break-all">{{ verifyingDomain?.verificationToken }}</pre>
            </div>
          </div>
          <div v-else class="space-y-3">
            <h4 class="font-semibold">文件验证</h4>
            <p class="text-sm text-muted-foreground">在您的网站根目录创建以下文件：</p>
            <div class="bg-muted p-4 rounded-lg space-y-2">
              <p class="text-sm"><strong>文件路径：</strong>/.well-known/easy1auth-verification.txt</p>
              <p class="text-sm"><strong>文件内容：</strong></p>
              <pre class="bg-background p-3 rounded text-sm font-mono break-all">{{ verifyingDomain?.verificationToken?.split('=')[1] }}</pre>
            </div>
          </div>
        </div>
        <div>
          <Button  @click="showVerifyDialog = false">取消</Button>
          <Button type="primary" :loading="verifying" :disabled="verifying" @click="handleVerifyDomainConfirm">验证</Button>
        </div>
      </div>
    </Modal>
    <contextHolder />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { Modal, Table, message } from 'antdv-next'
import { Plus } from '@lucide/vue'
import { useRoute } from 'vue-router'

import { customDomainApi, type CustomDomain, type CreateDomainDto, type UpdateSSLDto } from '@/api/customDomain'
import { messageTemplateApi, type MessageTemplate, type CreateTemplateDto } from '@/api/messageTemplate'

const route = useRoute()
const section = computed(() => String(route.meta.brandSection ?? 'domains'))
const pageTitle = computed(() => ({
  templates: '消息服务',
  domains: '自定义域名',
} as Record<string, string>)[section.value] ?? '品牌管理')
const pageDescription = computed(() => ({
  templates: '统一管理邮箱和短信通知模板',
  domains: '登记租户使用的登录域名并查看其状态',
} as Record<string, string>)[section.value] ?? '')
const templateType = ref('email')

const [modal, contextHolder] = Modal.useModal()

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

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN')
}

const loadDomains = async () => {
  domainsLoading.value = true
  try {
    const response = await customDomainApi.list()
    domains.value = response
  } catch (error) {
    console.error('加载域名列表失败:', error)
    message.error('加载域名列表失败')
  } finally {
    domainsLoading.value = false
  }
}

const handleCreateDomain = async () => {
  if (!domainForm.domain) {
    message.warning('请输入域名')
    return
  }

  creatingDomain.value = true
  try {
    await customDomainApi.create(domainForm)
    message.success('域名添加成功，请完成验证')
    showDomainDialog.value = false
    domainForm.domain = ''
    domainForm.verificationMethod = 'dns'
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '添加域名失败')
  } finally {
    creatingDomain.value = false
  }
}

const handleVerifyDomainConfirm = async () => {
  if (!verifyingDomain.value) return

  verifying.value = true
  try {
    await customDomainApi.verify(verifyingDomain.value.id)
    message.success('域名验证成功')
    showVerifyDialog.value = false
    verifyingDomain.value = null
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '域名验证失败')
  } finally {
    verifying.value = false
  }
}

const handleSaveSSL = async () => {
  if (!sslForm.sslCertificate || !sslForm.sslPrivateKey) {
    message.warning('请填写SSL证书和私钥')
    return
  }

  savingSSL.value = true
  try {
    await customDomainApi.updateSSL(currentDomainId.value, sslForm)
    message.success('SSL证书配置成功')
    showSSLDialog.value = false
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '配置SSL证书失败')
  } finally {
    savingSSL.value = false
  }
}

const handleDeleteDomain = async (domain: CustomDomain) => {
  const confirmed = await modal.confirm({
    title: '删除域名',
    content: '确定要删除该域名吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
  })
  if (!confirmed) return

  try {
    await customDomainApi.delete(domain.id)
    message.success('域名删除成功')
    loadDomains()
  } catch (error: any) {
    message.error(error.response?.data?.msg || '删除域名失败')
  }
}

const loadTemplates = async () => {
  templatesLoading.value = true
  try {
    const response = await messageTemplateApi.list()
    templates.value = response
  } catch (error) {
    console.error('加载模板列表失败:', error)
    message.error('加载消息模板失败')
    message.error('加载模板列表失败')
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

onMounted(() => {
  if (section.value === 'domains') loadDomains()
  if (section.value === 'templates') loadTemplates()
})
</script>
