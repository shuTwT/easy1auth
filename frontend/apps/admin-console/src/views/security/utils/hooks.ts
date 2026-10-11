import { computed } from 'vue'
import type { Ref } from 'vue'
import axios from 'axios'
import type { PasswordPolicy } from '@/api/security'
import type { PasswordRequirement, PasswordStrength } from './types'

const SPECIAL_CHAR_PATTERN = /[!@#$%^&*()_+\-=\[\]{}|;:,.<>?]/

/**
 * 密码检查：根据输入计算强度，并按密码策略生成逐项要求的满足状态。
 */
export function usePasswordChecker(password: Ref<string>, policy: Ref<PasswordPolicy | null>) {
  const passwordStrength = computed<PasswordStrength>(() => {
    const value = password.value
    if (!value) {
      return { level: 0, text: '', color: '#E5E7EB' }
    }

    let score = 0
    if (value.length >= 8) score++
    if (value.length >= 12) score++
    if (/[A-Z]/.test(value)) score++
    if (/[a-z]/.test(value)) score++
    if (/\d/.test(value)) score++
    if (SPECIAL_CHAR_PATTERN.test(value)) score++

    if (score <= 2) {
      return { level: 1, text: '弱', color: '#EF4444' }
    }
    if (score <= 4) {
      return { level: 2, text: '中', color: '#F59E0B' }
    }
    return { level: 3, text: '强', color: '#10B981' }
  })

  const requirements = computed<PasswordRequirement[]>(() => {
    const current = policy.value
    if (!current) {
      return []
    }
    const value = password.value
    const result: PasswordRequirement[] = [
      { key: 'length', label: `至少 ${current.minLength} 个字符`, met: value.length >= current.minLength }
    ]
    if (current.requireUppercase) {
      result.push({ key: 'uppercase', label: '包含大写字母', met: /[A-Z]/.test(value) })
    }
    if (current.requireLowercase) {
      result.push({ key: 'lowercase', label: '包含小写字母', met: /[a-z]/.test(value) })
    }
    if (current.requireNumbers) {
      result.push({ key: 'numbers', label: '包含数字', met: /\d/.test(value) })
    }
    if (current.requireSpecialChars) {
      result.push({ key: 'special', label: '包含特殊字符', met: SPECIAL_CHAR_PATTERN.test(value) })
    }
    return result
  })

  return {
    passwordStrength,
    requirements
  }
}

/**
 * 从接口异常中提取后端错误信息。
 */
export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<{ msg?: string }>(error)) {
    return error.response?.data?.msg || fallback
  }
  return error instanceof Error ? error.message : fallback
}
