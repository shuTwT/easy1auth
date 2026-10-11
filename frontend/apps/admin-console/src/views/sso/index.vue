<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Copy } from '@lucide/vue'
import { applicationApi } from '@/api/application'
import type { Application } from '@/types/application'
import IntegrationGuide from './components/IntegrationGuide.vue'
import { useClipboard, useSsoDict } from './utils/hooks'
import type { SsoStats } from './utils/types'

const loading = ref(false)
const applications = ref<Application[]>([])
const baseUrl = ref(window.location.origin)
const stats = ref<SsoStats>({
  totalApps: 0,
  activeApps: 0,
  webApps: 0,
  spaApps: 0,
  nativeApps: 0,
  machineApps: 0
})

const { getTypeText, getTypeVariant, getStatusText, getStatusVariant, buildStats } = useSsoDict()
const { copyToClipboard } = useClipboard()

const columns: TableProps<Application>['columns'] = [
  { title: '应用名称', dataIndex: 'name', width: 200 },
  { title: '应用类型', key: 'type', width: 120 },
  { title: 'Client ID', key: 'clientId', width: 300 },
  { title: '回调地址', key: 'redirectUris', width: 240 },
  { title: 'Access Token 有效期', key: 'accessTokenLifetime', width: 150, align: 'center' },
  { title: 'Refresh Token 有效期', key: 'refreshTokenLifetime', width: 150, align: 'center' },
  { title: '状态', key: 'status', width: 100, align: 'center' }
]

const statCards = [
  { key: 'totalApps', label: '应用总数' },
  { key: 'activeApps', label: '启用应用' },
  { key: 'webApps', label: 'Web应用' },
  { key: 'spaApps', label: '单页应用' },
  { key: 'nativeApps', label: '原生应用' },
  { key: 'machineApps', label: '机器应用' }
] as const

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await applicationApi.getList({ pageSize: 1000 })
    applications.value = res.items
    stats.value = buildStats(res.items, res.total)
  } catch (error: unknown) {
    console.error('加载应用列表失败:', error)
    message.error('加载应用列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)]">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">单点登录配置</h1>
        <p class="text-sm text-[#52677D]">管理 SSO 端点和应用集成配置</p>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-5 mb-6">
      <a-card v-for="card in statCards" :key="card.key">
        <div class="pt-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-[#102A43] mb-1">{{ stats[card.key] }}</div>
            <div class="text-sm text-[#52677D]">{{ card.label }}</div>
          </div>
        </div>
      </a-card>
    </div>

    <a-card>
      <div class="pt-6">
        <a-alert class="mb-5!" type="info" show-icon title="单点登录说明">
          <template #description>
            <div>
              <div>单点登录（SSO）允许用户使用一个账号登录多个应用。系统支持 OAuth 2.0 和 OpenID Connect 协议。</div>
              <div class="mt-3">
                <strong>OAuth 2.0 授权端点：</strong>
                <a-button type="link" size="small" class="p-0 h-auto" @click="copyToClipboard(`${baseUrl}/oauth2/authorize`)">
                  {{ baseUrl }}/oauth2/authorize
                </a-button>
              </div>
              <div class="mt-1">
                <strong>Token 端点：</strong>
                <a-button type="link" size="small" class="p-0 h-auto" @click="copyToClipboard(`${baseUrl}/oauth2/token`)">
                  {{ baseUrl }}/oauth2/token
                </a-button>
              </div>
              <div class="mt-1">
                <strong>用户信息端点：</strong>
                <a-button type="link" size="small" class="p-0 h-auto" @click="copyToClipboard(`${baseUrl}/oauth2/userinfo`)">
                  {{ baseUrl }}/oauth2/userinfo
                </a-button>
              </div>
            </div>
          </template>
        </a-alert>

        <a-table :columns="columns" :data-source="applications" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1260 }">
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.key === 'type'">
              <a-tag :color="getTypeVariant(item.type)">
                {{ getTypeText(item.type) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'clientId'">
              <div class="flex items-center gap-2">
                <span class="font-mono text-xs">{{ item.clientId }}</span>
                <a-button type="text" size="small" shape="circle" aria-label="复制客户端 ID" class="h-6 w-6" @click="copyToClipboard(item.clientId)">
                  <Copy class="size-3" />
                </a-button>
              </div>
            </template>
            <template v-else-if="column.key === 'redirectUris'">
              <div v-if="Array.isArray(item.redirectUris)">
                <div v-for="(uri, index) in item.redirectUris" :key="index" class="text-xs mb-1">
                  {{ uri }}
                </div>
              </div>
              <span v-else class="text-[#52677D]">-</span>
            </template>
            <template v-else-if="column.key === 'accessTokenLifetime'">{{ item.accessTokenLifetime }} 秒</template>
            <template v-else-if="column.key === 'refreshTokenLifetime'">{{ item.refreshTokenLifetime }} 秒</template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="getStatusVariant(item.status)">
                {{ getStatusText(item.status) }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </a-card>

    <IntegrationGuide :base-url="baseUrl" />
  </div>
</template>
