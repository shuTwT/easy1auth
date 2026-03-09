import bcrypt from 'bcryptjs'
import prisma from '../lib/prisma'

export interface PasswordPolicy {
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireNumbers: boolean
  requireSpecialChars: boolean
  specialChars: string
  maxAge: number
  historyCount: number
  preventReuse: boolean
}

const DEFAULT_POLICY: PasswordPolicy = {
  minLength: 8,
  requireUppercase: true,
  requireLowercase: true,
  requireNumbers: true,
  requireSpecialChars: true,
  specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?',
  maxAge: 90,
  historyCount: 5,
  preventReuse: true
}

export interface PasswordValidationResult {
  valid: boolean
  errors: string[]
  strength: 'weak' | 'medium' | 'strong'
}

export const validatePassword = (password: string, policy: PasswordPolicy = DEFAULT_POLICY): PasswordValidationResult => {
  const errors: string[] = []
  let strength: 'weak' | 'medium' | 'strong' = 'weak'

  if (password.length < policy.minLength) {
    errors.push(`密码长度至少${policy.minLength}位`)
  }

  if (policy.requireUppercase && !/[A-Z]/.test(password)) {
    errors.push('密码必须包含大写字母')
  }

  if (policy.requireLowercase && !/[a-z]/.test(password)) {
    errors.push('密码必须包含小写字母')
  }

  if (policy.requireNumbers && !/\d/.test(password)) {
    errors.push('密码必须包含数字')
  }

  if (policy.requireSpecialChars) {
    const hasSpecial = new RegExp(`[${policy.specialChars.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}]`).test(password)
    if (!hasSpecial) {
      errors.push('密码必须包含特殊字符')
    }
  }

  const hasUpper = /[A-Z]/.test(password)
  const hasLower = /[a-z]/.test(password)
  const hasNumber = /\d/.test(password)
  const hasSpecial = new RegExp(`[${policy.specialChars.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}]`).test(password)
  
  const typeCount = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length
  
  if (password.length >= 12 && typeCount >= 3) {
    strength = 'strong'
  } else if (password.length >= 8 && typeCount >= 2) {
    strength = 'medium'
  }

  return {
    valid: errors.length === 0,
    errors,
    strength
  }
}

export const checkPasswordHistory = async (userId: string, newPassword: string, historyCount: number = 5): Promise<boolean> => {
  const histories = await prisma.passwordHistory.findMany({
    where: { userId },
    orderBy: { createdAt: 'desc' },
    take: historyCount
  })

  for (const history of histories) {
    const isMatch = await bcrypt.compare(newPassword, history.password)
    if (isMatch) {
      return false
    }
  }

  return true
}

export const addPasswordToHistory = async (userId: string, hashedPassword: string): Promise<void> => {
  await prisma.passwordHistory.create({
    data: {
      userId,
      password: hashedPassword
    }
  })

  const count = await prisma.passwordHistory.count({
    where: { userId }
  })

  if (count > 10) {
    const oldest = await prisma.passwordHistory.findMany({
      where: { userId },
      orderBy: { createdAt: 'asc' },
      take: count - 10
    })

    for (const old of oldest) {
      await prisma.passwordHistory.delete({
        where: { id: old.id }
      })
    }
  }
}

export const checkPasswordExpiry = async (userId: string, maxAge: number = 90): Promise<{ expired: boolean; daysUntilExpiry: number }> => {
  const user = await prisma.admin.findUnique({
    where: { id: userId },
    select: { passwordChangedAt: true }
  })

  if (!user || !user.passwordChangedAt) {
    return { expired: true, daysUntilExpiry: 0 }
  }

  const lastChanged = new Date(user.passwordChangedAt)
  const now = new Date()
  const daysSinceChange = Math.floor((now.getTime() - lastChanged.getTime()) / (1000 * 60 * 60 * 24))
  const daysUntilExpiry = maxAge - daysSinceChange

  return {
    expired: daysUntilExpiry <= 0,
    daysUntilExpiry: Math.max(0, daysUntilExpiry)
  }
}

export const getPasswordPolicy = (): PasswordPolicy => {
  return DEFAULT_POLICY
}
