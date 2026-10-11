<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'antdv-next'
import { ShieldCheck } from '@lucide/vue'
import { userPoolPackageApi } from '@/api/userPoolPackage'
import type { UserPoolPackage, UserPoolPackageMutation } from '@/types/userPoolPackage'
import { packageFormRules } from '../utils/rules'

const emit = defineEmits<{
  success: []
  pickPermissions: [codes: string[]]
}>()

const packageDialogVisible = ref(false)
const submitting = ref(false)
const editingPackage = ref<UserPoolPackage | null>(null)

const createDefaultForm = (): UserPoolPackageMutation => {
  return {
    code: '',
    name: '',
    maxUsers: 100,
    maxApps: 10,
    permissionCodes: [],
  }
}

const packageForm = reactive<UserPoolPackageMutation>(createDefaultForm())

const packageDialogTitle = computed(() => {
  return editingPackage.value ? '编辑用户池套餐' : '创建用户池套餐'
})

const open = (item?: UserPoolPackage) => {
  if (item) {
    editingPackage.value = item
    Object.assign(packageForm, {
      code: item.code,
      name: item.name,
      maxUsers: item.maxUsers,
      maxApps: item.maxApps,
      permissionCodes: [...item.permissionCodes],
    })
  } else {
    editingPackage.value = null
    Object.assign(packageForm, createDefaultForm())
  }
  packageDialogVisible.value = true
}

const reopen = () => {
  packageDialogVisible.value = true
}

const setPermissionCodes = (codes: string[]) => {
  packageForm.permissionCodes = [...codes]
}

const handlePickPermissions = () => {
  packageDialogVisible.value = false
  emit('pickPermissions', [...packageForm.permissionCodes])
}

const submitPackage = async () => {
  submitting.value = true
  try {
    if (editingPackage.value) {
      await userPoolPackageApi.update(editingPackage.value.id, packageForm)
      message.success('套餐更新成功')
    } else {
      await userPoolPackageApi.create(packageForm)
      message.success('套餐创建成功')
    }
    packageDialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('保存用户池套餐失败:', error)
    message.error('保存用户池套餐失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open, reopen, setPermissionCodes })
</script>

<template>
  <a-modal v-model:open="packageDialogVisible" :footer="null">
    <div class="max-w-xl">
      <div>
        <h3>{{ packageDialogTitle }}</h3>
        <p>设置套餐标识、资源上限和初始权限。</p>
      </div>
      <a-form :model="packageForm" :rules="packageFormRules" layout="vertical" class="py-4" @finish="submitPackage">
        <div class="grid gap-4">
          <div class="grid gap-4 sm:grid-cols-2">
            <a-form-item label="套餐编码" name="code">
              <a-input v-model:value="packageForm.code" :disabled="!!editingPackage" placeholder="例如 professional" />
            </a-form-item>
            <a-form-item label="套餐名称" name="name">
              <a-input v-model:value="packageForm.name" placeholder="例如 专业版" />
            </a-form-item>
          </div>
          <div class="grid gap-4 sm:grid-cols-2">
            <a-form-item label="用户上限" name="maxUsers">
              <a-input-number v-model:value="packageForm.maxUsers" :min="1" :max="2147483647" />
            </a-form-item>
            <a-form-item label="应用上限" name="maxApps">
              <a-input-number v-model:value="packageForm.maxApps" :min="1" :max="2147483647" />
            </a-form-item>
          </div>
          <a-form-item label="套餐权限">
            <a-button html-type="button" class="w-full justify-between" @click="handlePickPermissions">
              <span>{{ packageForm.permissionCodes.length }} 项权限已选择</span><ShieldCheck class="size-4" />
            </a-button>
          </a-form-item>
        </div>
        <div class="flex justify-end gap-2">
          <a-button @click="packageDialogVisible = false">取消</a-button>
          <a-button type="primary" html-type="submit" :loading="submitting">保存套餐</a-button>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>
