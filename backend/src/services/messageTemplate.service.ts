import prisma from '../lib/prisma'
import { AppError } from '../middleware/errorHandler'

export interface MessageTemplateCreateDto {
  type: 'email' | 'sms'
  code: string
  name: string
  subject?: string
  content: string
  variables?: Record<string, string>
  isDefault?: boolean
}

export interface MessageTemplateUpdateDto {
  name?: string
  subject?: string
  content?: string
  variables?: Record<string, string>
  isDefault?: boolean
  status?: 'active' | 'inactive'
}

export interface MessageTemplateResponse {
  id: string
  type: string
  code: string
  name: string
  subject?: string
  content: string
  variables?: Record<string, string>
  isDefault: boolean
  status: string
  createdAt: Date
  updatedAt: Date
}

const DEFAULT_TEMPLATES: MessageTemplateCreateDto[] = [
  {
    type: 'email',
    code: 'verification_code',
    name: '验证码邮件',
    subject: '您的验证码 - {{appName}}',
    content: `尊敬的用户，

您的验证码是：{{code}}

验证码将在{{expireMinutes}}分钟后过期，请尽快使用。

如果这不是您的操作，请忽略此邮件。

{{appName}}`,
    variables: {
      code: '验证码',
      expireMinutes: '过期时间(分钟)',
      appName: '应用名称',
    },
    isDefault: true,
  },
  {
    type: 'email',
    code: 'password_reset',
    name: '密码重置邮件',
    subject: '重置您的密码 - {{appName}}',
    content: `尊敬的{{username}}，

您收到这封邮件是因为您申请了密码重置。

请点击以下链接重置您的密码：
{{resetLink}}

此链接将在{{expireHours}}小时后失效。

如果您没有申请密码重置，请忽略此邮件。

{{appName}}`,
    variables: {
      username: '用户名',
      resetLink: '重置链接',
      expireHours: '过期时间(小时)',
      appName: '应用名称',
    },
    isDefault: true,
  },
  {
    type: 'email',
    code: 'welcome',
    name: '欢迎邮件',
    subject: '欢迎加入 {{appName}}',
    content: `尊敬的{{username}}，

欢迎加入{{appName}}！

您的账号已创建成功，现在可以开始使用我们的服务。

如有任何问题，请随时联系我们的客服团队。

{{appName}}`,
    variables: {
      username: '用户名',
      appName: '应用名称',
    },
    isDefault: true,
  },
  {
    type: 'sms',
    code: 'verification_code',
    name: '验证码短信',
    content: '【{{appName}}】您的验证码是{{code}}，{{expireMinutes}}分钟内有效，请勿泄露给他人。',
    variables: {
      code: '验证码',
      expireMinutes: '过期时间(分钟)',
      appName: '应用名称',
    },
    isDefault: true,
  },
  {
    type: 'sms',
    code: 'password_reset',
    name: '密码重置短信',
    content: '【{{appName}}】您正在重置密码，验证码是{{code}}，{{expireMinutes}}分钟内有效。',
    variables: {
      code: '验证码',
      expireMinutes: '过期时间(分钟)',
      appName: '应用名称',
    },
    isDefault: true,
  },
]

export class MessageTemplateService {
  async listTemplates(
    tenantId: string,
    type?: 'email' | 'sms'
  ): Promise<MessageTemplateResponse[]> {
    const where: any = { tenantId }
    if (type) where.type = type

    const templates = await prisma.messageTemplate.findMany({
      where,
      orderBy: [{ type: 'asc' }, { code: 'asc' }],
    })

    return templates.map(template => ({
      id: template.id,
      type: template.type,
      code: template.code,
      name: template.name,
      subject: template.subject || undefined,
      content: template.content,
      variables: (template.variables as Record<string, string>) || undefined,
      isDefault: template.isDefault,
      status: template.status,
      createdAt: template.createdAt,
      updatedAt: template.updatedAt,
    }))
  }

  async getTemplate(tenantId: string, templateId: string): Promise<MessageTemplateResponse> {
    const template = await prisma.messageTemplate.findFirst({
      where: { id: templateId, tenantId },
    })

    if (!template) {
      throw new AppError('模板不存在', 404)
    }

    return {
      id: template.id,
      type: template.type,
      code: template.code,
      name: template.name,
      subject: template.subject || undefined,
      content: template.content,
      variables: (template.variables as Record<string, string>) || undefined,
      isDefault: template.isDefault,
      status: template.status,
      createdAt: template.createdAt,
      updatedAt: template.updatedAt,
    }
  }

  async createTemplate(
    tenantId: string,
    data: MessageTemplateCreateDto
  ): Promise<MessageTemplateResponse> {
    const existing = await prisma.messageTemplate.findUnique({
      where: {
        tenantId_type_code: {
          tenantId,
          type: data.type,
          code: data.code,
        },
      },
    })

    if (existing) {
      throw new AppError('该模板类型和代码已存在', 400)
    }

    if (data.isDefault) {
      await prisma.messageTemplate.updateMany({
        where: {
          tenantId,
          type: data.type,
          code: data.code,
          isDefault: true,
        },
        data: { isDefault: false },
      })
    }

    const template = await prisma.messageTemplate.create({
      data: {
        tenantId,
        type: data.type,
        code: data.code,
        name: data.name,
        subject: data.subject,
        content: data.content,
        variables: data.variables as any,
        isDefault: data.isDefault || false,
        status: 'active',
      },
    })

    return {
      id: template.id,
      type: template.type,
      code: template.code,
      name: template.name,
      subject: template.subject || undefined,
      content: template.content,
      variables: (template.variables as Record<string, string>) || undefined,
      isDefault: template.isDefault,
      status: template.status,
      createdAt: template.createdAt,
      updatedAt: template.updatedAt,
    }
  }

  async updateTemplate(
    tenantId: string,
    templateId: string,
    data: MessageTemplateUpdateDto
  ): Promise<MessageTemplateResponse> {
    const template = await prisma.messageTemplate.findFirst({
      where: { id: templateId, tenantId },
    })

    if (!template) {
      throw new AppError('模板不存在', 404)
    }

    if (data.isDefault) {
      await prisma.messageTemplate.updateMany({
        where: {
          tenantId,
          type: template.type,
          code: template.code,
          isDefault: true,
          NOT: { id: templateId },
        },
        data: { isDefault: false },
      })
    }

    const updatedTemplate = await prisma.messageTemplate.update({
      where: { id: templateId },
      data: {
        name: data.name,
        subject: data.subject,
        content: data.content,
        variables: data.variables as any,
        isDefault: data.isDefault,
        status: data.status,
      },
    })

    return {
      id: updatedTemplate.id,
      type: updatedTemplate.type,
      code: updatedTemplate.code,
      name: updatedTemplate.name,
      subject: updatedTemplate.subject || undefined,
      content: updatedTemplate.content,
      variables: (updatedTemplate.variables as Record<string, string>) || undefined,
      isDefault: updatedTemplate.isDefault,
      status: updatedTemplate.status,
      createdAt: updatedTemplate.createdAt,
      updatedAt: updatedTemplate.updatedAt,
    }
  }

  async deleteTemplate(tenantId: string, templateId: string): Promise<void> {
    const template = await prisma.messageTemplate.findFirst({
      where: { id: templateId, tenantId },
    })

    if (!template) {
      throw new AppError('模板不存在', 404)
    }

    await prisma.messageTemplate.delete({
      where: { id: templateId },
    })
  }

  async initDefaultTemplates(tenantId: string): Promise<void> {
    for (const template of DEFAULT_TEMPLATES) {
      const existing = await prisma.messageTemplate.findUnique({
        where: {
          tenantId_type_code: {
            tenantId,
            type: template.type,
            code: template.code,
          },
        },
      })

      if (!existing) {
        await prisma.messageTemplate.create({
          data: {
            tenantId,
            type: template.type,
            code: template.code,
            name: template.name,
            subject: template.subject,
            content: template.content,
            variables: template.variables as any,
            isDefault: template.isDefault,
            status: 'active',
          },
        })
      }
    }
  }

  async renderTemplate(
    tenantId: string,
    type: 'email' | 'sms',
    code: string,
    variables: Record<string, string>
  ): Promise<{ subject?: string; content: string }> {
    const template = await prisma.messageTemplate.findFirst({
      where: {
        tenantId,
        type,
        code,
        status: 'active',
      },
      orderBy: { isDefault: 'desc' },
    })

    if (!template) {
      throw new AppError('模板不存在', 404)
    }

    let content = template.content
    let subject = template.subject ?? undefined

    for (const [key, value] of Object.entries(variables)) {
      const regex = new RegExp(`{{${key}}}`, 'g')
      content = content.replace(regex, value)
      if (subject) {
        subject = subject.replace(regex, value)
      }
    }

    return { subject, content }
  }
}
