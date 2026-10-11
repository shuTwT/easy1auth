<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal, message } from 'antdv-next'
import type { TableProps } from 'antdv-next'
import { ArrowLeft, Plus } from '@lucide/vue'
import { userGroupApi } from '@/api/user-group'
import { useUserStore } from '@/stores/user'
import type { GroupType, UserGroup, User as GroupMember } from '@/types/user-group'
import AddMembersDialog from './components/AddMembersDialog.vue'
import { getApiErrorMessage, useGroupDict } from './utils/hooks'
import { groupDetailFormRules } from './utils/rules'
import type { AddMembersDialogExpose } from './utils/types'

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
const removingId = ref('')
const addMembersDialogRef = ref<AddMembersDialogExpose>()
const [modal, contextHolder] = Modal.useModal()
let scope = 0

const { groupTypeOptions, formatDate } = useGroupDict()

const form = reactive({ name: '', description: '', type: 'team' as GroupType })

const filteredMembers = computed(() => {
  const keyword = memberKeyword.value.trim().toLowerCase()
  return members.value.filter((member) => {
    return !keyword || [member.name, member.username, member.email].some((value) => value?.toLowerCase().includes(keyword))
  })
})
const visibleMembers = computed(() => {
  return filteredMembers.value.slice((memberPage.value - 1) * memberPageSize, memberPage.value * memberPageSize)
})
const memberIds = computed(() => members.value.map((member) => member.id))

const memberColumns: TableProps['columns'] = [
  { title: '姓名', dataIndex: 'name', width: 160 },
  { title: '用户名', dataIndex: 'username', width: 180 },
  { title: '邮箱', dataIndex: 'email' },
  { title: '操作', key: 'actions', width: 100 }
]

const resetForm = () => {
  if (group.value) {
    Object.assign(form, { name: group.value.name, description: group.value.description || '', type: group.value.type })
  }
}

const loadGroup = async (current = scope) => {
  loading.value = true
  try {
    const result = await userGroupApi.getById(groupId.value)
    if (current !== scope) {
      return
    }
    group.value = result
    resetForm()
  } catch (error: unknown) {
    if (current === scope) {
      message.error(getApiErrorMessage(error, '加载用户组详情失败'))
    }
  } finally {
    if (current === scope) {
      loading.value = false
    }
  }
}

const loadMembers = async (current = scope) => {
  membersLoading.value = true
  membersReady.value = false
  try {
    const result = await userGroupApi.getMembers(groupId.value)
    if (current !== scope) {
      return
    }
    members.value = result.members
    membersReady.value = true
    memberPage.value = Math.min(memberPage.value, Math.max(1, Math.ceil(filteredMembers.value.length / memberPageSize)))
    if (group.value) {
      group.value._count = { members: result.total }
    }
  } catch (error: unknown) {
    if (current === scope) {
      message.error(getApiErrorMessage(error, '加载用户组成员失败'))
    }
  } finally {
    if (current === scope) {
      membersLoading.value = false
    }
  }
}

const saveGroup = async () => {
  if (saving.value || !group.value) {
    return
  }
  const current = scope
  saving.value = true
  try {
    const result = await userGroupApi.update(groupId.value, { ...form, name: form.name.trim() })
    if (current !== scope) {
      return
    }
    group.value = result
    resetForm()
    message.success('用户组基本信息已保存')
  } catch (error: unknown) {
    if (current === scope) {
      message.error(getApiErrorMessage(error, '保存用户组失败'))
    }
  } finally {
    if (current === scope) {
      saving.value = false
    }
  }
}

const handleAddMembers = () => {
  addMembersDialogRef.value?.open()
}

const handleAddSuccess = () => {
  loadMembers(scope)
}

const removeMember = async (userId: string) => {
  const member = members.value.find((user) => user.id === userId)
  if (!member) {
    return
  }
  if (removingId.value || !membersReady.value) {
    return
  }
  const current = scope
  const confirmed = await modal.confirm({
    title: '移除成员',
    content: `确定将“${member.name || member.username}”移出当前用户组吗？`,
    okText: '移除',
    cancelText: '取消',
    okButtonProps: { danger: true }
  })
  if (!confirmed || current !== scope || removingId.value) {
    return
  }
  removingId.value = member.id
  try {
    await userGroupApi.removeMembers(groupId.value, { userIds: [member.id] })
    if (current !== scope) {
      return
    }
    message.success('移除成员成功')
    await loadMembers(current)
  } catch (error: unknown) {
    if (current === scope) {
      message.error(getApiErrorMessage(error, '移除成员失败'))
    }
  } finally {
    if (current === scope) {
      removingId.value = ''
    }
  }
}

onBeforeUnmount(() => {
  scope += 1
})

watch(memberKeyword, () => {
  memberPage.value = 1
})

watch([groupId, () => userStore.currentUserPool?.id], () => {
  scope += 1
  addMembersDialogRef.value?.close()
  group.value = null
  members.value = []
  membersReady.value = false
  memberKeyword.value = ''
  memberPage.value = 1
  saving.value = false
  removingId.value = ''
  loadGroup(scope)
  loadMembers(scope)
}, { immediate: true })
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] p-4 md:p-6">
    <div class="mb-6 flex flex-wrap items-start gap-4">
      <a-button @click="router.push('/user-group')">
        <ArrowLeft class="mr-2 size-4" />
        返回列表
      </a-button>
      <div class="min-w-0 flex-1">
        <h1 class="mb-1 break-words text-2xl font-bold text-[#102A43]">{{ group?.name || '用户组详情' }}</h1>
        <p class="text-sm text-[#52677D]">编辑用户组基本信息，添加或移除组成员</p>
      </div>
    </div>

    <a-card v-if="loading" :loading="true" />
    <template v-else-if="group">
      <a-card title="基本信息" class="mb-6!">
        <a-form :model="form" :rules="groupDetailFormRules" layout="vertical" :disabled="saving" @finish="saveGroup">
          <div class="grid grid-cols-1 gap-x-6 md:grid-cols-2">
            <a-form-item label="用户组名称" name="name">
              <a-input v-model:value="form.name" placeholder="请输入用户组名称" />
            </a-form-item>
            <a-form-item label="类型" name="type">
              <a-select v-model:value="form.type" :options="groupTypeOptions" />
            </a-form-item>
            <a-form-item label="描述" name="description" class="md:col-span-2">
              <a-textarea v-model:value="form.description" :rows="3" placeholder="请输入用户组描述" />
            </a-form-item>
          </div>
          <div class="flex flex-wrap items-center justify-between gap-4">
            <span class="text-sm text-[#52677D]">创建时间：{{ formatDate(group.createdAt) }}</span>
            <div class="flex gap-2">
              <a-button :disabled="saving" @click="resetForm">重置</a-button>
              <a-button type="primary" html-type="submit" :loading="saving">保存基本信息</a-button>
            </div>
          </div>
        </a-form>
      </a-card>

      <a-card :title="`用户组成员（${membersReady ? members.length : group._count?.members || 0}）`">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <a-input
            v-model:value="memberKeyword"
            allow-clear
            placeholder="搜索姓名、用户名或邮箱"
            aria-label="搜索当前成员"
            class="w-full! sm:w-72!"
          />
          <div class="flex gap-2">
            <a-button :disabled="membersLoading || !!removingId" @click="loadMembers(scope)">刷新成员</a-button>
            <a-button type="primary" :disabled="!membersReady || membersLoading || !!removingId" @click="handleAddMembers">
              <Plus class="mr-2 size-4" />
              添加成员
            </a-button>
          </div>
        </div>
        <a-table
          :columns="memberColumns"
          :data-source="visibleMembers"
          :loading="membersLoading"
          :pagination="false"
          row-key="id"
          :scroll="{ x: 650 }"
        >
          <template #bodyCell="{ column, record: member }">
            <a-button
              v-if="column.key === 'actions'"
              type="link"
              danger
              :loading="removingId === member.id"
              :disabled="!!removingId || !membersReady"
              @click="removeMember(member.id)"
            >
              移除
            </a-button>
          </template>
          <template #emptyText>
            <span>{{ membersReady ? (memberKeyword ? '没有匹配的成员' : '暂无成员，可点击“添加成员”') : '请点击“刷新成员”加载列表' }}</span>
          </template>
        </a-table>
        <div v-if="filteredMembers.length" class="mt-4 flex flex-wrap items-center justify-between gap-3">
          <span class="text-sm text-[#52677D]">共 {{ filteredMembers.length }} 位成员</span>
          <a-pagination
            v-model:current="memberPage"
            :page-size="memberPageSize"
            :total="filteredMembers.length"
            :show-size-changer="false"
            size="small"
          />
        </div>
      </a-card>
    </template>
    <a-card v-else>
      <a-empty description="暂无用户组详情">
        <a-button type="primary" @click="loadGroup(scope)">重新加载</a-button>
      </a-empty>
    </a-card>

    <AddMembersDialog
      ref="addMembersDialogRef"
      :group-id="groupId"
      :member-ids="memberIds"
      :members-ready="membersReady"
      @success="handleAddSuccess"
    />
    <contextHolder />
  </div>
</template>
