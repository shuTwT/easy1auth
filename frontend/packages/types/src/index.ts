export interface SocialSourceSummary {
  id: string
  type: string
  name: string
}

export type LoginMethod = 'password' | 'email' | 'social'

export interface LoginStyleBackground {
  mode: 'solid' | 'image'
  color: string
  imageUrl?: string | null
  overlayColor?: string | null
  overlayOpacity?: number
}

export interface LoginStyleCard {
  width: number
  radius: number
  shadow: boolean
  position: 'left' | 'center' | 'right'
}

export interface LoginStyleConfig {
  schemaVersion: number
  global: {
    title: string
    subtitle: string
    logoUrl?: string | null
    logoDarkUrl?: string | null
    background: LoginStyleBackground
    primaryColor: string
    language: 'zh-CN'
    card: LoginStyleCard
    customCss?: string | null
  }
  standard: {
    enabled: boolean
    methods: LoginMethod[]
    registrationEnabled: boolean
    termsRequired: boolean
  }
  qr: {
    enabled: boolean
    title?: string
    subtitle?: string
    iconUrl?: string | null
  }
}

export interface LoginStyleLegalDocuments {
  termsOfService?: string | null
  privacyPolicy?: string | null
}

export interface PublicLoginStyle {
  logo?: string | null
  logoDark?: string | null
  backgroundImage?: string | null
  backgroundColor: string
  primaryColor: string
  title: string
  subtitle: string
  customCss?: string | null
  loginMethods: string[]
  socialProviders: string[]
  socialSources?: SocialSourceSummary[]
  registrationEnabled?: boolean
  config?: LoginStyleConfig
  termsOfService?: string | null
  privacyPolicy?: string | null
}

export interface AuthInteractionContext {
  status: 'login' | 'mfa' | 'consent' | 'error' | 'expired'
  tenantId?: string
  csrfToken?: string
  style?: PublicLoginStyle
  clientName?: string
  scopes?: string[]
  client?: { clientId: string; name?: string; scopes: string[] }
  message?: string
}
