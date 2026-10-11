<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'antdv-next'
import { SOURCE_CONFIGS } from '@/types/loginIdentitySource'
import { enterpriseIdentitySourceApi } from '@/api/enterpriseIdentitySource'
import type { EnterpriseIdentitySource } from '@/types/enterpriseIdentitySource'
import { getApiErrorMessage } from '../utils/hooks'
import { createSourceFormRules } from '../utils/rules'
import type { SourceEditForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref<EnterpriseIdentitySource | null>(null)

const typeConfig = SOURCE_CONFIGS.feishu_web

const createDefaultForm = (): SourceEditForm => {
  return {
    name: '',
    type: 'feishu_web',
    mode: null,
    clientId: '',
    clientSecret: ''
  }
}

const form = ref<SourceEditForm>(createDefaultForm())

const rules = computed(() => createSourceFormRules(!!editing.value))

const open = (row?: EnterpriseIdentitySource) => {
  editing.value = row ?? null
  form.value = {
    name: row?.name ?? '',
    type: 'feishu_web',
    mode: row?.mode ?? null,
    clientId: row?.clientId ?? '',
    clientSecret: ''
  }
  modalOpen.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      type: form.value.type,
      mode: form.value.mode,
      clientId: form.value.clientId.trim()
    }
    const clientSecret = form.value.clientSecret.trim()
    if (editing.value) {
      await enterpriseIdentitySourceApi.update(
        editing.value.id,
        clientSecret ? { ...payload, clientSecret } : payload
      )
      message.success('企业身份源已更新')
    } else {
      await enterpriseIdentitySourceApi.create({ ...payload, clientSecret })
      message.success('企业身份源已创建')
    }
    modalOpen.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存企业身份源失败:', error)
    message.error(getApiErrorMessage(error, '保存企业身份源失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="modalOpen" :footer="null" destroy-on-hidden>
    <div>
      <h3>{{ editing ? '编辑企业身份源' : '添加企业身份源' }}</h3>
      <p class="text-sm text-[#52677D]">
        {{ editing ? '修改飞书网页授权的应用凭证。' : '配置飞书网页授权，应用凭证来自飞书开放平台的自建应用。' }}
      </p>
    </div>
    <a-form :model="form" :rules="rules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid gap-4">
        <a-form-item label="身份源类型" name="type">
          <a-input :value="typeConfig.name" disabled />
        </a-form-item>
        <a-form-item label="身份源名称" name="name">
          <a-input v-model:value="form.name" placeholder="例如：企业飞书登录" />
        </a-form-item>
        <a-form-item :label="typeConfig.clientIdLabel" name="clientId">
          <a-input v-model:value="form.clientId" placeholder="cli_xxx" />
        </a-form-item>
        <a-form-item :label="typeConfig.clientSecretLabel" name="clientSecret">
          <a-input
              v-model:value="form.clientSecret"
              type="password"
              :placeholder="editing ? '留空表示保持原值' : `请输入${typeConfig.clientSecretLabel}`"
          />
        </a-form-item>
      </div>
      <div class="flex justify-end gap-2">
        <a-button @click="modalOpen = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">确定</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
