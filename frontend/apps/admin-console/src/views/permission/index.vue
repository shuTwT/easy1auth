<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance } from 'antdv-next'
import { Table, Form, FormItem, message } from 'antdv-next'
import { Database, FolderTree, Plus, Pencil, Trash2, RefreshCw } from '@lucide/vue'
import { permissionApi } from '@/api/permission'
import type { Permission, PermissionSpace, PermissionSpaceInput, CreatePermissionDto } from '@/types/permission'
import type { PermissionTree } from '@/types/permission'

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
const resourceFormRef = ref<FormInstance>()
const resourceForm = reactive<CreatePermissionDto>({ name: '', code: '', description: '', type: 'data', parentId: undefined, resource: '', action: '', spaceId: undefined, operations: [] })
const spaceForm = reactive<PermissionSpaceInput>({ name: '', code: '', description: '' })
const resourceColumns = [
  { title: '资源名称', key: 'name', dataIndex: 'name', width: 240 }, { title: '资源编码', key: 'code', dataIndex: 'code', width: 210 },
  { title: '权限空间', key: 'space', width: 170 }, { title: '资源描述', key: 'description', width: 260 },
  { title: '上级资源权限', key: 'parent', width: 180 }, { title: '资源操作', key: 'operations', width: 260 }, { title: '操作', key: 'actions', width: 130, fixed: 'end' as const },
]
const spaceOptions = computed(() => spaces.value.map((space) => ({
  value: space.id,
  label: `${space.name}（${space.code}）`,
})))
const visibleResources = computed(() => {
  if (!selectedSpaceId.value) return resources.value
  const filter = (nodes: PermissionTree[]): PermissionTree[] => nodes.map((node) => ({ ...node, children: filter(node.children || []) })).filter((node) => node.spaceId === selectedSpaceId.value || node.children.length > 0)
  return filter(resources.value)
})
function spaceName(id?: string | null) { return spaces.value.find((item) => item.id === id)?.name || '-' }
async function loadData() {
  loading.value = true; spacesLoading.value = true
  try { const [tree, spaceRows] = await Promise.all([permissionApi.getTree(), permissionApi.getSpaces()]); resources.value = tree; spaces.value = spaceRows } catch { message.error('加载数据资源失败') } finally { loading.value = false; spacesLoading.value = false }
}
function openSpaceDialog(space?: PermissionSpace) { spaceEditing.value = space || null; Object.assign(spaceForm, { name: space?.name || '', code: space?.code || '', description: space?.description || '' }); spaceDialogVisible.value = true }
function openResourceDialog(resource?: PermissionTree) { resourceEditing.value = resource ? ({ ...resource } as unknown as Permission) : null; Object.assign(resourceForm, { name: resource?.name || '', code: resource?.code || '', description: resource?.description || '', type: 'data', parentId: undefined, resource: resource?.resource || '', action: resource?.action || '', spaceId: resource?.spaceId || selectedSpaceId.value, operations: resource?.operations || [] }); resourceDialogVisible.value = true }
async function saveSpace() { submitting.value = true; try { if (spaceEditing.value) await permissionApi.updateSpace(spaceEditing.value.id, spaceForm); else await permissionApi.createSpace(spaceForm); message.success(spaceEditing.value ? '权限空间已更新' : '权限空间已创建'); spaceDialogVisible.value = false; await loadData() } catch { message.error('保存权限空间失败') } finally { submitting.value = false } }
async function removeSpace(space: PermissionSpace) { try { await permissionApi.deleteSpace(space.id); message.success('权限空间已删除'); if (selectedSpaceId.value === space.id) selectedSpaceId.value = undefined; await loadData() } catch { message.error('删除权限空间失败') } }
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
    <div class="mb-6 flex items-start justify-between"><div><h1 class="mb-2 text-2xl font-bold text-foreground">数据资源权限</h1><p class="text-sm text-muted-foreground">按权限空间组织数据资源，并配置资源层级与可执行操作。</p></div><div class="flex gap-2"><Button @click="loadData"><RefreshCw class="mr-2 size-4" />刷新</Button><Button type="primary" @click="openResourceDialog()"><Plus class="mr-2 size-4" />新建数据资源</Button></div></div>
    <div class="grid grid-cols-1 gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
      <Card class="h-fit"><div class="mb-4 flex items-center justify-between"><div class="flex items-center gap-2 font-semibold"><FolderTree class="size-4 text-primary" />权限空间</div><Button type="link" size="small" class="p-0" @click="openSpaceDialog()"><Plus class="size-4" /></Button></div><Spin :spinning="spacesLoading"><div class="space-y-2"><button class="flex w-full items-center justify-between rounded-md px-3 py-2 text-left text-sm hover:bg-muted" :class="!selectedSpaceId ? 'bg-primary/10 text-primary' : ''" @click="selectedSpaceId = undefined"><span>全部资源</span><Tag>{{ resources.length }}</Tag></button><div v-for="space in spaces" :key="space.id" class="group flex items-center gap-1"><button class="min-w-0 flex-1 rounded-md px-3 py-2 text-left hover:bg-muted" :class="selectedSpaceId === space.id ? 'bg-primary/10 text-primary' : ''" @click="selectedSpaceId = space.id"><div class="truncate text-sm font-medium">{{ space.name }}</div><div class="truncate text-xs text-muted-foreground">{{ space.code }}</div></button><Button type="link" size="small" class="hidden p-1 group-hover:inline-flex" @click="openSpaceDialog(space)"><Pencil class="size-3.5" /></Button><Button type="link" size="small" danger class="hidden p-1 group-hover:inline-flex" @click="removeSpace(space)"><Trash2 class="size-3.5" /></Button></div><Empty v-if="!spaces.length" description="暂无权限空间" /></div></Spin></Card>
      <Card><div class="mb-4 flex items-center gap-3"><Database class="size-5 text-primary" /><div><div class="font-semibold">数据资源</div><div class="text-xs text-muted-foreground">资源名称、编码、权限空间及操作权限</div></div></div><Table :columns="resourceColumns" :data-source="visibleResources" :loading="loading" row-key="id" :pagination="false" :expandable="{ defaultExpandAllRows: true, indentSize: 20 }" :scroll="{ x: 1450 }"><template #bodyCell="{ column, record }"><template v-if="column.key === 'name'"><div class="flex items-center gap-2"><Database class="size-4 shrink-0 text-primary" /><span class="font-medium">{{ record.name }}</span></div></template><template v-else-if="column.key === 'code'"><code class="text-xs text-muted-foreground">{{ record.code }}</code></template><template v-else-if="column.key === 'space'"><Tag color="blue">{{ spaceName(record.spaceId) }}</Tag></template><template v-else-if="column.key === 'description'"><span class="text-sm text-muted-foreground">{{ record.description || '-' }}</span></template><template v-else-if="column.key === 'parent'"><span class="text-sm text-muted-foreground">{{ record.parent?.name || '-' }}</span></template><template v-else-if="column.key === 'operations'"><div class="flex flex-wrap gap-1"><Tag v-for="operation in (record.operations || [record.action])" :key="operation" color="green">{{ operation }}</Tag></div></template><template v-else-if="column.key === 'actions'"><div class="flex gap-2"><Button type="link" size="small" class="p-0" @click="openResourceDialog(record)">编辑</Button><Button type="link" size="small" danger class="p-0" @click="removeResource(record)">删除</Button></div></template></template></Table></Card>
    </div>
    <Modal v-model:open="spaceDialogVisible" :title="spaceEditing ? '编辑权限空间' : '新建权限空间'" :confirm-loading="submitting" @ok="saveSpace"><Form :model="spaceForm" layout="vertical" class="py-4"><FormItem label="空间名称" required><Input v-model:value="spaceForm.name" placeholder="例如：组织架构" /></FormItem><FormItem label="空间编码" required><Input v-model:value="spaceForm.code" :disabled="!!spaceEditing" placeholder="例如：organization" /></FormItem><FormItem label="空间描述"><InputTextArea v-model:value="spaceForm.description" :rows="3" /></FormItem></Form></Modal>
    <Modal v-model:open="resourceDialogVisible" :title="resourceEditing ? '编辑数据资源' : '新建数据资源'" :confirm-loading="submitting" @ok="saveResource"><Form ref="resourceFormRef" :model="resourceForm" layout="vertical" class="py-4"><FormItem name="name" label="资源名称" :rules="[{ required: true, whitespace: true, message: '请输入资源名称' }]"><Input v-model:value="resourceForm.name" placeholder="例如：用户" /></FormItem><FormItem name="code" label="资源编码" :rules="[{ required: true, whitespace: true, message: '请输入资源编码' }]"><Input v-model:value="resourceForm.code" :disabled="!!resourceEditing" placeholder="例如：user" /></FormItem><FormItem name="spaceId" label="权限空间" :rules="[{ required: true, message: '请选择权限空间' }]"><Select v-model:value="resourceForm.spaceId" allow-clear class="w-full" :options="spaceOptions" placeholder="请选择权限空间" /></FormItem><FormItem name="parentId" label="上级资源权限"><TreeSelect v-model:value="resourceForm.parentId" class="w-full" :tree-data="resources" :field-names="{ label: 'name', value: 'id', children: 'children' }" allow-clear tree-default-expand-all placeholder="请选择上级资源" /></FormItem><FormItem name="operations" label="资源操作"><Select v-model:value="resourceForm.operations" mode="tags" class="w-full" placeholder="输入操作后回车，例如 read、create" /></FormItem><FormItem name="description" label="资源描述"><InputTextArea v-model:value="resourceForm.description" :rows="3" /></FormItem></Form></Modal>
  </div>
</template>
