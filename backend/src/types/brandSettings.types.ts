export interface BrandSettings {
  // 登录页面设置
  loginPage: {
    logo?: string
    backgroundImage?: string
    backgroundColor?: string
    customCSS?: string
    title?: string
    subtitle?: string
  }
  
  // 管理面板设置
  adminPanel: {
    logo?: string
    favicon?: string
    primaryColor?: string
    customCSS?: string
    headerColor?: string
    sidebarColor?: string
  }
  
  // 法律文档
  legalDocuments: {
    termsOfService?: string
    privacyPolicy?: string
  }
  
  // 自定义域名
  customDomain?: string
}

export interface UpdateBrandSettingsDto {
  loginPage?: {
    logo?: string
    backgroundImage?: string
    backgroundColor?: string
    customCSS?: string
    title?: string
    subtitle?: string
  }
  
  adminPanel?: {
    logo?: string
    favicon?: string
    primaryColor?: string
    customCSS?: string
    headerColor?: string
    sidebarColor?: string
  }
  
  legalDocuments?: {
    termsOfService?: string
    privacyPolicy?: string
  }
  
  customDomain?: string
}

export interface BrandSettingsResponse {
  tenantId: string
  brandSettings: BrandSettings
  updatedAt: Date
}
