import Redis from 'ioredis'
import { sendVerificationCode } from './email.service'

const redis = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: parseInt(process.env.REDIS_PORT || '6379'),
  password: process.env.REDIS_PASSWORD,
  db: parseInt(process.env.REDIS_DB || '0')
})

const CODE_EXPIRE_TIME = 300 // 5分钟
const CODE_PREFIX = 'email_code:'
const CODE_RATE_LIMIT_PREFIX = 'email_rate:'

export const generateCode = (): string => {
  return Math.floor(100000 + Math.random() * 900000).toString()
}

export const storeCode = async (email: string, code: string): Promise<void> => {
  const key = `${CODE_PREFIX}${email}`
  await redis.setex(key, CODE_EXPIRE_TIME, code)
}

export const verifyCode = async (email: string, code: string): Promise<boolean> => {
  const key = `${CODE_PREFIX}${email}`
  const storedCode = await redis.get(key)
  
  if (storedCode && storedCode === code) {
    await redis.del(key)
    return true
  }
  
  return false
}

export const checkRateLimit = async (email: string): Promise<boolean> => {
  const key = `${CODE_RATE_LIMIT_PREFIX}${email}`
  const count = await redis.get(key)
  
  if (count && parseInt(count) >= 5) {
    return false
  }
  
  return true
}

export const incrementRateLimit = async (email: string): Promise<void> => {
  const key = `${CODE_RATE_LIMIT_PREFIX}${email}`
  const exists = await redis.exists(key)
  
  if (exists) {
    await redis.incr(key)
  } else {
    await redis.setex(key, 3600, '1')
  }
}

export const sendCode = async (email: string): Promise<{ success: boolean; message: string }> => {
  const rateLimited = await checkRateLimit(email)
  if (!rateLimited) {
    return { success: false, message: '发送频率过高，请1小时后再试' }
  }

  const code = generateCode()
  await storeCode(email, code)
  
  const sent = await sendVerificationCode(email, code)
  
  if (sent) {
    await incrementRateLimit(email)
    return { success: true, message: '验证码已发送' }
  }
  
  return { success: false, message: '验证码发送失败' }
}
