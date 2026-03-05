import Redis from 'ioredis'
import dotenv from 'dotenv'
import path from 'path'

dotenv.config({ path: path.resolve(process.cwd(), '.env.local') })
dotenv.config()

const redisHost = process.env.REDIS_HOST || 'localhost'
const redisPort = parseInt(process.env.REDIS_PORT || '6379', 10)
const redisPassword = process.env.REDIS_PASSWORD || ''
const redisDb = parseInt(process.env.REDIS_DB || '0', 10)

const redis = new Redis({
  host: redisHost,
  port: redisPort,
  password: redisPassword || undefined,
  db: redisDb,
  retryStrategy: (times: number) => {
    if (times > 3) {
      console.error('Redis connection failed after 3 retries')
      return null
    }
    return Math.min(times * 200, 2000)
  }
})

redis.on('connect', () => {
  console.log('Redis connected successfully')
})

redis.on('error', (error) => {
  console.error('Redis connection error:', error)
})

export default redis
