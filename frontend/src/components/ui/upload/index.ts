export { default as Upload } from './Upload.vue'

export interface UploadFile {
  name: string
  size: number
  type: string
  raw: File
  url?: string
  status?: 'ready' | 'uploading' | 'success' | 'fail'
}
