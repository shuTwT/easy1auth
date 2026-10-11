<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'antdv-next'
import { userPoolPackageApi } from '@/api/userPoolPackage'
import type { ManagementPermission, UserPoolPackage } from '@/types/userPoolPackage'
import { descendantCodes, usePermissionTree, useUserPoolPackageDict } from '../utils/hooks'
import type { PermissionTreeNode } from '../utils/types'

const props = defineProps<{
  permissions: ManagementPermission[]
}>()

const emit = defineEmits<{
  success: []
  select: [codes: string[]]
  cancelSelect: []
}>()

const permissionDialogVisible = ref(false)
const permissionSubmitting = ref(false)
const permissionReturnsToPackageForm = ref(false)
const permissionTarget = ref<UserPoolPackage | null>(null)
const selectedPermissions = ref<string[]>([])

const { permissionTree } = usePermissionTree(() => {
  return props.permissions
})
const { permissionTypeLabel, permissionTypeVariant } = useUserPoolPackageDict()

/**
 * 直接配置指定套餐的权限，保存后提交到后端。
 */
const open = (item: UserPoolPackage) => {
  permissionTarget.value = item
  permissionReturnsToPackageForm.value = false
  selectedPermissions.value = [...item.permissionCodes]
  permissionDialogVisible.value = true
}

/**
 * 从套餐表单中选择权限，确认后通过 select 事件回传，不直接提交。
 */
const openForSelect = (codes: string[]) => {
  permissionTarget.value = null
  permissionReturnsToPackageForm.value = true
  selectedPermissions.value = [...codes]
  permissionDialogVisible.value = true
}

const toggleTreePermission = (node: PermissionTreeNode) => {
  if (!node.code) {
    return
  }
  if (selectedPermissions.value.includes(node.code)) {
    const removals = ['directory', 'menu'].includes(node.type) ? descendantCodes(node) : [node.code]
    selectedPermissions.value = selectedPermissions.value.filter((code) => {
      return !removals.includes(code)
    })
    return
  }
  selectedPermissions.value = [...new Set([...selectedPermissions.value, ...node.ancestorCodes, node.code])]
}

const submitPermissions = async () => {
  if (selectedPermissions.value.length === 0) {
    message.error('请至少选择一项用户池权限')
    return
  }
  if (permissionReturnsToPackageForm.value) {
    permissionDialogVisible.value = false
    emit('select', [...selectedPermissions.value])
    return
  }
  if (!permissionTarget.value) {
    return
  }
  permissionSubmitting.value = true
  try {
    await userPoolPackageApi.replacePermissions(permissionTarget.value.id, selectedPermissions.value)
    message.success('套餐权限更新成功')
    permissionDialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('更新套餐权限失败:', error)
    message.error('更新套餐权限失败')
  } finally {
    permissionSubmitting.value = false
  }
}

const closePermissionDialog = () => {
  permissionDialogVisible.value = false
  if (permissionReturnsToPackageForm.value) {
    permissionReturnsToPackageForm.value = false
    emit('cancelSelect')
  }
}

defineExpose({ open, openForSelect })
</script>

<template>
  <a-modal v-model:open="permissionDialogVisible">
    <div class="max-w-3xl">
      <div>
        <h3>配置套餐权限{{ permissionTarget ? ` · ${permissionTarget.name}` : '' }}</h3>
      <p>按菜单层级选择权限；选择操作时会自动保留其父菜单，取消菜单会同时取消子菜单和关联操作。</p>
      </div>
      <div class="h-[min(60vh,520px)] overflow-y-auto rounded-md border p-4">
        <a-empty v-if="permissionTree.length === 0" class="py-12" description="暂无可用权限" />
        <a-tree
          v-else
          :tree-data="permissionTree"
          :field-names="{ key: 'id', title: 'label', children: 'children' }"
          :default-expand-all="true"
          :selectable="false"
        >
          <template #titleRender="data">
            <div class="flex min-w-0 items-center gap-2 py-1">
              <a-checkbox v-if="data.code" :checked="selectedPermissions.includes(data.code)" @update:checked="toggleTreePermission(data as unknown as PermissionTreeNode)" />
              <span :class="data.type === 'group' ? 'font-medium' : 'text-sm'">{{ data.label }}</span>
              <a-tag v-if="data.type !== 'group'" :color="permissionTypeVariant(data.type)">{{ permissionTypeLabel(data.type) }}</a-tag>
              <code v-if="data.code" class="ml-auto hidden truncate text-xs text-[#52677D] md:block">{{ data.code }}</code>
            </div>
          </template>
        </a-tree>
      </div>
    </div>
    <template #footer>
      <div class="flex items-center">
        <span class="mr-auto text-sm text-[#52677D]">已选择 {{ selectedPermissions.length }} 项</span>
        <div class="flex gap-2">
          <a-button @click="closePermissionDialog">取消</a-button>
          <a-button type="primary" :disabled="permissionSubmitting" @click="submitPermissions">{{ permissionSubmitting ? '保存中...' : '保存权限' }}</a-button>
        </div>
      </div>
    </template>
  </a-modal>
</template>
