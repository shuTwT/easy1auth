export interface AuthorizationCodeDto {
  client_id: string
  redirect_uri: string
  response_type: string
  scope?: string
  state?: string
  code_challenge?: string
  code_challenge_method?: string
}

export interface TokenRequestDto {
  grant_type: string
  code?: string
  redirect_uri?: string
  client_id?: string
  client_secret?: string
  refresh_token?: string
  code_verifier?: string
  scope?: string
}

export interface TokenResponse {
  access_token: string
  token_type: string
  expires_in: number
  refresh_token?: string
  scope?: string
  id_token?: string
}

export interface AuthorizationCode {
  code: string
  clientId: string
  redirectUri: string
  userId: string
  tenantId: string
  scope?: string
  expiresAt: Date
  codeChallenge?: string
  codeChallengeMethod?: string
}

export interface AccessToken {
  token: string
  clientId: string
  userId: string
  tenantId: string
  scope?: string
  expiresAt: Date
}

export interface RefreshToken {
  token: string
  clientId: string
  userId: string
  tenantId: string
  scope?: string
  expiresAt: Date
}

export interface UserInfo {
  sub: string
  name: string
  email: string
  email_verified: boolean
  phone?: string
  phone_verified: boolean
  avatar?: string
  department?: string
  position?: string
  tenant_id: string
  tenant_name: string
  roles?: string[]
  groups?: string[]
}

export interface OAuth2Error {
  error: string
  error_description: string
}

export type GrantType = 'authorization_code' | 'client_credentials' | 'refresh_token'
export type ResponseType = 'code' | 'token'
