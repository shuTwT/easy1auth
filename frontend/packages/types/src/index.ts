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
