import { message } from 'antdv-next'
import type { Application, ApplicationType } from '@/types/application'
import type { SsoStats } from './types'

const typeLabelMap: Record<ApplicationType, string> = {
  web: 'Web应用',
  spa: '单页应用',
  native: '原生应用',
  machine: '机器对机器'
}

const typeColorMap: Record<ApplicationType, string> = {
  web: 'blue',
  spa: 'green',
  native: 'purple',
  machine: 'orange'
}

/**
 * 单点登录字典：应用类型、状态的文本与颜色，以及统计数据计算。
 */
export function useSsoDict() {
  const getTypeText = (type: string) => {
    return typeLabelMap[type as ApplicationType] ?? '未知'
  }

  const getTypeVariant = (type: string) => {
    return typeColorMap[type as ApplicationType] ?? 'cyan'
  }

  const getStatusText = (status: string) => {
    return status === 'active' ? '启用' : '禁用'
  }

  const getStatusVariant = (status: string) => {
    return status === 'active' ? 'green' : 'red'
  }

  const buildStats = (apps: Application[], total: number): SsoStats => {
    return {
      totalApps: total,
      activeApps: apps.filter((app) => app.status === 'active').length,
      webApps: apps.filter((app) => app.type === 'web').length,
      spaApps: apps.filter((app) => app.type === 'spa').length,
      nativeApps: apps.filter((app) => app.type === 'native').length,
      machineApps: apps.filter((app) => app.type === 'machine').length
    }
  }

  return {
    getTypeText,
    getTypeVariant,
    getStatusText,
    getStatusVariant,
    buildStats
  }
}

/**
 * 复制文本到剪贴板，并通过 message 提示结果。
 */
export function useClipboard() {
  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text)
      message.success('已复制到剪贴板')
    } catch (error: unknown) {
      console.error('复制失败:', error)
      message.error('复制失败')
    }
  }

  return { copyToClipboard }
}
