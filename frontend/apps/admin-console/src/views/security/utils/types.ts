export interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface PasswordStrength {
  level: number
  text: string
  color: string
}

export interface PasswordRequirement {
  key: string
  label: string
  met: boolean
}
