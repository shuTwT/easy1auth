<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { Search } from '@lucide/vue'
import { userGroupApi } from '@/api/user-group'
import { userApi } from '@/api/user'
import type { User, UserQueryDto } from '@/types/user'
import { getApiErrorMessage } from '../utils/hooks'

const props = defineProps<{
  groupId: string
  memberIds: string[]
  membersReady: boolean
}>()

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const adding = ref(false)
const candidates = ref<User[]>([])
const candidatesTotal = ref(0)
const candidatesLoading = ref(false)
const selectedIds = ref<string[]>([])
const candidateQuery = reactive<UserQueryDto>({ page: 1, pageSize: 10, name: '', username: '' })
let scope = 0
let candidateRequest = 0

const memberIdSet = computed(() => new Set(props.memberIds))

const rowSelection = computed<TableProps<User>['rowSelection']>(() => ({
  selectedRowKeys: selectedIds.value,
  preserveSelectedRowKeys: true,
  onChange: (keys) => {
    selectedIds.value = keys.map(String)
  },
  getCheckboxProps: (user) => ({ disabled: memberIdSet.value.has(user.id) || adding.value })
}))

const candidateColumns: TableProps<User>['columns'] = [
  { title: '姓名', dataIndex: 'name', width: 140 },
  { title: '用户名', dataIndex: 'username', width: 160 },
  { title: '邮箱', dataIndex: 'email' },
  { title: '成员状态', key: 'membership', width: 100 }
]

const loadCandidates = async () => {
  const current = scope
  const requestId = ++candidateRequest
  candidatesLoading.value = true
  candidates.value = []
  try {
    const result = await userApi.getList({
      ...candidateQuery,
      name: candidateQuery.name?.trim() || undefined,
      username: candidateQuery.username?.trim() || undefined
    })
    if (current !== scope || requestId !== candidateRequest) {
      return
    }
    candidates.value = result.items
    candidatesTotal.value = result.total
  } catch (error: unknown) {
    if (current === scope && requestId === candidateRequest) {
      message.error(getApiErrorMessage(error, '加载可选用户失败'))
    }
  } finally {
    if (current === scope && requestId === candidateRequest) {
      candidatesLoading.value = false
    }
  }
}

const open = () => {
  selectedIds.value = []
  Object.assign(candidateQuery, { page: 1, name: '', username: '' })
  candidatesTotal.value = 0
  dialogVisible.value = true
  loadCandidates()
}

/**
 * 关闭弹窗并作废所有进行中的请求，用于用户组或用户池切换时重置状态。
 */
const close = () => {
  scope += 1
  candidateRequest += 1
  dialogVisible.value = false
  adding.value = false
  selectedIds.value = []
}

const handleSearch = () => {
  candidateQuery.page = 1
  loadCandidates()
}

const handlePageChange = (page: number) => {
  candidateQuery.page = page
  loadCandidates()
}

const handleSubmit = async () => {
  if (adding.value || !props.membersReady || selectedIds.value.length === 0) {
    return
  }
  const current = scope
  const userIds = selectedIds.value.filter((id) => !memberIdSet.value.has(id))
  if (userIds.length === 0) {
    return
  }
  adding.value = true
  try {
    await userGroupApi.addMembers(props.groupId, { userIds })
    if (current !== scope) {
      return
    }
    message.success('添加成员成功')
    dialogVisible.value = false
    selectedIds.value = []
    emit('success')
  } catch (error: unknown) {
    if (current === scope) {
      message.error(getApiErrorMessage(error, '添加成员失败'))
    }
  } finally {
    if (current === scope) {
      adding.value = false
    }
  }
}

onBeforeUnmount(() => {
  scope += 1
  candidateRequest += 1
})

defineExpose({ open, close })
</script>

<template>
  <a-modal
    v-model:open="dialogVisible"
    title="添加用户组成员"
    :width="800"
    :footer="null"
    :closable="!adding"
    :mask-closable="!adding"
    :keyboard="!adding"
  >
    <a-form layout="inline" class="mb-4! flex-wrap gap-y-3" @finish="handleSearch">
      <a-form-item label="姓名">
        <a-input v-model:value="candidateQuery.name" allow-clear placeholder="搜索姓名" :disabled="adding" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="candidateQuery.username" allow-clear placeholder="搜索用户名" :disabled="adding" />
      </a-form-item>
      <a-form-item>
        <a-button html-type="submit" :disabled="adding">
          <Search class="mr-2 size-4" />
          搜索
        </a-button>
      </a-form-item>
    </a-form>
    <a-table
      :columns="candidateColumns"
      :data-source="candidates"
      :row-selection="rowSelection"
      :loading="candidatesLoading"
      :pagination="false"
      row-key="id"
      :scroll="{ x: 620 }"
      size="small"
    >
      <template #bodyCell="{ column, record: user }">
        <a-tag v-if="column.key === 'membership' && memberIdSet.has(user.id)">已加入</a-tag>
      </template>
    </a-table>
    <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
      <span class="text-sm text-[#52677D]">已选择 {{ selectedIds.length }} 位用户</span>
      <a-pagination
        :current="candidateQuery.page"
        :page-size="candidateQuery.pageSize"
        :total="candidatesTotal"
        :show-size-changer="false"
        :disabled="adding"
        size="small"
        @change="handlePageChange"
      />
    </div>
    <div class="mt-6 flex justify-end gap-2">
      <a-button :disabled="adding" @click="dialogVisible = false">取消</a-button>
      <a-button type="primary" :loading="adding" :disabled="!selectedIds.length || !membersReady" @click="handleSubmit">
        添加选中成员
      </a-button>
    </div>
  </a-modal>
</template>
