<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { customDomainApi, type CustomDomain, type CreateDomainDto, type UpdateSSLDto } from '@/api/customDomain'
import { Modal, message } from 'antdv-next'
import { Plus } from '@lucide/vue'

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

onMounted(loadDomains)
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#102A43] mb-2">自定义域名</h1>
      <p class="text-sm text-[#52677D]">登记用户池使用的登录域名并查看其状态</p>
    </div>

    <a-card>
      <div class="px-2">
        <div class="flex justify-between items-center mb-5">
          <h3 class="text-base font-semibold">域名管理</h3>
          <a-button @click="showDomainDialog = true">
            <Plus class="size-4 mr-2" />
            添加域名
          </a-button>
        </div>

        <div class="rounded-md border mb-6">
          <a-table :columns="[
            { title: '域名', key: 'domain', width: 200 }, { title: 'SSL状态', key: 'sslStatus', width: 120 },
            { title: '验证方式', key: 'verificationMethod', width: 100 }, { title: '创建时间', key: 'createdAt', width: 180 },
            { title: '操作', key: 'actions', width: 200, align: 'right' }
          ]" :data-source="domains" :loading="domainsLoading" row-key="id" :pagination="false" :scroll="{ x: 800 }">
            <template #bodyCell="{ column, record: domain }">
              <template v-if="column.key === 'domain'">
                  <div class="flex items-center gap-2">
                    <span class="font-medium">{{ domain.domain }}</span>
                    <a-tag v-if="domain.status === 'verified'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">已验证</a-tag>
                    <a-tag v-else-if="domain.status === 'pending'" color="blue" class="bg-yellow-500/10 text-yellow-600 hover:bg-yellow-500/20">待验证</a-tag>
                    <a-tag v-else-if="domain.status === 'verifying'" >验证中</a-tag>
                    <a-tag v-else color="red">验证失败</a-tag>
                  </div>
              </template>
              <template v-else-if="column.key === 'sslStatus'">
                  <a-tag v-if="domain.sslStatus === 'active'" color="processing" class="bg-green-500/10 text-green-600 hover:bg-green-500/20">已配置</a-tag>
                  <a-tag v-else >未配置</a-tag>
              </template>
              <template v-else-if="column.key === 'verificationMethod'">{{ domain.verificationMethod === 'dns' ? 'DNS记录' : '文件验证' }}</template>
              <template v-else-if="column.key === 'createdAt'">{{ formatDate(domain.createdAt) }}</template>
              <template v-else-if="column.key === 'actions'">
                  <div class="flex justify-end gap-2">
                    <a-button v-if="domain.status !== 'verified'" type="link" size="small" class="h-auto p-0" disabled title="阶段 6 仅登记域名">验证未启用</a-button>
                    <a-button v-if="domain.status === 'verified'" type="link" size="small" class="h-auto p-0" disabled title="TLS 由外部网关管理">TLS由网关管理</a-button>
                    <a-button type="link" size="small" class="h-auto p-0 text-[#B91C1C]" @click="handleDeleteDomain(domain)">删除</a-button>
                  </div>
              </template>
            </template>
          </a-table>
        </div>

        <a-alert class="bg-[#F4F8FB] border" type="info" show-icon title="阶段 6 域名管理说明">
          <template #description>
            <div class="space-y-1">
              <div>当前仅登记域名和验证令牌，不执行自动所有权验证。</div>
              <div>TLS 证书由部署网关或证书控制面管理，Easy1Auth 不接收证书私钥。</div>
            </div>
          </template>
        </a-alert>
      </div>
    </a-card>

    <a-modal v-model:open="showDomainDialog" :footer="null">
      <div class="sm:max-w-[500px]">
        <div>
          <h3>添加域名</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">域名</label>
              <div class="flex">
                <div class="flex items-center px-3 bg-[#F4F8FB] border border-r-0 rounded-l-md text-sm text-[#52677D]">https://</div>
                <a-input v-model:value="domainForm.domain" placeholder="例如：login.yourcompany.com" class="rounded-l-none" />
              </div>
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">验证方式</label>
              <a-radio-group v-model:value="domainForm.verificationMethod">
                <div class="flex items-center gap-2">
                  <a-radio value="dns" />
                  <label class="text-sm cursor-pointer">DNS记录验证</label>
                </div>
                <div class="flex items-center gap-2">
                  <a-radio value="file" />
                  <label class="text-sm cursor-pointer">文件验证</label>
                </div>
              </a-radio-group>
            </div>
          </div>
        </form>
        <div>
          <a-button  @click="showDomainDialog = false">取消</a-button>
          <a-button type="primary" :loading="creatingDomain" :disabled="creatingDomain" @click="handleCreateDomain">添加</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal v-model:open="showSSLDialog" :footer="null">
      <div class="sm:max-w-[600px]">
        <div>
          <h3>配置SSL证书</h3>
        </div>
        <form>
          <div class="grid gap-4 py-4">
            <div class="grid gap-2">
              <label class="text-sm font-medium">SSL证书</label>
              <a-textarea v-model:value="sslForm.sslCertificate"
                :rows="8"
                placeholder="请粘贴SSL证书内容（PEM格式）"
              />
            </div>
            <div class="grid gap-2">
              <label class="text-sm font-medium">私钥</label>
              <a-textarea v-model:value="sslForm.sslPrivateKey"
                :rows="8"
                placeholder="请粘贴SSL私钥内容（PEM格式）"
              />
            </div>
          </div>
        </form>
        <div>
          <a-button  @click="showSSLDialog = false">取消</a-button>
          <a-button type="primary" :loading="savingSSL" :disabled="savingSSL" @click="handleSaveSSL">保存</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal v-model:open="showVerifyDialog" :footer="null">
      <div class="sm:max-w-[600px]">
        <div>
          <h3>域名验证</h3>
        </div>
        <div class="py-4">
          <a-alert class="mb-5!" type="info" show-icon title="请按照以下步骤完成验证" />
          <div v-if="verifyingDomain?.verificationMethod === 'dns'" class="space-y-3">
            <h4 class="font-semibold">DNS记录验证</h4>
            <p class="text-sm text-[#52677D]">在您的DNS服务商处添加以下TXT记录：</p>
            <div class="bg-[#F4F8FB] p-4 rounded-lg space-y-2">
              <p class="text-sm"><strong>记录类型：</strong>TXT</p>
              <p class="text-sm"><strong>主机记录：</strong>@ 或 留空</p>
              <p class="text-sm"><strong>记录值：</strong></p>
              <pre class="bg-[#F4F8FB] p-3 rounded text-sm font-mono break-all">{{ verifyingDomain?.verificationToken }}</pre>
            </div>
          </div>
          <div v-else class="space-y-3">
            <h4 class="font-semibold">文件验证</h4>
            <p class="text-sm text-[#52677D]">在您的网站根目录创建以下文件：</p>
            <div class="bg-[#F4F8FB] p-4 rounded-lg space-y-2">
              <p class="text-sm"><strong>文件路径：</strong>/.well-known/easy1auth-verification.txt</p>
              <p class="text-sm"><strong>文件内容：</strong></p>
              <pre class="bg-[#F4F8FB] p-3 rounded text-sm font-mono break-all">{{ verifyingDomain?.verificationToken?.split('=')[1] }}</pre>
            </div>
          </div>
        </div>
        <div>
          <a-button  @click="showVerifyDialog = false">取消</a-button>
          <a-button type="primary" :loading="verifying" :disabled="verifying" @click="handleVerifyDomainConfirm">验证</a-button>
        </div>
      </div>
    </a-modal>
    <contextHolder />
  </div>
</template>
