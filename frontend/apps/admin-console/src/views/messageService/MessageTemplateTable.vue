<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { TableProps } from 'antdv-next'
import type { MessageTemplate, MessageTemplateSearch } from '@/api/messageTemplate'

const props = defineProps<{
  type: MessageTemplate['type']
  templates: MessageTemplate[]
  loading: boolean
}>()
const emit = defineEmits<{
  edit: [template: MessageTemplate]
  delete: [template: MessageTemplate]
  search: [query: MessageTemplateSearch]
}>()

const keyword = ref('')
const status = ref<MessageTemplateSearch['status']>()
const current = ref(1)
const pageSize = ref(10)

const filteredTemplates = computed(() => props.templates.filter(template => template.type === props.type))

const columns = computed<TableProps<MessageTemplate>['columns']>(() => [
  { title: '模板名称', dataIndex: 'name', width: 150 },
  { title: '模板代码', dataIndex: 'code', width: 150 },
  { title: '默认', key: 'isDefault', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作',fixed: 'right', key: 'actions', width: 150 },
])

const pagination = computed<TableProps<MessageTemplate>['pagination']>(() => ({
  current: current.value,
  pageSize: pageSize.value,
  total: filteredTemplates.value.length,
  showSizeChanger: true,
  pageSizeOptions: [10, 20, 50],
  showTotal: total => `共 ${total} 条`,
  responsive: true,
}))

type TablePagination = NonNullable<Exclude<TableProps<MessageTemplate>['pagination'], boolean>>

const handleTableChange = (pagination: TablePagination) => {
  const nextPageSize = pagination.pageSize ?? pageSize.value
  current.value = nextPageSize === pageSize.value ? pagination.current ?? 1 : 1
  pageSize.value = nextPageSize
}

const handleSearch = (value: string) => {
  keyword.value = value
  current.value = 1
  emit('search', { search: value.trim() || undefined, status: status.value })
}

const resetSearch = () => {
  status.value = undefined
  handleSearch('')
}

watch(() => filteredTemplates.value.length, total => {
  current.value = Math.min(current.value, Math.max(1, Math.ceil(total / pageSize.value)))
})
</script>

<template>
  <div>
    <div class="mb-4 flex flex-wrap items-center gap-3">
      <a-input-search
        v-model:value="keyword"
        allow-clear
        enter-button="搜索"
        placeholder="搜索模板名称、代码、主题或内容"
        aria-label="搜索消息模板"
        class="w-full! sm:w-80!"
        @search="handleSearch"
      />
      <a-select
        v-model:value="status"
        allow-clear
        placeholder="全部状态"
        aria-label="筛选模板状态"
        class="w-32!"
        :options="[{ label: '启用', value: 'active' }, { label: '禁用', value: 'inactive' }]"
        @change="handleSearch(keyword)"
      />
      <a-button @click="resetSearch">重置</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="filteredTemplates"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: type === 'email' ? 810 : 910 }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isDefault'">
          <a-tag :color="record.isDefault ? 'green' : undefined">{{ record.isDefault ? '是' : '否' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 'active' ? 'green' : undefined">{{ record.status === 'active' ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <div class="flex justify-start gap-2">
            <a-button type="link" size="small" @click="emit('edit', record)">编辑</a-button>
            <a-button type="link" size="small" danger @click="emit('delete', record)">删除</a-button>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>
