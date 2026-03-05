import { PrismaClient } from '@prisma/client'
import { PrismaLibSql } from '@prisma/adapter-libsql'
import bcrypt from 'bcryptjs'

const adapter = new PrismaLibSql({
  url: 'file:./dev.db'
})

const prisma = new PrismaClient({ adapter })

async function main() {
  console.log('开始创建种子数据...')

  const hashedPassword = await bcrypt.hash('Admin123!@#', 10)

  const tenant = await prisma.tenant.upsert({
    where: { id: 'default-tenant' },
    update: {},
    create: {
      id: 'default-tenant',
      name: '默认租户',
      domain: 'default.easy1auth.com',
      plan: 'enterprise',
      maxUsers: 1000,
      maxApps: 100,
      status: 'active'
    }
  })

  console.log('✓ 租户创建成功:', tenant.name)

  const admin = await prisma.admin.upsert({
    where: { username: 'admin' },
    update: {},
    create: {
      id: 'admin-001',
      tenantId: tenant.id,
      username: 'admin',
      email: 'admin@easy1auth.com',
      password: hashedPassword,
      status: 'active'
    }
  })

  console.log('✓ 管理员创建成功:', admin.username)
  console.log('  用户名: admin')
  console.log('  邮箱: admin@easy1auth.com')
  console.log('  密码: Admin123!@#')

  const adminRole = await prisma.adminRole.upsert({
    where: { id: 'super-admin' },
    update: {},
    create: {
      id: 'super-admin',
      tenantId: tenant.id,
      name: '超级管理员',
      description: '拥有所有权限的系统管理员',
      permissions: ['*'],
      isSystem: true
    }
  })

  console.log('✓ 管理员角色创建成功:', adminRole.name)

  console.log('\n种子数据创建完成！')
  console.log('==========================================')
  console.log('登录信息:')
  console.log('  用户名: admin')
  console.log('  密码: Admin123!@#')
  console.log('==========================================')
}

main()
  .catch((e) => {
    console.error('种子数据创建失败:', e)
    process.exit(1)
  })
  .finally(async () => {
    await prisma.$disconnect()
  })
