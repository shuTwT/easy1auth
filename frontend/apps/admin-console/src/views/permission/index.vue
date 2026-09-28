<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import type {FormInstance} from 'antdv-next'
import {message} from 'antdv-next'
import {Database, FolderTree, Plus, Pencil, Trash2, RefreshCw} from '@lucide/vue'
import {permissionApi} from '@/api/permission'
import type {Permission, PermissionSpace, PermissionSpaceInput, CreatePermissionDto} from '@/types/permission'
import type {PermissionTree} from '@/types/permission'

const loading = ref(false)
const spacesLoading = ref(false)
const resources = ref<PermissionTree[]>([])
const spaces = ref<PermissionSpace[]>([])
const selectedSpaceId = ref<string>()
const resourceDialogVisible = ref(false)
const spaceDialogVisible = ref(false)
const resourceEditing = ref<Permission | null>(null)
const spaceEditing = ref<PermissionSpace | null>(null)
const submitting = ref(false)
const spaceSubmitting = ref(false)
const spaceFormRef = ref<FormInstance>()
let spaceLoadVersion = 0
const resourceFormRef = ref<FormInstance>()
const resourceForm = reactive<CreatePermissionDto>({
  name: '',
  code: '',
  description: '',
  type: 'data',
  parentId: undefined,
  resource: '',
  action: '',
  spaceId: undefined,
  operations: []
})
const spaceForm = reactive<PermissionSpaceInput>({name: '', code: '', description: ''})
const resourceColumns = [
  {title: '资源名称', key: 'name', dataIndex: 'name', width: 240}, {
    title: '资源编码',
    key: 'code',
    dataIndex: 'code',
    width: 210
  },
  {title: '权限空间', key: 'space', width: 170}, {title: '资源描述', key: 'description', width: 260},
  {title: '上级资源权限', key: 'parent', width: 180}, {
    title: '资源操作',
    key: 'operations',
    width: 260
  }, {title: '操作', key: 'actions', width: 130, fixed: 'end' as const},
]
const spaceOptions = computed(() => spaces.value.map((space) => ({
  value: space.id,
  label: `${space.name}（${space.code}）`,
})))
const commonOperationOptions = [
  { value: 'read', label: '读取（read）' },
  { value: 'create', label: '创建（create）' },
  { value: 'update', label: '更新（update）' },
  { value: 'delete', label: '删除（delete）' },
]
const operationOptions = computed(() => {
  const options = new Map(commonOperationOptions.map(option => [option.value, option]))
  const addOperation = (value: string) => {
    const operation = value.trim()
    if (operation && !options.has(operation)) {
      options.set(operation, { value: operation, label: operation })
    }
  }
  const collect = (nodes: PermissionTree[]) => {
    nodes.forEach(node => {
      ;(node.operations || []).forEach(addOperation)
      if (node.action) addOperation(node.action)
      collect(node.children || [])
    })
  }
  collect(resources.value)
  ;(resourceForm.operations || []).forEach(addOperation)
  return [...options.values()]
})
const visibleResources = computed(() => {
  if (!selectedSpaceId.value) return resources.value
  const filter = (nodes: PermissionTree[]): PermissionTree[] => nodes.map((node) => ({
    ...node,
    children: filter(node.children || [])
  })).filter((node) => node.spaceId === selectedSpaceId.value || node.children.length > 0)
  return filter(resources.value)
})

function spaceName(id?: string | null) {
  return spaces.value.find((item) => item.id === id)?.name || '-'
}

async function loadResources() {
  loading.value = true
  try {
    resources.value = await permissionApi.getTree()
  } catch {
    message.error('加载数据资源失败')
  } finally {
    loading.value = false
  }
}

async function loadSpaces() {
  const version = ++spaceLoadVersion
  spacesLoading.value = true
  try {
    const rows = await permissionApi.getSpaces()
    if (version === spaceLoadVersion) spaces.value = rows
  } catch {
    if (version === spaceLoadVersion) message.error('加载权限空间失败，请重试')
  } finally {
    if (version === spaceLoadVersion) spacesLoading.value = false
  }
}

async function loadData() {
  await Promise.all([loadResources(), loadSpaces()])
}

function openSpaceDialog(space?: PermissionSpace) {
  spaceEditing.value = space || null;
  Object.assign(spaceForm, {name: space?.name || '', code: space?.code || '', description: space?.description || ''});
  spaceDialogVisible.value = true
}

function openResourceDialog(resource?: PermissionTree) {
  resourceEditing.value = resource ? ({...resource} as unknown as Permission) : null;
  Object.assign(resourceForm, {
    name: resource?.name || '',
    code: resource?.code || '',
    description: resource?.description || '',
    type: 'data',
    parentId: undefined,
    resource: resource?.resource || '',
    action: resource?.action || '',
    spaceId: resource?.spaceId || selectedSpaceId.value,
    operations: [...(resource?.operations || [])]
  });
  resourceDialogVisible.value = true
  loadSpaces()
}

async function saveSpace() {
  if (spaceSubmitting.value) return
  try {
    await spaceFormRef.value?.validateFields()
  } catch {
    return
  }
  spaceSubmitting.value = true
  try {
    const editing = spaceEditing.value
    const input = { ...spaceForm, name: spaceForm.name.trim(), code: spaceForm.code.trim() }
    const saved = editing
      ? await permissionApi.updateSpace(editing.id, input)
      : await permissionApi.createSpace(input)
    spaceLoadVersion += 1
    spacesLoading.value = false
    spaces.value = [...spaces.value.filter(space => space.id !== saved.id), saved]
    if (!editing && resourceDialogVisible.value) resourceForm.spaceId = saved.id
    message.success(editing ? '权限空间已更新' : '权限空间已创建')
    spaceDialogVisible.value = false
    await loadSpaces()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存权限空间失败')
  } finally {
    spaceSubmitting.value = false
  }
}

async function removeSpace(space: PermissionSpace) {
  try {
    await permissionApi.deleteSpace(space.id);
    message.success('权限空间已删除');
    if (selectedSpaceId.value === space.id) selectedSpaceId.value = undefined;
    await loadData()
  } catch {
    message.error('删除权限空间失败')
  }
}

async function saveResource() {
  try {
    await resourceFormRef.value?.validateFields()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (resourceEditing.value) {
      await permissionApi.update(resourceEditing.value.id, resourceForm)
    } else {
      await permissionApi.create(resourceForm)
    }
    message.success(resourceEditing.value ? '数据资源已更新' : '数据资源已创建')
    resourceDialogVisible.value = false
    await loadData()
  } catch {
    message.error('保存数据资源失败')
  } finally {
    submitting.value = false
  }
}

async function removeResource(resource: PermissionTree) {
  try {
    await permissionApi.delete(resource.id)
    message.success('数据资源已删除')
    await loadData()
  } catch (error) {
    message.error(error instanceof Error && error.message ? error.message : '删除数据资源失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] p-6">
    <div class="mb-6 flex items-start justify-between">
      <div><h1 class="mb-2 text-2xl font-bold text-foreground">数据资源权限</h1>
        <p class="text-sm text-muted-foreground">按权限空间组织数据资源，并配置资源层级与可执行操作。</p></div>
      <div class="flex gap-2">
        <a-button @click="loadData">
          <RefreshCw class="mr-2 size-4"/>
          刷新
        </a-button>
        <a-button type="primary" @click="openResourceDialog()">
          <Plus class="mr-2 size-4"/>
          新建数据资源
        </a-button>
      </div>
    </div>
    <div class="grid grid-cols-1 gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
      <a-card class="h-fit">
        <div class="mb-4 flex items-center justify-between">
          <div class="flex items-center gap-2 font-semibold">
            <FolderTree class="size-4 text-primary"/>
            权限空间
          </div>
          <a-button type="link" size="small" class="p-0" @click="openSpaceDialog()">
            <Plus class="size-4"/>
          </a-button>
        </div>
        <a-spin :spinning="spacesLoading">
          <div class="space-y-2">
            <button
                class="flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm hover:bg-muted"
                :class="!selectedSpaceId ? 'bg-primary/10 text-primary' : ''" @click="selectedSpaceId = undefined">
              <span>全部资源</span>
              <a-tag>{{ resources.length }}</a-tag>
            </button>
            <div v-for="space in spaces" :key="space.id" class="group flex items-center gap-1">
              <button class="min-w-0 flex-1 rounded-md px-3 py-2 text-left hover:bg-muted"
                      :class="selectedSpaceId === space.id ? 'bg-primary/10 text-primary' : ''"
                      @click="selectedSpaceId = space.id">
                <div class="truncate text-sm font-medium">{{ space.name }}</div>
                <div class="truncate text-xs text-muted-foreground">{{ space.code }}</div>
              </button>
              <a-button type="link" size="small" class="hidden p-1 group-hover:inline-flex"
                        @click="openSpaceDialog(space)">
                <Pencil class="size-3.5"/>
              </a-button>
              <a-button type="link" size="small" danger class="hidden p-1 group-hover:inline-flex"
                        @click="removeSpace(space)">
                <Trash2 class="size-3.5"/>
              </a-button>
            </div>
            <a-empty v-if="!spaces.length" description="暂无权限空间"/>
          </div>
        </a-spin>
      </a-card>
      <a-card>
        <div class="mb-4 flex items-center gap-3">
          <Database class="size-5 text-primary"/>
          <div>
            <div class="font-semibold">数据资源</div>
            <div class="text-xs text-muted-foreground">资源名称、编码、权限空间及操作权限</div>
          </div>
        </div>
        <a-table :columns="resourceColumns" :data-source="visibleResources" :loading="loading" row-key="id"
                 :pagination="false" :expandable="{ defaultExpandAllRows: true, indentSize: 20 }" :scroll="{ x: 1450 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="flex items-center gap-2">
                <Database class="size-4 shrink-0 text-primary"/>
                <span class="font-medium">{{ record.name }}</span></div>
            </template>
            <template v-else-if="column.key === 'code'"><code class="text-xs text-muted-foreground">{{
                record.code
              }}</code></template>
            <template v-else-if="column.key === 'space'">
              <a-tag color="blue">{{ spaceName(record.spaceId) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'description'"><span
                class="text-sm text-muted-foreground">{{ record.description || '-' }}</span></template>
            <template v-else-if="column.key === 'parent'"><span
                class="text-sm text-muted-foreground">{{ record.parent?.name || '-' }}</span></template>
            <template v-else-if="column.key === 'operations'">
              <div class="flex flex-wrap gap-1">
                <a-tag v-for="operation in (record.operations || [record.action])" :key="operation" color="green">
                  {{ operation }}
                </a-tag>
              </div>
            </template>
            <template v-else-if="column.key === 'actions'">
              <div class="flex gap-2">
                <a-button type="link" size="small" class="p-0" @click="openResourceDialog(record)">编辑</a-button>
                <a-popconfirm title="确认删除该数据资源吗？" description="删除后将无法恢复。" ok-text="确认删除"
                              cancel-text="取消" ok-type="danger" @confirm="removeResource(record)">
                  <a-button type="link" size="small" danger class="p-0">删除</a-button>
                </a-popconfirm>
              </div>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
    <a-modal v-model:open="spaceDialogVisible" :title="spaceEditing ? '编辑权限空间' : '新建权限空间'"
             :confirm-loading="spaceSubmitting" :z-index="resourceDialogVisible ? 1100 : 1000" @ok="saveSpace">
      <a-form ref="spaceFormRef" :model="spaceForm" layout="vertical" class="py-4" :disabled="spaceSubmitting">
        <a-form-item name="name" label="空间名称" :rules="[{ required: true, whitespace: true, message: '请输入空间名称' }]">
          <a-input v-model:value="spaceForm.name" placeholder="例如：组织架构"/>
        </a-form-item>
        <a-form-item name="code" label="空间编码" :rules="[{ required: true, whitespace: true, message: '请输入空间编码' }]">
          <a-input v-model:value="spaceForm.code" :disabled="!!spaceEditing" placeholder="例如：organization"/>
        </a-form-item>
        <a-form-item label="空间描述">
          <a-textarea v-model:value="spaceForm.description" :rows="3"/>
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal v-model:open="resourceDialogVisible" :title="resourceEditing ? '编辑数据资源' : '新建数据资源'"
             :confirm-loading="submitting" @ok="saveResource">
      <a-form ref="resourceFormRef" :model="resourceForm" layout="vertical" class="py-4">
        <a-form-item name="name" label="资源名称"
                     :rules="[{ required: true, whitespace: true, message: '请输入资源名称' }]">
          <a-input v-model:value="resourceForm.name" placeholder="例如：用户"/>
        </a-form-item>
        <a-form-item name="code" label="资源编码"
                     :rules="[{ required: true, whitespace: true, message: '请输入资源编码' }]">
          <a-input v-model:value="resourceForm.code" :disabled="!!resourceEditing" placeholder="例如：user"/>
        </a-form-item>
        <a-form-item name="spaceId" label="权限空间" :rules="[{ required: true, message: '请选择权限空间' }]">
          <a-select v-model:value="resourceForm.spaceId" allow-clear show-search option-filter-prop="label"
                    class="w-full" :options="spaceOptions" :loading="spacesLoading" placeholder="请选择权限空间">
            <template #notFoundContent>
              <a-spin v-if="spacesLoading" size="small" />
              <span v-else>{{ spaces.length ? '暂无匹配的权限空间' : '暂无可选权限空间，请先新建' }}</span>
            </template>
          </a-select>
          <div class="mt-2 flex gap-3">
            <a-button type="link" size="small" class="p-0!" @click="openSpaceDialog()">新建权限空间</a-button>
            <a-button type="link" size="small" class="p-0!" :disabled="spacesLoading" @click="loadSpaces">刷新选项</a-button>
          </div>
        </a-form-item>
        <a-form-item name="parentId" label="上级资源权限">
          <a-tree-select v-model:value="resourceForm.parentId" class="w-full" :tree-data="resources"
                         :field-names="{ label: 'name', value: 'id', children: 'children' }" allow-clear
                         tree-default-expand-all placeholder="请选择上级资源"/>
        </a-form-item>
        <a-form-item name="operations" label="资源操作">
          <a-select v-model:value="resourceForm.operations" mode="tags" class="w-full" :options="operationOptions"
                    option-filter-prop="label" placeholder="请选择操作，也可输入自定义操作后回车"/>
        </a-form-item>
        <a-form-item name="description" label="资源描述">
          <a-textarea v-model:value="resourceForm.description" :rows="3"/>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
