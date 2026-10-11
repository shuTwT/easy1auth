<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'antdv-next'
import { Plus } from '@lucide/vue'
import { userApi } from '@/api/user'
import type { CreateUserDto, User } from '@/types/user'
import { getApiErrorMessage, useAvatarUpload } from '../utils/hooks'
import { userFormRules, userPasswordRules } from '../utils/rules'
import type { UserEditForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const currentId = ref<string | undefined>(undefined)

const createDefaultForm = (): UserEditForm => {
  return {
    username: '',
    email: '',
    password: '',
    phone: '',
    name: '',
    avatar: '',
    department: '',
    position: ''
  }
}

const userForm = ref<UserEditForm>(createDefaultForm())

const { avatarLoading, beforeAvatarUpload, cancelAvatarRead } = useAvatarUpload((dataUrl) => {
  userForm.value.avatar = dataUrl
})

watch(dialogVisible, (visible) => {
  if (!visible) {
    cancelAvatarRead()
  }
}, { flush: 'sync' })

const open = (row?: User) => {
  cancelAvatarRead()
  if (row) {
    dialogTitle.value = '编辑用户'
    currentId.value = row.id
    userForm.value = {
      username: row.username,
      email: row.email || '',
      password: '',
      phone: row.phone || '',
      name: row.name,
      avatar: row.avatar || '',
      department: row.department || '',
      position: row.position || ''
    }
  } else {
    dialogTitle.value = '新增用户'
    currentId.value = undefined
    userForm.value = createDefaultForm()
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (avatarLoading.value) {
    return
  }
  try {
    if (currentId.value) {
      const { password: _password, ...payload } = userForm.value
      await userApi.update(currentId.value, payload)
      message.success('更新成功')
    } else {
      await userApi.create(userForm.value as CreateUserDto)
      message.success('创建成功')
    }
    dialogVisible.value = false
    emit('success')
  } catch (error: unknown) {
    console.error('保存用户失败:', error)
    message.error(getApiErrorMessage(error, '保存用户失败'))
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" :title="dialogTitle" :width="720" :footer="null">
    <a-form :model="userForm" :rules="userFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid grid-cols-1 gap-x-6 md:grid-cols-2">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="userForm.username" placeholder="请输入用户名" :disabled="!!currentId" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="userForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item v-if="!currentId" label="密码" name="password" :rules="userPasswordRules">
          <a-input v-model:value="userForm.password" type="password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="userForm.name" placeholder="请输入姓名" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="userForm.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="部门" name="department">
          <a-input v-model:value="userForm.department" placeholder="请输入部门" />
        </a-form-item>
        <a-form-item label="岗位" name="position">
          <a-input v-model:value="userForm.position" placeholder="请输入岗位" />
        </a-form-item>
        <a-form-item label="头像" name="avatar">
          <div class="flex items-start gap-3">
            <a-upload
                accept="image/jpeg,image/png,image/webp"
                list-type="picture-card"
                :file-list="[]"
                :show-upload-list="false"
                :multiple="false"
                :disabled="avatarLoading"
                :before-upload="beforeAvatarUpload"
            >
              <button
                  type="button"
                  class="flex h-full w-full flex-col items-center justify-center gap-2 overflow-hidden rounded-lg border-0 bg-transparent p-0"
                  :disabled="avatarLoading"
                  :aria-label="userForm.avatar ? '更换头像' : '上传头像'"
              >
                <a-spin v-if="avatarLoading" size="small" />
                <img
                    v-else-if="userForm.avatar"
                    :src="userForm.avatar"
                    alt="用户头像预览"
                    class="h-full w-full object-cover"
                />
                <template v-else>
                  <Plus :size="20" />
                  <span class="text-sm">上传头像</span>
                </template>
              </button>
            </a-upload>
            <div class="pt-1">
              <p class="text-xs text-[#52677D]">支持 JPG、PNG、WebP，最大 2 MB</p>
              <a-button
                  v-if="userForm.avatar"
                  type="link"
                  size="small"
                  :disabled="avatarLoading"
                  @click="userForm.avatar = ''"
              >
                移除头像
              </a-button>
            </div>
          </div>
        </a-form-item>
      </div>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :disabled="avatarLoading">确定</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
