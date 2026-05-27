import { PrismaClient } from '@prisma/client'
import { PrismaLibSql } from '@prisma/adapter-libsql'

const adapter = new PrismaLibSql({
  url: 'file:./data/dev.db'
})

const prisma = new PrismaClient({ adapter })

export default prisma
