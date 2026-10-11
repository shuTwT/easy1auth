<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { FolderTree, Plus, RefreshCw, Search } from '@lucide/vue'
import { permissionApi } from '@/api/permission'
import type { PermissionSpace } from '@/types/permission'
import SpaceEditDialog from '../components/SpaceEditDialog.vue'
import { useSpaceList } from '../utils/hooks'
import type { SpaceEditDialogExpose } from '../utils/types'

const spaceEditDialogRef = ref<SpaceEditDialogExpose>()
const search = ref('')

const { spaces, spacesLoading, loadSpaces, mergeSpace } = useSpaceList()

const columns: TableProps<PermissionSpace>['columns'] = [
  { title: '空间名称', key: 'name', dataIndex: 'name', width: 220 },
  { title: '空间编码', key: 'code', dataIndex: 'code', width: 200 },
  { title: '空间描述', key: 'description', width: 320 },
  { title: '创建时间', key: 'createdAt', width: 190 },
  { title: '更新时间', key: 'updatedAt', width: 190 },
  { title: '操作', key: 'actions', width: 130, fixed: 'end' }
]

const formatTime = (value: string) => {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

const loadData = () => {
  return loadSpaces(search.value.trim() || undefined)
}

const handleSearch = () => {
  loadData()
}

/** 清空搜索词后自动回到全量列表。 */
const handleSearchChange = () => {
  if (!search.value) {
    loadData()
  }
}

const handleOpenDialog = (space?: PermissionSpace) => {
  spaceEditDialogRef.value?.open(space)
}

const handleSaved = (saved: PermissionSpace) => {
  return mergeSpace(saved)
}

const removeSpace = async (space: PermissionSpace) => {
  try {
    await permissionApi.deleteSpace(space.id)
    message.success('权限空间已删除')
    await loadData()
  } catch (error: unknown) {
    console.error('删除权限空间失败:', error)
    message.error('删除权限空间失败，请确认空间下已没有数据资源')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] p-6">
    <div class="mb-6 flex items-start justify-between">
      <div>
        <h1 class="mb-2 text-2xl font-bold text-[#102A43]">权限空间管理</h1>
        <p class="text-sm text-[#52677D]">维护数据资源的权限空间，供数据资源归类与授权范围划分使用。</p>
      </div>
      <div class="flex gap-2">
        <a-button :disabled="spacesLoading" @click="loadData">
          <RefreshCw class="mr-2 size-4" :class="spacesLoading ? 'animate-spin' : ''" />
          刷新
        </a-button>
        <a-button type="primary" @click="handleOpenDialog()">
          <Plus class="mr-2 size-4" />
          新建权限空间
        </a-button>
      </div>
    </div>

    <a-card>
      <div class="mb-4 flex items-center gap-3">
        <FolderTree class="size-5 text-[#0369A1]" />
        <div class="flex-1">
          <div class="font-semibold">权限空间</div>
          <div class="text-xs text-[#52677D]">空间名称、编码与描述</div>
        </div>
        <a-input
          v-model:value="search"
          class="w-64!"
          allow-clear
          placeholder="搜索空间名称或编码"
          @press-enter="handleSearch"
          @change="handleSearchChange"
        >
          <template #prefix>
            <Search class="size-4 text-[#52677D]" />
          </template>
        </a-input>
        <a-button @click="handleSearch">搜索</a-button>
      </div>
      <a-table
        :columns="columns"
        :data-source="spaces"
        :loading="spacesLoading"
        row-key="id"
        :pagination="false"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <code class="text-xs text-[#52677D]">{{ record.code }}</code>
          </template>
          <template v-else-if="column.key === 'description'">
            <span class="text-sm text-[#52677D]">{{ record.description || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            <span class="text-sm text-[#52677D]">{{ formatTime(record.createdAt) }}</span>
          </template>
          <template v-else-if="column.key === 'updatedAt'">
            <span class="text-sm text-[#52677D]">{{ formatTime(record.updatedAt) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="flex gap-2">
              <a-button type="link" size="small" class="p-0" @click="handleOpenDialog(record as PermissionSpace)">编辑</a-button>
              <a-popconfirm
                title="确认删除该权限空间吗？"
                description="仅当空间下没有数据资源时才能删除，删除后无法恢复。"
                ok-text="确认删除"
                cancel-text="取消"
                ok-type="danger"
                @confirm="removeSpace(record as PermissionSpace)"
              >
                <a-button type="link" size="small" danger class="p-0">删除</a-button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <SpaceEditDialog ref="spaceEditDialogRef" @success="handleSaved" />
  </div>
</template>
