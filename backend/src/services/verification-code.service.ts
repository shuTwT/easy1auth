import redis from '../lib/redis'

const VERIFICATION_CODE_PREFIX = 'verification_code:'
const CODE_EXPIRY = 300
const CODE_LENGTH = 6

export class VerificationCodeService {
  static generateCode(): string {
    let code = ''
    for (let i = 0; i < CODE_LENGTH; i++) {
      code += Math.floor(Math.random() * 10).toString()
    }
    return code
  }

  static async sendCode(email: string): Promise<{ code: string; message: string }> {
    const code = this.generateCode()
    const key = `${VERIFICATION_CODE_PREFIX}${email}`

    const existingCode = await redis.get(key)
    if (existingCode) {
      const ttl = await redis.ttl(key)
      return {
        code: existingCode,
        message: `验证码已发送，请${ttl}秒后重试`
      }
    }

    await redis.setex(key, CODE_EXPIRY, code)

    console.log(`[验证码] 邮箱: ${email}, 验证码: ${code}, 有效期: ${CODE_EXPIRY}秒`)

    return {
      code,
      message: '验证码已发送到您的邮箱（开发模式：验证码已打印到控制台）'
    }
  }

  static async verifyCode(email: string, code: string): Promise<boolean> {
    const key = `${VERIFICATION_CODE_PREFIX}${email}`
    const storedCode = await redis.get(key)

    if (!storedCode) {
      return false
    }

    if (storedCode !== code) {
      return false
    }

    await redis.del(key)
    return true
  }

  static async getCode(email: string): Promise<string | null> {
    const key = `${VERIFICATION_CODE_PREFIX}${email}`
    return await redis.get(key)
  }
}
