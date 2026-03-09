import { TOTP, generateSecret, generateURI, verify } from 'otplib'
import QRCode from 'qrcode'
import prisma from '../lib/prisma'
import { sendVerificationCode } from './email.service'

const ISSUER = 'Easy1Auth'
const totp = new TOTP()

export interface MfaSetupResult {
  secret: string
  qrCodeUrl: string
  backupCodes: string[]
}

export const generateMfaSecret = (): string => {
  return generateSecret()
}

export const generateMfaQrCode = async (email: string, secret: string): Promise<string> => {
  const otpauth = generateURI({
    secret,
    label: email,
    issuer: ISSUER
  })
  const qrCodeUrl = await QRCode.toDataURL(otpauth)
  return qrCodeUrl
}

export const verifyTotp = async (secret: string, token: string): Promise<boolean> => {
  try {
    const result = await verify({ secret, token })
    return result.valid === true
  } catch {
    return false
  }
}

export const generateBackupCodes = (count: number = 10): string[] => {
  const codes: string[] = []
  for (let i = 0; i < count; i++) {
    const code = Math.random().toString(36).substring(2, 10).toUpperCase()
    codes.push(code)
  }
  return codes
}

export const setupMfa = async (userId: string, email: string): Promise<MfaSetupResult> => {
  const secret = generateMfaSecret()
  const qrCodeUrl = await generateMfaQrCode(email, secret)
  const backupCodes = generateBackupCodes()

  await prisma.admin.update({
    where: { id: userId },
    data: {
      mfaSecret: secret,
      mfaType: 'totp'
    }
  })

  return {
    secret,
    qrCodeUrl,
    backupCodes
  }
}

export const enableMfa = async (userId: string, token: string): Promise<{ success: boolean; message: string }> => {
  const user = await prisma.admin.findUnique({
    where: { id: userId }
  })

  if (!user || !user.mfaSecret) {
    return { success: false, message: 'MFA未设置' }
  }

  const isValid = await verifyTotp(user.mfaSecret, token)
  if (!isValid) {
    return { success: false, message: '验证码错误' }
  }

  await prisma.admin.update({
    where: { id: userId },
    data: { mfaEnabled: true }
  })

  return { success: true, message: 'MFA已启用' }
}

export const disableMfa = async (userId: string, token: string): Promise<{ success: boolean; message: string }> => {
  const user = await prisma.admin.findUnique({
    where: { id: userId }
  })

  if (!user || !user.mfaSecret) {
    return { success: false, message: 'MFA未设置' }
  }

  const isValid = await verifyTotp(user.mfaSecret, token)
  if (!isValid) {
    return { success: false, message: '验证码错误' }
  }

  await prisma.admin.update({
    where: { id: userId },
    data: {
      mfaEnabled: false,
      mfaSecret: null,
      mfaType: null
    }
  })

  return { success: true, message: 'MFA已禁用' }
}

export const verifyMfa = async (userId: string, token: string): Promise<{ success: boolean; message: string }> => {
  const user = await prisma.admin.findUnique({
    where: { id: userId }
  })

  if (!user || !user.mfaEnabled || !user.mfaSecret) {
    return { success: false, message: 'MFA未启用' }
  }

  const isValid = await verifyTotp(user.mfaSecret, token)
  if (!isValid) {
    return { success: false, message: '验证码错误' }
  }

  return { success: true, message: '验证成功' }
}

export const sendMfaEmailCode = async (userId: string): Promise<{ success: boolean; message: string }> => {
  const user = await prisma.admin.findUnique({
    where: { id: userId }
  })

  if (!user || !user.email) {
    return { success: false, message: '用户邮箱不存在' }
  }

  const code = Math.floor(100000 + Math.random() * 900000).toString()
  const expiresAt = new Date(Date.now() + 5 * 60 * 1000)

  await prisma.mfaToken.create({
    data: {
      userId,
      token: code,
      type: 'email',
      expiresAt
    }
  })

  const sent = await sendVerificationCode(user.email, code)
  if (!sent) {
    return { success: false, message: '验证码发送失败' }
  }

  return { success: true, message: '验证码已发送到邮箱' }
}

export const verifyMfaEmailCode = async (userId: string, code: string): Promise<{ success: boolean; message: string }> => {
  const token = await prisma.mfaToken.findFirst({
    where: {
      userId,
      token: code,
      type: 'email',
      used: false,
      expiresAt: { gt: new Date() }
    }
  })

  if (!token) {
    return { success: false, message: '验证码错误或已过期' }
  }

  await prisma.mfaToken.update({
    where: { id: token.id },
    data: { used: true }
  })

  return { success: true, message: '验证成功' }
}
