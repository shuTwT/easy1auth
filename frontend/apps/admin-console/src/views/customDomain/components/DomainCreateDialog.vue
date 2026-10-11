<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { customDomainApi } from '@/api/customDomain'
import { getApiErrorMessage, useDomainDict } from '../utils/hooks'
import { domainFormRules } from '../utils/rules'
import type { DomainCreateForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const { verificationMethodOptions } = useDomainDict()

const dialogVisible = ref(false)
const submitting = ref(false)

const createDefaultForm = (): DomainCreateForm => {
  return {
    domain: '',
    verificationMethod: 'dns'
  }
}

const domainForm = ref<DomainCreateForm>(createDefaultForm())

const open = () => {
  domainForm.value = createDefaultForm()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await customDomainApi.create({
      domain: domainForm.value.domain.trim(),
      verificationMethod: domainForm.value.verificationMethod
    })
    message.success('域名添加成功，请完成验证')
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('添加域名失败:', error)
    message.error(getApiErrorMessage(error, '添加域名失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" title="添加域名" :footer="null">
    <a-form :model="domainForm" :rules="domainFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <a-form-item label="域名" name="domain">
        <a-input v-model:value="domainForm.domain" placeholder="例如：login.yourcompany.com" addon-before="https://" />
      </a-form-item>
      <a-form-item label="验证方式" name="verificationMethod">
        <a-radio-group v-model:value="domainForm.verificationMethod" :options="verificationMethodOptions" />
      </a-form-item>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">添加</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
