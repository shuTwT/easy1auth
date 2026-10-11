<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Database, Plus, RefreshCw } from '@lucide/vue'
import { permissionApi } from '@/api/permission'
import type { PermissionSpace, PermissionTree } from '@/types/permission'
import ResourceEditDialog from '../components/ResourceEditDialog.vue'
import SpaceEditDialog from '../components/SpaceEditDialog.vue'
import { useSpaceList, useSpaceOptions, useVisibleResources } from '../utils/hooks'
import type { ResourceEditDialogExpose, SpaceEditDialogExpose } from '../utils/types'

const loading = ref(false)
const resources = ref<PermissionTree[]>([])
const selectedSpaceId = ref<string>()

const resourceEditDialogRef = ref<ResourceEditDialogExpose>()
const spaceEditDialogRef = ref<SpaceEditDialogExpose>()

const { spaces, spacesLoading, loadSpaces, mergeSpace } = useSpaceList()
const { spaceOptions, getSpaceName } = useSpaceOptions(spaces)
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

const handleOpenSpaceDialog = () => {
  spaceEditDialogRef.value?.open()
}

const handleSpaceSaved = async (saved: PermissionSpace, created: boolean) => {
  await mergeSpace(saved)
  if (created) {
    resourceEditDialogRef.value?.selectSpace(saved.id)
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
        <h1 class="mb-2 text-2xl font-bold text-[#102A43]">数据资源管理</h1>
        <p class="text-sm text-[#52677D]">维护权限空间下的数据资源，并配置资源层级与可执行操作。</p>
      </div>
      <div class="flex gap-2">
        <a-button :disabled="loading" @click="loadData">
          <RefreshCw class="mr-2 size-4" :class="loading ? 'animate-spin' : ''" />
          刷新
        </a-button>
        <a-button type="primary" @click="handleAddResource">
          <Plus class="mr-2 size-4" />
          新建数据资源
        </a-button>
      </div>
    </div>

    <a-card>
      <div class="mb-4 flex items-center gap-3">
        <Database class="size-5 text-[#0369A1]" />
        <div class="flex-1">
          <div class="font-semibold">数据资源</div>
          <div class="text-xs text-[#52677D]">资源名称、编码、权限空间及操作权限</div>
        </div>
        <a-select
          v-model:value="selectedSpaceId"
          class="w-64!"
          allow-clear
          placeholder="全部权限空间"
          :options="spaceOptions"
          :loading="spacesLoading"
          option-filter-prop="label"
          show-search
        />
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

    <ResourceEditDialog
      ref="resourceEditDialogRef"
      :resources="resources"
      :spaces="spaces"
      :spaces-loading="spacesLoading"
      @success="loadData"
      @create-space="handleOpenSpaceDialog"
      @refresh-spaces="loadSpaces"
    />
    <SpaceEditDialog ref="spaceEditDialogRef" @success="handleSpaceSaved" />
  </div>
</template>
