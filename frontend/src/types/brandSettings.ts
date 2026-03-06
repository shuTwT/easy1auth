export interface BrandSettings {
  loginPage: {
    logo?: string
    backgroundImage?: string
    backgroundColor?: string
    customCSS?: string
    title?: string
    subtitle?: string
  }
  
  adminPanel: {
    logo?: string
    favicon?: string
    primaryColor?: string
    customCSS?: string
    headerColor?: string
    sidebarColor?: string
  }
  
  legalDocuments: {
    termsOfService?: string
    privacyPolicy?: string
  }
  
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
  updatedAt: string
}
