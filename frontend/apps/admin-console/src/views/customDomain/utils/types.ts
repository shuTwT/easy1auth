export interface SelectOption<T extends string = string> {
  value: T
  label: string
}

export interface DomainCreateForm {
  domain: string
  verificationMethod: 'dns' | 'file'
}

export interface DomainCreateDialogExpose {
  open: () => void
}
