import { onBeforeUnmount, ref } from 'vue'
import axios from 'axios'
import { message } from 'antdv-next'
import type { UploadProps } from 'antdv-next'
import type { UserStatus } from '@/types/user'
import type { SelectOption } from './types'

const statusLabelMap: Record<UserStatus, string> = {
  active: '正常',
  disabled: '禁用',
  locked: '锁定'
}

const statusColorMap: Record<UserStatus, string> = {
  active: 'green',
  disabled: 'orange',
  locked: 'red'
}

const AVATAR_ACCEPT_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const AVATAR_MAX_SIZE = 2 * 1024 * 1024

/**
 * 用户字典：状态选项、文本与颜色，以及时间格式化。
 */
export function useUserDict() {
  const userStatusOptions: SelectOption<UserStatus>[] = Object.entries(statusLabelMap).map(([value, label]) => {
    return { value: value as UserStatus, label }
  })

  const getStatusText = (status: string) => {
    return statusLabelMap[status as UserStatus] ?? '未知'
  }

  const getStatusVariant = (status: string) => {
    return statusColorMap[status as UserStatus] ?? 'default'
  }

  const formatDate = (date?: string) => {
    return date ? new Date(date).toLocaleString() : '-'
  }

  return {
    userStatusOptions,
    getStatusText,
    getStatusVariant,
    formatDate
  }
}

/**
 * 头像上传：校验格式与大小，读取为 DataURL 后通过回调交给调用方。
 * 组件卸载时会自动中止未完成的读取。
 */
export function useAvatarUpload(onLoaded: (dataUrl: string) => void) {
  const avatarLoading = ref(false)
  let avatarReader: FileReader | null = null

  const cancelAvatarRead = () => {
    const reader = avatarReader
    avatarReader = null
    reader?.abort()
    avatarLoading.value = false
  }

  const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
    if (!AVATAR_ACCEPT_TYPES.includes(file.type)) {
      message.error('请选择 JPG、PNG 或 WebP 格式的图片')
      return false
    }
    if (file.size > AVATAR_MAX_SIZE) {
      message.error('头像图片不能超过 2 MB')
      return false
    }

    cancelAvatarRead()
    const reader = new FileReader()
    avatarReader = reader
    avatarLoading.value = true
    reader.onload = () => {
      if (avatarReader === reader && typeof reader.result === 'string') {
        onLoaded(reader.result)
      }
    }
    reader.onerror = () => {
      if (avatarReader === reader) {
        message.error('头像图片读取失败，请重新选择')
      }
    }
    reader.onloadend = () => {
      if (avatarReader === reader) {
        avatarReader = null
        avatarLoading.value = false
      }
    }
    reader.readAsDataURL(file)
    return false
  }

  onBeforeUnmount(cancelAvatarRead)

  return {
    avatarLoading,
    beforeAvatarUpload,
    cancelAvatarRead
  }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data?.msg || fallback
  }
  return fallback
}
