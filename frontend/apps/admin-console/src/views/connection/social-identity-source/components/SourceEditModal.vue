<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'antdv-next'
import { SOURCE_CONFIGS } from '@/types/loginIdentitySource'
import { socialIdentitySourceApi } from '@/api/socialIdentitySource'
import type { SocialIdentitySource } from '@/types/socialIdentitySource'
import { getApiErrorMessage, useSocialSourceDict } from '../utils/hooks'
import { createSourceFormRules } from '../utils/rules'
import type { SourceEditForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const { typeOptions } = useSocialSourceDict()

const modalOpen = ref(false)
const submitting = ref(false)
const editing = ref<SocialIdentitySource | null>(null)

const createDefaultForm = (): SourceEditForm => {
  return {
    name: '',
    type: 'github',
    mode: null,
    clientId: '',
    clientSecret: ''
  }
}

const form = ref<SourceEditForm>(createDefaultForm())

const typeConfig = computed(() => SOURCE_CONFIGS[form.value.type])
const rules = computed(() => createSourceFormRules(form.value.type, !!editing.value))

const open = (row?: SocialIdentitySource) => {
  editing.value = row ?? null
  form.value = row
    ? {
        name: row.name,
        type: row.type,
        mode: row.mode,
        clientId: row.clientId,
        clientSecret: ''
      }
    : createDefaultForm()
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
      await socialIdentitySourceApi.update(
        editing.value.id,
        clientSecret ? { ...payload, clientSecret } : payload
      )
      message.success('社会化身份源已更新')
    } else {
      await socialIdentitySourceApi.create({ ...payload, clientSecret })
      message.success('社会化身份源已创建')
    }
    modalOpen.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存社会化身份源失败:', error)
    message.error(getApiErrorMessage(error, '保存社会化身份源失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="modalOpen" :footer="null" destroy-on-hidden>
    <div>
      <h3>{{ editing ? '编辑社会化身份源' : '添加社会化身份源' }}</h3>
      <p class="text-sm text-[#52677D]">配置微信、GitHub、Gitee 等社会化登录凭证。</p>
    </div>
    <a-form :model="form" :rules="rules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid gap-4">
        <a-form-item label="身份源类型" name="type">
          <a-select v-model:value="form.type" :options="typeOptions" :disabled="!!editing" />
        </a-form-item>
        <a-form-item label="身份源名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入身份源名称" />
        </a-form-item>
        <a-form-item :label="typeConfig.clientIdLabel" name="clientId">
          <a-input v-model:value="form.clientId" :placeholder="`请输入${typeConfig.clientIdLabel}`" />
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
