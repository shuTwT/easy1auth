import prisma from '../lib/prisma'
import { AppError } from '../middleware/errorHandler'
import {
  BrandSettings,
  UpdateBrandSettingsDto,
  BrandSettingsResponse,
} from '../types/brandSettings.types'

const DEFAULT_BRAND_SETTINGS: BrandSettings = {
  loginPage: {
    title: 'Easy1Auth',
    subtitle: '企业级身份管理平台',
    backgroundColor: '#f5f7fa',
  },
  adminPanel: {
    primaryColor: '#409eff',
    headerColor: '#ffffff',
    sidebarColor: '#304156',
  },
  legalDocuments: {},
}

export class BrandSettingsService {
  async getBrandSettings(tenantId: string): Promise<BrandSettingsResponse> {
    const tenant = await prisma.tenant.findUnique({
      where: { id: tenantId },
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    const brandSettings = tenant.brandSettings 
      ? (tenant.brandSettings as unknown as BrandSettings)
      : DEFAULT_BRAND_SETTINGS

    return {
      tenantId,
      brandSettings: {
        ...DEFAULT_BRAND_SETTINGS,
        ...brandSettings,
      },
      updatedAt: tenant.updatedAt,
    }
  }

  async updateBrandSettings(
    tenantId: string,
    data: UpdateBrandSettingsDto
  ): Promise<BrandSettingsResponse> {
    const tenant = await prisma.tenant.findUnique({
      where: { id: tenantId },
    })

    if (!tenant) {
      throw new AppError('租户不存在', 404)
    }

    const currentSettings = tenant.brandSettings
      ? (tenant.brandSettings as unknown as BrandSettings)
      : DEFAULT_BRAND_SETTINGS

    const updatedSettings: BrandSettings = {
      loginPage: {
        ...currentSettings.loginPage,
        ...data.loginPage,
      },
      adminPanel: {
        ...currentSettings.adminPanel,
        ...data.adminPanel,
      },
      legalDocuments: {
        ...currentSettings.legalDocuments,
        ...data.legalDocuments,
      },
      customDomain: data.customDomain || currentSettings.customDomain,
    }

    const updatedTenant = await prisma.tenant.update({
      where: { id: tenantId },
      data: {
        brandSettings: updatedSettings as any,
      },
    })

    return {
      tenantId,
      brandSettings: updatedSettings,
      updatedAt: updatedTenant.updatedAt,
    }
  }

  async getPublicBrandSettings(domain?: string): Promise<BrandSettings> {
    let tenant

    if (domain) {
      tenant = await prisma.tenant.findFirst({
        where: {
          domain,
          status: 'active',
        },
      })
    }

    if (!tenant) {
      return DEFAULT_BRAND_SETTINGS
    }

    const brandSettings = tenant.brandSettings
      ? (tenant.brandSettings as unknown as BrandSettings)
      : DEFAULT_BRAND_SETTINGS

    return {
      ...DEFAULT_BRAND_SETTINGS,
      ...brandSettings,
    }
  }

  async validateCustomDomain(tenantId: string, domain: string): Promise<boolean> {
    const existingTenant = await prisma.tenant.findFirst({
      where: {
        domain,
        NOT: {
          id: tenantId,
        },
      },
    })

    return !existingTenant
  }
}
