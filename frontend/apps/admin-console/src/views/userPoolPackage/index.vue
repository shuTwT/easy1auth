<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import {
  AppWindow,
  Check,
  Gauge,
  Package,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  Users,
} from '@lucide/vue'
import { userPoolPackageApi } from '@/api/userPoolPackage'
import type {
  ManagementPermission,
  UserPoolPackage,
  UserPoolPackageStatus,
} from '@/types/userPoolPackage'
import PackageEditDialog from './components/PackageEditDialog.vue'
import PackagePermissionDialog from './components/PackagePermissionDialog.vue'
import PackageDeleteDialog from './components/PackageDeleteDialog.vue'
import { useUserPoolPackageDict } from './utils/hooks'
import type {
  PackageDeleteDialogExpose,
  PackageEditDialogExpose,
  PackagePermissionDialogExpose,
} from './utils/types'

const packages = ref<UserPoolPackage[]>([])
const permissionCatalog = ref<ManagementPermission[]>([])
const loading = ref(false)
const search = ref('')

const packageEditDialogRef = ref<PackageEditDialogExpose>()
const packagePermissionDialogRef = ref<PackagePermissionDialogExpose>()
const packageDeleteDialogRef = ref<PackageDeleteDialogExpose>()

const { statusVariant, formatDate, formatLimit } = useUserPoolPackageDict()

const activeCount = computed(() => {
  return packages.value.filter((item) => {
    return item.status === 'active'
  }).length
})

const defaultPackage = computed(() => {
  return packages.value.find((item) => {
    return item.defaultPackage
  })
})

const totalPermissionCount = computed(() => {
  return new Set(packages.value.flatMap((item) => {
    return item.permissionCodes
  })).size
})

const filteredPackages = computed(() => {
  return packages.value.filter((item) => {
    return !search.value || `${item.name} ${item.code}`.toLowerCase().includes(search.value.toLowerCase())
  })
})

const packageColumns: TableProps<UserPoolPackage>['columns'] = [
  { title: '套餐', key: 'package', width: 250 },
  { title: '状态', key: 'status', width: 110 },
  { title: '资源配额', key: 'quota', width: 280 },
  { title: '权限', key: 'permissions', width: 120 },
  { title: '更新时间', key: 'updatedAt', width: 190 },
  { title: '操作', key: 'actions', width: 300, fixed: 'end' },
]

const loadData = async () => {
  loading.value = true
  try {
    const [packageRows, permissions] = await Promise.all([
      userPoolPackageApi.list(),
      userPoolPackageApi.permissionCatalog(),
    ])
    packages.value = packageRows
    permissionCatalog.value = permissions
  } catch (error) {
    console.error('加载用户池套餐失败:', error)
    message.error('加载用户池套餐失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  packageEditDialogRef.value?.open()
}

const openEditDialog = (item: UserPoolPackage) => {
  packageEditDialogRef.value?.open(item)
}

const openPermissionDialog = (item: UserPoolPackage) => {
  packagePermissionDialogRef.value?.open(item)
}

const openDeleteDialog = (item: UserPoolPackage) => {
  packageDeleteDialogRef.value?.open(item)
}

const handlePickPermissions = (codes: string[]) => {
  packagePermissionDialogRef.value?.openForSelect(codes)
}

const handlePermissionSelect = (codes: string[]) => {
  packageEditDialogRef.value?.setPermissionCodes(codes)
  packageEditDialogRef.value?.reopen()
}

const handlePermissionCancelSelect = () => {
  packageEditDialogRef.value?.reopen()
}

const toggleStatus = async (item: UserPoolPackage) => {
  const nextStatus: UserPoolPackageStatus = item.status === 'active' ? 'inactive' : 'active'
  try {
    await userPoolPackageApi.updateStatus(item.id, nextStatus)
    message.success(nextStatus === 'active' ? '套餐已启用' : '套餐已停用')
    await loadData()
  } catch (error) {
    console.error('更新套餐状态失败:', error)
    message.error('更新套餐状态失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="p-6 min-h-[calc(100vh-64px)] space-y-6">
    <div class="flex justify-between items-start mb-6">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#102A43] mb-2">用户池套餐</h1>
        <p class="text-sm text-[#52677D]">配置用户池的资源配额和可用管理权限。</p>
      </div>
      <div class="flex gap-3">
        <a-button :disabled="loading" @click="loadData">
          <RefreshCw class="size-4 mr-2" :class="loading ? 'animate-spin' : ''" />
          刷新
        </a-button>
        <a-button @click="openCreateDialog">
          <Plus class="size-4 mr-2" />
          新建套餐
        </a-button>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <a-card>
        <div class="pb-3"><p class="text-sm text-[#52677D]">套餐总数</p><h3 class="text-2xl font-bold text-[#102A43]">{{ packages.length }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-[#52677D]"><Package class="size-3" /> 已配置的套餐</div>
      </a-card>
      <a-card>
        <div class="pb-3"><p class="text-sm text-[#52677D]">启用中</p><h3 class="text-2xl font-bold text-[#102A43]">{{ activeCount }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-[#52677D]"><Check class="size-3" /> 可分配给新用户池</div>
      </a-card>
      <a-card>
        <div class="pb-3"><p class="text-sm text-[#52677D]">默认套餐</p><h3 class="truncate text-2xl font-bold text-[#102A43]">{{ defaultPackage?.name || '-' }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-[#52677D]"><Gauge class="size-3" /> 新用户池的默认方案</div>
      </a-card>
      <a-card>
        <div class="pb-3"><p class="text-sm text-[#52677D]">已使用权限</p><h3 class="text-2xl font-bold text-[#102A43]">{{ totalPermissionCount }}</h3></div>
        <div class="flex items-center gap-2 text-xs text-[#52677D]"><ShieldCheck class="size-3" /> 去重后的权限项</div>
      </a-card>
    </div>

    <a-alert
      type="info"
      show-icon
      title="权限作用域提示"
      description="这里只能配置用户池作用域权限；平台级权限不会出现在权限目录中。"
      class="mb-3!"
    />

    <a-card>
      <div class="gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h3>套餐列表</h3>
          <p>套餐变更会影响已绑定该套餐的普通用户池。</p>
        </div>
        <div class="relative w-full md:w-72">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-[#52677D]" />
          <a-input v-model:value="search" class="pl-9" placeholder="搜索套餐名称或编码" />
        </div>
      </div>
      <div>
        <a-spin v-if="loading" class="flex min-h-40 items-center justify-center" />
        <a-empty v-else-if="filteredPackages.length === 0" class="flex min-h-40 flex-col items-center justify-center" description="暂无匹配套餐" />
        <a-table
          v-else
          :columns="packageColumns"
          :data-source="filteredPackages"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 1250 }"
        >
          <template #bodyCell="{ column, record: item }">
            <template v-if="column.key === 'package'">
                <div class="flex items-center gap-3">
                  <div class="flex size-9 items-center justify-center rounded-lg bg-[#0369A1]/10 text-[#0369A1]"><Package /></div>
                  <div>
                    <div class="flex items-center gap-2 font-medium">{{ item.name }}<a-tag v-if="item.defaultPackage" color="blue">默认</a-tag></div>
                    <code class="text-xs text-[#52677D]">{{ item.code }}</code>
                  </div>
                </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusVariant(item.status)">{{ item.status === 'active' ? '启用中' : '已停用' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'quota'">
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-[#52677D]">
                  <span class="inline-flex items-center gap-1"><Users /> {{ formatLimit(item.maxUsers) }} 用户</span>
                  <span class="inline-flex items-center gap-1"><AppWindow /> {{ formatLimit(item.maxApps) }} 应用</span>
                </div>
            </template>
            <template v-else-if="column.key === 'permissions'">
              <button class="cursor-pointer text-left text-sm text-[#0369A1] underline-offset-4 transition-colors hover:underline" @click="openPermissionDialog(item)">{{ item.permissionCodes.length }} 项权限</button>
            </template>
            <template v-else-if="column.key === 'updatedAt'">
              <span class="text-sm text-[#52677D]">{{ formatDate(item.updatedAt) }}</span>
            </template>
            <template v-else-if="column.key === 'actions'">
                <div class="flex flex-wrap justify-end gap-1">
                  <a-button type="link" size="small" class="h-auto p-0" @click="openPermissionDialog(item)"><ShieldCheck class="size-3 mr-1" />权限</a-button>
                  <a-button type="link" size="small" class="h-auto p-0" @click="openEditDialog(item)"><Pencil class="size-3 mr-1" />编辑</a-button>
                  <a-button type="link" size="small" class="h-auto p-0" :disabled="item.defaultPackage" @click="toggleStatus(item)">{{ item.status === 'active' ? '停用' : '启用' }}</a-button>
                  <a-button type="link" size="small" class="h-auto p-0 text-[#B91C1C]" :disabled="item.defaultPackage" @click="openDeleteDialog(item)"><Trash2 class="size-3 mr-1" />删除</a-button>
                </div>
            </template>
          </template>
        </a-table>
      </div>
    </a-card>

    <PackageEditDialog ref="packageEditDialogRef" @success="loadData" @pick-permissions="handlePickPermissions" />

    <PackagePermissionDialog
      ref="packagePermissionDialogRef"
      :permissions="permissionCatalog"
      @success="loadData"
      @select="handlePermissionSelect"
      @cancel-select="handlePermissionCancelSelect"
    />

    <PackageDeleteDialog ref="packageDeleteDialogRef" @success="loadData" />
  </div>
</template>
