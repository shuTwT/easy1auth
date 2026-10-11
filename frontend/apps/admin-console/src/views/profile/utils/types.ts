export interface ProfileForm {
  username: string
  phone: string
}

export interface EmailChangeForm {
  email: string
  code: string
}

export type EmailChangeStep = 'email' | 'code'

export interface EmailChangeDialogExpose {
  open: () => void
}
