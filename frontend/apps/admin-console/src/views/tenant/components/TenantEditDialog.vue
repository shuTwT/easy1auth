<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'antdv-next'
import { tenantApi } from '@/api/tenant'
import { useUserStore } from '@/stores/user'
import type { Tenant, TenantInput } from '@/types/tenant'
import { errorMessage, useTenantDict } from '../utils/hooks'
import { tenantCodePattern, tenantRuleMessages } from '../utils/rules'

const emit = defineEmits<{
  success: []
}>()

const userStore = useUserStore()
const { statusOptions } = useTenantDict()

const editorOpen = ref(false)
const saving = ref(false)
const editing = ref<Tenant | null>(null)
const form = reactive<TenantInput>({ name: '', code: '', description: '', status: 'active' })

const open = (row?: Tenant) => {
  editing.value = row || null
  Object.assign(form, {
    name: row?.name || '',
    code: row?.code || '',
    description: row?.description || '',
    status: row?.status || 'active',
  })
  editorOpen.value = true
}

const close = () => {
  editorOpen.value = false
}

const save = async () => {
  if (saving.value) {
    return
  }
  const input: TenantInput = {
    name: form.name.trim(),
    code: form.code.trim().toLowerCase(),
    description: form.description?.trim(),
    status: form.status,
  }
  if (!input.name) {
    message.warning(tenantRuleMessages.nameRequired)
    return
  }
  if (!tenantCodePattern.test(input.code)) {
    message.warning(tenantRuleMessages.codeInvalid)
    return
  }
  saving.value = true
  const poolId = userStore.currentUserPool?.id
  try {
    if (editing.value) {
      await tenantApi.update(editing.value.id, input)
    } else {
      await tenantApi.create(input)
    }
    if (poolId !== userStore.currentUserPool?.id) {
      return
    }
    editorOpen.value = false
    message.success(editing.value ? '租户已更新' : '租户已创建')
    emit('success')
  } catch (error) {
    message.error(errorMessage(error, '保存租户失败'))
  } finally {
    saving.value = false
  }
}

defineExpose({ open, close })
</script>

<template>
  <a-modal v-model:open="editorOpen" :title="editing ? '编辑租户' : '新建租户'" ok-text="保存" cancel-text="取消" :confirm-loading="saving" :mask="{ closable: false }" @ok="save">
    <a-form layout="vertical" class="mt-4" @submit.prevent="save">
      <a-form-item label="租户名称" required><a-input v-model:value="form.name" :maxlength="200" placeholder="请输入租户名称" /></a-form-item>
      <a-form-item label="租户编码" required><a-input v-model:value="form.code" :maxlength="100" placeholder="小写字母、数字、下划线或连字符" /></a-form-item>
      <a-form-item label="描述"><a-textarea v-model:value="form.description" :maxlength="2000" :rows="3" placeholder="可选，说明租户用途" /></a-form-item>
      <a-form-item label="状态"><a-select v-model:value="form.status" :options="statusOptions" /></a-form-item>
    </a-form>
  </a-modal>
</template>
