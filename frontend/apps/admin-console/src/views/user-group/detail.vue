<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { ArrowLeft, Plus, Search } from '@lucide/vue'
import { userGroupApi } from '@/api/user-group'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { GroupType, UserGroup, User as GroupMember } from '@/types/user-group'
import type { User, UserQueryDto } from '@/types/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const groupId = computed(() => String(route.params.id || ''))
const group = ref<UserGroup | null>(null)
const loading = ref(false)
const saving = ref(false)
const members = ref<GroupMember[]>([])
const membersLoading = ref(false)
const membersReady = ref(false)
const memberKeyword = ref('')
const memberPage = ref(1)
const memberPageSize = 10
const addOpen = ref(false)
const adding = ref(false)
const removingId = ref('')
const candidates = ref<User[]>([])
const candidatesTotal = ref(0)
const candidatesLoading = ref(false)
const selectedIds = ref<string[]>([])
const [modal, contextHolder] = Modal.useModal()
let scope = 0
let candidateRequest = 0

const form = reactive({ name: '', description: '', type: 'team' as GroupType })
const rules = {
  name: [{ required: true, whitespace: true, message: '请输入用户组名称' }],
  type: [{ required: true, message: '请选择类型' }],
}
const typeOptions = [
  { value: 'team', label: '团队' },
  { value: 'department', label: '部门' },
  { value: 'project', label: '项目' },
  { value: 'organization', label: '组织' },
]
const candidateQuery = reactive<UserQueryDto>({ page: 1, pageSize: 10, name: '', username: '' })
const filteredMembers = computed(() => {
  const keyword = memberKeyword.value.trim().toLowerCase()
  return members.value.filter(member => !keyword || [member.name, member.username, member.email]
    .some(value => value?.toLowerCase().includes(keyword)))
})
const visibleMembers = computed(() => filteredMembers.value.slice((memberPage.value - 1) * memberPageSize, memberPage.value * memberPageSize))
const memberIds = computed(() => new Set(members.value.map(member => member.id)))
const rowSelection = computed<TableProps<User>['rowSelection']>(() => ({
  selectedRowKeys: selectedIds.value,
  preserveSelectedRowKeys: true,
  onChange: keys => { selectedIds.value = keys.map(String) },
  getCheckboxProps: user => ({ disabled: memberIds.value.has(user.id) || adding.value }),
}))
const memberColumns: TableProps['columns'] = [
  { title: '姓名', dataIndex: 'name', width: 160 },
  { title: '用户名', dataIndex: 'username', width: 180 },
  { title: '邮箱', dataIndex: 'email' },
  { title: '操作', key: 'actions', width: 100 },
]
const candidateColumns: TableProps<User>['columns'] = [
  { title: '姓名', dataIndex: 'name', width: 140 },
  { title: '用户名', dataIndex: 'username', width: 160 },
  { title: '邮箱', dataIndex: 'email' },
  { title: '成员状态', key: 'membership', width: 100 },
]

function errorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data?.msg || fallback
  }
  return error instanceof Error ? error.message : fallback
}

function resetForm() {
  if (group.value) {
    Object.assign(form, { name: group.value.name, description: group.value.description || '', type: group.value.type })
  }
}

async function loadGroup(current = scope) {
  loading.value = true
  try {
    const result = await userGroupApi.getById(groupId.value)
    if (current !== scope) return
    group.value = result
    resetForm()
  } catch (error) {
    if (current === scope) message.error(errorMessage(error, '加载用户组详情失败'))
  } finally {
    if (current === scope) loading.value = false
  }
}

async function loadMembers(current = scope) {
  membersLoading.value = true
  membersReady.value = false
  try {
    const result = await userGroupApi.getMembers(groupId.value)
    if (current !== scope) return
    members.value = result.members
    membersReady.value = true
    memberPage.value = Math.min(memberPage.value, Math.max(1, Math.ceil(filteredMembers.value.length / memberPageSize)))
    if (group.value) group.value._count = { members: result.total }
  } catch (error) {
    if (current === scope) message.error(errorMessage(error, '加载用户组成员失败'))
  } finally {
    if (current === scope) membersLoading.value = false
  }
}

async function saveGroup() {
  if (saving.value || !group.value) return
  const current = scope
  saving.value = true
  try {
    const result = await userGroupApi.update(groupId.value, { ...form, name: form.name.trim() })
    if (current !== scope) return
    group.value = result
    resetForm()
    message.success('用户组基本信息已保存')
  } catch (error) {
    if (current === scope) message.error(errorMessage(error, '保存用户组失败'))
  } finally {
    if (current === scope) saving.value = false
  }
}

async function loadCandidates() {
  const current = scope
  const requestId = ++candidateRequest
  candidatesLoading.value = true
  candidates.value = []
  try {
    const result = await userApi.getList({ ...candidateQuery, name: candidateQuery.name?.trim() || undefined, username: candidateQuery.username?.trim() || undefined })
    if (current !== scope || requestId !== candidateRequest) return
    candidates.value = result.items
    candidatesTotal.value = result.total
  } catch (error) {
    if (current === scope && requestId === candidateRequest) message.error(errorMessage(error, '加载可选用户失败'))
  } finally {
    if (current === scope && requestId === candidateRequest) candidatesLoading.value = false
  }
}

function openAddMembers() {
  selectedIds.value = []
  Object.assign(candidateQuery, { page: 1, name: '', username: '' })
  candidatesTotal.value = 0
  addOpen.value = true
  loadCandidates()
}

function searchCandidates() {
  candidateQuery.page = 1
  loadCandidates()
}

function candidatePageChange(page: number) {
  candidateQuery.page = page
  loadCandidates()
}

async function addMembers() {
  if (adding.value || !membersReady.value || selectedIds.value.length === 0) return
  const current = scope
  const userIds = selectedIds.value.filter(id => !memberIds.value.has(id))
  if (!userIds.length) return
  adding.value = true
  try {
    await userGroupApi.addMembers(groupId.value, { userIds })
    if (current !== scope) return
    message.success('添加成员成功')
    addOpen.value = false
    selectedIds.value = []
    await loadMembers(current)
  } catch (error) {
    if (current === scope) message.error(errorMessage(error, '添加成员失败'))
  } finally {
    if (current === scope) adding.value = false
  }
}

async function removeMember(userId: string) {
  const member = members.value.find(user => user.id === userId)
  if (!member) return
  if (removingId.value || adding.value || !membersReady.value) return
  const current = scope
  const confirmed = await modal.confirm({
    title: '移除成员',
    content: `确定将“${member.name || member.username}”移出当前用户组吗？`,
    okText: '移除', cancelText: '取消', okButtonProps: { danger: true },
  })
  if (!confirmed || current !== scope || removingId.value) return
  removingId.value = member.id
  try {
    await userGroupApi.removeMembers(groupId.value, { userIds: [member.id] })
    if (current !== scope) return
    message.success('移除成员成功')
    await loadMembers(current)
  } catch (error) {
    if (current === scope) message.error(errorMessage(error, '移除成员失败'))
  } finally {
    if (current === scope) removingId.value = ''
  }
}

onBeforeUnmount(() => {
  scope += 1
  candidateRequest += 1
})

watch(memberKeyword, () => { memberPage.value = 1 })
watch([groupId, () => userStore.currentUserPool?.id], () => {
  scope += 1
  candidateRequest += 1
  group.value = null
  members.value = []
  membersReady.value = false
  memberKeyword.value = ''
  memberPage.value = 1
  addOpen.value = false
  saving.value = false
  adding.value = false
  removingId.value = ''
  selectedIds.value = []
  loadGroup(scope)
  loadMembers(scope)
}, { immediate: true })
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] p-4 md:p-6">
    <div class="mb-6 flex flex-wrap items-start gap-4">
      <a-button @click="router.push('/user-group')"><ArrowLeft class="mr-2 size-4" />返回列表</a-button>
      <div class="min-w-0 flex-1">
        <h1 class="mb-1 break-words text-2xl font-bold text-[#102A43]">{{ group?.name || '用户组详情' }}</h1>
        <p class="text-sm text-[#52677D]">编辑用户组基本信息，添加或移除组成员</p>
      </div>
    </div>

    <a-card v-if="loading" :loading="true" />
    <template v-else-if="group">
      <a-card title="基本信息" class="mb-6!">
        <a-form :model="form" :rules="rules" layout="vertical" :disabled="saving" @finish="saveGroup">
          <div class="grid grid-cols-1 gap-x-6 md:grid-cols-2">
            <a-form-item label="用户组名称" name="name"><a-input v-model:value="form.name" placeholder="请输入用户组名称" /></a-form-item>
            <a-form-item label="类型" name="type"><a-select v-model:value="form.type" :options="typeOptions" /></a-form-item>
            <a-form-item label="描述" name="description" class="md:col-span-2"><a-textarea v-model:value="form.description" :rows="3" placeholder="请输入用户组描述" /></a-form-item>
          </div>
          <div class="flex flex-wrap items-center justify-between gap-4">
            <span class="text-sm text-[#52677D]">创建时间：{{ new Date(group.createdAt).toLocaleString() }}</span>
            <div class="flex gap-2">
              <a-button :disabled="saving" @click="resetForm">重置</a-button>
              <a-button type="primary" html-type="submit" :loading="saving">保存基本信息</a-button>
            </div>
          </div>
        </a-form>
      </a-card>

      <a-card :title="`用户组成员（${membersReady ? members.length : group._count?.members || 0}）`">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <a-input v-model:value="memberKeyword" allow-clear placeholder="搜索姓名、用户名或邮箱" aria-label="搜索当前成员" class="w-full! sm:w-72!" />
          <div class="flex gap-2">
            <a-button :disabled="membersLoading || !!removingId || adding" @click="loadMembers(scope)">刷新成员</a-button>
            <a-button type="primary" :disabled="!membersReady || membersLoading || !!removingId || adding" @click="openAddMembers"><Plus class="mr-2 size-4" />添加成员</a-button>
          </div>
        </div>
        <a-table :columns="memberColumns" :data-source="visibleMembers" :loading="membersLoading" :pagination="false" row-key="id" :scroll="{ x: 650 }">
          <template #bodyCell="{ column, record: member }">
            <a-button v-if="column.key === 'actions'" type="link" danger :loading="removingId === member.id" :disabled="!!removingId || adding || !membersReady" @click="removeMember(member.id)">移除</a-button>
          </template>
          <template #emptyText><span>{{ membersReady ? (memberKeyword ? '没有匹配的成员' : '暂无成员，可点击“添加成员”') : '请点击“刷新成员”加载列表' }}</span></template>
        </a-table>
        <div v-if="filteredMembers.length" class="mt-4 flex flex-wrap items-center justify-between gap-3">
          <span class="text-sm text-[#52677D]">共 {{ filteredMembers.length }} 位成员</span>
          <a-pagination v-model:current="memberPage" :page-size="memberPageSize" :total="filteredMembers.length" :show-size-changer="false" size="small" />
        </div>
      </a-card>
    </template>
    <a-card v-else><a-empty description="暂无用户组详情"><a-button type="primary" @click="loadGroup(scope)">重新加载</a-button></a-empty></a-card>

    <a-modal v-model:open="addOpen" title="添加用户组成员" :width="800" :footer="null" :closable="!adding" :mask-closable="!adding" :keyboard="!adding">
      <a-form layout="inline" class="mb-4! flex-wrap gap-y-3" @finish="searchCandidates">
        <a-form-item label="姓名"><a-input v-model:value="candidateQuery.name" allow-clear placeholder="搜索姓名" :disabled="adding" /></a-form-item>
        <a-form-item label="用户名"><a-input v-model:value="candidateQuery.username" allow-clear placeholder="搜索用户名" :disabled="adding" /></a-form-item>
        <a-form-item><a-button html-type="submit" :disabled="adding"><Search class="mr-2 size-4" />搜索</a-button></a-form-item>
      </a-form>
      <a-table :columns="candidateColumns" :data-source="candidates" :row-selection="rowSelection" :loading="candidatesLoading" :pagination="false" row-key="id" :scroll="{ x: 620 }" size="small">
        <template #bodyCell="{ column, record: user }"><a-tag v-if="column.key === 'membership' && memberIds.has(user.id)">已加入</a-tag></template>
      </a-table>
      <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
        <span class="text-sm text-[#52677D]">已选择 {{ selectedIds.length }} 位用户</span>
        <a-pagination :current="candidateQuery.page" :page-size="candidateQuery.pageSize" :total="candidatesTotal" :show-size-changer="false" :disabled="adding" size="small" @change="candidatePageChange" />
      </div>
      <div class="mt-6 flex justify-end gap-2">
        <a-button :disabled="adding" @click="addOpen = false">取消</a-button>
        <a-button type="primary" :loading="adding" :disabled="!selectedIds.length || !membersReady" @click="addMembers">添加选中成员</a-button>
      </div>
    </a-modal>
    <contextHolder />
  </div>
</template>
