export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface SsoStats {
  totalApps: number
  activeApps: number
  webApps: number
  spaApps: number
  nativeApps: number
  machineApps: number
}

export type GuideTab = 'oauth2' | 'pkce' | 'client'
