<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { positionApi } from '@/api/position'
import type { Position } from '@/types/position'
import { usePositionDict } from '../utils/hooks'
import { positionFormRules } from '../utils/rules'
import type { PositionEditForm } from '../utils/types'

const emit = defineEmits<{
  success: []
}>()

const { getLevelColor, getLevelText } = usePositionDict()

const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const submitting = ref(false)
const currentId = ref<string | undefined>(undefined)

const createDefaultForm = (): PositionEditForm => {
  return {
    name: '',
    code: '',
    description: '',
    departmentId: undefined,
    level: 1,
    sequence: '',
    maxCount: undefined
  }
}

const positionForm = ref<PositionEditForm>(createDefaultForm())

const open = (row?: Position) => {
  if (row) {
    dialogTitle.value = '编辑岗位'
    currentId.value = row.id
    positionForm.value = {
      name: row.name,
      code: row.code,
      description: row.description || '',
      departmentId: row.departmentId || undefined,
      level: row.level,
      sequence: row.sequence || '',
      maxCount: row.maxCount || undefined
    }
  } else {
    dialogTitle.value = '新增岗位'
    currentId.value = undefined
    positionForm.value = createDefaultForm()
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const payload = {
      ...positionForm.value,
      name: positionForm.value.name.trim(),
      code: positionForm.value.code.trim(),
      level: positionForm.value.level ?? 1
    }
    if (currentId.value) {
      await positionApi.update(currentId.value, payload)
      message.success('更新成功')
    } else {
      await positionApi.create(payload)
      message.success('创建成功')
    }
    dialogVisible.value = false
    emit('success')
  } catch (error: any) {
    console.error('保存岗位失败:', error)
    message.error(error.response?.data?.msg || '保存岗位失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<template>
  <a-modal v-model:open="dialogVisible" :title="dialogTitle" :footer="null">
    <a-form :model="positionForm" :rules="positionFormRules" layout="vertical" class="py-4" @finish="handleSubmit">
      <div class="grid gap-4">
        <a-form-item label="岗位名称" name="name">
          <a-input v-model:value="positionForm.name" placeholder="请输入岗位名称" />
        </a-form-item>
        <a-form-item label="岗位编码" name="code">
          <a-input
            v-model:value="positionForm.code"
            placeholder="请输入岗位编码（大写字母和下划线）"
            :disabled="!!currentId"
          />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="positionForm.description" placeholder="请输入岗位描述" :rows="3" />
        </a-form-item>
        <a-form-item label="岗位级别" name="level">
          <a-slider v-model:value="positionForm.level" :min="1" :max="10" :step="1" />
          <div class="text-center mt-2">
            <a-tag :color="getLevelColor(positionForm.level)">
              {{ positionForm.level }} - {{ getLevelText(positionForm.level) }}
            </a-tag>
          </div>
        </a-form-item>
        <a-form-item label="最大人数" name="maxCount">
          <a-input-number v-model:value="positionForm.maxCount" :min="1" :max="999" />
        </a-form-item>
        <a-form-item label="排序" name="sequence">
          <a-input v-model:value="positionForm.sequence" placeholder="请输入排序标识" />
        </a-form-item>
      </div>
      <div class="flex justify-end gap-2">
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" html-type="submit" :loading="submitting">确定</a-button>
      </div>
    </a-form>
  </a-modal>
</template>
