<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { messageTemplateApi } from '@/api/messageTemplate'
import type { MessageTemplate } from '@/api/messageTemplate'
import { getApiErrorMessage, useTemplateDict } from '../utils/hooks'
import { templateFormRules } from '../utils/rules'
import type { TemplateEditForm, TemplateType } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const { templateTypeOptions } = useTemplateDict()

const dialogVisible = ref(false)
const submitting = ref(false)
const currentId = ref<string | undefined>(undefined)

const createDefaultForm = (type: TemplateType): TemplateEditForm => {
  return {
    type,
    code: '',
    name: '',
    subject: '',
    content: '',
    isDefault: false
  }
}

const templateForm = ref<TemplateEditForm>(createDefaultForm('email'))

const open = (type: TemplateType, row?: MessageTemplate) => {
  if (row) {
    currentId.value = row.id
    templateForm.value = {
      type: row.type,
      code: row.code,
      name: row.name,
      subject: row.subject || '',
      content: row.content,
      isDefault: row.isDefault
    }
  } else {
    currentId.value = undefined
    templateForm.value = createDefaultForm(type)
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    if (currentId.value) {
      await messageTemplateApi.update(currentId.value, {
        name: templateForm.value.name,
        subject: templateForm.value.subject,
        content: templateForm.value.content,
        isDefault: templateForm.value.isDefault
      })
      message.success('模板更新成功')
    } else {
      await messageTemplateApi.create(templateForm.value)
      message.success('模板创建成功')
    }
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存模板失败:', error)
    message.error(getApiErrorMessage(error, '保存模板失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" :title="currentId ? '编辑模板' : '新建模板'" :footer="null">
    <a-form :model="templateForm" :rules="templateFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <a-form-item name="type" label="模板类型">
        <a-radio-group v-model:value="templateForm.type" :disabled="!!currentId" :options="templateTypeOptions" />
      </a-form-item>
      <a-form-item name="code" label="模板代码">
        <a-input v-model:value="templateForm.code" placeholder="例如：verification_code" :disabled="!!currentId" />
      </a-form-item>
      <a-form-item name="name" label="模板名称">
        <a-input v-model:value="templateForm.name" placeholder="请输入模板名称" />
      </a-form-item>
      <a-form-item name="subject" label="邮件主题">
        <a-input v-model:value="templateForm.subject" placeholder="请输入邮件主题，支持变量如 {{appName}}" />
      </a-form-item>
      <a-form-item name="content" label="模板内容">
        <a-textarea
          v-model:value="templateForm.content"
          :rows="10"
          placeholder="请输入模板内容，支持变量如 {{code}}, {{username}} 等"
        />
      </a-form-item>
      <a-form-item name="isDefault" label="设为默认">
        <a-switch v-model:checked="templateForm.isDefault" />
      </a-form-item>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
