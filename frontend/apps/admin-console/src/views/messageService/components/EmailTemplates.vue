<script setup lang="ts">
import type { MessageTemplate, MessageTemplateSearch } from '@/api/messageTemplate'
import MessageTemplateTable from './MessageTemplateTable.vue'

defineProps<{ templates: MessageTemplate[]; loading: boolean }>()
const emit = defineEmits<{
  edit: [template: MessageTemplate]
  delete: [template: MessageTemplate]
  search: [query: MessageTemplateSearch]
}>()
</script>

<template>
  <MessageTemplateTable
    type="email"
    :templates="templates"
    :loading="loading"
    @edit="emit('edit', $event)"
    @delete="emit('delete', $event)"
    @search="emit('search', $event)"
  />
</template>
