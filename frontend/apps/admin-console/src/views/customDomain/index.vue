<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Plus } from '@lucide/vue'
import { customDomainApi } from '@/api/customDomain'
import type { CustomDomain } from '@/api/customDomain'
import DomainCreateDialog from './components/DomainCreateDialog.vue'
import { getApiErrorMessage, useDomainDict } from './utils/hooks'
import type { DomainCreateDialogExpose } from './utils/types'

const domains = ref<CustomDomain[]>([])
const loading = ref(false)
const domainCreateDialogRef = ref<DomainCreateDialogExpose>()

const [modal, contextHolder] = Modal.useModal()

const {
  getDomainStatusText,
  getDomainStatusColor,
  getSslStatusText,
  getSslStatusColor,
  getVerificationMethodText,
  formatDate
} = useDomainDict()

const columns: TableProps<CustomDomain>['columns'] = [
  { title: '域名', key: 'domain', width: 200 },
  { title: 'SSL状态', key: 'sslStatus', width: 120 },
  { title: '验证方式', key: 'verificationMethod', width: 100 },
  { title: '创建时间', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 200, align: 'right' }
]

const loadDomains = async () => {
  loading.value = true
  try {
    domains.value = await customDomainApi.list()
  } catch (error: unknown) {
    console.error('加载域名列表失败:', error)
    message.error('加载域名列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  domainCreateDialogRef.value?.open()
}

const handleDelete = async (domain: CustomDomain) => {
  const confirmed = await modal.confirm({
    title: '删除域名',
    content: '确定要删除该域名吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed) {
    return
  }
  try {
    await customDomainApi.delete(domain.id)
    message.success('域名删除成功')
    loadDomains()
  } catch (error: unknown) {
    console.error('删除域名失败:', error)
    message.error(getApiErrorMessage(error, '删除域名失败'))
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
      <div class="flex justify-between items-center mb-5">
        <h3 class="text-base font-semibold">域名管理</h3>
        <a-button @click="handleAdd">
          <Plus class="size-4 mr-2" />
          添加域名
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="domains"
        :loading="loading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 800 }"
        class="mb-6"
      >
        <template #bodyCell="{ column, record: domain }">
          <template v-if="column.key === 'domain'">
            <div class="flex items-center gap-2">
              <span class="font-medium">{{ domain.domain }}</span>
              <a-tag :color="getDomainStatusColor(domain.status)">{{ getDomainStatusText(domain.status) }}</a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'sslStatus'">
            <a-tag :color="getSslStatusColor(domain.sslStatus)">{{ getSslStatusText(domain.sslStatus) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'verificationMethod'">
            {{ getVerificationMethodText(domain.verificationMethod) }}
          </template>
          <template v-else-if="column.key === 'createdAt'">{{ formatDate(domain.createdAt) }}</template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex justify-end gap-2">
              <a-button
                v-if="domain.status !== 'verified'"
                type="link"
                size="small"
                class="h-auto p-0"
                disabled
                title="阶段 6 仅登记域名"
              >
                验证未启用
              </a-button>
              <a-button
                v-else
                type="link"
                size="small"
                class="h-auto p-0"
                disabled
                title="TLS 由外部网关管理"
              >
                TLS由网关管理
              </a-button>
              <a-button type="link" size="small" danger class="h-auto p-0" @click="handleDelete(domain)">删除</a-button>
            </div>
          </template>
        </template>
      </a-table>

      <a-alert class="border" type="info" show-icon title="阶段 6 域名管理说明">
        <template #description>
          <div class="space-y-1">
            <div>当前仅登记域名和验证令牌，不执行自动所有权验证。</div>
            <div>TLS 证书由部署网关或证书控制面管理，Easy1Auth 不接收证书私钥。</div>
          </div>
        </template>
      </a-alert>
    </a-card>

    <DomainCreateDialog ref="domainCreateDialogRef" @success="loadDomains" />
    <contextHolder />
  </div>
</template>
