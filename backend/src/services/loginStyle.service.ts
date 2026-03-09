import prisma from '../lib/prisma'
import { AppError } from '../middleware/errorHandler'

export interface LoginStyleUpdateDto {
  logo?: string
  logoDark?: string
  backgroundImage?: string
  backgroundColor?: string
  primaryColor?: string
  title?: string
  subtitle?: string
  customCSS?: string
  loginMethods?: string[]
  socialProviders?: string[]
}

export interface LoginStyleResponse {
  id: string
  logo?: string
  logoDark?: string
  backgroundImage?: string
  backgroundColor: string
  primaryColor: string
  title: string
  subtitle: string
  customCSS?: string
  loginMethods?: string[]
  socialProviders?: string[]
  createdAt: Date
  updatedAt: Date
}

const DEFAULT_LOGIN_STYLE: Omit<LoginStyleResponse, 'id' | 'createdAt' | 'updatedAt'> = {
  backgroundColor: '#f5f7fa',
  primaryColor: '#0369A1',
  title: 'Easy1Auth',
  subtitle: '企业级身份管理平台',
  loginMethods: ['password', 'email', 'passkey'],
  socialProviders: [],
}

export class LoginStyleService {
  async getLoginStyle(tenantId: string): Promise<LoginStyleResponse> {
    let style = await prisma.loginStyle.findUnique({
      where: { tenantId },
    })

    if (!style) {
      style = await prisma.loginStyle.create({
        data: {
          tenantId,
          ...DEFAULT_LOGIN_STYLE,
          loginMethods: DEFAULT_LOGIN_STYLE.loginMethods as any,
          socialProviders: DEFAULT_LOGIN_STYLE.socialProviders as any,
        },
      })
    }

    return {
      id: style.id,
      logo: style.logo || undefined,
      logoDark: style.logoDark || undefined,
      backgroundImage: style.backgroundImage || undefined,
      backgroundColor: style.backgroundColor,
      primaryColor: style.primaryColor,
      title: style.title,
      subtitle: style.subtitle,
      customCSS: style.customCSS || undefined,
      loginMethods: (style.loginMethods as string[]) || undefined,
      socialProviders: (style.socialProviders as string[]) || undefined,
      createdAt: style.createdAt,
      updatedAt: style.updatedAt,
    }
  }

  async updateLoginStyle(
    tenantId: string,
    data: LoginStyleUpdateDto
  ): Promise<LoginStyleResponse> {
    let style = await prisma.loginStyle.findUnique({
      where: { tenantId },
    })

    if (!style) {
      style = await prisma.loginStyle.create({
        data: {
          tenantId,
          ...DEFAULT_LOGIN_STYLE,
          loginMethods: DEFAULT_LOGIN_STYLE.loginMethods as any,
          socialProviders: DEFAULT_LOGIN_STYLE.socialProviders as any,
        },
      })
    }

    const updatedStyle = await prisma.loginStyle.update({
      where: { tenantId },
      data: {
        logo: data.logo,
        logoDark: data.logoDark,
        backgroundImage: data.backgroundImage,
        backgroundColor: data.backgroundColor,
        primaryColor: data.primaryColor,
        title: data.title,
        subtitle: data.subtitle,
        customCSS: data.customCSS,
        loginMethods: data.loginMethods as any,
        socialProviders: data.socialProviders as any,
      },
    })

    return {
      id: updatedStyle.id,
      logo: updatedStyle.logo || undefined,
      logoDark: updatedStyle.logoDark || undefined,
      backgroundImage: updatedStyle.backgroundImage || undefined,
      backgroundColor: updatedStyle.backgroundColor,
      primaryColor: updatedStyle.primaryColor,
      title: updatedStyle.title,
      subtitle: updatedStyle.subtitle,
      customCSS: updatedStyle.customCSS || undefined,
      loginMethods: (updatedStyle.loginMethods as string[]) || undefined,
      socialProviders: (updatedStyle.socialProviders as string[]) || undefined,
      createdAt: updatedStyle.createdAt,
      updatedAt: updatedStyle.updatedAt,
    }
  }

  async getPublicLoginStyle(domain?: string): Promise<LoginStyleResponse> {
    if (!domain) {
      return {
        id: 'default',
        ...DEFAULT_LOGIN_STYLE,
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    }

    const customDomain = await prisma.customDomain.findFirst({
      where: {
        domain,
        status: 'verified',
      },
    })

    if (!customDomain) {
      return {
        id: 'default',
        ...DEFAULT_LOGIN_STYLE,
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    }

    const style = await prisma.loginStyle.findUnique({
      where: { tenantId: customDomain.tenantId },
    })

    if (!style) {
      return {
        id: 'default',
        ...DEFAULT_LOGIN_STYLE,
        createdAt: new Date(),
        updatedAt: new Date(),
      }
    }

    return {
      id: style.id,
      logo: style.logo || undefined,
      logoDark: style.logoDark || undefined,
      backgroundImage: style.backgroundImage || undefined,
      backgroundColor: style.backgroundColor,
      primaryColor: style.primaryColor,
      title: style.title,
      subtitle: style.subtitle,
      customCSS: style.customCSS || undefined,
      loginMethods: (style.loginMethods as string[]) || undefined,
      socialProviders: (style.socialProviders as string[]) || undefined,
      createdAt: style.createdAt,
      updatedAt: style.updatedAt,
    }
  }
}
