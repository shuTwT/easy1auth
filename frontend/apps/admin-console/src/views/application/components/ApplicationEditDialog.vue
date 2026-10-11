<script setup lang="ts">

import {computed, ref} from "vue";
import {applicationApi} from "@/api/application.ts";
import {message} from "antdv-next";
import type {Application} from "@/types/application.ts";

const emit = defineEmits<{
  submit:[type:string,secret:string]
}>()

const dialogVisible=ref(false);
const dialogTitle= ref('')
const isEditing = ref(false)

const inlineForm=ref<{
  id:string|undefined
  name:string|undefined,
  type:string|undefined,
  description:string|undefined,
  logo:string|undefined,
  redirectUris:string[],
  allowedGrantTypes:string[],
  accessTokenLifetime:number|undefined,
  refreshTokenLifetime:number|undefined
}>({
  id:undefined,
  name: '',
  logo: '',
  description: '',
  type: 'web',
  redirectUris: [],
  allowedGrantTypes: ['authorization_code'],
  accessTokenLifetime: 3600,
  refreshTokenLifetime: 2592000
})

const resetForm= ()=>{
  inlineForm.value={
    id:undefined,
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

const redirectUriInput = ref('')

const inlineFormRules = {
  name: [{ required: true, message: '请输入应用名称' }],
  type: [{ required: true, message: '请选择应用类型' }],
}

const applicationTypeOptions = [
  { value: 'web', label: 'Web应用' },
  { value: 'native', label: '原生应用' },
  { value: 'spa', label: '单页应用' },
  { value: 'machine', label: '机器对机器' },
]

const open= (type:'create'|'edit',row?:Partial<Application>)=>{
  resetForm()
  if(type=='create'){
    dialogTitle.value='新增应用'
  }else{
    dialogTitle.value='编辑应用'
    inlineForm.value=row as any

  }
  dialogVisible.value=true
}

const handleAddRedirectUri = () => {
  if (redirectUriInput.value) {
    if (!inlineForm.value.redirectUris) {
      inlineForm.value.redirectUris = []
    }
    inlineForm.value.redirectUris.push(redirectUriInput.value)
    redirectUriInput.value = ''
  }
}

const handleRemoveRedirectUri = (index: number) => {
  inlineForm.value.redirectUris?.splice(index, 1)
}

const toggleGrantType = (type: string) => {
  const index = inlineForm.value.allowedGrantTypes?.indexOf(type) ?? -1
  if (index > -1) {
    inlineForm.value.allowedGrantTypes?.splice(index, 1)
  } else {
    if (!inlineForm.value.allowedGrantTypes) {
      inlineForm.value.allowedGrantTypes = []
    }
    inlineForm.value.allowedGrantTypes.push(type)
  }
}

const formatLifetime = (seconds?: number) => {
  if (!seconds) {
    return 'N/A'
  }
  if (seconds < 3600) {
    return `${Math.floor(seconds / 60)} 分钟`
  } else if (seconds < 86400) {
    return `${Math.floor(seconds / 3600)} 小时`
  } else {
    return `${Math.floor(seconds / 86400)} 天`
  }
}

const handleSubmit = async () => {
  let newClientSecret = '';
  try {
    if (isEditing.value&&inlineForm.value.id) {
      const res = await applicationApi.update(inlineForm.value.id, inlineForm.value)
      if (res.clientSecret) {
        newClientSecret = res.clientSecret
      }
      message.success('更新成功')
      dialogVisible.value = false
      emit('submit','edit',newClientSecret)
    } else {
      const createPayload = {
        name: inlineForm.value.name,
        type: inlineForm.value.type
      }
      const res = await applicationApi.create(createPayload)
      newClientSecret = res.clientSecret ?? ''
      message.success('创建成功')
      dialogVisible.value = false
      emit('submit','create',newClientSecret)
    }

  } catch (error: unknown) {
    console.error('保存应用失败:', error)
    message.error('保存应用失败')
  }
}

defineExpose({open})
</script>

<template>
  <a-modal v-model:open="dialogVisible" >
    <div class="max-w-xl">
      <div>
        <h3>{{ dialogTitle }}</h3>
      </div>
      <a-form :model="inlineForm" :rules="inlineFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
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
              <div v-if="inlineForm.redirectUris && inlineForm.redirectUris.length > 0" class="flex flex-wrap gap-2">
                <a-tag
                    v-for="(uri, index) in inlineForm.redirectUris"
                    :key="index"
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
                      :checked="inlineForm.allowedGrantTypes?.includes('authorization_code')"
                      @update:checked="toggleGrantType('authorization_code')"
                  />
                  <span class="text-sm">授权码模式</span>
                </div>
                <div class="flex items-center gap-2">
                  <a-checkbox
                      :checked="inlineForm.allowedGrantTypes?.includes('client_credentials')"
                      @update:checked="toggleGrantType('client_credentials')"
                  />
                  <span class="text-sm">客户端凭证模式</span>
                </div>
                <div class="flex items-center gap-2">
                  <a-checkbox
                      :checked="inlineForm.allowedGrantTypes?.includes('refresh_token')"
                      @update:checked="toggleGrantType('refresh_token')"
                  />
                  <span class="text-sm">刷新令牌</span>
                </div>
              </div>
            </a-form-item>
            <a-form-item label="访问令牌有效期" name="accessTokenLifetime">
              <div class="flex items-center gap-2">
                <a-input-number
                    v-model:value="inlineForm.accessTokenLifetime"
                    :min="60"
                    :max="86400"
                />
                <span class="text-sm text-[#52677D]">秒 ({{ formatLifetime(inlineForm.accessTokenLifetime) }})</span>
              </div>
            </a-form-item>
            <a-form-item label="刷新令牌有效期" name="refreshTokenLifetime">
              <div class="flex items-center gap-2">
                <a-input-number
                    v-model:value="inlineForm.refreshTokenLifetime"
                    :min="3600"
                    :max="31536000"
                />
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
    </div>
  </a-modal>
</template>

<style scoped>

</style>