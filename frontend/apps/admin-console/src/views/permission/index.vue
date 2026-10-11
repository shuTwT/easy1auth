<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Database, FolderTree, Plus, Pencil, Trash2, RefreshCw } from '@lucide/vue'
import { permissionApi } from '@/api/permission'
import type { PermissionSpace, PermissionTree } from '@/types/permission'
import ResourceEditDialog from './components/ResourceEditDialog.vue'
import SpaceEditDialog from './components/SpaceEditDialog.vue'
import { useSpaceOptions, useVisibleResources } from './utils/hooks'
import type { ResourceEditDialogExpose, SpaceEditDialogExpose } from './utils/types'

const loading = ref(false)
const spacesLoading = ref(false)
const resources = ref<PermissionTree[]>([])
const spaces = ref<PermissionSpace[]>([])
const selectedSpaceId = ref<string>()
let spaceLoadVersion = 0

const resourceEditDialogRef = ref<ResourceEditDialogExpose>()
const spaceEditDialogRef = ref<SpaceEditDialogExpose>()

const { getSpaceName } = useSpaceOptions(spaces)
const { visibleResources } = useVisibleResources(resources, selectedSpaceId)

const resourceColumns: TableProps<PermissionTree>['columns'] = [
  { title: '资源名称', key: 'name', dataIndex: 'name', width: 240 },
  { title: '资源编码', key: 'code', dataIndex: 'code', width: 210 },
  { title: '权限空间', key: 'space', width: 170 },
  { title: '资源描述', key: 'description', width: 260 },
  { title: '上级资源权限', key: 'parent', width: 180 },
  { title: '资源操作', key: 'operations', width: 260 },
  { title: '操作', key: 'actions', width: 130, fixed: 'end' }
]

const loadResources = async () => {
  loading.value = true
  try {
    resources.value = await permissionApi.getTree()
  } catch (error: unknown) {
    console.error('加载数据资源失败:', error)
    message.error('加载数据资源失败')
  } finally {
    loading.value = false
  }
}

const loadSpaces = async () => {
  const version = ++spaceLoadVersion
  spacesLoading.value = true
  try {
    const rows = await permissionApi.getSpaces()
    if (version === spaceLoadVersion) {
      spaces.value = rows
    }
  } catch (error: unknown) {
    console.error('加载权限空间失败:', error)
    if (version === spaceLoadVersion) {
      message.error('加载权限空间失败，请重试')
    }
  } finally {
    if (version === spaceLoadVersion) {
      spacesLoading.value = false
    }
  }
}

const loadData = async () => {
  await Promise.all([loadResources(), loadSpaces()])
}

const handleAddResource = () => {
  resourceEditDialogRef.value?.open(undefined, selectedSpaceId.value)
  loadSpaces()
}

const handleEditResource = (resource: PermissionTree) => {
  resourceEditDialogRef.value?.open(resource, selectedSpaceId.value)
  loadSpaces()
}

const handleOpenSpaceDialog = (space?: PermissionSpace) => {
  spaceEditDialogRef.value?.open(space)
}

const handleSpaceSaved = async (saved: PermissionSpace, created: boolean) => {
  // 使进行中的空间加载结果失效，并先本地合并，避免下拉出现空窗
  spaceLoadVersion += 1
  spacesLoading.value = false
  spaces.value = [...spaces.value.filter((space) => space.id !== saved.id), saved]
  if (created) {
    resourceEditDialogRef.value?.selectSpace(saved.id)
  }
  await loadSpaces()
}

const removeSpace = async (space: PermissionSpace) => {
  try {
    await permissionApi.deleteSpace(space.id)
    message.success('权限空间已删除')
    if (selectedSpaceId.value === space.id) {
      selectedSpaceId.value = undefined
    }
    await loadData()
  } catch (error: unknown) {
    console.error('删除权限空间失败:', error)
    message.error('删除权限空间失败')
  }
}

const removeResource = async (resource: PermissionTree) => {
  try {
    await permissionApi.delete(resource.id)
    message.success('数据资源已删除')
    await loadData()
  } catch (error: unknown) {
    console.error('删除数据资源失败:', error)
    message.error(error instanceof Error && error.message ? error.message : '删除数据资源失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] p-6">
    <div class="mb-6 flex items-start justify-between">
      <div>
        <h1 class="mb-2 text-2xl font-bold text-[#102A43]">数据资源权限</h1>
        <p class="text-sm text-[#52677D]">按权限空间组织数据资源，并配置资源层级与可执行操作。</p>
      </div>
      <div class="flex gap-2">
        <a-button @click="loadData">
          <RefreshCw class="mr-2 size-4" />
          刷新
        </a-button>
        <a-button type="primary" @click="handleAddResource">
          <Plus class="mr-2 size-4" />
          新建数据资源
        </a-button>
      </div>
    </div>
    <div class="grid grid-cols-1 gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
      <a-card class="h-fit">
        <div class="mb-4 flex items-center justify-between">
          <div class="flex items-center gap-2 font-semibold">
            <FolderTree class="size-4 text-[#0369A1]" />
            权限空间
          </div>
          <a-button type="link" size="small" class="p-0" @click="handleOpenSpaceDialog()">
            <Plus class="size-4" />
          </a-button>
        </div>
        <a-spin :spinning="spacesLoading">
          <div class="space-y-2">
            <button
              class="flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm hover:bg-[#F4F8FB]"
              :class="!selectedSpaceId ? 'bg-[#0369A1]/10 text-[#0369A1]' : ''"
              @click="selectedSpaceId = undefined"
            >
              <span>全部资源</span>
              <a-tag>{{ resources.length }}</a-tag>
            </button>
            <div v-for="space in spaces" :key="space.id" class="group flex items-center gap-1">
              <button
                class="min-w-0 flex-1 rounded-md px-3 py-2 text-left hover:bg-[#F4F8FB]"
                :class="selectedSpaceId === space.id ? 'bg-[#0369A1]/10 text-[#0369A1]' : ''"
                @click="selectedSpaceId = space.id"
              >
                <div class="truncate text-sm font-medium">{{ space.name }}</div>
                <div class="truncate text-xs text-[#52677D]">{{ space.code }}</div>
              </button>
              <a-button type="link" size="small" class="hidden p-1 group-hover:inline-flex" @click="handleOpenSpaceDialog(space)">
                <Pencil class="size-3.5" />
              </a-button>
              <a-button type="link" size="small" danger class="hidden p-1 group-hover:inline-flex" @click="removeSpace(space)">
                <Trash2 class="size-3.5" />
              </a-button>
            </div>
            <a-empty v-if="!spaces.length" description="暂无权限空间" />
          </div>
        </a-spin>
      </a-card>
      <a-card>
        <div class="mb-4 flex items-center gap-3">
          <Database class="size-5 text-[#0369A1]" />
          <div>
            <div class="font-semibold">数据资源</div>
            <div class="text-xs text-[#52677D]">资源名称、编码、权限空间及操作权限</div>
          </div>
        </div>
        <a-table
          :columns="resourceColumns"
          :data-source="visibleResources"
          :loading="loading"
          row-key="id"
          :pagination="false"
          :expandable="{ defaultExpandAllRows: true, indentSize: 20 }"
          :scroll="{ x: 1450 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="flex items-center gap-2">
                <Database class="size-4 shrink-0 text-[#0369A1]" />
                <span class="font-medium">{{ record.name }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'code'">
              <code class="text-xs text-[#52677D]">{{ record.code }}</code>
            </template>
            <template v-else-if="column.key === 'space'">
              <a-tag color="blue">{{ getSpaceName(record.spaceId) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="text-sm text-[#52677D]">{{ record.description || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'parent'">
              <span class="text-sm text-[#52677D]">{{ record.parent?.name || '-' }}</span>
            </template>
            <template v-else-if="column.key === 'operations'">
              <div class="flex flex-wrap gap-1">
                <a-tag v-for="operation in (record.operations || [record.action])" :key="operation" color="green">
                  {{ operation }}
                </a-tag>
              </div>
            </template>
            <template v-else-if="column.key === 'actions'">
              <div class="flex gap-2">
                <a-button type="link" size="small" class="p-0" @click="handleEditResource(record as PermissionTree)">编辑</a-button>
                <a-popconfirm
                  title="确认删除该数据资源吗？"
                  description="删除后将无法恢复。"
                  ok-text="确认删除"
                  cancel-text="取消"
                  ok-type="danger"
                  @confirm="removeResource(record as PermissionTree)"
                >
                  <a-button type="link" size="small" danger class="p-0">删除</a-button>
                </a-popconfirm>
              </div>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <ResourceEditDialog
      ref="resourceEditDialogRef"
      :resources="resources"
      :spaces="spaces"
      :spaces-loading="spacesLoading"
      @success="loadData"
      @create-space="handleOpenSpaceDialog()"
      @refresh-spaces="loadSpaces"
    />
    <SpaceEditDialog ref="spaceEditDialogRef" @success="handleSpaceSaved" />
  </div>
</template>
