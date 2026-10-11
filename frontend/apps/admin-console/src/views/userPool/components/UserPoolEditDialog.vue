<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { message } from 'antdv-next'
import { userPoolApi } from '@/api/userPool'
import type { UserPool } from '@/types/userPool'
import type { UserPoolPackage } from '@/types/userPoolPackage'
import { useUserPoolDict } from '../utils/hooks'
import { userPoolFormRules } from '../utils/rules'
import type { UserPoolEditForm } from '../utils/types'

const props = defineProps<{
  packages: UserPoolPackage[]
}>()

const emit = defineEmits<{
  success: []
}>()

const { formatLimit } = useUserPoolDict()

const userPoolDialogVisible = ref(false)
const submitting = ref(false)
const editingUserPool = ref<UserPool | null>(null)
const userPoolForm = reactive<UserPoolEditForm>({ name: '', packageId: undefined })

const dialogTitle = computed(() => {
  return '编辑用户池'
})

const packageOptions = computed(() => {
  return props.packages.map((item) => {
    return {
      value: item.id,
      label: `${item.name} · ${formatLimit(item.maxUsers)} 用户 / ${formatLimit(item.maxApps)} 应用`,
    }
  })
})

const open = (userPool: UserPool) => {
  if (!userPool.userPoolPackage) {
    return
  }
  editingUserPool.value = userPool
  Object.assign(userPoolForm, { name: userPool.name, packageId: userPool.userPoolPackage.id })
  userPoolDialogVisible.value = true
}

const submitUserPool = async () => {
  submitting.value = true
  try {
    if (!editingUserPool.value) {
      return
    }
    await userPoolApi.update(editingUserPool.value.id, { name: userPoolForm.name.trim(), packageId: userPoolForm.packageId })
    message.success('用户池更新成功')
    userPoolDialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('保存用户池失败:', error)
    message.error('保存用户池失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="userPoolDialogVisible" :footer="null">
    <div class="w-full max-w-lg">
      <div>
        <h3>{{ dialogTitle }}</h3>
        <p>{{ editingUserPool ? '修改用户池名称或更换为启用中的套餐。' : '新用户池会绑定套餐，并将当前管理员设为用户池管理员。' }}</p>
      </div>
      <a-form :model="userPoolForm" :rules="userPoolFormRules" layout="vertical" class="py-4" @finish="submitUserPool">
        <div class="grid gap-4">
          <a-form-item label="用户池名称" name="name">
            <a-input v-model:value="userPoolForm.name" placeholder="请输入用户池名称" />
          </a-form-item>
          <a-form-item label="用户池套餐" name="packageId">
            <a-select v-model:value="userPoolForm.packageId" class="w-full" placeholder="请选择用户池套餐" :options="packageOptions" />
          </a-form-item>
        </div>
        <div class="flex justify-end gap-2">
          <a-button @click="userPoolDialogVisible = false">取消</a-button>
          <a-button type="primary" html-type="submit" :loading="submitting">保存</a-button>
        </div>
      </a-form>
    </div>
  </a-modal>
</template>
