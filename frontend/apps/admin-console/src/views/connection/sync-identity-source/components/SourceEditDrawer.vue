<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'antdv-next'
import { Copy } from '@lucide/vue'
import { SYNC_IDENTITY_SOURCE_API_PATH, syncIdentitySourceApi } from '@/api/syncIdentitySource'
import type { SyncIdentitySource } from '@/types/syncIdentitySource'
import { getApiErrorMessage, useSyncDict } from '../utils/hooks'
import { createSourceFormRules } from '../utils/rules'
import type { SourceEditForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const { statusOptions } = useSyncDict()

const drawerOpen = ref(false)
const submitting = ref(false)
const editing = ref<SyncIdentitySource | null>(null)

const createDefaultForm = (): SourceEditForm => {
  return {
    name: '',
    appId: '',
    appSecret: '',
    verificationToken: '',
    encryptKey: '',
    status: 'active'
  }
}

const form = ref<SourceEditForm>(createDefaultForm())

const rules = computed(() => createSourceFormRules(!!editing.value))

const publicBaseUrl = computed(() => {
  return import.meta.env.VITE_SYNC_IDENTITY_PUBLIC_BASE_URL
    || import.meta.env.VITE_ENTERPRISE_IDENTITY_PUBLIC_BASE_URL
    || window.location.origin
})

const callbackUrl = computed(() => {
  if (!editing.value) {
    return '创建后生成专属回调地址'
  }
  return `${publicBaseUrl.value}/api${SYNC_IDENTITY_SOURCE_API_PATH}/${editing.value.id}/feishu/events`
})

const secretLabel = (label: string) => {
  return editing.value ? `${label}（留空则不修改）` : label
}

const open = (row?: SyncIdentitySource) => {
  editing.value = row ?? null
  form.value = row
    ? { ...createDefaultForm(), name: row.name, appId: row.appId, status: row.status }
    : createDefaultForm()
  drawerOpen.value = true
}

const handleCopyCallback = async () => {
  try {
    await navigator.clipboard.writeText(callbackUrl.value)
    message.success('回调地址已复制')
  } catch (error: unknown) {
    console.error('复制失败:', error)
    message.error('复制失败')
  }
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = { ...form.value }
    if (editing.value) {
      await syncIdentitySourceApi.update(editing.value.id, payload)
      message.success('身份源已更新')
    } else {
      await syncIdentitySourceApi.create(payload)
      message.success('身份源已创建')
    }
    drawerOpen.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存同步身份源失败:', error)
    message.error(getApiErrorMessage(error, '保存同步身份源失败'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-drawer
    v-model:open="drawerOpen"
    :title="editing ? '编辑飞书同步身份源' : '添加飞书同步身份源'"
    size="large"
    destroy-on-hidden
  >
    <a-form :model="form" :rules="rules" layout="vertical" @finish="handleSubmit">
      <a-form-item label="身份源名称" name="name">
        <a-input v-model:value="form.name" placeholder="例如：总部飞书通讯录" />
      </a-form-item>
      <a-form-item label="飞书 App ID" name="appId">
        <a-input v-model:value="form.appId" placeholder="cli_xxx" />
      </a-form-item>
      <a-form-item :label="secretLabel('App Secret')" name="appSecret">
        <a-input v-model:value="form.appSecret" type="password" />
      </a-form-item>
      <a-form-item :label="secretLabel('Verification Token')" name="verificationToken">
        <a-input v-model:value="form.verificationToken" type="password" />
      </a-form-item>
      <a-form-item :label="secretLabel('Encrypt Key')" name="encryptKey">
        <a-input v-model:value="form.encryptKey" type="password" />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select v-model:value="form.status" :options="statusOptions" />
      </a-form-item>

      <div class="rounded-lg border bg-[#F4F8FB]/40 p-4 text-sm">
        <p class="font-medium">飞书事件配置</p>
        <p class="mt-2 break-all text-[#52677D]">{{ callbackUrl }}</p>
        <a-button class="mt-2" size="small" html-type="button" :disabled="!editing" @click="handleCopyCallback">
          <Copy class="size-3" />
          复制回调地址
        </a-button>
        <p class="mt-3 text-[#52677D]">
          在飞书自建应用中配置该地址，并订阅用户、部门的创建、更新、删除事件；应用需具备通讯录、邮箱、手机和部门信息读取权限。
        </p>
      </div>

      <div class="mt-6 flex justify-end gap-2">
        <a-button html-type="button" @click="drawerOpen = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
      </div>
    </a-form>
  </a-drawer>
</template>
