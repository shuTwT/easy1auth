<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { Trash2 } from '@lucide/vue'
import { applicationApi } from '@/api/application'
import type { Application, CreateApplicationDto, UpdateApplicationDto } from '@/types/application'
import { useApplicationDict, getApiErrorMessage } from '../utils/hooks'
import { applicationEditRules } from '../utils/rules'
import type { ApplicationEditForm, EditDialogMode } from '../utils/types'

const emit = defineEmits<{
  submit: [mode: EditDialogMode, secret: string]
}>()

const { applicationTypeOptions, formatLifetime } = useApplicationDict()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEditing = ref(false)
const redirectUriInput = ref('')

const createDefaultForm = (): ApplicationEditForm => {
  return {
    id: undefined,
    name: '',
    logo: '',
    description: '',
    type: 'web',
    redirectUris: [],
    allowedGrantTypes: ['authorization_code'],
    accessTokenLifetime: 3600,
    refreshTokenLifetime: 2592000
  }
}

const inlineForm = ref<ApplicationEditForm>(createDefaultForm())

const open = (mode: EditDialogMode, row?: Partial<Application>) => {
  inlineForm.value = createDefaultForm()
  redirectUriInput.value = ''
  isEditing.value = mode === 'edit'
  if (mode === 'create') {
    dialogTitle.value = '新增应用'
  } else {
    dialogTitle.value = '编辑应用'
    inlineForm.value = {
      id: row?.id,
      name: row?.name ?? '',
      logo: row?.logo ?? '',
      description: row?.description ?? '',
      type: row?.type ?? 'web',
      redirectUris: [...(row?.redirectUris ?? [])],
      allowedGrantTypes: [...(row?.allowedGrantTypes ?? [])],
      accessTokenLifetime: row?.accessTokenLifetime,
      refreshTokenLifetime: row?.refreshTokenLifetime
    }
  }
  dialogVisible.value = true
}

const handleAddRedirectUri = () => {
  const uri = redirectUriInput.value.trim()
  if (!uri) {
    return
  }
  inlineForm.value.redirectUris.push(uri)
  redirectUriInput.value = ''
}

const handleRemoveRedirectUri = (index: number) => {
  inlineForm.value.redirectUris.splice(index, 1)
}

const toggleGrantType = (type: string) => {
  const index = inlineForm.value.allowedGrantTypes.indexOf(type)
  if (index > -1) {
    inlineForm.value.allowedGrantTypes.splice(index, 1)
  } else {
    inlineForm.value.allowedGrantTypes.push(type)
  }
}

const handleSubmit = async () => {
  try {
    if (isEditing.value && inlineForm.value.id) {
      const { id, ...rest } = inlineForm.value
      const payload: UpdateApplicationDto = {
        name: rest.name,
        logo: rest.logo,
        description: rest.description,
        type: rest.type,
        redirectUris: rest.redirectUris,
        allowedGrantTypes: rest.allowedGrantTypes,
        accessTokenLifetime: rest.accessTokenLifetime,
        refreshTokenLifetime: rest.refreshTokenLifetime
      }
      const res = await applicationApi.update(id, payload)
      message.success('更新成功')
      dialogVisible.value = false
      emit('submit', 'edit', res.clientSecret ?? '')
    } else {
      const createPayload: CreateApplicationDto = {
        name: inlineForm.value.name ?? '',
        type: inlineForm.value.type
      }
      const res = await applicationApi.create(createPayload)
      message.success('创建成功')
      dialogVisible.value = false
      emit('submit', 'create', res.clientSecret ?? '')
    }
  } catch (error: unknown) {
    console.error('保存应用失败:', error)
    message.error(getApiErrorMessage(error, '保存应用失败'))
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" :title="dialogTitle" :footer="null">
    <a-form :model="inlineForm" :rules="applicationEditRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid gap-4">
        <a-form-item label="应用名称" name="name">
          <a-input v-model:value="inlineForm.name" placeholder="请输入应用名称" />
        </a-form-item>
        <a-form-item label="应用类型" name="type">
          <a-select v-model:value="inlineForm.type" :options="applicationTypeOptions" />
        </a-form-item>
        <template v-if="isEditing">
          <a-form-item label="应用描述" name="description">
            <a-textarea v-model:value="inlineForm.description" :rows="3" placeholder="请输入应用描述" />
          </a-form-item>
          <a-form-item label="应用Logo" name="logo">
            <a-input v-model:value="inlineForm.logo" placeholder="请输入Logo URL" />
          </a-form-item>
          <a-form-item label="重定向URI">
            <div class="flex gap-2 mb-2">
              <a-input v-model:value="redirectUriInput" placeholder="请输入重定向URI" class="flex-1" />
              <a-button html-type="button" @click="handleAddRedirectUri">添加</a-button>
            </div>
            <div v-if="inlineForm.redirectUris.length > 0" class="flex flex-wrap gap-2">
              <a-tag
                  v-for="(uri, index) in inlineForm.redirectUris"
                  :key="uri"
                  color="blue"
                  class="cursor-pointer"
                  @click="handleRemoveRedirectUri(index)"
              >
                {{ uri }}
                <Trash2 class="size-3 ml-1" />
              </a-tag>
            </div>
          </a-form-item>
          <a-form-item label="授权类型">
            <div class="flex flex-col gap-2">
              <div class="flex items-center gap-2">
                <a-checkbox
                    :checked="inlineForm.allowedGrantTypes.includes('authorization_code')"
                    @update:checked="toggleGrantType('authorization_code')"
                />
                <span class="text-sm">授权码模式</span>
              </div>
              <div class="flex items-center gap-2">
                <a-checkbox
                    :checked="inlineForm.allowedGrantTypes.includes('client_credentials')"
                    @update:checked="toggleGrantType('client_credentials')"
                />
                <span class="text-sm">客户端凭证模式</span>
              </div>
              <div class="flex items-center gap-2">
                <a-checkbox
                    :checked="inlineForm.allowedGrantTypes.includes('refresh_token')"
                    @update:checked="toggleGrantType('refresh_token')"
                />
                <span class="text-sm">刷新令牌</span>
              </div>
            </div>
          </a-form-item>
          <a-form-item label="访问令牌有效期" name="accessTokenLifetime">
            <div class="flex items-center gap-2">
              <a-input-number v-model:value="inlineForm.accessTokenLifetime" :min="60" :max="86400" />
              <span class="text-sm text-[#52677D]">秒 ({{ formatLifetime(inlineForm.accessTokenLifetime) }})</span>
            </div>
          </a-form-item>
          <a-form-item label="刷新令牌有效期" name="refreshTokenLifetime">
            <div class="flex items-center gap-2">
              <a-input-number v-model:value="inlineForm.refreshTokenLifetime" :min="3600" :max="31536000" />
              <span class="text-sm text-[#52677D]">秒 ({{ formatLifetime(inlineForm.refreshTokenLifetime) }})</span>
            </div>
          </a-form-item>
        </template>
      </div>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit">确定</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
